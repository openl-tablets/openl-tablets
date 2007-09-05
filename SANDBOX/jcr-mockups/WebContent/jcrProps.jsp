<?xml version="1.0" encoding="UTF-8"?>
<jsp:root version="1.2" 
	xmlns:f="http://java.sun.com/jsf/core" 
	xmlns:h="http://java.sun.com/jsf/html" 
	xmlns:jsp="http://java.sun.com/JSP/Page" 
	xmlns:a4j="https://ajax4jsf.dev.java.net/ajax" 
	xmlns:rich="http://richfaces.ajax4jsf.org/rich" 
>
<jsp:directive.page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8"/>

		<rich:panel bodyClass="inpanelBody">
			<f:facet name="header">
				<h:outputText value="Properties of Elements" />
			</f:facet>

			<rich:tabPanel>
				<rich:tab label="Properties">
				
					<jsp:include page="jcrPropProject.jsp"/>

				</rich:tab>
				<rich:tab label="Versions">
				
					<jsp:include page="jcrPropVersions.jsp"/>
					
				</rich:tab>
				<rich:tab label="Elements">

					<jsp:include page="jcrPropElements.jsp"/>

				</rich:tab>
			</rich:tabPanel>
		</rich:panel>

</jsp:root>
