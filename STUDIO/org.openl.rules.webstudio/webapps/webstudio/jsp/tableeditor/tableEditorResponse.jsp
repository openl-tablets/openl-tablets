<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c"%>
<%response.setHeader( "Pragma", "no-cache" );response.setHeader( "Cache-Control", "no-cache" );response.setDateHeader( "Expires", 0 );%>
<f:view>
  <h:outputText value='#{tableEditorController.response}' escape="false" />
</f:view>
