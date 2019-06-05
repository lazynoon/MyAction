package myaction.utils;

public class IPMask {
	private long ipNet = 0;
	private long ipMask = 0; //初始化时，会做位翻转。即，默认为: 255.255.255.255
	
	private IPMask() {
	}
	
	/**
	 * 解析IP地址
	 * @param ipNet
	 * @return 解析失败，返回null。成功返回 IPMask 对象
	 */
	public static IPMask parse(String ipNet) {
		if(ipNet == null) {
			return null;
		}
		ipNet = ipNet.trim();
		String[] arr = StringUtil.split(ipNet, '/');
		long ipNum = convertIP(arr[0]);
		if(ipNum < 0) {
			return null;
		}
		int maskLen = 32;
		if(arr.length > 1) {
			maskLen = MathUtil.parseInt(arr[1]);
			maskLen = Math.max(0, maskLen);
			maskLen = Math.min(32, maskLen);
		}
		IPMask mask = new IPMask();
		maskLen = 32 - maskLen; //反转
		for(int i=0; i<maskLen; i++) {
			mask.ipMask = (mask.ipMask << 1) | 0x1;
		}
		mask.ipMask = ~ mask.ipMask;
		mask.ipNet = mask.ipMask & ipNum;
		return mask;
	}
	
	public boolean isSameNet(String ip) {
		long ipNum = convertIP(ip);
		return isSameNet(ipNum);
	}
	
	public boolean isSameNet(long ipNum) {
		long a = ipNum & ipMask;
		return (a == ipNet);
	}
	
	public static long convertIP(String ip) {
		long ipNum = -1;
		String[] arr = StringUtil.split(ip, '.');
		if(arr.length != 4) {
			return ipNum;
		}
		ipNum = ((long)MathUtil.parseInt(arr[0]) << 24)
				| (MathUtil.parseInt(arr[1]) << 16)
				| (MathUtil.parseInt(arr[2]) << 8)
				| MathUtil.parseInt(arr[3]);
		return ipNum;
	}
}
