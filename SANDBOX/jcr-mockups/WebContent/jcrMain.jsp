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
  .smlBackButton {
  	font-size: 9px;
    position: absolute;
    top: 2px;
    left: 2px;
  }

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
/*    background-color:#f1f1f1; */
  }
  .columnTree {
  }
  .columnProps {
  	width: 100%;
  }
  .niceLabel {
    font-size:12px;
  }

  .masthead {
	width: 100%;
	height: 70px;
	background-image: url(../images/header_logo.jpg);
	background-repeat: no-repeat;
	border-bottom: 1px solid #CCCCCC;
	min-width: 800px;
	max-width: 1400px;
	position: relative;
  }
  
  .logo {
    position: absolute;
    top: 10px;
    right: 40px;
  }

  BODY {
	background: #eceef8;
  }
  
  .dr-pnl-h, .dr-tbpnl-tb-act, .dr-tbpnl-tb-inact, .dr-table-headercell {
    background-image: none;
  }
  
<!--
    scrollbar-base-color: #BED6F8;
    scrollbar-shadow-color: #F2F7FF;
    scrollbar-highlight-color: #F2F7FF;
    scrollbar-arrow-color: #000000;
    scrollbar-darkshadow-color: #C6DEFF;
    scrollbar-3dlight-color: #C6DEFF;
-->
</style>
		<h:form>

<div class="masthead">
	<f:verbatim>&amp;nbsp;</f:verbatim>
		<h:commandButton styleClass="smlBackButton" value="index.jsp" action="index"/>
</div>
<h:graphicImage styleClass="logo" url="/images/exigen_logo.gif"/>

			<rich:spacer height="10"/>
			<rich:separator height="6" />
			<rich:spacer height="10"/>
		
<div class="sample-container" >
	<h:panelGrid columns="2" headerClass="outpanelHeader" cellspacing="10" columnClasses="columnTree, columnProps">
	
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
