<%@ page import = "org.openl.util.benchmark.*" %>


<jsp:useBean id='studio' scope='session' class="org.openl.rules.ui.WebStudio" />
<jsp:useBean id="explanator" scope="session" class="org.openl.rules.ui.Explanator"/>


<%@include file="checkTimeout.jspf"%>



<% 
	String s_id = request.getParameter("elementID"); 
   	int elementID = -100; 	
   	if (s_id != null)
   	{
     	elementID = Integer.parseInt(s_id);
     	studio.setTableID(elementID);
    }
    else 
      elementID = studio.getTableID(); 	
   String url = studio.getModel().makeXlsUrl(elementID);
   String uri = studio.getModel().getUri(elementID);
   String text = org.openl.rules.webtools.indexer.FileIndexer.showElementHeader(uri);   
   String name = studio.getModel().getDisplayNameFull(elementID);
   org.openl.rules.ui.Explanator.setCurrent(explanator);
   String testName = request.getParameter("testName");
   String testID = request.getParameter("testID");
   String testDescr = request.getParameter("testDescr");
	   
%>





<html>

<head>
<meta http-equiv="Content-Type" content="text/html; charset=windows-1257">
<title><%=text%></title>
<link href="webresource/css/openl/style1.css" rel="stylesheet" type="text/css"/>

<script type="text/javascript">



function toggleSelectAll(N)
{
 sa=document.getElementById("selectAll");
 for(i = 0; i < N; i++)
   jssetchecked("BM"+i, sa.checked);

}


function jssetchecked(id,value)
{
 el=document.getElementById(id);
   el.checked=value;

}

</script>

<%@include file="common.jspf"%>

</head>

<body>


<h3>Results of benchmarking <%=name%> <%=testDescr == null ? "" : " ("+testDescr + ")"%> </h3>
<p>  

<%

	
	boolean selectAllSelected = request.getParameter("selectAll") != null;
	
   if (request.getParameter("delete") != null)
   {
	BenchmarkInfo[] bu = studio.getBenchmarks();
	for (int i = bu.length; i >= 0; --i)
	{
	  if (request.getParameter("BM"+i) != null)
	    studio.removeBenchmark(i);
	}
   }	
   else if (request.getParameter("compare") != null)
   {}
   else		
   {
   		BenchmarkInfo buLast =  studio.getModel().benchmarkElement(elementID, testName, testID, testDescr, 3000);
//   Object res =  studio.getModel().runElement(elementID);
		studio.addBenchmark(buLast);
   }
   
   
   
   	
	BenchmarkInfo[] bu = studio.getBenchmarks();
  	BenchmarkInfo[] cmpbu = new BenchmarkInfo[bu.length];
%>

<form>
 <table class="ov-matrix">
 <tr>
 	<th/>
 	<th class='ov-matrix'>Name</th>
 	<th class='ov-matrix'>Run(ms)</th>
 	<th class='ov-matrix'>Runs/sec</th>
 	<th class='ov-matrix'>Unit Type</th>
 	<th class='ov-matrix'>Units</th>
 	<th class='ov-matrix'>Unit(ms)</th>
 	<th class='ov-matrix'>Units/sec</th>
 	<th><input type="checkbox" <%=selectAllSelected?" checked='checked'":""%>  name="selectAll" id="selectAll" onChange="toggleSelectAll(<%=bu.length%>)"/></th>
 </tr>
 
<%
	for(int i = 0; i < bu.length; ++i){ 
		boolean selected =  request.getParameter("BM"+i) != null;
		if (selected)
  		{
  			cmpbu[i] = bu[i];
  		}
%> 
 <tr>
 <td align="right"><%=i+1%>.</td>
 <td class='ov-matrix'><%=bu[i].getName()%></td>
 <td class='ov-matrix' align="right"><%=bu[i].msrun()%></td> 
 <td class='ov-matrix' align="right"><%=bu[i].runssec()%></td>
 <td class='ov-matrix'><%=bu[i].unitName()%></td>
 <td class='ov-matrix'align="right"><%=bu[i].getUnit().nUnitRuns()%></td>
 <td class='ov-matrix' align="right"><%=bu[i].msrununit()%></td>
 <td class='ov-matrix' align="right"><%=bu[i].runsunitsec()%></td>
 <td><input type="checkbox" <%=selected?" checked='checked'":""%>  name="BM<%=i%>" id="BM<%=i%>"/></td> 
 </tr>
<%}%>
<tr>
	<td/><td><input type="submit" name="compare" value="Compare"/></td> 
	<td><input type="submit" name="delete" value="Delete"/></td> 
</tr> 	
 </table>	


</form>


<p>

<table class="ov-matrix">
<%
  if (request.getParameter("compare") != null)
  {
  	
	BenchmarkOrder[] bo = BenchmarkInfo.order(cmpbu);
	  	
  	for(int i = 0; i < cmpbu.length;++i)
  	{
  		if (cmpbu[i] != null)
  		{
  			int size = 3;
  			String color = "black";
  			switch(bo[i].getOrder())
  			{
  			  case 1:
  				size=5;
  				color="red";
  				break;  
  			  case 2:
  				size=4;
  				color="green";
  				break;  
  			  case 3:
  				size=4;
  				color="blue";
  				break;  
  			}
%>
 <tr>
 <td align="right"><%=i+1%>.</td>
 <td class='ov-matrix'><%=cmpbu[i].getName()%></td>
 <td class='ov-matrix' align="right"><%=cmpbu[i].runsunitsec()%></td>
 <td class='ov-matrix' align="right"><font size="<%=size%>%" color="<%=color%>"><%=bo[i].getOrder()%></td>
 <td class='ov-matrix' align="right"><%=BenchmarkInfo.printDouble(bo[i].getRatio(),2)%></td>
 
 </tr>

<% 
		}
	}
	
  }	

%>

</table>


</body>
