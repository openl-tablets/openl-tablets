<!DOCTYPE html>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<html>

    <c:set var="contextPath" value="${pageContext.request.contextPath}"/>

    <head>
        <meta charset="UTF-8" />

        <title>OpenL Tablets Web Services</title>

        <link href="${contextPath}/css/common.css" rel="stylesheet" />

        <style>
            html {
                font-family: verdana, helvetica, arial, sans-serif;
            }
            .date-time {
                padding-left: 3px;
                color: #9a9a9a;
            }
            a {
                padding: 3px;
                color: #0078D0;
            }

            .service {
                color: #444;
                white-space: nowrap;
                border-bottom: #cccccc dotted 1px;
                padding: 10px 0;

            }
            .service:last-child {
                border: 0;
            }

            .service-description {
                min-height: 16px;
                padding-top: 3px;
                padding-bottom: 3px;
            }

            .expand-button, .collapse-button {
                margin: 0;
                cursor: pointer;
                width: 16px;
                height: 16px;
                display: inline-block;
                vertical-align: bottom;
            }

            .expand-button {
                background: url(${contextPath}/images/plus.png) no-repeat center;
            }

            .collapse-button {
                background: url(${contextPath}/images/minus.png) no-repeat center;
            }

            .methods {
                margin-left: 29px;
                margin-top: 2px;
                display: none;
            }

            .methods > div {
                background: url(${contextPath}/images/bullet.png) no-repeat left center;
                min-height: 16px;
                padding-left: 18px;
                padding-top: 1px;
                padding-bottom: 1px;
                font-size: 11px;
                vertical-align: middle;
            }
            .collapse-button ~ .methods {
                display: block;
            }
        </style>

        <script>
            function pressButton(button) {
                if (button.className == "expand-button") {
                    // Expand the node
                    button.className = "collapse-button";
                } else {
                    // Collapse the node
                    button.className = "expand-button";
                }
            }
        </script>
    </head>

    <body>
        <div id="header">OpenL Tablets Web Services</div>
        <div id="main">
            <div>
                <form>
                    <div>
                        <c:if test="${requestScope.services == null}">
                            <c:redirect url="/"/>
                        </c:if>
                        
                        <c:if test="${!empty requestScope.services}">
                            <h2>Available services:</h2>
                                <c:forEach items="${services}" var="service">
                                        <div class="service">
                                            <span class="date-time">Started time: <fmt:formatDate value="${service.startedTime}" pattern="MM/dd/yyyy hh:mm:ss a"/></span>

                                                <div class="service-description">
                                                    <span class="expand-button" onclick="pressButton(this);"></span>
                                                    <span class="service-name"><c:out value="${service.name}"/></span>
                                                    <div class="methods">
                                                        <c:forEach items="${service.methodNames}" var="methodName">
                                                            <div><c:out value="${methodName}"/></div>
                                                        </c:forEach>
                                                    </div>
                                                </div>
                                                <c:forEach items="${service.serviceResources}" var="serviceResource">
                                                    <a href="${serviceResource.url.contains('://') ? '' : contextPath.concat('/')}${serviceResource.url}">${serviceResource.name}</a>
                                                </c:forEach>
                                        </div>
                                    </c:forEach>
                                </tbody>
                            </table>
                        </c:if>
                        <c:if test="${empty requestScope.services}">
                            <h2>There are no available services</h2>
                        </c:if>
                    </div>

                </form>
            </div>
        </div>
        <div id="footer">
            &#169; 2018 <a style="text-decoration: none" href="http://openl-tablets.org" target="_blank">OpenL Tablets</a>
        </div>
    </body>
</html>