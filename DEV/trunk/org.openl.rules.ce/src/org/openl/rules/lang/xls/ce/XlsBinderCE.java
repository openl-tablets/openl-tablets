package org.openl.rules.lang.xls.ce;

import org.openl.conf.IUserContext;
import org.openl.rules.calc.ce.SpreadsheetNodeBinderCE;
import org.openl.rules.data.IDataBase;
import org.openl.rules.data.ce.DataBaseCE;
import org.openl.rules.lang.xls.XlsBinder;
import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.lang.xls.binding.AXlsTableBinder;

public class XlsBinderCE  extends XlsBinder{

    public static final String DEFAULT_OPENL_NAME_MT = "org.openl.rules.java.ce";
	
	
	public XlsBinderCE(IUserContext userContext) {
		super(userContext);
	}


	@Override
	protected String getDefaultOpenLName() {
		return DEFAULT_OPENL_NAME_MT;
	}


	@Override
	protected IDataBase getModuleDatabase() {
		return new DataBaseCE();
	}


	SpreadsheetNodeBinderCE spreadsheetBinderCE = new SpreadsheetNodeBinderCE();
	
	@Override
	protected AXlsTableBinder findBinder(String tableSyntaxNodeType) {
		if (tableSyntaxNodeType.equals(XlsNodeTypes.XLS_SPREADSHEET.toString()))
			return spreadsheetBinderCE;
		return super.findBinder(tableSyntaxNodeType);
	}
	
	
	

	
	
}
