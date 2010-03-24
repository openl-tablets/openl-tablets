/**
 * Created Apr 1, 2007
 */
package org.openl.rules.ui;

public class BorderStyle
{
	/**
	 * @param w
	 * @param style2
	 * @param rgb2
	 */
	
	static public final BorderStyle NONE = 
		new BorderStyle(1, "solid", new short[]{0xBB,0xBB,0xDD});
	
	public BorderStyle(int w, String style, short[] rgb)
	{
		this.width = w;
		this.style = style;
		this.rgb = rgb;
	}

	/**
	 * 
	 */
	public BorderStyle()
	{
	}

	int width = 0;

	String style = "none";

	short[] rgb = { 0, 0, 0 };
}