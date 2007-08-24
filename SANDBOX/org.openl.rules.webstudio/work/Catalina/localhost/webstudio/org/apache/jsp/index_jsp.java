package org.apache.jsp;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.jsp.*;

public final class index_jsp extends org.apache.jasper.runtime.HttpJspBase
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

      out.write("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Frameset//EN\" \"http://www.w3.org/TR/html4/frameset.dtd\">\r\n");
      out.write("\r\n");
      out.write("<html>\r\n");
      out.write("<head>\r\n");
      out.write("<title>OpenL Web Studio</title>\r\n");
      out.write("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=iso-8859-1\">\r\n");
      out.write("</head>\r\n");
      out.write("\r\n");
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

	String mode = request.getParameter("mode");
	if (mode != null)
	  studio.setMode(mode);
	String reload = request.getParameter("reload");
	if (reload != null)
	  studio.reset();
	String selected = request.getParameter("select_wrapper");
		
	studio.select(selected);
	  

      out.write("\r\n");
      out.write("\r\n");
      out.write("<frameset rows=\"70,*\">\r\n");
      out.write("<frame src=\"header.jsp\" name=\"header\" scrolling=\"no\"  noresize resize=\"no\" />\r\n");
      out.write("\r\n");
      out.write("<frameset cols=\"*,80%\" framespacing=\"0\" frameborder=\"1\" resize=\"resize\"  scrolling=\"auto\" >\r\n");
      out.write("<frameset rows=\"*,1\" framespacing=\"0\"  scrolling=\"auto\" >\r\n");
      out.write("    <frame src=\"tree.jsp\" name=\"leftFrame\" scrolling=\"auto\">\r\n");
      out.write("    <frame src=\"html/nothing.html\" name=\"show_app_hidden\">\r\n");
      out.write("</frameset>  \r\n");
      out.write("<frame src=\"html/ws-intro.html\" name=\"mainFrame\" scrolling=\"auto\"/>\r\n");
      out.write("<!--  \r\n");
      out.write("<frame src=\"html/uwdemo/welcome.html\" name=\"mainFrame\" scrolling=\"auto\"/>\r\n");
      out.write("-->\r\n");
      out.write("\r\n");
      out.write("</frameset>\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("</frameset>\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("<noframes>\r\n");
      out.write("<body>\r\n");
      out.write("    <p>To view content you need frames capable browser\r\n");
      out.write("</body>\r\n");
      out.write("</noframes>\r\n");
      out.write("\r\n");
      out.write("</frameset>\r\n");
      out.write("\r\n");
      out.write("</html>\r\n");
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
