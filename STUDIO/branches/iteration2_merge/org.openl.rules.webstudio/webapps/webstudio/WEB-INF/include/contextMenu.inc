<%@ page import = "org.openl.rules.webstudio.web.util.WebStudioUtils" %>
<%@ page import="org.openl.rules.util.net.NetUtils" %>
<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/popup/popupmenu.js"></script>
<script type="text/javascript">
    function openMenu(menuId, td, event) {
        if (!menuId) menuId = 'contextMenu';
        PopupMenu.sheduleShowMenu(menuId, event, 400);
    }

    function closeMenu(td) {
        PopupMenu.cancelShowMenu();
    }

    function triggerEdit(f) {
        var uri = $(PopupMenu.lastTarget).down('input').value;
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

<form name="editForm" action="${pageContext.request.contextPath}/faces/facelets/tableeditor/showTable.xhtml">
<input type="hidden" name="uri" value="<%=uri%>">
<input type="hidden" name="mode" value="edit">
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
        <%if (studio.getModel().isReadOnly()) {%>
        <tr><td class="da">Edit</td></tr>
        <tr><td class="da">Edit in Excel</td></tr>
        <%} else {%>
        <tr><td><a href="javascript:triggerEdit(document.forms.editForm)">Edit</a></td></tr>
        <tr><td><a href="javascript:triggerEditXls(document.forms.editFormXls)">Edit in Excel</a></td></tr>
        <%}%>
        <tr><td><a href="javascript:triggerSearch(document.forms.searchForm)">Search</a></td></tr>
    </table>
</div>