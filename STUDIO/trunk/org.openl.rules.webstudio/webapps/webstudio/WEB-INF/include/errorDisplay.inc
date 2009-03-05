<script type="text/javascript">
    function errorClicked(uri) {
        var f = document.forms.errorForm;
        f.cell.value = uri.toQueryParams().range;
        f.submit();
    }
</script>

<form name="errorForm" action="${pageContext.request.contextPath}/faces/facelets/tableeditor/showTable.xhtml">
    <input type="hidden" name="mode" value="edit"></input>
    <input type="hidden" name="elementID" value="<%=elementID%>">
    <input type="hidden" name="cell" value="">
    <input type="hidden" name="view" value="view.developer"></input>
</form>