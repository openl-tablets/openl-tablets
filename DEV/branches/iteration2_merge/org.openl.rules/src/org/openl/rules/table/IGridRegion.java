/*
 * Created on Oct 28, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.table;

/**
 * @author snshor
 * 
 */
public interface IGridRegion
{
	int getTop();

	int getLeft();

	int getRight();

	int getBottom();



	static public class Tool
	{
		static public boolean intersects(IGridRegion i1, IGridRegion i2)
		{
			int left = Math.max(i1.getLeft(), i2.getLeft());
			int right = Math.min(i1.getRight(), i2.getRight());
			if (right < left)
				return false;
			int top = Math.max(i1.getTop(), i2.getTop());
			int bottom = Math.min(i1.getBottom(), i2.getBottom());
			return top <= bottom;
		}

		static public IGridRegion intersect(IGridRegion i1, IGridRegion i2)
		{
			int left = Math.max(i1.getLeft(), i2.getLeft());
			int right = Math.min(i1.getRight(), i2.getRight());
			int top = Math.max(i1.getTop(), i2.getTop());
			int bottom = Math.min(i1.getBottom(), i2.getBottom());
			return top <= bottom && left <= right ? new GridRegion(top, left, bottom, right) : null;
		}
		
		
		static public boolean contains(IGridRegion i1, int x , int y)
		{
			return  i1.getLeft() <= x && x <= i1.getRight() 
			     && i1.getTop()  <= y && y <= i1.getBottom();
		}
		
		public static int height(IGridRegion i1)
		{
			return i1.getBottom() - i1.getTop() + 1;
		}

		public static int width(IGridRegion i1)
		{
			return i1.getRight() - i1.getLeft() + 1;
		}

		/**
		 * @param intersection
		 * @param dx
		 * @param dy
		 * @return
		 */
		public static GridRegion move(IGridRegion reg, int dx, int dy)
		{
			return new GridRegion(reg.getTop() + dy, reg.getLeft() + dx, reg.getBottom() + dy, reg.getRight() + dx);
		}

		
		
	}

}
