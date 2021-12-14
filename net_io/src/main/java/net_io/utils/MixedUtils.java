package net_io.utils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

public class MixedUtils {
	public static String trim(String str) {
		if(str == null) return "";
		return str.trim();
	}
	
	public static boolean isEmpty(Object obj) {
		if(obj == null) return true;
		if(obj instanceof String && ((String)obj).length() == 0) return true;
		return false;
	}
	
	/**
	 * 解析一个短整数
	 * @param str
	 * @return short
	 */
	public static short parseShort(String str) {
		if(str == null || str.length() == 0  || str.length() > 6) return 0;
		for(int i=0; i<str.length(); i++) {
			char ch = str.charAt(i);
			if(ch != '-' && (ch < '0' || ch > '9')) return 0;
		}
		return Short.parseShort(str);
	}

	/**
	 * 解析一个整数
	 * @param str
	 * @return intval
	 */
	public static int parseInt(String str) {
		if(str == null || str.length() == 0  || str.length() > 11) return 0;
		for(int i=0; i<str.length(); i++) {
			char ch = str.charAt(i);
			if(ch != '-' && (ch < '0' || ch > '9')) return 0;
		}
		return Integer.parseInt(str);
	}

	/**
	 * 解析一个长整数
	 * @param str
	 * @return longval
	 */
	public static long parseLong(String str) {
		if(str == null || str.length() == 0  || str.length() > 21) return 0;
		for(int i=0; i<str.length(); i++) {
			char ch = str.charAt(i);
			if(ch != '-' && (ch < '0' || ch > '9')) return 0;
		}
		return Long.parseLong(str);
	}

	/**
	 * 解析一个双精度数
	 * @param str
	 * @return doubleval
	 */
	public static double parseDouble(String str) {
		if(isNumeric(str) == false) return 0;
		return Double.parseDouble(str);
	}
	
	/**
	 * 解析一个单精度数
	 * @param str
	 * @return floatval
	 */
	public static float parseFloat(String str) {
		if(isNumeric(str) == false) return 0;
		return Float.parseFloat(str);
	}

	/**
	 * 是否数字类型
	 * @param str
	 * @return boolean
	 */
	public static boolean isNumeric(String str) {
		if(str == null) {
			return false;
		}
		int len = str.length();
		if(len == 0 || len >= 38) {
			return false;
		}
		int pointPos = -1;
		int ePos = -1;
		for(int i=0; i<len; i++) {
			char ch = str.charAt(i);
			if(ch >= '0' && ch <= '9') {
				continue;
			}
			if(ch == '-') {
				if(i != 0) {
					return false; //负数符号，只能出现在第一个
				}
			} else if(ch == '.') {
				if(i == 0 || pointPos >= 0) {
					return false; //小数点，不能出现在第一个，也不能重复
				}
				pointPos = i;
			} else if(ch == 'E') {
				if(i == 0 || ePos >= 0) {
					return false; //科学计数符号，不能出现在第一个，也不能重复
				}
				ePos = i;
			} else {
				return false;
			}
		}
		if(pointPos == len-1) return false; //"."出现在第一位，或者是末尾都是不对的
		if(ePos == len-1) return false; //"E"出现在第一位，或者是末尾都是不对的
		return true;
	}

