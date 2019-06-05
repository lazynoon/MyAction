package myaction.extend;

import net_io.utils.MixedUtils;

public class ActionPackage {
	// 模块名
	private String moduleName;
	// 包名
	private String pkgName = null;
	// 前置路径
	private String prefixPath = null;
	
	// 每一个action模块对应一个ActionPackage
	public ActionPackage(String moduleName) {
		this.moduleName = moduleName;
	}
	
	// 非null非空字符串检测
	public boolean isValid() {
		return ! MixedUtils.isEmpty(pkgName);
	}

	public String getModuleName() {
		return moduleName;
	}

	public String getPkgName() {
		return pkgName;
	}

	/**
	 * 前置路径,以 / 结尾
	 * @return
	 */
	public String getPrefixPath() {
		return prefixPath;
	}

	public void setPkgName(String pkgName) {
		this.pkgName = pkgName;
	}

	public void setPrefixPath(String prefixPath) {
		if(prefixPath != null) {
			prefixPath = prefixPath.trim();
			if(prefixPath.length() == 0) {
				prefixPath = null;
			} else if(prefixPath.endsWith("/") == false) {
				prefixPath += "/";	
			}
		}
		this.prefixPath = prefixPath;
	}
	
}
