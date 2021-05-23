package net_io.mixed;

import net_io.utils.Mixed;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

public class ClassBindElement implements Mixed.TYPE{
	/** 绑定名称 **/
	protected String bindName = null;
	/** 来源名称（属性名称或方法名，仅当绑定名称与来源名称不同时，来源名称非空） **/
	protected String sourceName = null;
	protected Field field = null;
	protected Method method = null;
	protected JsonField jsonField = null;
	protected SetterParameterType setterType = null;

	@Override
	public int hashCode() {
		int code = 0;
		boolean hasCode = false;
		if (bindName != null) {
			code ^= bindName.hashCode();
			hasCode = true;
		}
		if (sourceName != null) {
			code ^= sourceName.hashCode();
		}
		if (field != null) {
			code ^= field.hashCode();
			hasCode = true;
		}
		if (method != null) {
			code ^= method.hashCode();
			hasCode = true;
		}
		if (!hasCode) {
			code = super.hashCode();
		}
		return code;
	}

	@Override
	public boolean equals(Object another) {
		if (another == null) {
			return false;
		}
		if (another instanceof ClassBindElement == false) {
			return false;
		}
		ClassBindElement that = (ClassBindElement) another;
		if (this.bindName != null) {
			if (!this.bindName.equals(that.bindName)) {
				return false;
			}
		} else if (that.bindName != null) {
			return false;
		}
		if (this.sourceName != null) {
			if (!this.sourceName.equals(that.sourceName)) {
				return false;
			}
		} else if (that.sourceName != null) {
			return false;
		}
		if (this.field != that.field) {
			return false;
		}
		if (this.method != that.method) {
			return false;
		}
		if (this.jsonField != that.jsonField) {
			return false;
		}
		return true;
	}

	@Override
	public Mixed toMixed() {
		Mixed info = new Mixed();
		info.put("bind_name", bindName);
		if (method != null) {
			info.put("method_name", method.getName());
		} else if (field != null) {
			info.put("field_name", field.getName());
		}
		if (jsonField != null) {
			Mixed jsonFieldInfo = new Mixed();
			jsonFieldInfo.put("name", jsonField.name());
			jsonFieldInfo.put("required", jsonField.required());
			jsonFieldInfo.put("access", jsonField.access());
			info.put("json_field", jsonFieldInfo);
		}
		if (setterType != null) {
			info.put("setter_type", setterType);
		}
		return info;
	}

	@Override
	public String toString() {
		return toMixed().toString();
	}

	public static class SetterParameterType implements Mixed.TYPE {
		protected Class<?> clazz;
		protected Class<?> genericsKey = null;
		protected Class<?> genericsValue = null;

		public SetterParameterType(Type parameterType) {
			if (parameterType instanceof ParameterizedType) {
				ParameterizedType genericsType = (ParameterizedType) parameterType;
				Type[] actualTypeArguments = genericsType.getActualTypeArguments();
				clazz = (Class<?>) genericsType.getRawType();
				if (actualTypeArguments != null && actualTypeArguments.length > 0) {
					if (actualTypeArguments.length > 1) {
						if (clazz.isAssignableFrom(Map.class)) {
							genericsKey = (Class<?>) actualTypeArguments[0];
							genericsValue = (Class<?>) actualTypeArguments[1];
						}
					} else {
						if (clazz.isAssignableFrom(List.class)) {
							genericsValue = (Class<?>) actualTypeArguments[0];
						}
					}
				}
			} else {
				clazz = (Class<?>) parameterType;
			}
		}

		@Override
		public Mixed toMixed() {
			Mixed info = new Mixed();
			info.put("class_name", clazz.getName());
			if (genericsKey != null) {
				info.put("generics_key", genericsKey.getName());
			}
			if (genericsValue != null) {
				info.put("generics_value", genericsValue.getName());
			}
			return info;
		}
	}

}
