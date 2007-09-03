<?xml version="1.0" encoding="UTF-8"?>
<jsp:root version="1.2" 
	xmlns:f="http://java.sun.com/jsf/core" 
	xmlns:h="http://java.sun.com/jsf/html" 
	xmlns:jsp="http://java.sun.com/JSP/Page" 
	xmlns:a4j="https://ajax4jsf.dev.java.net/ajax" 
	xmlns:rich="http://richfaces.ajax4jsf.org/rich" 
>
<jsp:directive.page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8"/>

	<rich:dataTable value="#{versionHandler.versions}" var="ver">
		<f:facet name="header">
			<rich:columnGroup>
				<rich:column>
					<h:outputText value="Actions" />
				</rich:column>
				<rich:column>
					<h:outputText value="Version" />
				</rich:column>
				<rich:column>
					<h:outputText value="Date Time" />
				</rich:column>
				<rich:column>
					<h:outputText value="User" />
				</rich:column>
				<rich:column>
					<h:outputText value="Comments" />
				</rich:column>
			</rich:columnGroup>
		</f:facet>

		<rich:columnGroup>
			<rich:column>
				<f:verbatim>---</f:verbatim>
			</rich:column>
			<rich:column>
				<h:outputText value="#{ver.versionName}" />
			</rich:column>
			<rich:column>
				<h:outputText value="#{ver.lastModified}" />
			</rich:column>
			<rich:column>
				<h:outputText value="#{ver.user}" />
			</rich:column>
			<rich:column>
				<h:outputText value="#{ver.comments}" />
			</rich:column>
		</rich:columnGroup>
	</rich:dataTable>

</jsp:root>
