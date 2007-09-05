<?xml version="1.0" encoding="UTF-8"?>
<jsp:root version="1.2" 
	xmlns:f="http://java.sun.com/jsf/core" 
	xmlns:h="http://java.sun.com/jsf/html" 
	xmlns:jsp="http://java.sun.com/JSP/Page" 
	xmlns:a4j="https://ajax4jsf.dev.java.net/ajax" 
	xmlns:rich="http://richfaces.ajax4jsf.org/rich" 
>
<jsp:directive.page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8"/>

<f:view>
<style>
  .gridContent {
  	vertical-align:top;
  }
  
  .outpanelHeader {
  	height:4px;
  	border-width: 0;
  }
  .inpanelBody {
    height:300px;
    overflow:auto;
  }
  .niceLabel {
    font-size:12px;
  }
  
  BODY {
    scrollbar-base-color: #BED6F8;
    scrollbar-shadow-color: #F2F7FF;
    scrollbar-highlight-color: #F2F7FF;
    scrollbar-arrow-color: #000000;
    scrollbar-darkshadow-color: #C6DEFF;
    scrollbar-3dlight-color: #C6DEFF;
  }
  
</style>
		<h:form>
<div>
			<h:outputText>Back &lt;&lt;&lt;</h:outputText>
			<h:commandButton value="index.jsp" action="index"/>
</div>
			<rich:spacer height="10"/>
			<rich:separator height="6" />
			<rich:spacer height="10"/>
		
<div class="sample-container" >
	<h:panelGrid columns="2" headerClass="outpanelHeader">
	
		<f:subview id="svTree">
			<jsp:include page="jcrTree.jsp"/>
		</f:subview>

		<f:subview id="svProps">
			<jsp:include page="jcrProps.jsp"/>
		</f:subview>
		
	</h:panelGrid>
</div>

		</h:form>
</f:view>

</jsp:root>
