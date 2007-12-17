/**
 * Created Jan 26, 2007
 */
package org.openl.rules.ui;

import org.openl.base.INamedThing;
import org.openl.meta.DoubleValue;
import org.openl.util.ITreeElement;
import org.openl.util.StringTool;

/**
 * @author snshor
 *
 */
public class ExplainTreeRenderer extends DTreeRenderer
{

	static String[][] icons = {
		 {"value", "images/value.gif", "images/value.gif", "images/value.gif", "images/value.gif",}
		,{"formula.+", "images/operator-plus.gif","images/operator-plus.gif","images/operator-plus.gif","images/operator-plus.gif"}
		,{"formula.*", "images/operator-mul.gif", "images/operator-mul.gif", "images/operator-mul.gif", "images/operator-mul.gif",}
		,{"formula./", "images/operator-div.gif", "images/operator-div.gif", "images/operator-div.gif", "images/operator-div.gif",}
		,{"formula.-", "images/operator-min.gif", "images/operator-min.gif", "images/operator-min.gif", "images/operator-min.gif",}
		,{"formula", "images/operator.gif","images/operator.gif", "images/operator.gif","images/operator.gif"}
		,{"function", "images/method.gif","images/method.gif", "images/method.gif","images/method.gif"}
		}; 

	
	/**
	 * @param jsp
	 * @param frame
	 * @param icons
	 */
	public ExplainTreeRenderer(String jsp, String frame)
	{
		super(jsp, frame, icons);
	}

	/* (non-Javadoc)
	 * @see org.openl.rules.ui.DTreeRenderer#makeURL(org.openl.util.ITreeElement)
	 */
	protected String makeURL(ITreeElement element)
	{
		DoubleValue dv = (DoubleValue)element;
		String url = dv.getMetaInfo() == null  ? null : dv.getMetaInfo().getSourceUrl();
//		if (url != null)
//		{
//			url = WebTool.makeXlsOrDocUrl(url);
//		}	
		return targetJsp + "?uri=" + StringTool.encodeURL(""+url) + "&text=" + getDisplayName(element, INamedThing.REGULAR);
		
//		return targetJsp + "?uri=" + map.getID(element);
	}

	protected String getDisplayName(Object obj, int mode)
	{
		
		return super.getDisplayName(obj, mode+1);
	}
	
	

}
