package org.apache.jsp;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.jsp.*;
import org.openl.rules.ui.*;

public final class header_jsp extends org.apache.jasper.runtime.HttpJspBase
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
      out.write("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" />\r\n");
      out.write("<title>OpenL Web Studio</title>\r\n");
      out.write("<link rel=\"stylesheet\" type=\"text/css\" href=\"");
      out.print( request.getContextPath() );
      out.write("/css/style1.css\"></link>\r\n");
      out.write("<link rel=\"stylesheet\" type=\"text/css\" href=\"");
      out.print( request.getContextPath() );
      out.write("/css/2col_leftNav.css\"></link>\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("</head>\r\n");
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
      out.write("<body>\r\n");
      out.write("<map id=\"menumap\" name=\"menumap\">\r\n");
      out.write("\r\n");
      out.write("<area shape=\"rect\" \r\n");
      out.write("coords=\"49,7,218,58\" \r\n");
      out.write("alt=\"OpenL Tablets Project on Sourceforge\"\r\n");
      out.write("href=\"http://openl-tablets.sourceforge.net/\" target=\"_new\" title=\"OpenL Tablets Project on Sourceforge\"/>\r\n");
      out.write("\r\n");
      out.write("</map>\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("<div id=\"container\">\r\n");
      out.write("\t<div id=\"masthead\">\r\n");
      out.write("\t  <div id=\"search\">\r\n");
      out.write("<table width=\"97%\" cellspacing=\"0\" cellpadding=\"1\">\r\n");
      out.write("<tr><td align=\"right\" style=\"font-size: 40%;\">\r\n");
      out.write("\t  <form target=\"_top\" action=\"index.jsp\">\r\n");
      out.write("\t  <span style=\"\tfont-size:300%;font-weight: bold;color:#8B4513;\">Select OpenL Module:</span>\r\n");
      out.write("<select name=\"select_wrapper\" onchange=\"submit()\">\r\n");


	String selected = request.getParameter("select_wrapper");
		
	OpenLWrapperInfo[] wrappers = studio.getWrappers();
	
//	studio.select(selected);
	for(int i = 0; i < wrappers.length; ++i)	
	{
	   boolean current = studio.getCurrentWrapper() == wrappers[i];
	   if (current)
	     selected = wrappers[i].getWrapperClassName(); 	
	

      out.write("\r\n");
      out.write("<option value=\"");
      out.print(wrappers[i].getWrapperClassName());
      out.write('"');
      out.write(' ');
      out.write(' ');
      out.print(current ? " selected='selected'" : "");
      out.write(' ');
      out.write(' ');
      out.write('>');
      out.print(studio.getMode().getDisplayName(wrappers[i]));
      out.write("</option>\r\n");
}
      out.write("\r\n");
      out.write("</select>\r\n");
      out.write("</form>\r\n");
      out.write("</td>\r\n");
      out.write("</tr>\r\n");
      out.write("\t\t\t<tr>\r\n");
      out.write("\t\t\t<td align=\"right\">\r\n");
      out.write("\t\t\t<table style=\"border-style: solid; border-width: 1; border-color: black\" cellspacing=\"0\" cellpadding=\"2\" >\r\n");
      out.write("\t\t\t<td>\r\n");
      out.write("\t\t\t\t<a href=\"index.jsp?mode=business\" title=\"Business View\" target=\"_top\"><img border=0 src=\"");
      out.print( request.getContextPath());
      out.write("/images/business-view.png\"></a>\r\n");
      out.write("\t\t\t\t&nbsp;\r\n");
      out.write("\t\t\t\t<a href=\"index.jsp?mode=developer\" title=\"Developer View\" target=\"_top\"><img border=0 src=\"");
      out.print( request.getContextPath());
      out.write("/images/developer-view.png\"></a>  \r\n");
      out.write("\t\t\t&nbsp;&nbsp;\r\n");
      out.write("\t\t\t\r\n");
      out.write("\t\t\t\t<a target=\"mainFrame\" href=\"");
      out.print( request.getContextPath());
      out.write("/jsp/search/search.jsp?searchQuery=&quot;openl tablets&quot;\" title=\"Search\"><img border=0 src=\"");
      out.print( request.getContextPath());
      out.write("/images/search.png\"></a>\r\n");
      out.write("\t\t\t\t&nbsp;\r\n");
      out.write("\t\t\t\t<a href=\"index.jsp?reload=true&select_wrapper=");
      out.print(selected);
      out.write("\" title=\"Refresh Project(s)\" target=\"_top\"><img border=0 src=\"");
      out.print( request.getContextPath());
      out.write("/images/refresh.gif\"></a>\r\n");
      out.write("\t\t\t\t&nbsp;\r\n");
      out.write("\t\t\t\t<a target=\"mainFrame\" href=\"");
      out.print( request.getContextPath());
      out.write("/html/ws-intro.html\" title=\"Help\"><img border=0 src=\"");
      out.print( request.getContextPath());
      out.write("/images/help.gif\"></a>\r\n");
      out.write("\t\t\t</td>\r\n");
      out.write("\t\t\t</table>\r\n");
      out.write("\t\t\t\r\n");
      out.write("\t\t\t</tr>\r\n");
      out.write("\t\t\t</table>\t\t\t \r\n");
      out.write("\t\t</div>\t  \r\n");
      out.write("\t  </div>\r\n");
      out.write("\t</div>\r\n");
      out.write("<!-- end masthead -->\r\n");
      out.write("</body>\r\n");
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
