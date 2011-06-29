<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="f" uri="http://java.sun.com/jsf/core" %>
<%@ taglib prefix="h" uri="http://java.sun.com/jsf/html" %>

<html>
<head>
    <title>Web service interface</title>
    <style type="text/css">
        .messages ul {
            margin: 0 0 10px 5px;
            padding: 0;
            list-style-type: none;
        }

        .error {
            color: red;
        }
    </style>
</head>
<body>

<f:view>
    <h:form>
        <div 
            <h2>Call methods</h2>
            <table border="0">
                <tr>
                    <td>Coverages:</td>
                    <td>
                        <h:commandButton value="execute" action="#{wsbean.getCoverage}" immediate="true"/>
                    </td>
                </tr>
                <tr>
                    <td>Theft Ratings:</td>
                    <td>
                        <h:commandButton value="execute" action="#{wsbean.getTheftRating}" immediate="true"/>
                    </td>
                </tr>
                <tr>
                    <td nowrap="nowrap">Driver Age Type Decision Table(age = <h:inputText size="4" value="#{wsbean.param1}"/>, gender = <h:inputText size="6" value="#{wsbean.param2}"/>):</td>
                    <td>
                        <h:commandButton value="execute" action="#{wsbean.driverAgeType}"/>
                    </td>
                </tr>
            </table>
        </div>

<p/><p/>



        <div>
            <h:panelGroup rendered="#{not empty wsbean.methodName}">
                <h:outputText value="<p/><p/><p/><p/><h2> Result for <i>#{wsbean.methodName}</i> method</h2>" escape="false"/>
                <h:dataTable value="#{wsbean.result}" var="s">
                    <h:column>
                        <h:outputText value="#{s}"/>
                    </h:column>
                </h:dataTable>
            </h:panelGroup>

            <h:panelGroup styleClass="messages">
                <h:messages errorClass="error"/>
            </h:panelGroup>
        </div>
    </h:form>
</f:view>


</body>
</html>