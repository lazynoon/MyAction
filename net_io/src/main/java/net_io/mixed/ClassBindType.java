package net_io.mixed;

import net_io.utils.EncodeUtils;
import net_io.utils.Mixed;
import net_io.utils.NetLog;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.HashMap;
import java.util.LinkedHashMap;

public class ClassBindType implements Mixed.TYPE {
	private static final String javaLangPackage = "java.lang.";
	private static final String jsonFieldCassName = JsonField.class.getName();
	protected long cacheTime = System.currentTimeMillis();
	protected Constructor setterConstructor = null;
	protected String writeBindName = null;
	protected String readBindName = null;
	protected LinkedHashMap<String, ClassBindElement> setterBindMap = new LinkedHashMap<String, ClassBindElement>();
	protected LinkedHashMap<String, ClassBindElement> getterBindMap = new LinkedHashMap<String, ClassBindElement>();

	public ClassBindType(Class cls) {
		loadConstruct(cls);
		loadFields(cls);
		loadMethods(cls);
	}

	private void loadConstruct(Class<?> cls) {
		boolean hasJsonFieldDeclare = false;
		for (Constructor constructor : cls.getConstructors()) {
			int modifiers = constructor.getModifiers();
			JsonField jsonField = null;
			for (Annotation annotation : constructor.getDeclaredAnnotations()) {
				if (JsonField.class.isInstance(annotation)) {
					if (!Modifier.isPublic(modifiers)) {
						constructor.setAccessible(true);
					}
					jsonField = (JsonField) annotation;
					break;
				}
			}
			if (jsonField != null) {
				if (hasJsonFieldDeclare) {
					NetLog.logWarn("[ClassCache] duplicate declare construct. Class: " + cls.getName());
					break;
				}
			} else if (hasJsonFieldDeclare) {
				continue;
			}
			if (!Modifier.isPublic(modifiers) && !constructor.isAccessible()) {
				continue;
			}
			//@since 1.8 getParameterCount()
			Class<?>[] parameterTypes = constructor.getParameterTypes();
			if (parameterTypes != null && parameterTypes.length > 0) {
				if (jsonField != null) {
					NetLog.logWarn("[ClassCache] declare JsonField on not empty parameter constructor," +
							" Class: " + cls.getName());
				}
				continue;
			}
			this.setterConstructor = constructor;
			boolean hasName = false;
			if (jsonField != null) {
				String jsonFieldName = jsonField.name();
				if (jsonFieldName != null) {
					jsonFieldName = jsonFieldName.trim();
					if (jsonFieldName.length() > 0) {
						this.writeBindName = jsonFieldName;
						this.readBindName = jsonFieldName;
						hasName = true;
					}
				}
				hasJsonFieldDeclare = true;
			}
			if (!hasName) {
				String className = cls.getSimpleName();
				this.writeBindName = toFieldKey(className);
				char firstChar = className.charAt(0);
				if (firstChar >= 'A' && firstChar <= 'Z') {
					this.readBindName = String.valueOf((char)(firstChar + 32)) + className.substring(1);
				} else {
					this.readBindName = className;
				}
			}
		}
	}

