package myaction.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.RandomAccessFile;


public class FileUtil {
	public static void redirectSystemOutput(File stdoutFile, File stderrFile) throws IOException {
		PrintStream stdout = _createPrintStream(stdoutFile);
		System.setOut(stdout);
		if(stderrFile == null || stdoutFile.equals(stderrFile)) {
			System.setErr(stdout);
		} else {
			System.setErr(_createPrintStream(stderrFile));
		}

	}
	private static PrintStream _createPrintStream(File file) throws IOException {
		String parent = file.getParent();
		if(parent != null) {
			File dir = new File(parent);
			if(dir.exists() == false) {
				dir.mkdirs();
			}
		}
		if(file.exists() == false) {
			file.createNewFile();
		}
		if(file.canWrite() == false || file.canRead() == false) {
			throw new IOException("File Read&Write permit check error: "+file.getAbsolutePath());
		}
		FileOutputStream out = new  FileOutputStream(file, true);
		PrintStream writer = new PrintStream(out);
		return writer;
	}
	
	public static String getContent(String filename) throws IOException {
		return new String(getBinaryContent(filename));
	}
	
	public static byte[] getBinaryContent(String filename) throws IOException {
		File file = new File(filename);
		if(file.exists() == false) {
			throw new IOException("File is not exists: "+filename);
		}
		RandomAccessFile accessFile = new RandomAccessFile(file, "r");
		byte[] bts = null;
		try {
			bts = new byte[(int)accessFile.length()];
			for(int i=0; i<bts.length; i++) {
				bts[i] = accessFile.readByte();
			}
		} finally {
			accessFile.close();
		}
		return bts;
	}

	public static void putContent(String filename, String content) throws IOException {
		putContent(filename, content.getBytes());
	}
	
	public static void putContent(String filename, byte[] bts) throws IOException {
		File file = new File(filename);
		if(file.exists()) {
			file.delete();
		}
		RandomAccessFile accessFile = new RandomAccessFile(file, "rw");
		try {
			for(int i=0; i<bts.length; i++) {
				accessFile.write(bts[i]);
			}
		} finally {
			accessFile.close();
		}
	}

}
