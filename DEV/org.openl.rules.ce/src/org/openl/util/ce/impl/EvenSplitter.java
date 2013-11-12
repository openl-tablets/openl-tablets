package org.openl.util.ce.impl;

import java.util.ArrayList;
import java.util.List;

import org.openl.util.ce.IActivity;

public class EvenSplitter<T extends IActivity> {
	
	T[] all; 
	int n;
	long[] sizes;
	long runningMax;
	List<T>[] res; 

	@SuppressWarnings("unchecked")
	public EvenSplitter(T[] all, int n) {
		super();
		this.all = all;
		this.n = n;
		sizes = new long[n];
		runningMax = 0;
		res = new List[n]; 
	}
	

	
	static  public <V  extends IActivity> List<V>[] split(V[] all, int n)
	{
		return new EvenSplitter<V>(all, n).split();
	}


	private List<T>[] split() {
		
		
		int index = 0;
		int next = 1;
		
		for (int i = 0; i <  all.length; i++) {
			long d = all[i].duration();
		
			if (sizes[index] > sizes[next])
			{
				index = next;
				next = (next +1) % sizes.length;
				
				put(index, i);
				
				continue;
			}	
			
			
			put(index, i);
			
			
			
		}
		
		
		
		return res;
	}


	private void put(int index, int i) {
		if (res[index] == null)
		{
			res[index] = new ArrayList<T>();
		}
		
		res[index].add(all[i]);
		runningMax = Math.max(runningMax, sizes[index] += all[i].duration());
		
		
	} 
	
	
	
	public static void main(String[] args) {
		int[] x = {24, 12, 8, 7 , 6, 5, 4, 3, 2, 1};
		IActivity[] all = new IActivity[x.length]; 
		
		for (int i = 0; i < x.length; i++) {
			all[i] = makeA(x[i]);
		}
		
		List<IActivity>[] res = split(all, 2);
		
		for (int i = 0; i < res.length; i++) {
			System.out.println(res[i]);
		}

		res = split(all, 3);
		
		for (int i = 0; i < res.length; i++) {
			System.out.println(res[i]);
		}
		
		
	}



	private static IActivity makeA(final int d) {
	return new IActivity() {

		@Override
		public List dependsOn() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public long duration() {
			// TODO Auto-generated method stub
			return d;
		}

		@Override
		public String toString() {
			
			return ""+ duration();
		}


		
		
		
	};
	
	}

}
