<%
	String wbName = request.getParameter("wbName");
	if (wbName != null)
		org.openl.rules.webtools.ExcelLauncher.launch(
		"LaunchExcel.vbs", 
		request.getParameter("wbPath"),
		wbName,
		request.getParameter("wsName"),
		request.getParameter("range")
		
		);


	String wdName = request.getParameter("wdName");
	if (wdName != null)
		org.openl.rules.webtools.WordLauncher.launch(
		"LaunchWord.vbs", 
		request.getParameter("wdPath"),
		wdName,
		request.getParameter("wdParStart"),
		request.getParameter("wdParEnd")
		
		);
	
%>