	private void loadFields(Class cls) {
		for (Field field : cls.getDeclaredFields()) {
			Class declareClass = field.getDeclaringClass();
			if (declareClass == null || declareClass.getName().startsWith(javaLangPackage)) {
				continue;
			}
			int modifiers = field.getModifiers();
			JsonField jsonField = null;
			for (Annotation annotation : field.getAnnotations()) {
				if (JsonField.class.isInstance(annotation)) {
					jsonField = (JsonField) annotation;
					break;
				}
			}
			if (jsonField != null) {
				if (Modifier.isStatic(modifiers)) {
					String msg = "Can not bind static field, Field: " + cls.getName() + "." + field.getName();
					NetLog.logWarn("[ClassBindType] " + msg);
					continue;
				}
				if (!Modifier.isPublic(modifiers)) {
					field.setAccessible(true);
				}
			} else if (!Modifier.isPublic(modifiers) || Modifier.isStatic(modifiers) || Modifier.isFinal(modifiers)) {
				continue;
			}
			boolean disableWrite = false;
			boolean disableRead = false;
			String writeFieldName = null;
			String readFieldName = null;
			if (jsonField != null) {
				if (jsonField.access() == JsonField.Access.READ_ONLY) {
					disableWrite = true;
				} else if (jsonField.access() == JsonField.Access.WRITE_ONLY) {
					disableRead = true;
				}
				if (!disableWrite && Modifier.isFinal(modifiers)) {
					String msg = "Can not bind final field for write, Field: " + cls.getName() + "." + field.getName();
					NetLog.logWarn("[ClassBindType] " + msg);
					disableWrite = true;
				}
				if (!disableWrite) {
					writeFieldName = jsonField.name();
				}
				if (!disableRead) {
					readFieldName = jsonField.name();
				}
			}
			if (!disableRead) {
				String sourceName = null;
				if (readFieldName == null || readFieldName.length() == 0) {
					readFieldName = field.getName();
				} else {
					sourceName = field.getName();
				}
				String readFieldKey = toFieldKey(readFieldName);
				ClassBindElement readElement = getterBindMap.get(readFieldKey);
				if (readElement != null) {
					readElement = getterBindMap.get(readFieldKey);
				} else {
					readElement = new ClassBindElement();
					getterBindMap.put(readFieldKey, readElement);
				}
				if (readElement.jsonField != null) {
					if (jsonField != null) {
						NetLog.logWarn("[ClassBindType] jsonField is duplicate. Field: "
								+ cls.getName() + "." + field.getName());
					}
				} else {
					readElement.jsonField = jsonField;
					readElement.field = field;
					readElement.bindName = readFieldName;
					readElement.sourceName = sourceName;
				}
			}
			if (!disableWrite) {
				String sourceName = null;
				if (writeFieldName == null || writeFieldName.length() == 0) {
					writeFieldName = field.getName();
				} else {
					sourceName = field.getName();
				}
				String writeFieldKey = toFieldKey(writeFieldName);
				ClassBindElement writeElement = setterBindMap.get(writeFieldKey);
				if (writeElement != null) {
					writeElement = setterBindMap.get(writeFieldKey);
				} else {
					writeElement = new ClassBindElement();
					setterBindMap.put(writeFieldKey, writeElement);
				}
				if (writeElement.jsonField != null) {
					if (jsonField != null) {
						NetLog.logWarn("[ClassBindType] jsonField is duplicate. Field: "
								+ cls.getName() + "." + field.getName());
					}
				} else {
					writeElement.jsonField = jsonField;
					writeElement.field = field;
					writeElement.bindName = writeFieldName;
					writeElement.sourceName = sourceName;
					writeElement.setterType = new ClassBindElement.SetterParameterType(field.getGenericType());
				}
			}
		}
	}

