/**
 *  OpenL Tablets,  2006
 *  https://sourceforge.net/projects/openl-tablets/ 
 */
package org.openl.rules.table.ui;

/**
 * @author snshor
 *
 */
public interface IColorFilter 
{
	short[] filterColor(short[] color);
	
	static final public short[] BLACK = {0,0,0};
	static final public short[] WHITE = {0xff,0xff,0xff};
	static final public short[] RED = 	{0xff,0   ,0   };
	static final public short[] GREEN = {0   ,0xff,0   };
	static final public short[] BLUE =  {0   ,0   ,0xff};
	static final public short[] YELLOW = {0xff,0xff,0};
	static final public short[] MAGENTA = {0xff,0,0xff};
	static final public short[] CYAN = {0,0xff,0xff};

	
	static public final String[] COLOR_NAMES = {"black", "white", "red", "green", "blue","yellow", "cyan", "magenta", };
	static public final short[][] COLORS = {BLACK, WHITE, RED, GREEN, BLUE, YELLOW, CYAN, MAGENTA};
	
	static public class Tool
	{
		static public short[] getColor(String name)
		{
			for (int i = 0; i < COLOR_NAMES.length; i++)
			{
				if (COLOR_NAMES[i].equals(name))
					return COLORS[i];
			}
			return null;
		}
	}
	
}
