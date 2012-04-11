/**
 * Created Jul 26, 2007
 */
package org.openl.util.benchmark;

/**
 * @author snshor
 *
 */
public class BenchmarkOrder implements Comparable<BenchmarkOrder>
{
	
	int index; 
	BenchmarkInfo info;
	int order;
	double ratio;
	

	/**
	 * @param i
	 * @param info
	 */
	public BenchmarkOrder(int i, BenchmarkInfo info)
	{
		this.index = i;
		this.info = info;
	}

	
	
	public BenchmarkInfo getInfo()
	{
		return this.info;
	}

	public int getOrder()
	{
		return this.order;
	}

	public void setOrder(int order)
	{
		this.order = order;
	}

	public double getRatio()
	{
		return this.ratio;
	}

	public void setRatio(double ratio)
	{
		this.ratio = ratio;
	}

	public int getIndex()
	{
		return this.index;
	}



	public int compareTo(BenchmarkOrder arg0)
	{
		double x = info.drunsunitsec() - arg0.info.drunsunitsec();
		return  x > 0 ? -1 :
			  x == 0 ? 0 : 1;
	}



	public boolean equals(Object arg0)
	{
		if (arg0 instanceof BenchmarkOrder)
		{
			BenchmarkOrder bo = (BenchmarkOrder) arg0;
			return bo.info.drunsunitsec() == info.drunsunitsec();
		}
		return false;
	}



	public int hashCode()
	{
		return (int)info.drunsunitsec();
	}
	
	
	

}