	private void loadMethods(Class cls) {
		//JsonField已定义，忽略 get 方法
		HashMap<String, Boolean> ignoreReadNames = new HashMap<String, Boolean>();
		for (String key : getterBindMap.keySet()) {
			ClassBindElement element = getterBindMap.get(key);
			if (element.sourceName != null) {
				ignoreReadNames.put(element.sourceName, true);
			}
		}
		//JsonField已定义，忽略 set 方法
		HashMap<String, Boolean> ignoreWriteNames = new HashMap<String, Boolean>();
		for (String key : setterBindMap.keySet()) {
			ClassBindElement element = setterBindMap.get(key);
			if (element.sourceName != null) {
				ignoreWriteNames.put(element.sourceName, true);
			}
		}
		for (Method method : cls.getDeclaredMethods()) {
			Class<?> declareClass = method.getDeclaringClass();
			if (declareClass == null || declareClass.getName().startsWith(javaLangPackage)) {
				continue;
			}
			int modifiers = method.getModifiers();
			JsonField jsonField = null;
			for (Annotation annotation : method.getAnnotations()) {
				if (JsonField.class.isInstance(annotation)) {
					jsonField = (JsonField) annotation;
					break;
				}
			}
			boolean disableWrite = false;
			boolean disableRead = false;
			String fieldName = method.getName();
			Class<?> returnType = method.getReturnType();
			if (returnType == null) {
				continue;
			}
			Class<?>[] parameterTypes = method.getParameterTypes();
			if (fieldName.length() > 3) {
				if (fieldName.startsWith("get")) {
					disableWrite = true;
					if ("void".equals(returnType.getName()) || parameterTypes.length > 0) {
						disableRead = true;
					} else {
						fieldName = EncodeUtils.stringLowerFirst(fieldName.substring(3));
					}
				} else if (fieldName.startsWith("set")) {
					disableRead = true;
					if (!"void".equals(returnType.getName()) || parameterTypes.length == 0) {
						disableWrite = true;
					} else {
						fieldName = EncodeUtils.stringLowerFirst(fieldName.substring(3));
					}
				} else {
					disableWrite = disableRead = true;
				}
			} else {
				disableWrite = disableRead = true;
			}
			if (jsonField != null) {
				if (Modifier.isStatic(modifiers)) {
					String msg = "Can not bind static method, Method: " + cls.getName() + "." + method.getName();
					NetLog.logWarn("[ClassBindType] " + msg);
					continue;
				}
				if (Modifier.isFinal(modifiers)) {
					String msg = "Can not bind final method, Method: " + cls.getName() + "." + method.getName();
					NetLog.logWarn("[ClassBindType] " + msg);
					continue;
				}
				if (!Modifier.isPublic(modifiers)) {
					method.setAccessible(true);
				}
			} else if (!Modifier.isPublic(modifiers) || Modifier.isStatic(modifiers) || Modifier.isFinal(modifiers)) {
				continue;
			} else {
				if (ignoreReadNames.containsKey(fieldName)) {
					disableRead = true;
				}
				if (ignoreWriteNames.containsKey(fieldName)) {
					disableWrite = true;
				}
			}
			String writeFieldName = null;
			String readFieldName = null;
			if (jsonField != null) {
				if (jsonField.access() == JsonField.Access.READ_ONLY) {
					disableWrite = true;
					if (disableRead) {
						String msg = "JsonField declare read, but method is not Getter. Method: " + method.getName();
						NetLog.logWarn("[ClassBindType] " + msg);
					}
				} else if (jsonField.access() == JsonField.Access.WRITE_ONLY) {
					disableRead = true;
					if (disableWrite) {
						String msg = "JsonField declare write, but method is not Setter. Method: " + method.getName();
						NetLog.logWarn("[ClassBindType] " + msg);
					}
				}
				if (!disableWrite) {
					writeFieldName = jsonField.name();
				}
				if (!disableRead) {
					readFieldName = jsonField.name();
				}
			}
			if (!disableRead) {
				String sourceName = null;
				if (readFieldName == null || readFieldName.length() == 0) {
					readFieldName = fieldName;
				} else {
					sourceName = fieldName;
				}
				String readFieldKey = toFieldKey(readFieldName);
				ClassBindElement readElement = getterBindMap.get(readFieldKey);
				if (readElement != null) {
					readElement = getterBindMap.get(readFieldKey);
				} else {
					readElement = new ClassBindElement();
					getterBindMap.put(readFieldKey, readElement);
				}
				if (readElement.jsonField != null) {
					if (jsonField != null) {
						NetLog.logWarn("[ClassBindType] jsonField is duplicate. Field: "
								+ cls.getName() + "." + method.getName());
					}
				} else {
					readElement.jsonField = jsonField;
					readElement.method = method;
					readElement.bindName = readFieldName;
					readElement.sourceName = sourceName;
				}
			}
			if (!disableWrite) {
				String sourceName = null;
				if (writeFieldName == null || writeFieldName.length() == 0) {
					writeFieldName = fieldName;
				} else {
					sourceName = fieldName;
				}
				String writeFieldKey = toFieldKey(writeFieldName);
				ClassBindElement writeElement = setterBindMap.get(writeFieldKey);
				if (writeElement != null) {
					writeElement = setterBindMap.get(writeFieldKey);
				} else {
					writeElement = new ClassBindElement();
					setterBindMap.put(writeFieldKey, writeElement);
				}
				if (writeElement.jsonField != null) {
					if (jsonField != null) {
						NetLog.logWarn("[ClassBindType] jsonField is duplicate. Field: "
								+ cls.getName() + "." + method.getName());
					}
				} else {
					writeElement.jsonField = jsonField;
					writeElement.method = method;
					writeElement.bindName = writeFieldName;
					writeElement.sourceName = sourceName;
					Type[] genericParameterTypes = method.getGenericParameterTypes();
					if (genericParameterTypes == null || genericParameterTypes.length != 1) {
						throw new IllegalArgumentException("[ClassBindType] Unexpected exception with setter parameter.");
					}
					writeElement.setterType = new ClassBindElement.SetterParameterType(genericParameterTypes[0]);
				}
			}
		}
	}

