/**
 * Created Jan 26, 2007
 */
package org.openl.rules.ui;

import org.openl.util.ITreeElement;

/**
 * @author snshor
 *
 */
public class TraceTreeRenderer extends DTreeRenderer
{

	static String[][] icons = {
		 {"decision", "images/ruleset.gif","images/ruleset-h.gif","images/ruleset-error.png","images/ruleset-error.png"}
		,{"rule", "images/test_ok.gif","images/test_ok.gif", "images/data-error.png","images/data-error.png"}
		}; 

	
	/**
	 * @param jsp
	 * @param frame
	 * @param icons
	 */
	public TraceTreeRenderer(String jsp, String frame)
	{
		super(jsp, frame, icons);
	}

	protected String makeURL(ITreeElement element)
	{
		return targetJsp + "?elementID=" + map.getID(element);
	}

}
