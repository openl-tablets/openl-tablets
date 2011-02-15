/**
 * Copyright (c) 2004 Exigen Properties, Inc. and/or affiliates.
 * All Rights Reserved.
 *  $Header: /cvs/src/VisiFlowInt/Products/B302/Prototypes/LiveExcel/le-webservice/src/main/java/com/exigen/le/webservices/server/Projects.java,v 1.3 2009/06/12 12:16:49 vabramovs Exp $
 * 
 */
package com.exigen.le.collections;
import java.util.List;

import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;


import com.exigen.le.LE_Value;
import com.exigen.le.project.VersionDesc;



//START SNIPPET: service

@WebService
public interface CollectionsI {
	
	@WebResult(name = "result", targetNamespace = "") List<LE_Value> SERVICE_PROJECTS(@WebParam(name="version") VersionDesc version,@WebParam(name="context") Collections context);
    
	@WebResult(name = "result", targetNamespace = "")List<LE_Value> SERVICE_MAINPROJECT(@WebParam(name="version") VersionDesc version,@WebParam(name="context") Collections context);

	@WebResult(name = "result", targetNamespace = "") List<LE_Value> SERVICE_SAVEARAGE(@WebParam(name="version") VersionDesc version,@WebParam(name="context") Collections context);
   
	@WebResult(name = "result", targetNamespace = "") List<LE_Value> SERVICE_JOSALARY(@WebParam(name="version") VersionDesc version,@WebParam(name="context") Collections context);
   
	@WebResult(name = "result", targetNamespace = "") List<LE_Value> SERVICE_CHIEFSALARY(@WebParam(name="version") VersionDesc version,@WebParam(name="context") Collections context);

}

//END SNIPPET: service

/*
 * $Log: Projects.java,v $
 * Revision 1.3  2009/06/12 12:16:49  vabramovs
 * getSampleData() support
 *
 * Revision 1.2  2009/06/11 06:48:56  vabramovs
 * Full interface "Projects" support
 *
 *
 */
