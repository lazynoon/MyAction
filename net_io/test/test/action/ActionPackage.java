package test.action;

import net_io.utils.MixedUtils;

public class ActionPackage {
	private String moduleName;
	private String pkgName = null;
	private String prefixPath = null;
	
	public ActionPackage(String moduleName) {
		this.moduleName = moduleName;
	}
	
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
