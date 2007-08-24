package org.apache.jsp;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.jsp.*;
import org.openl.rules.ui.*;

public final class tree_jsp extends org.apache.jasper.runtime.HttpJspBase
    implements org.apache.jasper.runtime.JspSourceDependent {

  private static java.util.List _jspx_dependants;

  public Object getDependants() {
    return _jspx_dependants;
  }

  public void _jspService(HttpServletRequest request, HttpServletResponse response)
        throws java.io.IOException, ServletException {

    JspFactory _jspxFactory = null;
    PageContext pageContext = null;
    HttpSession session = null;
    ServletContext application = null;
    ServletConfig config = null;
    JspWriter out = null;
    Object page = this;
    JspWriter _jspx_out = null;
    PageContext _jspx_page_context = null;


    try {
      _jspxFactory = JspFactory.getDefaultFactory();
      response.setContentType("text/html");
      pageContext = _jspxFactory.getPageContext(this, request, response,
      			null, true, 8192, true);
      _jspx_page_context = pageContext;
      application = pageContext.getServletContext();
      config = pageContext.getServletConfig();
      session = pageContext.getSession();
      out = pageContext.getOut();
      _jspx_out = out;

      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("<html>\r\n");
      out.write("<head>\r\n");
      out.write("<title>Tree Sample</title>\r\n");
      out.write("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=iso-8859-1\">\r\n");
      out.write("<link rel=\"stylesheet\" type=\"text/css\" href=\"css/dtree.css\"></link>\r\n");
      out.write("<link rel=\"stylesheet\" type=\"text/css\" href=\"css/style1.css\"></link>\r\n");
      out.write("<script language=\"JavaScript\" type=\"text/JavaScript\" src=\"dtree.js\"></script>\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("<style type=\"text/css\">\r\n");
      out.write("\r\n");
      out.write("BODY {\r\n");
      out.write("\tpadding: 0;\r\n");
      out.write("\tmargin: 0 0 0 10px;\r\n");
      out.write("\tbackground: #eceef8;\r\n");
      out.write("\t}\r\n");
      out.write("\r\n");
      out.write("\t\r\n");
      out.write("#tree {\r\n");
      out.write("\ttext-overflow: ellipsis; \r\n");
      out.write("\toverflow : hidden\r\n");
      out.write("\twidth: 100%;\r\n");
      out.write("\theight: 100%;\r\n");
      out.write("\t}\r\n");
      out.write("\r\n");
      out.write("</style>\r\n");
      out.write("</head>\r\n");
      out.write("<body>\r\n");
      out.write("\r\n");
      org.openl.rules.ui.WebStudio studio = null;
      synchronized (session) {
        studio = (org.openl.rules.ui.WebStudio) _jspx_page_context.getAttribute("studio", PageContext.SESSION_SCOPE);
        if (studio == null){
          studio = new org.openl.rules.ui.WebStudio();
          _jspx_page_context.setAttribute("studio", studio, PageContext.SESSION_SCOPE);
        }
      }
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("<p/><p/>\r\n");
      out.write("\r\n");
      out.write("<table width=95% style=\"border-style: solid; border-width: 1;\">\r\n");
      out.write("<tr>\r\n");
      out.write("<td>\r\n");

	if (studio.getModel().getAllTestMethods() != null && studio.getModel().getAllTestMethods().getTests().length > 0)
	{

      out.write("\r\n");
      out.write("<a href=\"jsp/runAllTests.jsp\" target=\"mainFrame\" title=\"Run All Tests\"><img border=\"0\" src=\"images/test_ok.gif\"/></a>\r\n");
      out.write("\r\n");

	}

      out.write("\r\n");
      out.write("<a href=\"index.jsp?reload=true\" title=\"Refresh Project\" target=\"_top\"><img border=0 src=\"");
      out.print( request.getContextPath());
      out.write("/images/refresh.gif\"></a>\r\n");
      out.write("\r\n");
      out.write("</td>\r\n");
      out.write("\r\n");
      out.write("<td align=right >\r\n");
      out.write("<a href=\"javascript: d.openAll(); d.o(0);\" title=\"Expand All\"><img border=\"0\" src=\"images/expandall.gif\"/></a>\r\n");
      out.write("<a href=\"javascript: d.closeAll()\" title=\"Collapse All\" > <img border=\"0\" src=\"images/collapseall.gif\"/></a>\r\n");
      out.write("\r\n");
      out.write("</td>\r\n");
      out.write("</tr></table>\r\n");
      out.write("\r\n");
      out.write("<p/>\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("<div class=\"errmsg\" id=\"msg\">\r\n");
      out.write("</div>\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("<div class=\"dtree\" id=\"tree\">\r\n");
      out.write("</div>\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("<script language=\"JavaScript\" defer=\"defer\">\r\n");
      out.write("d = new dTree('d');\r\n");
      out.write("\r\n");
      out.print(studio.getModel().renderTree("jsp/showTable.jsf"));
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("document.getElementById('tree').innerHTML = d;\r\n");
      out.write("</script>              \r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("</body>\r\n");
      out.write("</html>");
    } catch (Throwable t) {
      if (!(t instanceof SkipPageException)){
        out = _jspx_out;
        if (out != null && out.getBufferSize() != 0)
          out.clearBuffer();
        if (_jspx_page_context != null) _jspx_page_context.handlePageException(t);
      }
    } finally {
      if (_jspxFactory != null) _jspxFactory.releasePageContext(_jspx_page_context);
    }
  }
}
