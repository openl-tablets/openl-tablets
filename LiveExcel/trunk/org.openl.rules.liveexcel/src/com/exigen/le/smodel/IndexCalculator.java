package com.exigen.le.smodel;

import java.util.Arrays;

/**
 * @author vabramovs
 *
 */
public class IndexCalculator {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		char[] c1 = {'A'};
		System.out.println("A: " + calculateIndex(c1));
		
		char[] c11 = {'Z'};
		System.out.println("Z: " + calculateIndex(c11));
		
		char[] c21 = {'A','A'};
		System.out.println("AA: " + calculateIndex(c21));

		char[] c22 = {'A','B'};
		System.out.println("AB: " + calculateIndex(c22));

		char[] c23 = {'B','A'};
		System.out.println("BA: " + calculateIndex(c23));

		
		System.out.println("1:" + new String(calculateColumn(1)));
		System.out.println("26:" + new String(calculateColumn(26)));
		System.out.println("27:" + new String(calculateColumn(27)));
		System.out.println("28:" + new String(calculateColumn(28)));
		System.out.println("53:" + new String(calculateColumn(53)));
		
	}
	
	
	/**
	 * Calculates index of Excel column based on its name:
	 * A - 1;
	 * Z - 26;
	 * AA - 27;
	 * AB - 28;
	 * BA - 53
	 * @param column Column name, e.g. {'A'} or   {'B', 'A'} 
	 * @return 
	 */
	public static int calculateIndex(char[] column){
		
		int power =1;
		int total = 0;
		for (int cntr=column.length-1; cntr>=0; cntr--){
			total+=numberFromLetter(column[cntr])*power;
			power = power*27;
		}
		int zeros = calculateZeros(column);
		return total-zeros;
		
	}
	
	private static int calculateZeros(char[] column){
		if (column.length == 1){
			return 0;
		}
		if (column[0]=='A'){
			return 1+ calculateZeros(Arrays.copyOfRange(column, 1, column.length));
		}
		
		
		int nozero = numberFromLetter(column[0]);
		for (int i=1;i<column.length; i++){
			nozero*=26;
		}
		int total = 0;
		int power =1;
		
		
		for (int cntr = column.length-1; cntr>0; cntr--){
			total+= numberFromLetter('Z')*power;
			power=power*27;
		}
		total+=(numberFromLetter(column[0])-1)*power;
		int zeros = total-nozero;
		return zeros+1+calculateZeros(Arrays.copyOfRange(column, 1, column.length));
		
	}
	
	private static int numberFromLetter(char c){
		return (c-'A'+1);
	}


	/**
	 * Calculates Excel column name based on index
	 * 1 - A
	 * 26 - Z
	 * 27 - AA
	 * 53 - BA
	 * @param index  (one based)
	 * @return Excel column name
	 */
	public static char[] calculateColumn(int index){
		
		char[] column = {'Z'};
		// estimate from top
		while (calculateIndex(column)<index){
			int l = column.length+1;
			column = new char[l];
			Arrays.fill(column, 'Z');
		}
		
		int cntr=0;
		while (calculateIndex(column) != index){
			if (calculateIndex(column)>index){
				column[cntr]-=1;
			} else {
				column[cntr]+=1;
				cntr++;
				if (cntr>=column.length){
					throw new RuntimeException("Oops");
				}	
			}	
		}
		return column;
		
	}

}
