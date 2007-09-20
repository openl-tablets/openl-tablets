<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c"%>

<html>

<head>
<meta http-equiv="Content-Type" content="text/html; charset=windows-1257">
<title></title>
<link href="../css/style1.css" rel="stylesheet" type="text/css">
<link href="../css/tableEditor.css" rel="stylesheet" type="text/css">

</head>

<body>

<f:view>
  <script type="text/javascript" src="<h:outputText value='#{facesContext.externalContext.request.contextPath}'/>/javascript/prototype/prototype-1.5.1.js"></script>
  <script type="text/javascript" src="<h:outputText value='#{facesContext.externalContext.request.contextPath}'/>/javascript/TableEditor.js"></script>
  <script type="text/javascript" src="<h:outputText value='#{facesContext.externalContext.request.contextPath}'/>/javascript/BaseEditor.js"></script>
  <script type="text/javascript" src="<h:outputText value='#{facesContext.externalContext.request.contextPath}'/>/javascript/TextEditor.js"></script>


  <div id="tableEditor"/>

  <script>
    var tableEditor = new TableEditor("tableEditor", "<h:outputText value='#{facesContext.externalContext.request.contextPath}'/>/jsp/tableeditor/load.jsf?elementID=3");
    tableEditor.saveUrl = "<h:outputText value='#{facesContext.externalContext.request.contextPath}'/>/jsp/tableeditor/save.jsf";
  </script>
</f:view>

</body>
</html>