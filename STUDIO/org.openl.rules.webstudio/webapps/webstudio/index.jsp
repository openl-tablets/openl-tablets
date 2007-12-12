<?xml version="1.0" encoding="UTF-8"?>
  <jsp:root version="2.0"
  xmlns:jsp="http://java.sun.com/JSP/Page">

  <jsp:output omit-xml-declaration="true"
    doctype-root-element="HTML"
    doctype-system="http://www.w3.org/TR/html4/strict.dtd"
    doctype-public="-//W3C//DTD HTML 4.01//EN"/>

  <jsp:directive.page contentType="text/html; charset=UTF-8"/>

  <jsp:scriptlet>
    response.sendRedirect("repository/main.xhtml");
  </jsp:scriptlet>

</jsp:root>