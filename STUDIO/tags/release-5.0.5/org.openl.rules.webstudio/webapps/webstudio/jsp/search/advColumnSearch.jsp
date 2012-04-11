<fieldset>
<legend>Column Selector</legend>


<p/>

<table class="search-table">
<th/>
<th class="search" colspan ='3'>Column Attribute</th>
<th class="search">Condition</th>
<th class="search">Value</th>


<%
W = 7;




SearchElement[] columnElements = searchBean.getColumnElements();
String[] colTypeValues = searchBean.colTypeValues;

for(int i= 0; i < columnElements.length; ++i)
{
	if (i > 0)
	{
		GroupOperator colGop = columnElements[i].getOperator();
		String[] colGopValues = searchBean.getGopValues();
		String colGopID = "colGop"+i;
%>

<tr>
	<td id="td<%=colGopID%>" colspan="<%=W%>" align="<%=colGop.isGroup()?"center":"left"%>" class="<%=colGop.isGroup()?"search-group":"search-element"%>">
	<select name="<%=colGopID%>"  id="<%=colGopID%>" onchange="alignGop('<%=colGopID%>')" class="<%=colGop.isGroup()?"search-group":"search-element"%>">
	<%
	for(int j=0; j < colGopValues.length; ++j)
	{	
	%>
	<option value="<%=colGopValues[j]%>" <%=colGopValues[j].equals(colGop.getName())? "selected='selected'":""%> ><%=colGopValues[j]%></option>
	<%
	}
	%>
</select>
	
	</td>
</tr>
<%
	}//if
	
	boolean isNotFlag = columnElements[i].isNotFlag();
	String colNfID = "colNf"+i;
	String[] colNfValues = searchBean.nfValues;
	
%> 
<tr>

<td>&nbsp;&nbsp;&nbsp;&nbsp;</td>

<td>
	<select name="<%=colNfID%>"  id="<%=colNfID%>">
	<%
	for(int j=0; j < colNfValues.length; ++j)
	{	
	%>
	<option value="<%=colNfValues[j]%>"  <%=isNotFlag && (j == 1) ? "selected='selected'":""%> > <%=colNfValues[j]%> </option>
	<%
	}
	%>
</td>


<%
	String colTypeID = "colType"+i;
	String colTypeValue = columnElements[i].getType();
	String colValue1ID = "colValue1_"+i;
	String colValue2ID = "colValue2_"+i;
	
	
%>

<td>
	<select name="<%=colTypeID%>"  id="<%=colTypeID%>">
	<%
	for(int j=0; j < colTypeValues.length; ++j)
	{	
	%>
	<option value="<%=colTypeValues[j]%>"  <%=colTypeValues[j].equals(colTypeValue) ? "selected='selected'":""%>> <%=colTypeValues[j]%> </option>
	<%
	}
	%>
</td>


<%
	String colOpType1ID = "colOpType1"+i;
	String colOpType1Value = columnElements[i].getOpType1();
	String[] colOpType1Values = searchBean.opTypeValues();
	
	
%>

<td>
	<select name="<%=colOpType1ID%>"  id="<%=colOpType1ID%>">
	<%
	for(int j=0; j < colOpType1Values.length; ++j)
	{	
	%>
	<option value="<%=colOpType1Values[j]%>"  <%=colOpType1Values[j].equals(colOpType1Value) ? "selected='selected'":""%> > <%=colOpType1Values[j]%> </option>
	<%
	}
	%>
</td>


<%
	String colValue1value = columnElements[i].getValue1();
%>



<td>
  <input class="search-input" id="<%=colValue1ID%>"  name="<%=colValue1ID%>" value="<%=colValue1value%>" 
    onClick="jscleanAny('<%=colValue1ID%>', '<%=ISearchConstants.ANY%>')"  />
</td>


<%
	String colOpType2ID = "colOpType2"+i;
	String colOpType2Value = columnElements[i].getOpType2();
	String[] colOpType2Values = searchBean.opTypeValues();
	
	
%>

<td>
	<select name="<%=colOpType2ID%>"  id="<%=colOpType2ID%>">
	<%
	for(int j=0; j < colOpType2Values.length; ++j)
	{	
	%>
	<option value="<%=colOpType2Values[j]%>"  <%=colOpType2Values[j].equals(colOpType2Value) ? "selected='selected'":""%> > <%=colOpType2Values[j]%> </option>
	<%
	}
	%>
</td>

<%
	
	String colValue2value = columnElements[i].getValue2();
	
	
%>



<td>
  <input class="search-input" id="<%=colValue2ID%>"  name="<%=colValue2ID%>" value="<%=colValue2value%>" onClick="jscleanAny('<%=colValue2ID%>', '<%=ISearchConstants.ANY%>')" 
</td>


<td>
	<a href="javascript:img_action_click('colAdd<%=i%>')" title="Add Condition">
	<img src="../../images/add_obj.gif" onClick=""></a>
<%if(i > 0){%>	
	<a href="javascript:img_action_click('colDelete<%=i%>')" title="Remove Condition">
	<img src="../../images/delete.gif" onClick=""></a>
<%}%>	
</td>


</tr>

<%
}
%>

</table>

</fieldset>
