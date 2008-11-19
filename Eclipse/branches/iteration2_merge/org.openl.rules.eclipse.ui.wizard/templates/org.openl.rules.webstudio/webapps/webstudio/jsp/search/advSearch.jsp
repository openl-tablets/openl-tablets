<%@ page import = "org.openl.rules.search.*" %>
<jsp:useBean id='studio' scope='session' class="org.openl.rules.ui.WebStudio" />
<jsp:useBean id='searchBean' scope='session' class="org.openl.rules.search.OpenLAdvancedSearch" />



<%
   	String[] tableTypeButtons = searchBean.getTypeButtons();

	String[] typeValues = searchBean.typeValues;
	boolean[] typeNeedValue1 = searchBean.typeNeedValue1;

	String img_action = request.getParameter("img_action");

	for(int i = 0; i < 20; ++i)
	{
		String gopID = "gop"+i;
		String nfID = "nf"+i;
		String typeID = "type"+i;
		String value1ID = "value1_"+i;
		String opTypeID = "opType"+i;
		String value2ID = "value2_"+i;

		if (request.getParameter(typeID) != null)
		{
		  searchBean.fillTableElement(i
			,request.getParameter(gopID)
			,request.getParameter(nfID)
			,request.getParameter(typeID)
			,request.getParameter(value1ID)
			,request.getParameter(opTypeID)
			,request.getParameter(value2ID)
			);
		}

	}


	for(int i = 0; i < 20; ++i)
	{
		String colGopID = "colGop"+i;
		String colNfID = "colNf"+i;
		String colTypeID = "colType"+i;
		String colOpType1ID = "colOpType1"+i;
		String colValue1ID = "colValue1_"+i;
		String colOpType2ID = "colOpType2"+i;
		String colValue2ID = "colValue2_"+i;

		if (request.getParameter(colTypeID) != null)
		{
		  searchBean.fillColumnElement(i
			,request.getParameter(colGopID)
			,request.getParameter(colNfID)
			,request.getParameter(colTypeID)
			,request.getParameter(colOpType1ID)
			,request.getParameter(colValue1ID)
			,request.getParameter(colOpType2ID)
			,request.getParameter(colValue2ID)
			);
		}

	}


	if (img_action != null)
	{
		searchBean.editAction(img_action);
	}


%>



<head>
<link href="webresource/css/openl/style1.css" rel="stylesheet" type="text/css"/>

<%@include file="openl/advSearch.css"%>

<%@include file="advSearch.js"%>
<script type="text/javascript" src="webresource/javascript/prototype/prototype-1.5.1.js"></script>

</head>

<body onload="body_load()">


<form id="adv_search_form" action="advSearch.jsp" >

<fieldset>
<legend>Table Selector</legend>

<%
for(int i=0; i < tableTypeButtons.length; ++i)
{
  String id = "select" + tableTypeButtons[i];
  String value = request.getParameter(id);
  searchBean.selectType(i, "true".equals(value));
%>

	<input type="checkbox" id="<%=id%>" name="<%=id%>" value="true" <%= searchBean.selectType(i) ? "checked='checked'" : ""%> />
	<span class="search-table-type"><%=tableTypeButtons[i]%></span>
<%
}
%>

<a href="javascript:jssetAllTypes()">Set All</a>
<a href="javascript:jsclearAllTypes()">Clear All</a>


<p/>

<table class="search-table">
<th/>
<th class="search" colspan ='3'>Table Attribute</th>
<th class="search">Condition</th>
<th class="search">Value</th>


<%
int W = 7;


SearchElement[] tableElements = searchBean.getTableElements();

