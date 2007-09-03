<?xml version="1.0" encoding="UTF-8"?>
<jsp:root version="1.2" 
	xmlns:f="http://java.sun.com/jsf/core" 
	xmlns:h="http://java.sun.com/jsf/html" 
	xmlns:jsp="http://java.sun.com/JSP/Page" 
	xmlns:a4j="https://ajax4jsf.dev.java.net/ajax" 
	xmlns:rich="http://richfaces.ajax4jsf.org/rich" 
>
<jsp:directive.page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8"/>

	<h:panelGrid columns="2">
		<h:outputLabel for="p1" styleClass="niceLabel">
			<h:outputText value="Effective Date:" />
		</h:outputLabel>
		<h:inputText id="p1" value="09/01/2007"></h:inputText>

		<h:outputLabel for="p2" styleClass="niceLabel">
			<h:outputText value="Expiration Date:" />
		</h:outputLabel>
		<h:inputText id="p2" value="01/01/2008"></h:inputText>

		<h:outputLabel for="p3" styleClass="niceLabel">
			<h:outputText value="Status:" />
		</h:outputLabel>
		<h:selectOneMenu id="p3" value="DRAFT">
			<f:selectItem itemValue="INITIAL" />
			<f:selectItem itemValue="DRAFT" />
			<f:selectItem itemValue="PRODUCTION" />
			<f:selectItem itemValue="OBSOLETE" />
		</h:selectOneMenu>
	</h:panelGrid>

</jsp:root>
