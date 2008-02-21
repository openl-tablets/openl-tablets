/**
 * Created Jan 26, 2007
 */
package org.openl.rules.ui;

import org.openl.rules.lang.xls.ITableNodeTypes;
import org.openl.util.ITreeElement;

/**
 * @author snshor
 *
 */
public class ProjectTreeRenderer extends DTreeRenderer implements IProjectTypes, ITableNodeTypes
{
	
	static String[][] icons = {
		{"workbook", "images/excel-workbook.png","images/excel-workbook.png","images/excel-workbook-error.png","images/excel-workbook-error.png"}
		,{"worksheet", "images/worksheet.gif","images/worksheet.gif","images/worksheet-error.png","images/worksheet-error.png"}
//		,{PT_TABLE + "." + XLS_DT, "images/ruleset.gif","images/ruleset-h.gif","images/ruleset-error.png","images/ruleset-error.png"}
		,{PT_TABLE + "." + XLS_DT, "images/dt3.png","images/dt3.png","images/dt3-error.png","images/dt3-error.png", "images/dt3-check.png", "images/dt3-check.png", }
		,{PT_TABLE + "." + XLS_DATA, "images/data.gif","images/data.gif", "images/data-error.png","images/data-error.png"}
		,{PT_TABLE + "." + XLS_DATATYPE, "images/dataobject.gif","images/dataobject.gif","images/dataobject-error.png","images/dataobject-error.png"}
		,{PT_TABLE + "." + XLS_ENVIRONMENT, "images/config_obj.gif","images/config_obj.gif","images/config_obj-error.png","images/config_obj-error.png"}
		,{PT_TABLE + "." + XLS_METHOD, "images/method.gif","images/method.gif", "images/method-error.png","images/method-error.png"}
		,{PT_TABLE + "." + XLS_TEST_METHOD, "images/test_ok.gif","images/test_ok.gif", "images/test_ok-error.gif","images/test_ok-error.gif"}
		,{PT_TABLE + "." + XLS_RUN_METHOD, "images/test.gif","images/test.gif", "images/test-error.png","images/test-error.png"}
		}; 
	
	
	ProjectModel project;

	/**
	 * @param jsp
	 * @param frame
	 * @param icons
	 */
	public ProjectTreeRenderer(ProjectModel project, String jsp, String frame)
	{
		super(jsp, frame, icons);
		this.project = project;
	}

	protected String makeURL(ITreeElement element)
	{
		
		if (element.getType().startsWith(PT_TABLE + "."))
			return targetJsp + "?elementID=" + map.getID(element);
		
		return null;
	}

	
	public ProjectTreeElement getElement(int id)
	{
		return (ProjectTreeElement)map.getObject(id);
	}

	protected int getState(ITreeElement element)
	{
		ProjectTreeElement pte = (ProjectTreeElement)element;
		if (pte.hasProblem())
			return 1;
		if (pte.tsn != null && project.isTestable(pte.tsn))
			return 2;
			
		
		return  0;
	}
	
}