	/**
	 * 严格模式检查，是否为 int 类型字符串
	 * @param str 数字字符串
	 * @return true为int类型（可安全转换为Integer对象），false非int类型
	 */
	public static boolean isInt(String str) {
		try {
			Integer.parseInt(str);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	/**
	 * 严格模式检查，是否为 long 类型字符串
	 * @param str 数字字符串
	 * @return true为long类型（可安全转换为Long对象），false非long类型
	 */
	public static boolean isLong(String str) {
		try {
			Long.parseLong(str);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	/**
	 * 严格模式检查，是否为 float 类型字符串
	 * @param str 数字字符串
	 * @return true为float类型（可安全转换为Float对象），false非float类型
	 */
	public static boolean isFloat(String str) {
		try {
			Float.parseFloat(str);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	/**
	 * 严格模式检查，是否为 double 类型字符串
	 * @param str 数字字符串
	 * @return true为double类型（可安全转换为Double对象），false非double类型
	 */
	public static boolean isDouble(String str) {
		try {
			Double.parseDouble(str);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	public static String formatSimpleXml(Mixed data) throws ParserConfigurationException, TransformerException {
		return formatSimpleXml(data, null);
	}

	public static String formatSimpleXml(Mixed data, String rootNodeName) throws ParserConfigurationException, TransformerException {
		Document doc = JSONUtils.toDOM(data, rootNodeName);
		TransformerFactory ft = TransformerFactory.newInstance();
		Transformer transformer = ft.newTransformer();
		transformer.setOutputProperty("encoding", "UTF-8");
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		transformer.transform(new DOMSource(doc), new StreamResult(bos));
		return bos.toString();
	}

	public static Mixed parseSimpleXml(String content) throws ParserConfigurationException, SAXException {
		return parseSimpleXml(new ByteArrayInputStream(content.getBytes(EncodeUtils.Charsets.UTF_8)));
	}

	public static Mixed parseSimpleXml(InputStream content) throws ParserConfigurationException, SAXException {
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			Document doc = dbf.newDocumentBuilder().parse(content);
			content.close();
			return parseSimpleXml(doc);
		} catch (IOException e) {
			throw new ParserConfigurationException("[IOException] " + e.getMessage());
		}
	}

	public static Mixed parseSimpleXml(Document doc) throws ParserConfigurationException {
		Element rootElement = doc.getDocumentElement();
		return parseSimpleXmlChildren(rootElement, false);
	}

	private static Mixed parseSimpleXmlChildren(Node node, boolean parentSingle) {
		Mixed data = new Mixed();
		NodeList childNodes = node.getChildNodes();
		int length = childNodes.getLength();
		HashMap<String, Integer> childNameCount = new HashMap<String, Integer>();
		for (int i=0; i<length; i++) {
			Node child = childNodes.item(i);
			Integer count = childNameCount.get(child.getNodeName());
			if (count == null) {
				childNameCount.put(child.getNodeName(), 1);
			} else {
				childNameCount.put(child.getNodeName(), count.intValue() + 1);
			}
		}
		boolean selfSingle = length == 1;
		for (int i=0; i<length; i++) {
			Node child = childNodes.item(i);
			int nodeType = child.getNodeType();
//			if (nodeType != 1 && nodeType != 3) {
//				System.out.println("nodeType: "+nodeType);
//			}
			if (nodeType == Node.TEXT_NODE) {
				continue;
			}
			String nodeName = child.getNodeName();
			//唯一文本提升一级
			if (child.hasChildNodes()) {
				NodeList childChildren = child.getChildNodes();
				if (childChildren.getLength() == 1
						&& childChildren.item(0).getNodeType() == Node.TEXT_NODE) {
					child = childChildren.item(0);
				}
			}
			int nameCount = childNameCount.get(nodeName).intValue();
			if (nameCount > 1 || (parentSingle && selfSingle)) {
				Mixed list;
				if (data.containsKey(nodeName)) {
					list = data.get(nodeName);
				} else {
					list = new Mixed();
					data.put(nodeName, list);
				}
				if (child.hasChildNodes()) {
					list.add(parseSimpleXmlChildren(child, selfSingle));
				} else {
					String nodeValue = child.getNodeValue();
					list.add(nodeValue);
				}
			} else {
				if (child.hasChildNodes()) {
					Node firstNode = child.getFirstChild();
					if (firstNode != null
							&& firstNode.getNodeType() == Node.CDATA_SECTION_NODE
							&& child.getChildNodes().getLength() == 1) {
						data.put(nodeName, firstNode.getNodeValue()); //CDATA内容
					} else {
						data.put(nodeName, parseSimpleXmlChildren(child, selfSingle));
					}
				} else {
					String nodeValue = child.getNodeValue();
					if ("true".equals(nodeValue)) {
						data.put(nodeName, true);
					} else if ("false".equals(nodeValue)) {
						data.put(nodeName, false);
					} else if (MixedUtils.isNumeric(nodeValue) && MixedUtils.isInt(nodeValue)) {
						data.put(nodeName, Integer.parseInt(nodeValue));
					} else {
						data.put(nodeName, nodeValue);
					}
				}
			}
		}
		return data;

	}




}
