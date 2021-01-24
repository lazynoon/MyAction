package myaction.model;

import java.util.ArrayList;
import java.util.List;

public class ArraySort<T> {
	private ArrayList<T> dataList = new ArrayList<T>();
	private ArrayList<Rank> rankList = new ArrayList<Rank>();
	
	public ArraySort<T> add(T obj, long rank1) {
		rankList.add(new Rank(dataList.size(), rank1));
		dataList.add(obj);
		return this;
	}
	
	public ArraySort<T> add(T obj, long rank1, long rank2) {
		rankList.add(new Rank(dataList.size(), rank1, rank2));
		dataList.add(obj);
		return this;
	}
	
	public List<T> sort() {
		return sort(true, true);
	}
	
	public List<T> rsort() {
		return sort(false, false);
	}
	
	public List<T> sort(boolean sort1, boolean sort2) {
		Rank[] sortArr = rankList.toArray(new Rank[0]);
		for(int i=sortArr.length-1; i>0; i--) {
			for(int j=0; j<i; j++) {
				if(down(sortArr[j], sortArr[j+1], sort1, sort2)) {
					Rank tmp = sortArr[j];
					sortArr[j] = sortArr[j+1];
					sortArr[j+1] = tmp;
				}
			}
		}
		ArrayList<T> result = new ArrayList<T>();
		for(Rank item : sortArr) {
			result.add(dataList.get(item.index));
		}
		return result;
	}
	
	boolean down(Rank r1, Rank r2, boolean s1, boolean s2) {
		if(r1.rank1 > r2.rank1) {
			return s1;
		}
		if(r1.rank1 < r2.rank1) {
			return !s1;					
		}
		if(r1.rank2 > r2.rank2) {
			return s2;
		}
		if(r1.rank2 < r2.rank2) {
			return !s2;					
		}
		return false;
	}
	
	private static class Rank {
		int index;
		long rank1 = 0;
		long rank2 = 0;
		Rank(int index, long rank1) {
			this.index = index;
			this.rank1 = rank1;
		}
		Rank(int index, long rank1, long rank2) {
			this.index = index;
			this.rank1 = rank1;
			this.rank2 = rank2;
		}
		

	}
}
