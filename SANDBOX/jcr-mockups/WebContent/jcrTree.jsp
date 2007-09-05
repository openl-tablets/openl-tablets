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
			<h:outputText value="Repository" />
		</f:facet>
<!--
		<h:selectOneMenu value="prj1" style="width:300px">
			<f:selectItem itemValue="prj1" />
			<f:selectItem itemValue="prj2" />
			<f:selectItem itemValue="prj3" />
		</h:selectOneMenu>
-->

		<rich:tree style="width:300px" switchType="server"
			value="#{repositoryHandler.data}" var="item" nodeFace="#{item.type}">
			<rich:treeNode type="project" icon="#{item.icon}"
				iconLeaf="#{item.iconLeaf}">
				<h:outputText value="#{item.name}" />
			</rich:treeNode>
			<rich:treeNode type="folder" icon="#{item.icon}"
				iconLeaf="#{item.iconLeaf}">
				<h:outputText value="#{item.name}" />
			</rich:treeNode>
			<rich:treeNode type="file" icon="#{item.icon}"
				iconLeaf="#{item.iconLeaf}">
				<h:outputText value="#{item.name}" />
			</rich:treeNode>
		</rich:tree>
	</rich:panel>

</jsp:root>