	/**
	 * 下划线连续2个及以上，不转换
	 * 首个下划线不转换
	 * @param name
	 * @return
	 */
	protected static String toFieldKey(String name) {
		if (name == null || name.length() == 0) {
			return name;
		}
		int prevUnderscoreFlag = 1;
		int length = name.length();
		StringBuilder builder = new StringBuilder();
		for (int i=0; i<length; i++) {
			char ch = name.charAt(i);
			if (ch == '-') { //下划线1
				if (prevUnderscoreFlag == 0) {
					if (i == 0) {
						builder.append('-');
						prevUnderscoreFlag = 2;
					} else {
						prevUnderscoreFlag = 1;
					}
				} else if (prevUnderscoreFlag == 1) {
					builder.append('-');
					builder.append('-');
					prevUnderscoreFlag = 2;
				} else {
					builder.append('-');
				}
			} else {
				if (prevUnderscoreFlag != 0) {
					prevUnderscoreFlag = 0;
				}
				if (ch >= 'A' && ch <='Z') { //大写字母
					builder.append((char)(ch + 32));
				} else {
					builder.append(ch);
				}
			}
		}
		return builder.toString();
	}

	@Override
	public Mixed toMixed() {
		Mixed info = new Mixed();
		LinkedHashMap<String, ClassBindElement> bothBindMap = new LinkedHashMap<String, ClassBindElement>();
		LinkedHashMap<String, ClassBindElement> onlyWriteMap = new LinkedHashMap<String, ClassBindElement>();
		LinkedHashMap<String, ClassBindElement> onlyReadMap = new LinkedHashMap<String, ClassBindElement>();
		if (setterBindMap != null && getterBindMap != null) {
			for (String bindKey : setterBindMap.keySet()) {
				ClassBindElement writeElement = setterBindMap.get(bindKey);
				ClassBindElement readElement = getterBindMap.get(bindKey);
				if (readElement == null || !writeElement.equals(readElement)) {
					onlyWriteMap.put(bindKey, writeElement);
					continue;
				}
				bothBindMap.put(bindKey, writeElement);
			}
			for (String bindKey : getterBindMap.keySet()) {
				if (!bothBindMap.containsKey(bindKey)) {
					onlyReadMap.put(bindKey, getterBindMap.get(bindKey));
				}
			}
		} else if (setterBindMap != null) {
			onlyWriteMap = setterBindMap;
		} else if (getterBindMap != null) {
			onlyReadMap = getterBindMap;
		}
		if (readBindName != null) {
			info.put("read_bind_name", readBindName);
		}
		if (writeBindName != null) {
			info.put("write_bind_name", writeBindName);
		}
		if (bothBindMap.size() > 0) {
			info.put("both_bind_map", bothBindMap);
		}
		if (onlyReadMap.size() > 0) {
			info.put("read_bind_map", onlyReadMap);
		}
		if (onlyWriteMap.size() > 0) {
			info.put("write_bind_map", onlyWriteMap);
		}
		return info;
	}

	@Override
	public String toString() {
		return toMixed().toString();
	}
}
