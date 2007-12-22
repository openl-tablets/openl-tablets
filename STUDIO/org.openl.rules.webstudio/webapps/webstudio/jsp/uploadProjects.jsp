<%@ page import="org.openl.rules.webstudio.web.util.WebStudioUtils" %>
<%@ page import="org.openl.meta.DoubleValue" %>
<%@ page import="org.openl.rules.ui.Explanation" %>
<%@ page import="org.openl.rules.ui.OpenLWrapperInfo" %>
<%@ page import="org.openl.rules.webtools.ExcelLauncher" %>
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://richfaces.ajax4jsf.org/rich" prefix="rich" %>


<jsp:useBean id='studio' scope='session' class="org.openl.rules.ui.WebStudio" />
<%@include file="checkTimeout.jsp"%>

<%if (!WebStudioUtils.isLocalRequest(request)) {%>
<script type="text/javascript">alert("This action is available only from the machine server runs at.")</script>
<%  return;} %>

<html>
  <head>
      <title>Simple jsp page</title>
      <link href="../css/style1.css" rel="stylesheet" type="text/css">
      <link href="../css/default.css" rel="stylesheet" type="text/css">
  </head>
  <body style="padding-left:5px;">
  <f:view>
    <h:form>
    <h2><span class="success">Upload selected Projects</span></h2>

    <rich:dataTable value="#{localUpload.projects4Upload}" rendered="#{not empty localUpload.projects4Upload}" var="item" cellspacing="0px" width="600" styleClass="standard" rowClasses="odd,even" >
      <f:facet name="header">
        <rich:columnGroup>
          <rich:column><h:outputText value="Selected" /></rich:column>
          <rich:column><h:outputText value="Name" /></rich:column>
        </rich:columnGroup>
      </f:facet>

      <rich:column style="text-align:center; width: 20px;"><h:selectBooleanCheckbox value="#{item.selected}" /></rich:column>
      <rich:column><h:outputText value="#{item.projectName}" /></rich:column>
    </rich:dataTable>

    <br />
    <h:commandButton styleClass="button" action="#{localUpload.upload}" rendered="#{not empty localUpload.projects4Upload}" value="Upload" />
    <h:outputText value="All projects are already in repository" rendered="#{empty localUpload.projects4Upload}" />

  </h:form>
      <h:messages infoClass="success" errorClass="error" showDetail="true" showSummary="false" tooltip="true" />
  </f:view>
  </body>
</html>