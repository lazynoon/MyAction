package myaction.utils;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class StringUtil {
	public static final Charset asciiCharset = Charset.forName("ISO-8859-1");
	public static final Charset utf8Charset = Charset.forName("UTF-8");
	public static final String charset = "UTF-8";
	/** 检查是否含有HTML代码的正则表达式 **/
	private static Pattern hasHtmlCodePattern = null;
	private static final Map<Integer, Integer> fullWidthChars = new HashMap<Integer, Integer>();
	private static final char[] hexChars = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
	private static final String[] EMPTY_STRING_ARRAY = new String[0];
	static {
		fullWidthChars.put((int)'　', (int)' ');
		fullWidthChars.put((int)'，', (int)',');
		fullWidthChars.put((int)'。', (int)'.');
		fullWidthChars.put((int)'‘', (int)'\'');
		fullWidthChars.put((int)'’', (int)'\'');
		fullWidthChars.put((int)'“', (int)'"');
		fullWidthChars.put((int)'”', (int)'"');
		fullWidthChars.put((int)'：', (int)':');
		fullWidthChars.put((int)'；', (int)';');
		fullWidthChars.put((int)'［', (int)'[');
		fullWidthChars.put((int)'］', (int)']');
		fullWidthChars.put((int)'{', (int)'{');
		fullWidthChars.put((int)'}', (int)'}');
		fullWidthChars.put((int)'－', (int)'-');
		fullWidthChars.put((int)'＿', (int)'_');
		fullWidthChars.put((int)'＝', (int)'=');
		fullWidthChars.put((int)'＋', (int)'+');
		for(int i=0; i<10; i++) {
			fullWidthChars.put((int)'０'+i, (int)'0'+i);
		}
		for(int i=0; i<26; i++) {
			fullWidthChars.put((int)'ａ'+i, (int)'a'+i);
		}
		for(int i=0; i<26; i++) {
			fullWidthChars.put((int)'Ａ'+i, (int)'A'+i);
		}
		fullWidthChars.put((int)'！', (int)'!');
		fullWidthChars.put((int)'＠', (int)'@');
		fullWidthChars.put((int)'#', (int)'#');
		fullWidthChars.put((int)'＄', (int)'$');
		fullWidthChars.put((int)'％', (int)'%');
		fullWidthChars.put((int)'＾', (int)'^');
		fullWidthChars.put((int)'＆', (int)'&');
		fullWidthChars.put((int)'＊', (int)'*');
		fullWidthChars.put((int)'（', (int)'(');
		fullWidthChars.put((int)'）', (int)')');
		fullWidthChars.put((int)'／', (int)'/');
		fullWidthChars.put((int)'？', (int)'?');
		fullWidthChars.put((int)'＼', (int)'\\');
		fullWidthChars.put((int)'｜', (int)'|');
	}
	
	/**
	 * 转为半角字符
	 * @param ch
	 * @return 若为转换，原样返回
	 */
	public static char toHalfWidth(char ch) {
		Integer r = fullWidthChars.get((int)ch);
		if(r == null) {
			return ch;
		}
		return (char) r.intValue();
	}

	public static int matchLength(String s) {
		try {
			return s.getBytes(charset).length;
		} catch (UnsupportedEncodingException e) {
			return 0;
		}
	}

	/**
	 * 字符串联合
	 * 
	 * @param seperate
	 * @param list
	 * @return String
	 */
	@SuppressWarnings("rawtypes")
	public static String implode(String seperate, List list) {
		if (list == null) {
			return null;
		}
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < list.size(); i++) {
			if (i > 0) {
				sb.append(seperate);
			}
			sb.append(list.get(i));
		}
		return sb.toString();
	}

	/**
	 * 字符串联合
	 * 
	 * @param seperate
	 * @param arr
	 * @return String
	 */
	public static String implode(String seperate, Object[] arr) {
		if (arr == null) {
			return null;
		}
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < arr.length; i++) {
			if (i > 0) {
				sb.append(seperate);
			}
			sb.append(arr[i]);
		}
		return sb.toString();
	}

	/**
	 * 自动添加前缀字符
	 * 
	 * @param str 源字符串
	 * @param repeatChar 重复的字符串
	 * @param minSize 最少字符数
	 * @return String
	 */
	public static String addPrefix(String str, char repeatChar, int minSize) {
		int len = minSize - str.length();
		if (len <= 0) {
			return str;
		}
		for (int i = 0; i < len; i++) {
			str = repeatChar + str;
		}
		return str;
	}

	/**
	 * 自动添加前缀字符
	 * 
	 * @param num 字符
	 * @param repeatChar 重复的字符串
	 * @param minSize 最少字符数
	 * @return String
	 */
	public static String addPrefix(int num, char repeatChar, int minSize) {
		return addPrefix(String.valueOf(num), repeatChar, minSize);
	}

	/**
	 * round函数对浮点数进行四舍五入
	 * @param num
	 * @param prec
	 * @return 取值后字符串
	 * @throws UnsupportedEncodingException
	 */
	public static String round(double num, int prec) throws UnsupportedEncodingException {
		String str = String.valueOf(num);
		int pos = str.indexOf('.');
		if(pos < 0) {
			return str;
		}
		if(prec >= str.length() - pos - 1) {
			return str;
		}
		byte s1[] = str.substring(0, pos).getBytes("UTF-8");
		byte s2[] = str.substring(pos+1, prec).getBytes("UTF-8");
		byte s3[] = str.substring(prec).getBytes("UTF-8");
		int plus = 0;
		int i;
		for(i=s3.length-1; i>=0; i--) {
			int n = s3[i] - '0' + plus;
			if(n >= 5) {
				plus = 1;
			} else {
				plus = 0;
			}
		}
		if(plus > 0) {
			for(i=s2.length-1; i>=0; i--) {
				int n = s2[i] - '0' + plus;
				if(n >= 10) {
					s2[i] = '0';
					plus = 1;
				} else {
					s2[i] = '0' ;
					s2[i] += (byte)n;
					break;
				}
			}
		}
		if(plus > 0) {
			for(i=s1.length-1; i>=0; i--) {
				int n = s1[i] - '0' + plus;
				if(n >= 10) {
					s1[i] = '0';
					plus = 1;
				} else {
					s1[i] = '0' ;
					s1[i] += (byte)n;
					break;
				}
			}
		}
		str = new String(s1, "UTF-8");
		if(plus > 0) {
			str = "1" + str;
		}
		if(s2.length > 0) {
			str += "." + new String(s2, "UTF-8");
		}
		return str;
	}
	
	/**
	 * 字符串截取（与substring不同的是，预先对str进行检测，避免错误）
	 * @param str 源字符串
	 * @param len 截取的长度
	 * @return 截取后的字符串
	 */
	public static String cutString(String str, int len) {
		if(str == null) {
			return null;
		}
		if(len <= 0) {
			return "";
		}
		if(len >= str.length()) {
			return str;
		}
		return str.substring(0, len);
	}
	
	/**
	 * 将 List<Integer> 转换为 List<String> 
	 * @param list List<Integer>
	 */
	public static List<String> toString(List<Integer> list) {
		if(list == null) {
			return null;
		}
		ArrayList<String> ret = new ArrayList<String>();
		for(Integer i : list) {
			if(i != null) {
				ret.add(i.toString());
			} else {
				ret.add("0");
			}
		}
		return ret;
	}
	
	/**
	 * trim之后，空字符串，返回null
	 */
	public static String emptyToNull(String str) {
		if(str == null) {
			return null;
		}
		str = str.trim();
		if(str.length() == 0) {
			return null;
		}
		return str;
	}

	/**
	 * null之后转换为空字符
	 */
	public static String nullToEmpty(String str) {
		if(str == null) {
			return "";
		}
		return str;
	}
	
	/** 是否含有HTML代码 **/
	public static boolean hasHtmlCode(String str) {
		if(str == null || str.length() == 0) {
			return false;
		}
		if(str.indexOf("<") < 0) {
			return false;
		}
		if(hasHtmlCodePattern == null) {
			hasHtmlCodePattern = Pattern.compile("(?s)<[A-Za-z]+.*>");
		}
		return hasHtmlCodePattern.matcher(str).find();
	}
	
	/**
	 * 是否含有“增补平面”的Unicode字符
	 */
	public static boolean hasUnicodeSMP(String str) {
		if(str == null || str.length() == 0) {
			return false;
		}
		int len = str.length();
		for(int i=0; i<len; i++) {
			char ch = str.charAt(i);
			if(ch >= 0xD800 && ch <= 0xDFFF) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * 将“增补平面”的Unicode字符编码为 “U+Unicode码”格式
	 */
	public static String encodeUnicodeSMP(String str) {
		if(str == null || str.length() == 0) {
			return str;
		}
		StringBuffer buff = new StringBuffer();
		int len = str.length();
		for(int i=0; i<len; i++) {
			char ch = str.charAt(i);
			if(ch < 0xD800 || ch > 0xDFFF) {
				buff.append(ch);
				continue;
			}
			i++;
			char ch2 = 0;
			if(i<len) {
				ch2 = str.charAt(i);
			}
			buff.append(convertCharByUnicodeSMP(ch, ch2));
		}
		return buff.toString();
	}
	
	/** 将两个char表示的字符，转换为编码U+编码（错误的Unicode字符，用#+开头） **/
	private static String convertCharByUnicodeSMP(char ch1, char ch2) {
		StringBuffer buff = new StringBuffer();
		if(ch1 <0xD800 || ch1 > 0xDFFF || ch2 < 0xDC00 || ch2 > 0xDFFF) {
			buff.append("#+");
		} else {
			buff.append("U+");
			ch1 -= 0xD800;
			ch2 -= 0xDC00;
			ch2 |= (ch1 & 0x3F) << 10;
			ch1 >>>= 6;
			ch1++;
		}
		if(ch1 > 0xF) { //错误字符
			if(ch1 > 0xFF) {
				buff.append(hexChars[(ch1 >>> 12) & 0xF]);
				buff.append(hexChars[(ch1 >>> 8) & 0xF]);
			}
			buff.append(hexChars[(ch1 >>> 4) & 0xF]);
		}
		buff.append(hexChars[ch1 & 0xF]);
		buff.append(hexChars[(ch2 >>> 12) & 0xF]);
		buff.append(hexChars[(ch2 >>> 8) & 0xF]);
		buff.append(hexChars[(ch2 >>> 4) & 0xF]);
		buff.append(hexChars[ch2 & 0xF]);
		return buff.toString();
	}
	
    // Splitting
    //-----------------------------------------------------------------------
    /**
     * <p>Splits the provided text into an array, using whitespace as the
     * separator.
     * Whitespace is defined by {@link Character#isWhitespace(char)}.</p>
     *
     * <p>The separator is not included in the returned String array.
     * Adjacent separators are treated as one separator.
     * For more control over the split use the StrTokenizer class.</p>
     *
     * <p>A {@code null} input String returns {@code null}.</p>
     *
     * <pre>
     * StringUtils.split(null)       = null
     * StringUtils.split("")         = []
     * StringUtils.split("abc def")  = ["abc", "def"]
     * StringUtils.split("abc  def") = ["abc", "def"]
     * StringUtils.split(" abc ")    = ["abc"]
     * </pre>
     *
     * @param str  the String to parse, may be null
     * @return an array of parsed Strings, {@code null} if null String input
     */
    public static String[] split(String str) {
        return split(str, null, -1);
    }

    /**
     * <p>Splits the provided text into an array, separator specified.
     * This is an alternative to using StringTokenizer.</p>
     *
     * <p>The separator is not included in the returned String array.
     * Adjacent separators are treated as one separator.
     * For more control over the split use the StrTokenizer class.</p>
     *
     * <p>A {@code null} input String returns {@code null}.</p>
     *
     * <pre>
     * StringUtils.split(null, *)         = null
     * StringUtils.split("", *)           = []
     * StringUtils.split("a.b.c", '.')    = ["a", "b", "c"]
     * StringUtils.split("a..b.c", '.')   = ["a", "b", "c"]
     * StringUtils.split("a:b:c", '.')    = ["a:b:c"]
     * StringUtils.split("a b c", ' ')    = ["a", "b", "c"]
     * </pre>
     *
     * @param str  the String to parse, may be null
     * @param separatorChar  the character used as the delimiter
     * @return an array of parsed Strings, {@code null} if null String input
     * @since 2.0
     */
    public static String[] split(String str, char separatorChar) {
        return splitWorker(str, separatorChar, false);
    }

    /**
     * <p>Splits the provided text into an array, separators specified.
     * This is an alternative to using StringTokenizer.</p>
     *
     * <p>The separator is not included in the returned String array.
     * Adjacent separators are treated as one separator.
     * For more control over the split use the StrTokenizer class.</p>
     *
     * <p>A {@code null} input String returns {@code null}.
     * A {@code null} separatorChars splits on whitespace.</p>
     *
     * <pre>
     * StringUtils.split(null, *)         = null
     * StringUtils.split("", *)           = []
     * StringUtils.split("abc def", null) = ["abc", "def"]
     * StringUtils.split("abc def", " ")  = ["abc", "def"]
     * StringUtils.split("abc  def", " ") = ["abc", "def"]
     * StringUtils.split("ab:cd:ef", ":") = ["ab", "cd", "ef"]
     * </pre>
     *
     * @param str  the String to parse, may be null
     * @param separatorChars  the characters used as the delimiters,
     *  {@code null} splits on whitespace
     * @return an array of parsed Strings, {@code null} if null String input
     */
    public static String[] split(String str, String separatorChars) {
        return splitWorker(str, separatorChars, -1, false);
    }

    /**
     * <p>Splits the provided text into an array with a maximum length,
     * separators specified.</p>
     *
     * <p>The separator is not included in the returned String array.
     * Adjacent separators are treated as one separator.</p>
     *
     * <p>A {@code null} input String returns {@code null}.
     * A {@code null} separatorChars splits on whitespace.</p>
     *
     * <p>If more than {@code max} delimited substrings are found, the last
     * returned string includes all characters after the first {@code max - 1}
     * returned strings (including separator characters).</p>
     *
     * <pre>
     * StringUtils.split(null, *, *)            = null
     * StringUtils.split("", *, *)              = []
     * StringUtils.split("ab de fg", null, 0)   = ["ab", "cd", "ef"]
     * StringUtils.split("ab   de fg", null, 0) = ["ab", "cd", "ef"]
     * StringUtils.split("ab:cd:ef", ":", 0)    = ["ab", "cd", "ef"]
     * StringUtils.split("ab:cd:ef", ":", 2)    = ["ab", "cd:ef"]
     * </pre>
     *
     * @param str  the String to parse, may be null
     * @param separatorChars  the characters used as the delimiters,
     *  {@code null} splits on whitespace
     * @param max  the maximum number of elements to include in the
     *  array. A zero or negative value implies no limit
     * @return an array of parsed Strings, {@code null} if null String input
     */
    public static String[] split(String str, String separatorChars, int max) {
        return splitWorker(str, separatorChars, max, false);
    }

    /**
     * Performs the logic for the {@code split} and
     * {@code splitPreserveAllTokens} methods that do not return a
     * maximum array length.
     *
     * @param str  the String to parse, may be {@code null}
     * @param separatorChar the separate character
     * @param preserveAllTokens if {@code true}, adjacent separators are
     * treated as empty token separators; if {@code false}, adjacent
     * separators are treated as one separator.
     * @return an array of parsed Strings, {@code null} if null String input
     */
    private static String[] splitWorker(String str, char separatorChar, boolean preserveAllTokens) {
        // Performance tuned for 2.0 (JDK1.4)

        if (str == null) {
            return null;
        }
        int len = str.length();
        if (len == 0) {
            return EMPTY_STRING_ARRAY;
        }
        List<String> list = new ArrayList<String>();
        int i = 0, start = 0;
        boolean match = false;
        boolean lastMatch = false;
        while (i < len) {
            if (str.charAt(i) == separatorChar) {
                if (match || preserveAllTokens) {
                    list.add(str.substring(start, i));
                    match = false;
                    lastMatch = true;
                }
                start = ++i;
                continue;
            }
            lastMatch = false;
            match = true;
            i++;
        }
        if (match || preserveAllTokens && lastMatch) {
            list.add(str.substring(start, i));
        }
        return list.toArray(new String[list.size()]);
    }

    /**
     * Performs the logic for the {@code split} and
     * {@code splitPreserveAllTokens} methods that return a maximum array
     * length.
     *
     * @param str  the String to parse, may be {@code null}
     * @param separatorChars the separate character
     * @param max  the maximum number of elements to include in the
     *  array. A zero or negative value implies no limit.
     * @param preserveAllTokens if {@code true}, adjacent separators are
     * treated as empty token separators; if {@code false}, adjacent
     * separators are treated as one separator.
     * @return an array of parsed Strings, {@code null} if null String input
     */
    private static String[] splitWorker(String str, String separatorChars, int max, boolean preserveAllTokens) {
        // Performance tuned for 2.0 (JDK1.4)
        // Direct code is quicker than StringTokenizer.
        // Also, StringTokenizer uses isSpace() not isWhitespace()

        if (str == null) {
            return null;
        }
        int len = str.length();
        if (len == 0) {
            return EMPTY_STRING_ARRAY;
        }
        List<String> list = new ArrayList<String>();
        int sizePlus1 = 1;
        int i = 0, start = 0;
        boolean match = false;
        boolean lastMatch = false;
        if (separatorChars == null) {
            // Null separator means use whitespace
            while (i < len) {
                if (Character.isWhitespace(str.charAt(i))) {
                    if (match || preserveAllTokens) {
                        lastMatch = true;
                        if (sizePlus1++ == max) {
                            i = len;
                            lastMatch = false;
                        }
                        list.add(str.substring(start, i));
                        match = false;
                    }
                    start = ++i;
                    continue;
                }
                lastMatch = false;
                match = true;
                i++;
            }
        } else if (separatorChars.length() == 1) {
            // Optimise 1 character case
            char sep = separatorChars.charAt(0);
            while (i < len) {
                if (str.charAt(i) == sep) {
                    if (match || preserveAllTokens) {
                        lastMatch = true;
                        if (sizePlus1++ == max) {
                            i = len;
                            lastMatch = false;
                        }
                        list.add(str.substring(start, i));
                        match = false;
                    }
                    start = ++i;
                    continue;
                }
                lastMatch = false;
                match = true;
                i++;
            }
        } else {
            // standard case
            while (i < len) {
                if (separatorChars.indexOf(str.charAt(i)) >= 0) {
                    if (match || preserveAllTokens) {
                        lastMatch = true;
                        if (sizePlus1++ == max) {
                            i = len;
                            lastMatch = false;
                        }
                        list.add(str.substring(start, i));
                        match = false;
                    }
                    start = ++i;
                    continue;
                }
                lastMatch = false;
                match = true;
                i++;
            }
        }
        if (match || preserveAllTokens && lastMatch) {
            list.add(str.substring(start, i));
        }
        return list.toArray(new String[list.size()]);
    }

	
}
