<script type="text/javascript">
    function editError(uri, cell) {
        var f = document.forms.errorForm;
        f.uri.value = uri;
        f.cell.value = cell;
        f.submit();
    }
</script>

<form name="errorForm" action="${pageContext.request.contextPath}/faces/facelets/tableeditor/showTable.xhtml">
    <input type="hidden" name="mode" value="edit"></input>
    <input type="hidden" name="uri" value="">
    <input type="hidden" name="cell" value="">
    <input type="hidden" name="view" value="view.developer"></input>
</form>