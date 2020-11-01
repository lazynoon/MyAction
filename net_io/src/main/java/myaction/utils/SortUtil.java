package myaction.utils;

import java.util.Arrays;
import java.util.Comparator;

public class SortUtil {
	public static final int ASC = 0;
	public static final int DESC = 1;

	/**
	 * 按值排序，且值域改为索引值（丢弃用于排序的原值，相同的值，原位置优先）
	 * @param arr
	 * @param orderBy
	 */
	public static int[] sortFilpToIndex(int[] arr, int orderBy) {
		if(arr == null || arr.length == 0) {
			return arr;
		}
		int[][] buff = new int[arr.length][];
		for(int i=0; i<arr.length; i++) {
			int[] item = new int[2];
			item[0] = arr[i];
			item[1] = i;
			buff[i] = item;
		}
		if(orderBy == DESC) { //降序
			for(int i=0; i<buff.length; i++) {
				for(int j=1; j<buff.length; j++) {
					if(buff[j][0] > buff[j-1][0]) {
						int[] temp = buff[j];
						buff[j] = buff[j-1];
						buff[j-1] = temp;
					}
				}
			}
		} else { //升序
			for(int i=0; i<buff.length; i++) {
				for(int j=1; j<buff.length; j++) {
					if(buff[j][0] < buff[j-1][0]) {
						int[] temp = buff[j];
						buff[j] = buff[j-1];
						buff[j-1] = temp;
					}
				}
			}			
		}
		int sortIndex[] = new int[buff.length];
		for(int i=0; i<buff.length; i++) {
			sortIndex[i] = buff[i][1];
		}
		return sortIndex;
	}
	
	public static String[] sort(String[] arr, final int orderBy) {
		String[] buff = new String[arr.length];
		System.arraycopy(arr, 0, buff, 0, arr.length);
		if(orderBy == DESC) {
			Arrays.sort(buff, stringDescComparator);
		} else {
			Arrays.sort(buff, stringAscComparator);
		}
		return buff;
	}

	/** String ASC **/
	private static final Comparator<String> stringAscComparator = new Comparator<String>() {
		@Override
		public int compare(String after, String before) {
			return 0 - before.compareTo(after);
		}
	};

	/** String DESC **/
	private static final Comparator<String> stringDescComparator = new Comparator<String>() {
		@Override
		public int compare(String after, String before) {
			return before.compareTo(after);
		}
	};

}
