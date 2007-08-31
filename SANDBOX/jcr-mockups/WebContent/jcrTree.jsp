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
	
		<rich:panel bodyClass="inpanelBody">
			<f:facet name="header">
				<h:outputText value="Repository" />
			</f:facet>

			<rich:tree style="width:300px" 
			switchType="server" value="#{repositoryHandler.data}" var="item" nodeFace="#{item.type}" >
				<rich:treeNode type="project" icon="#{item.icon}" iconLeaf="#{item.iconLeaf}">
					<h:outputText value="#{item.name}" />
				</rich:treeNode>
				<rich:treeNode type="folder" icon="#{item.icon}" iconLeaf="#{item.iconLeaf}">
					<h:outputText value="#{item.name}" />
				</rich:treeNode>
				<rich:treeNode type="file" icon="#{item.icon}" iconLeaf="#{item.iconLeaf}">
					<h:outputText value="#{item.name}" />
				</rich:treeNode>
			</rich:tree>
		</rich:panel>
		
		<rich:panel bodyClass="inpanelBody">
			<f:facet name="header">
				<h:outputText value="Properties of Elements" />
			</f:facet>

			<rich:tabPanel style="width:600px">
				<rich:tab label="Properties">
				<f:subview id="props">
					<h:panelGrid columns="2">
						<h:outputLabel for="p1" styleClass="niceLabel">
							<h:outputText value="Effective Date:"/>
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
				</f:subview>
				</rich:tab>
				<rich:tab label="Versions">
				
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
				<h:outputText value="#{ver.versionName}"/>
			</rich:column>
			<rich:column>
				<h:outputText value="#{ver.lastModified}"/>
			</rich:column>
			<rich:column>
				<h:outputText value="#{ver.user}"/>
			</rich:column>
			<rich:column>
				<h:outputText value="#{ver.comments}"/>
			</rich:column>
		</rich:columnGroup>
</rich:dataTable>
					
				</rich:tab>
				<rich:tab label="Elements">
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
				<h:outputText value="#{el.name}"/>
			</rich:column>
			<rich:column>
				<h:outputText value="#{el.version}"/>
			</rich:column>
			<rich:column>
				<h:outputText value="#{el.lastModified}"/>
			</rich:column>
			<rich:column>
				<h:outputText value="#{el.lastModifiedBy}"/>
			</rich:column>
		</rich:columnGroup>
</rich:dataTable>
				</rich:tab>
			</rich:tabPanel>
		</rich:panel>
	</h:panelGrid>
</div>

		</h:form>
</f:view>

</jsp:root>
