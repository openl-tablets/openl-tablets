package org.openl.rules.ruleservice.servlet;

import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;

import org.apache.cxf.transport.servlet.CXFServlet;

@WebServlet(value = "/*", loadOnStartup = 1, initParams = {
        @WebInitParam(name = "static-welcome-file", value = "/index.html"),
        @WebInitParam(name = "static-resources-list", value = "/favicon.ico"),
        @WebInitParam(name = "service-list-path", value = "cxf-services"),
        @WebInitParam(name = "hide-service-list-page", value = "true") })
public class WSServlet extends CXFServlet {
}