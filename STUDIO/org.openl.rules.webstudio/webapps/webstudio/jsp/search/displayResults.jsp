

<%
for(int i = 0; i < values.length; ++i)
{
	String icon = null;
	if (values[i][0].indexOf(".xls") >= 0)
	  icon="../../images/excel-workbook.png";
	  
	if (values[i][0].indexOf(".doc") >= 0)
	  icon="../../images/word-doc.png";
%>
<table cellspacing=0 width=80%>
<tr>
<td width="16"><%if (icon!=null){%> <img src="<%=icon%>"/>  <%}%></td>
<td valign=top> 
  <%=values[i][0]%>
</td>
</tr>
<tr><td/>
<td><font size=-1><%=values[i][1]%><font></td>
</tr>
</table><p/>

<%
}
%>



</body>
</html>

