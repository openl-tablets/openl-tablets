<script type="text/javascript">
    function errorClicked(uri) {
        var f = document.forms.errorForm;
        f.cell.value = uri.toQueryParams().range;
        f.submit();
    }
</script>

<form name="errorForm" action="${pageContext.request.contextPath}/jsp/tableeditor/showTableEditor2.jsf">
<input type="hidden" name="elementID" value="<%=elementID%>">
<input type="hidden" name="cell" value="">
<input type="hidden" name="view" value="<%=org.openl.rules.dt.IDecisionTableConstants.VIEW_DEVELOPER%>" />
</form>
