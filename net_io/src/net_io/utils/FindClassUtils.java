package net_io.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

public class FindClassUtils {
	public static void main(String[] args) {
		// String ss="cn.yyzx.test.TestC";
		// try {
		// Thread a=(Thread) Class.forName(ss).newInstance();
		// a.start();
		// } catch (Exception e) {
		// e.printStackTrace();
		// }
		List<String> cls = getClassInPackage("myaction");
		for (String s : cls) {
			System.out.println("class11:"+s);
		}
	}

	public static List<String> getClassInPackage(String pkgName) {
		List<String> ret = new ArrayList<String>();
		try {
			for(File classPath : CLASS_PATH_ARRAY) {
				if(!classPath.exists())
					continue;
				for(String className : _getClassInPackage(pkgName, classPath)) {
					ret.add(className);
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		return ret;
		
	}
 
	public static List<String> getClassInPackage(String pkgName, String classPath) {
		try {
			return _getClassInPackage(pkgName, new File(classPath));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	private static List<String> _getClassInPackage(String pkgName, File classPath) throws IOException {
		List<String> ret = new ArrayList<String>();
		String searchPath = pkgName.replace('.', '/') + "/";
		if (!classPath.exists()) {
			return ret;
		}
		if (classPath.isDirectory()) {
			ArrayList<File> dirs = new ArrayList<File>();
			ArrayList<String> pkgNames = new ArrayList<String>(); //package名称。与 dirs 一一对应
			dirs.add(new File(classPath, searchPath));
			pkgNames.add(pkgName);
			for(int i=0; i<dirs.size(); i++) {
				File dir = dirs.get(i);
				if(!dir.exists()) {
					continue;
				}
				for(File file : dir.listFiles()) {
					String filename = file.getName();
					if(filename.startsWith(".")) {
						continue; //ignore hidden file
					}
					if(file.isDirectory()) {
						dirs.add(new File(dir, filename));
						pkgNames.add(pkgNames.get(i)+"."+filename);
					} else if(filename.endsWith(".class")) {
						String clsName = pkgNames.get(i) + "." + filename.substring(0, filename.length() - 6);
						ret.add(clsName);
						if(NetLog.LOG_LEVEL <= NetLog.RECORD_ALL) {
							NetLog.logDebug("[FindClass] class in dir:"+clsName);
						}
					} else {
						//ignore other file
					}
				}
			}
		} else {
			FileInputStream fis = new FileInputStream(classPath);
			JarInputStream jis = new JarInputStream(fis, false);
			JarEntry e = null;
			while ((e = jis.getNextJarEntry()) != null) {
				String filename = e.getName();
				if (filename.startsWith(searchPath) && filename.endsWith(".class")) {
					String clsName = filename.replace('/', '.').substring(0, filename.length() - 6);
					ret.add(clsName);
					if(NetLog.LOG_LEVEL <= NetLog.RECORD_ALL) {
						NetLog.logDebug("[FindClass] class in jar:"+filename);
					}
				}
				jis.closeEntry();
			}
			jis.close();
		}
		return ret;
	}

	private static String[] CLASS_PATH_PROP = { "java.class.path", "java.ext.dirs", "sun.boot.class.path" };

	private static List<File> CLASS_PATH_ARRAY = getClassPath();

	private static List<File> getClassPath() {
		List<File> ret = new ArrayList<File>();
		String delim = ":";
		if (System.getProperty("os.name").indexOf("Windows") != -1)
			delim = ";";
		if(NetLog.LOG_LEVEL <= NetLog.RECORD_ALL) {
			NetLog.logDebug("[FindClass] current OS: "+System.getProperty("os.name"));
		}
		for (String pro : CLASS_PATH_PROP) {
			String[] pathes = System.getProperty(pro).split(delim);
			for (String path : pathes) {
				if(NetLog.LOG_LEVEL <= NetLog.RECORD_ALL) {
					NetLog.logDebug("[FindClass] auto search classpath: "+path);
				}
				ret.add(new File(path));
			}
		}
		return ret;
	}
}
