package org.openl.rules.lang.xls.ce;

import org.openl.conf.IUserContext;
import org.openl.rules.data.IDataBase;
import org.openl.rules.data.ce.DataBaseCE;
import org.openl.rules.lang.xls.XlsBinder;

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
	
	
	

	
	
}
