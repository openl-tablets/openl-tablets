package demo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.openl.rules.dt.DecisionTable;
import org.openl.rules.dt.IDecisionTableConstants;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.ui.OpenLWebProjectInfo;
import org.openl.rules.ui.OpenLWrapperInfo;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;
import org.openl.types.IOpenMethod;
import org.openl.types.impl.AOpenClass;

public class UpdateProject 
{
	
	static final public String WS= "..";
//	static final public String PROJECT_NAME = "com.zadmobile.demo";
	static final public String PROJECT_NAME = "org.openl.tablets.tutorial1";
	static final public String PACKAGE_NAME = PROJECT_NAME;
//	static final public String WRAPPER_NAME = PACKAGE_NAME + "." + "ZadRulesJavaWrapper";
	static final public String WRAPPER_NAME = PACKAGE_NAME + "." + "Tutorial_1Wrapper";
	
	
	
//	static final public String METHOD_NAME = "AdspaceRuleCheck2";
	static final public String METHOD_NAME = "hello5";
	
	static final boolean orientationVertical = false;	
	

	
	public static void main(String[] args) throws Exception 
	{
		OpenLWebProjectInfo wpi = new OpenLWebProjectInfo(WS, PROJECT_NAME);
		OpenLWrapperInfo wrapperInfo = new OpenLWrapperInfo(WRAPPER_NAME, wpi);
		
		WebStudio ws = new WebStudio();
		ProjectModel pm = new ProjectModel(ws);
		
		pm.setWrapperInfo(wrapperInfo);
		
		
		for(Iterator<IOpenMethod> mm = pm.getWrapper().getOpenClass().methods(); mm.hasNext(); )
			System.out.println(mm.next());
		
		DecisionTable dt = (DecisionTable)AOpenClass.getSingleMethod(METHOD_NAME, pm.getWrapper().getOpenClass().methods());
		
		ILogicalTable table = dt.getTableSyntaxNode().getSubTables().get(IDecisionTableConstants.VIEW_BUSINESS);
		
		
		ILogicalTable row = table.rows(0, 1);
		
		int w = row.getLogicalWidth();
		
		System.out.println("ROWS");
		
		for (int i = 0; i < w; i++) {
			System.out.println(row.getLogicalColumn(i).getGridTable().getStringValue(0,0));
		}
		
		ILogicalTable col = table.columns(0, 1);
		
		System.out.println("COLUMNS");
		int h = col.getLogicalHeight();
		for (int i = 0; i < h; i++) {
			System.out.println(row.getLogicalRow(i).getGridTable().getStringValue(0,0));
		}
		
		
		System.out.println("Normal Orientation: " + table.getGridTable().isNormalOrientation());
		
		
		TableTemplateAdaptor tta =	 new TableTemplateAdaptor(table);
		
		ArrayList<HashMap<String, String>> maps = new ArrayList<HashMap<String,String>>();
		
		for (int i = 0; i < 5; i++) {
			HashMap<String, String> m1 = new HashMap<String, String>();
			
			m1.put("From", "33" + i);
			
			if (i < 2)
				m1.put("To", "44" + i);
			else
				m1.put("To", "");
			maps.add(m1);
			
		}
		
		HashMap<String, String>[] rules = maps.toArray(new HashMap[0]);
		tta.addRules(rules );
		tta.save();
		
		
	}
	
	
	public void loadProject()
	{
		
	}
	
}
