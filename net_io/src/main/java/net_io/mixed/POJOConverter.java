package net_io.mixed;

import myaction.utils.DateUtil;
import net_io.utils.EncodeUtils;
import net_io.utils.JSONException;
import net_io.utils.Mixed;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class POJOConverter {
	private ObjectConvertConfig objectConvertConfig = new ObjectConvertConfig();

	public Mixed toMixed(Object obj) {
		if (obj == null) {
			return new Mixed(Mixed.ENTITY_TYPE.NULL);
		} else if (obj instanceof Mixed) {
			return (Mixed) obj;
		} else {
			return new Mixed(obj, this);
		}
	}

	public String toJSON(Object obj) {
		Mixed data = toMixed(obj);
		return data.toJSON();
	}

	public <T> T toSingleObject(Mixed data, Class<T> typeObject) throws JSONException {
		if (data == null || data.isSelfNull()) {
			return null;
		}
		if (data.type() != Mixed.ENTITY_TYPE.MAP) {
			throw new IllegalArgumentException("not map");
		}
		ClassBindType bindType = ClassBindTypeInstance.getInstance(typeObject);
		T result = null;
		try {
			result = (T) bindType.setterConstructor.newInstance();
		} catch (InstantiationException e) {
			String msg = e.getMessage();
			throw new JSONException("[InstantiationException]" + msg);
		} catch (IllegalAccessException e) {
			String msg = e.getMessage();
			throw new JSONException("[IllegalAccessException]" + msg);
		} catch (InvocationTargetException e) {
			String msg = e.getMessage();
			throw new JSONException("[InvocationTargetException]" + msg);
		}
		for (String key : data.keys()) {
			ClassBindElement bindElement = bindType.setterBindMap.get(key.toLowerCase());
			if (bindElement == null) {
				continue; //未定义的属性
			}
			Mixed item = data.get(key);
			ClassBindElement.SetterParameterType setterType = bindElement.setterType;
			Object value;
			if (isPrimitiveClass(setterType.clazz) ) {
				value = getPrimitiveValue(item, setterType.clazz);
			} else if (setterType.clazz.isArray()) {
				value = toArrayObject(item, setterType.clazz);
			} else if (List.class.isAssignableFrom(setterType.clazz)) {
				value = toListObject(item, setterType.genericsValue);
			} else if (Map.class.isAssignableFrom(setterType.clazz)) {
				value = toMapObject(item, setterType.genericsKey, setterType.genericsValue);
			} else {
				value = toSingleObject(data.get(key), setterType.clazz);
			}
			try {
				if (bindElement.method != null) {
					bindElement.method.invoke(result, value);
				} else if (bindElement.field != null) {
					bindElement.field.set(result, value);
				}
			} catch (IllegalAccessException e) {
				String msg = e.getMessage();
				throw new JSONException("[IllegalAccessException]" + msg);
			} catch (InvocationTargetException e) {
				String msg = e.getMessage();
				throw new JSONException("[InvocationTargetException]" + msg);
			}

		}
		return result;
	}

	public <T> List<T> toListObject(Mixed data, Class<T> typeValue) throws JSONException {
		int size = data.size();
		ArrayList<T> result = new ArrayList<T>();
		for (int i=0; i<size; i++) {
			Mixed item = data.get(i);
			Object value;
			if (isPrimitiveClass(typeValue)) {
				value = getPrimitiveValue(item, typeValue);
			} else if (typeValue.isArray()) {
				value = toArrayObject(item, typeValue);
			} else {
				value = toSingleObject(item, typeValue);
			}
			result.add((T) value);
		}
		return result;
	}

	public <T> T toArrayObject(Mixed data, Class<T> typeValue) throws JSONException {
		int size = data.size();
		Class<?> componentType = typeValue.getComponentType();
		T result = (T) Array.newInstance(componentType, size);
		for (int i=0; i<size; i++) {
			Mixed item = data.get(i);
			Object value;
			if (isPrimitiveClass(componentType)) {
				value = getPrimitiveValue(item, componentType);
			} else if (componentType.isArray()) {
				value = toArrayObject(item, componentType);
			} else {
				value = toSingleObject(item, componentType);
			}
			Array.set(result, i, value);
		}
		return result;
	}

	public <TK, TV> Map<TK,TV> toMapObject(Mixed data, Class<TK> typeKey, Class<TV> typeValue) throws JSONException {
		if (typeKey == null) {
			throw new JSONException("toMapObject typeKey is null");
		}
		if (!isPrimitiveClass(typeKey)) {
			throw new JSONException("toMapObject typeKey must be Primitive Class");
		}
		LinkedHashMap<TK, TV> result = new LinkedHashMap<TK, TV>();
		for (String key : data.keys()) {
			Mixed item = data.get(key);
			Object value;
			if (isPrimitiveClass(typeValue)) {
				value = getPrimitiveValue(item, typeValue);
			} else {
				value = toSingleObject(item, typeValue);
			}
			if (typeKey == String.class) {
				result.put((TK) key, (TV) value);
			} else {
				TK keyValue = (TK) getPrimitiveValue(new Mixed(key), typeKey);
				result.put(keyValue, (TV) value);
			}
		}
		return result;
	}

	public Object _toMixedHashMap(Object obj) {
		if (obj == null) {
			throw new IllegalArgumentException("POJOConverter.toLinkedHashMap parameter obj is null");
		}
		if (obj instanceof Date) {
			DateFormat dateFormat = (DateFormat) objectConvertConfig.dateFormat.clone();
			return dateFormat.format((Date) obj);
		}
		LinkedHashMap<String, Mixed> data = new LinkedHashMap<String, Mixed>();
		ClassBindType bindType = ClassBindTypeInstance.getInstance(obj.getClass());
		try {
			for (String key : bindType.getterBindMap.keySet()) {
				ClassBindElement element = bindType.getterBindMap.get(key);
				String toKey = getKeyForWrite(element.bindName);
				Object toValue = null;
				if (element.method != null) {
					toValue = element.method.invoke(obj);
				} else if (element.field != null) {
					toValue = element.field.get(obj);
				} else {
					throw new RuntimeException("can not find method or field");
				}
				if (toValue == null) {
					continue;
				}
				if (toValue instanceof Mixed) {
					data.put(toKey, (Mixed) toValue);
				} else {
					data.put(toKey, new Mixed(toValue, this));
				}
			}
			return data;
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	public ObjectConvertConfig getObjectConvertConfig() {
		return objectConvertConfig;
	}

	public void setObjectConvertConfig(ObjectConvertConfig objectConvertConfig) {
		if (objectConvertConfig == null) {
			throw new IllegalArgumentException("objectConvertConfig is null");
		}
		this.objectConvertConfig = objectConvertConfig;
	}

	private String getKeyForWrite(String bindName) {
		char firstChar;
		switch (objectConvertConfig.namingStrategy) {
			case SnakeCase:
				bindName = ClassBindTypeInstance.toSnakeName(bindName);
				break;
			case UpperCamelCase:
				bindName = EncodeUtils.stringLowerFirst(bindName);
				break;
			case LowerCamelCase:
				bindName = EncodeUtils.stringUpperFirst(bindName);
				break;
			case LowerCase:
				bindName = bindName.toLowerCase();
				break;
			case UpperCase:
				bindName = bindName.toUpperCase();
				break;
			default:
				throw new IllegalArgumentException("Unsupported NamingStrategy: " + objectConvertConfig.namingStrategy
						+ ", BindName: " + bindName);
		}
		return bindName;
	}

	private static boolean isPrimitiveClass(Class<?> cls) {
		if (cls.isPrimitive()
				|| Number.class.isAssignableFrom(cls)
				|| cls == String.class
				|| cls == Date.class
				|| cls == Boolean.class
				|| cls == Character.class) {
			return true;
		} else {
			return false;
		}
	}

	private Object getPrimitiveValue(Mixed sourceValue, Class<?> valueClass) throws JSONException {
		Object targetValue;
		if (valueClass.isPrimitive()) {
			if (valueClass == int.class) {
				targetValue = sourceValue.toIntValue();
			} else if (valueClass == long.class) {
				targetValue = sourceValue.toLongValue();
			} else if (valueClass == float.class) {
				targetValue = sourceValue.toFloatValue();
			} else if (valueClass == double.class) {
				targetValue = sourceValue.toDoubleValue();
			} else if (valueClass == short.class) {
				targetValue = sourceValue.toShortValue();
			} else if (valueClass == char.class) {
				targetValue = sourceValue.toCharValue();
			} else if (valueClass == byte.class) {
				targetValue = sourceValue.toByteValue();
			} else if (valueClass == boolean.class) {
				targetValue = sourceValue.toBooleanValue();
			} else {
				throw new JSONException("Not support primitive type: " + valueClass.getName());
			}
		} else if (Number.class.isAssignableFrom(valueClass)) {
			if (valueClass == Integer.class) {
				targetValue = sourceValue.toIntValue();
			} else if (valueClass == Long.class) {
				targetValue = sourceValue.toLongValue();
			} else if (valueClass == Float.class) {
				targetValue = sourceValue.toFloatValue();
			} else if (valueClass == Double.class) {
				targetValue = sourceValue.toDoubleValue();
			} else if (valueClass == Short.class) {
				targetValue = sourceValue.toShortValue();
			} else if (valueClass == Byte.class) {
				targetValue = sourceValue.toByteValue();
			} else if (valueClass == BigDecimal.class) {
				String str = sourceValue.toString();
				if (str == null || str.length() == 0) {
					return null;
				}
				targetValue = new BigDecimal(str);
			} else if (valueClass == BigInteger.class) {
				String str = sourceValue.toString();
				if (str == null || str.length() == 0) {
					return null;
				}
				targetValue = new BigInteger(str);
			} else if (valueClass == AtomicInteger.class) {
				targetValue = new AtomicInteger(sourceValue.toIntValue());
			} else if (valueClass == AtomicLong.class) {
				targetValue = new AtomicLong(sourceValue.toLongValue());
			} else {
				throw new JSONException("Not support number class: " + valueClass.getName());
			}
		} else if (valueClass == String.class) {
			targetValue = sourceValue.toString();
		} else if (valueClass == Date.class) {
			String str = sourceValue.toString();
			if (str == null || str.length() == 0) {
				return null;
			}
			try {
				DateFormat dateFormat = (DateFormat) objectConvertConfig.dateFormat.clone();
				targetValue = dateFormat.parse(str);
			} catch (ParseException e) {
				throw new JSONException("ParseException - " + e.getMessage());
			}
		} else if (valueClass == Boolean.class) {
			targetValue = sourceValue.toBooleanValue();
		} else if (valueClass == Character.class) {
			targetValue = sourceValue.toCharValue();
		} else {
			throw new JSONException("Not support primitive class: " + valueClass.getName());
		}
		return targetValue;
	}


}