for(int i= 0; i < tableElements.length; ++i)
{
	if (i > 0)
	{
		GroupOperator gop = tableElements[i].getOperator();
		String[] gopValues = searchBean.getGopValues();
		String gopID = "gop"+i;
%>

<tr>
	<td id="td<%=gopID%>" colspan="<%=W%>" align="<%=gop.isGroup()?"center":"left"%>" class="<%=gop.isGroup()?"search-group":"search-element"%>">
	<select name="<%=gopID%>"  id="<%=gopID%>" onchange="alignGop('<%=gopID%>')" class="<%=gop.isGroup()?"search-group":"search-element"%>">
	<%
	for(int j=0; j < gopValues.length; ++j)
	{
	%>
	<option value="<%=gopValues[j]%>" <%=gopValues[j].equals(gop.getName())? "selected='selected'":""%> ><%=gopValues[j]%></option>
	<%
	}
	%>
</select>

	</td>
</tr>
<%
	}//if

	boolean isNotFlag = tableElements[i].isNotFlag();
	String nfID = "nf"+i;
	String[] nfValues = searchBean.nfValues;

%>
<tr>

<td>&nbsp;&nbsp;&nbsp;&nbsp;</td>

<td>
	<select name="<%=nfID%>"  id="<%=nfID%>">
	<%
	for(int j=0; j < nfValues.length; ++j)
	{
	%>
	<option value="<%=nfValues[j]%>"  <%=isNotFlag && (j == 1) ? "selected='selected'":""%> > <%=nfValues[j]%> </option>
	<%
	}
	%>
</td>


<%
	String typeID = "type"+i;
	String typeValue = tableElements[i].getType();
	String value1ID = "value1_"+i;
	String value2ID = "value2_"+i;


%>

<td>
	<select name="<%=typeID%>"  id="<%=typeID%>" onChange="makeValue1Visible('<%=typeID%>', '<%=value1ID%>')">
	<%
	for(int j=0; j < typeValues.length; ++j)
	{
	%>
	<option value="<%=typeValues[j]%>"  <%=typeValues[j].equals(typeValue) ? "selected='selected'":""%>> <%=typeValues[j]%> </option>
	<%
	}
	%>
</td>


<%
	String value1value = tableElements[i].getValue1();
%>



<td>
  <input class="search-input" id="<%=value1ID%>"  name="<%=value1ID%>" value="<%=value1value%>"
  <%=searchBean.showValue1(typeValue) ? "" : "disabled='true'"%>  onClick="jscleanAny('<%=value1ID%>', '<%=ISearchConstants.ANY%>')"  />
</td>


<%
	String opTypeID = "opType"+i;
	String opTypeValue = tableElements[i].getOpType2();
	String[] opTypeValues = searchBean.opTypeValues();


%>

<td>
	<select name="<%=opTypeID%>"  id="<%=opTypeID%>">
	<%
	for(int j=0; j < opTypeValues.length; ++j)
	{
	%>
	<option value="<%=opTypeValues[j]%>"  <%=opTypeValues[j].equals(opTypeValue) ? "selected='selected'":""%> > <%=opTypeValues[j]%> </option>
	<%
	}
	%>
</td>

<%

	String value2value = tableElements[i].getValue2();


%>



<td>
  <input class="search-input" id="<%=value2ID%>"  name="<%=value2ID%>" value="<%=value2value%>" onClick="jscleanAny('<%=value2ID%>', '<%=ISearchConstants.ANY%>')" >
</td>


<td>
	<a href="javascript:img_action_click('add<%=i%>')" title="Add Condition"><img src="webresource/images/add_obj.gif" onClick=""></a>
<%if(i > 0){%>
	<a href="javascript:img_action_click('delete<%=i%>')" title="Remove Condition"><img src="webresource/images/delete.gif" onClick=""></a>
<%}%>
</td>


</tr>

<%
}
%>

</table>

</fieldset>



<%@include file="advColumnSearch.jspf"%>




<input type="submit" value="Search" name="Search" />

<input id="img_action" name="img_action" value="" style="visibility:hidden"/>

</form>


<%

	if (studio.getModel() == null || !studio.getModel().isReady())
	{
%>
		<p class="warning">There is no valid projects in workspace. Or your session have expired and you need to reload project</p>

<%
	}
	else if (request.getParameter("Search") != null)
	{
   		Object res =  studio.getModel().runSearch(searchBean);

%>
  		<%=studio.getModel().displayResult(res)%>
    <script type="text/javascript" src="webresource/javascript/popup/popupmenu.js"></script>
<script type="text/javascript">
    function cellMouseOver(td, event) {
        PopupMenu.sheduleShowMenu('contextMenu', event, 700);
    }

    function cellMouseOut(td) {
        PopupMenu.cancelShowMenu();
    }
    function triggerEdit(f) {
        var lastTarget = $(PopupMenu.lastTarget)
        var uri = lastTarget.down('input').value;
        f.elementID.value = lastTarget.id.split("-")[1];
        f.cell.value = uri.toQueryParams().cell;
        f.submit();
    }
    function triggerEditXls(f) {
        f.uri.value = $(PopupMenu.lastTarget).down('input').value;
        f.submit();
    }
    function triggerSearch(f) {
        f.searchQuery.value = $A($(PopupMenu.lastTarget).childNodes).find(function(s) {return s.nodeName == "#text"})
                .nodeValue.sub(/^[.;,! \t\n()^&*%=?\-'"+<>]+/, "").split(/[.;,! \t\n()^&*%=?\-'"+<>]+/, 3)
                .reject(function(s) {return !s}).join(" ");
       f.submit();
    }
</script>

<form name="editForm" action="${pageContext.request.contextPath}/jsp/tableeditor/showTableEditor2.jsf">
<input type="hidden" name="elementID" value="">
<input type="hidden" name="cell" value="">
<input type="hidden" name="view" value="<%=studio.getModel().getTableView(request.getParameter("view"))%>" />
</form>
<form name="editFormXls" action="${pageContext.request.contextPath}/jsp/showLinks.jsp" target="show_app_hidden">
    <input type="hidden" name="uri" value="">
</form>
<form name="searchForm" action="${pageContext.request.contextPath}/jsp/search/search.jsp">
    <input type="hidden" name="searchQuery" value="">
</form>

<div id="contextMenu" style="display:none;">
    <table cellpadding="1px">
        <%if (!studio.getModel().isReadOnly()) {%>
        <tr><td><a href="javascript:triggerEdit(document.forms.editForm)">Edit</a></td></tr>
        <tr><td><a href="javascript:triggerEditXls(document.forms.editFormXls)">Edit in Excel</a></td></tr>
        <%} else {%>
        <tr><td class="da">Edit</td></tr>
        <tr><td class="da">Edit in Excel</td></tr>
        <%}%>
        <tr><td><a href="javascript:triggerSearch(document.forms.searchForm)">Search</a></td></tr>
    </table>
</div>
<%
	}
%>



</body>
</html>

