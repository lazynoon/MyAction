package net_io.mixed;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class ObjectConvertConfig {
	public enum NamingStrategy {
		SnakeCase, //蛇形
		UpperCamelCase, //上驼峰
		LowerCamelCase, //下驼峰
		LowerCase, //全部小写
		UpperCase, //全部大写
	}
	private static final DateFormat defaultDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	DateFormat dateFormat = defaultDateFormat;
	NamingStrategy namingStrategy = NamingStrategy.LowerCamelCase;


	public DateFormat getDateFormat() {
		return dateFormat;
	}

	public void setDateFormat(DateFormat dateFormat) {
		if (namingStrategy == null) {
			throw new IllegalArgumentException("dateFormat is null");
		}
		this.dateFormat = dateFormat;
	}

	public NamingStrategy getNamingStrategy() {
		return namingStrategy;
	}

	public void setNamingStrategy(NamingStrategy namingStrategy) {
		if (namingStrategy == null) {
			throw new IllegalArgumentException("namingStrategy is null");
		}
		this.namingStrategy = namingStrategy;
	}
}
