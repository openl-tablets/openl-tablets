package org.openl.rules.ui;

import java.util.HashMap;

import org.openl.base.INamedThing;
import org.openl.rules.lang.xls.ITableNodeTypes;

public class DTreeModel implements IProjectTypes, ITableNodeTypes
{
	public void renderElement(ProjectTreeElement parent, ProjectTreeElement element, String targetJsp,  StringBuffer buf)
	{
//	d.add(id, parentId, name, url, title, target, icon, iconOpen, open) {

		buf.append("d.add(");
		int parentId = getID(parent);
		int id = getID(element);
		String sfx = (element.getNameCount() < 2 ? "":"("+element.getNameCount()+")");
		String name = element.getDisplayName(INamedThing.SHORT)+sfx;
		String url = makeURL(element, targetJsp);
		String title = element.getDisplayName(INamedThing.REGULAR);
		String target = "mainFrame";
		String icon = getIcon(element);
		String iconOpen = getIconOpen(element);
		String open = null;
		
		
		buf.append(id).append(',');
		buf.append(parentId).append(',');
		buf.append(jsStrName(name)).append(',');
		buf.append(jsStr(url)).append(',');
		buf.append(jsStrName(title)).append(',');
		buf.append(jsStr(target)).append(',');
		buf.append(jsStr(icon)).append(',');
		buf.append(jsStr(iconOpen)).append(',');
		buf.append(jsStr(open));
		
		buf.append(");");
		buf.append("\n");
	}
	
	
	

	
	public static String jsStr(String string)
	{
		if (string == null)
			return  "''";
		
		
		return  "'" + string  + "'";
	}
	
	
	public static String jsStrName(String string)
	{
		if (string == null)
			return  "''";
		
		int idx = string.indexOf('\n');
		if (idx > 0)
			string = string.substring(0,idx) + " ...";
		
		idx = string.indexOf('\'');
		if (idx > 0)
			string = string.substring(0,idx) + " ...";
		
//		if (string.length() > 30)
//			string = string.substring(0, 29) + " ...";
		
		return  "'" + string  + "'";
	}


	static final int errIndex(ProjectTreeElement element)
	{
		return element.hasProblem() ? 1: 0;
	}
	
	static final int ERR_SHIFT=2;
	
	private String getIconOpen(ProjectTreeElement element)
	{
			int err = errIndex(element);
		
			for (int i = 0; i < icons.length; i++)
			{
				if (icons[i][0].equals(element.getType()))
					return icons[i][2 + ERR_SHIFT*err];
			}
			
			
			
		return element.isLeaf() ? DEFAULT_ICON_LEAF[err] : DEFAULT_ICON_OPEN[err];
	}
	
	static final String[]  
			DEFAULT_ICON_OPEN = {"images/dtree/folder-o-n.gif","images/folder-o-error.png"},  
			DEFAULT_ICON_CLOSED = {"images/dtree/folder-c-n.gif","images/folder-c-error.png"},
			DEFAULT_ICON_LEAF = {"images/categoryset.gif", "images/categoryset.gif"};

	static String[][] icons = {
		{"workbook", "images/excel-workbook.png","images/excel-workbook.png","images/excel-workbook-error.png","images/excel-workbook-error.png"}
		,{"worksheet", "images/worksheet.gif","images/worksheet.gif","images/worksheet-error.png","images/worksheet-error.png"}
		,{PT_TABLE + "." + XLS_DT, "images/ruleset.gif","images/ruleset-h.gif","images/ruleset-error.png","images/ruleset-error.png"}
		,{PT_TABLE + "." + XLS_DATA, "images/data.gif","images/data.gif", "images/data-error.png","images/data-error.png"}
		,{PT_TABLE + "." + XLS_DATATYPE, "images/dataobject.gif","images/dataobject.gif","images/dataobject-error.png","images/dataobject-error.png"}
		,{PT_TABLE + "." + XLS_ENVIRONMENT, "images/config_obj.gif","images/config_obj.gif","images/config_obj-error.png","images/config_obj-error.png"}
		,{PT_TABLE + "." + XLS_METHOD, "images/method.gif","images/method.gif", "images/method-error.png","images/method-error.png"}
		,{PT_TABLE + "." + XLS_TEST_METHOD, "images/test.gif","images/test.gif", "images/test-error.png","images/test-error.png"}
		,{PT_TABLE + "." + XLS_RUN_METHOD, "images/test.gif","images/test.gif", "images/test-error.png","images/test-error.png"}
		}; 
	

	private String getIcon(ProjectTreeElement element)
	{
		int err = errIndex(element);
		for (int i = 0; i < icons.length; i++)
		{
			if (icons[i][0].equals(element.getType()))
				return icons[i][1 + ERR_SHIFT*err];
		}
		
		return element.isLeaf() ? DEFAULT_ICON_LEAF[err] : DEFAULT_ICON_CLOSED[err];
	}


	private String makeURL(ProjectTreeElement element, String targetJsp)
	{
		if (element.getType().startsWith(PT_TABLE + "."))
			return targetJsp + "?elementID=" + getID(element);
		
		return null;
	}


	synchronized static int getID(ProjectTreeElement element)
	{
		if (element == null)
			return -1;
		Integer id = (Integer)elementID.get(element);
		if (id == null)
		{
			id = new Integer(++uniqueID);
			elementID.put(element, id);
			idElement.put(id, element);
		}
		return id.intValue();
	}
	
	static HashMap elementID = new HashMap();
	static HashMap idElement = new HashMap();
	
	static int uniqueID = 0;

	public static ProjectTreeElement getElement(int elementID)
	{
		return  (ProjectTreeElement)idElement.get(new Integer(elementID));
	}
}
