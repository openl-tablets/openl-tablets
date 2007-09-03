<?xml version="1.0" encoding="UTF-8"?>
<jsp:root version="1.2" 
	xmlns:f="http://java.sun.com/jsf/core" 
	xmlns:h="http://java.sun.com/jsf/html" 
	xmlns:jsp="http://java.sun.com/JSP/Page" 
	xmlns:a4j="https://ajax4jsf.dev.java.net/ajax" 
	xmlns:rich="http://richfaces.ajax4jsf.org/rich" 
>
<jsp:directive.page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8"/>

	<rich:dataTable value="#{elementHandler.elements}" var="el">
		<f:facet name="header">
			<rich:columnGroup>
				<rich:column>
					<h:outputText value="Actions" />
				</rich:column>
				<rich:column>
					<h:outputText value="Folder/File" />
				</rich:column>
				<rich:column>
					<h:outputText value="Version" />
				</rich:column>
				<rich:column>
					<h:outputText value="Last Modified" />
				</rich:column>
				<rich:column>
					<h:outputText value="Modified By" />
				</rich:column>
			</rich:columnGroup>
		</f:facet>

		<rich:columnGroup>
			<rich:column>
				<f:verbatim>---</f:verbatim>
			</rich:column>
			<rich:column>
				<h:outputText value="#{el.name}" />
			</rich:column>
			<rich:column>
				<h:outputText value="#{el.version}" />
			</rich:column>
			<rich:column>
				<h:outputText value="#{el.lastModified}" />
			</rich:column>
			<rich:column>
				<h:outputText value="#{el.lastModifiedBy}" />
			</rich:column>
		</rich:columnGroup>
	</rich:dataTable>

</jsp:root>
