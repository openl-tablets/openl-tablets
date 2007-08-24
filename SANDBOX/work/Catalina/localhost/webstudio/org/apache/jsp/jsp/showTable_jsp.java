package org.apache.jsp.jsp;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.jsp.*;
import org.openl.rules.webtools.*;
import javax.faces.context.FacesContext;
import org.openl.rules.ui.*;
import org.openl.jsf.*;

public final class showTable_jsp extends org.apache.jasper.runtime.HttpJspBase
    implements org.apache.jasper.runtime.JspSourceDependent {

  private static java.util.List _jspx_dependants;

  static {
    _jspx_dependants = new java.util.ArrayList(2);
    _jspx_dependants.add("/jsp/checkTimeout.jsp");
    _jspx_dependants.add("/jsp/showRuns.jsp");
  }

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
      out.write("\r\n");
      out.write('\r');
      out.write('\n');
 
	if (studio.getModel().getWrapper() == null)
	{

      out.write("\r\n");
      out.write("\t<span class=\"error\">\r\n");
      out.write("\t\t<h3>There is a serious possibility that while you were \r\n");
      out.write("\t\tabsent, your session have expired. <p> Don't worry, <a href=\"../index.jsp\" target=\"_top\">click here</a> and start again. \r\n");
      out.write("\t</span>\r\n");
	
		return;
	}

      out.write('	');
      out.write('\r');
      out.write('\n');
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");

	String s_id = request.getParameter("elementID"); 
   	int elementID = -100; 	
   	if (s_id != null)
   	{
     	elementID = Integer.parseInt(s_id);
     	studio.setTableID(elementID);
    }
    else 
      elementID = studio.getTableID(); 	
   String url = studio.getModel().makeXlsUrl(elementID);
   String uri = studio.getModel().getUri(elementID);
   String text = org.openl.rules.webtools.indexer.FileIndexer.showElementHeader(uri);   
   String name = studio.getModel().getDisplayNameFull(elementID);
   boolean isRunnable  = studio.getModel().isRunnable(elementID);
   boolean isTestable  = studio.getModel().isTestable(elementID);
   org.openl.syntax.ISyntaxError[] se = studio.getModel().getErrors(elementID);
   

      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("<html>\r\n");
      out.write("\r\n");
      out.write("<head>\r\n");
      out.write("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=windows-1257\">\r\n");
      out.write("<title>");
      out.print(text);
      out.write("</title>\r\n");
      out.write("<link href=\"../css/style1.css\" rel=\"stylesheet\" type=\"text/css\">\r\n");
      out.write("\r\n");
      out.write("<script type=\"text/javascript\">\r\n");
      out.write("function open_win(url)\r\n");
      out.write("{\r\n");
      out.write("   window.open(url,\"_blank\",\"toolbar=no, location=no, directories=no, status=no, menubar=no, scrollbars=yes, resizable=yes, copyhistory=yes, width=900, height=700, top=20, left=100\")\r\n");
      out.write("}\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("</script>\r\n");
      out.write("\r\n");
      out.write("</head>\r\n");
      out.write("\r\n");
      out.write("<!--  added by Kazimirski:begin -->\r\n");
      out.write("<script language=\"javascript\" type=\"text/javascript\" src=\"../js/spreadsheet_navigation.js\"></script>\r\n");
      out.write("<body  onkeydown='javascript:bodyOnKeyUp(event);' onmouseup='bodyOnMouseDown(event);'>\r\n");
      out.write("<!--  added by Kazimirski:end -->\r\n");
      out.write("\r\n");
      out.write("<table><tr>\r\n");
      out.write("<td>\r\n");
      out.write("<img src=\"../images/excel-workbook.png\"/>\r\n");
      out.write("<a class=\"left\" href=\"showLinks.jsp?");
      out.print(url);
      out.write("\" target=\"show_app_hidden\" title=\"");
      out.print(uri);
      out.write("\">\r\n");
      out.write("      &nbsp;");
      out.print(text+ " : " + name);
      out.write("</a>\r\n");
      out.write("      \r\n");

	if (isRunnable && se.length == 0)
	{
	  String tgtUrl = "../treeview.jsp?title=Trace&treejsp=tracetree.jsp&relwidth=70&mainjsp=jsp/showTraceTable.jsp&elementID=" +elementID + "&first=true";

      out.write("   \r\n");
      out.write("\r\n");
      out.write("&nbsp;<a href=\"runMethod.jsp?elementID=");
      out.print(elementID);
      out.write("\" title=\"Run\"><img border=0 src=\"../images/test.gif\"/></a>   \r\n");
      out.write("&nbsp;<a onClick=\"open_win('");
      out.print(tgtUrl);
      out.write("', 800, 600)\" href=\"#\"  title=\"Trace\"><img border=0 src=\"../images/trace.gif\"/></a>   \r\n");
      out.write(" \r\n");
      out.write("\r\n");

	}
	

	if (isTestable && se.length == 0)
	{
	  String tgtUrl = "../treeview.jsp?title=Trace&treejsp=tracetree.jsp&relwidth=70&mainjsp=jsp/showTraceTable.jsp&elementID=" +elementID + "&first=true";

      out.write("   \r\n");
      out.write("\r\n");
      out.write("&nbsp;<a href=\"runAllTests.jsp?elementID=");
      out.print(elementID);
      out.write("\" title=\"Test\"><img border=0 src=\"../images/test_ok.gif\"/></a>   \r\n");
      out.write("\r\n");

	}



      out.write("\r\n");
      out.write("</td>\r\n");
      out.write("<td>\r\n");

	String[] menuParamsView = {"transparency", "filterType", "view"}; 
	String parsView = WebTool.listParamsExcept(menuParamsView, request);

	
	String view = request.getParameter("view");
	if (view == null)
	{
		view = studio.getModel().getTableView();
	}

      out.write("\r\n");
      out.write("\r\n");
      out.write("&nbsp;<a class=\"image2\" href=\"?");
      out.print(parsView);
      out.write("&view=view.business\"><img border=0 src=\"../images/business-view.png\" title=\"Business View\"/></a>\r\n");
      out.write("&nbsp;<a class=\"image2\" href=\"?");
      out.print(parsView);
      out.write("&view=view.developer\"><img border=0 src=\"../images/developer-view.png\" title=\"Developer (Full) View\"/></a>\r\n");
      out.write("</td>\r\n");
      out.write("</tr></table>      \r\n");
      out.write("\r\n");
      out.print(studio.getModel().showErrors(elementID));
      out.write("\r\n");
      out.write("\r\n");
      out.write("<!--\r\n");
      out.write("<a href=\"showTableEditor.jsp?elementID=");
      out.print(elementID);
      out.write("\">Edit Table</a>\r\n");
      out.write("-->\r\n");
      out.write("\r\n");
      out.write("<div>\r\n");
      out.write("&nbsp;");
      out.print(studio.getModel().showTable(elementID, view));
      out.write("\r\n");
      out.write("</div>\r\n");
      out.write("\r\n");

     org.openl.rules.ui.AllTestsRunResult atr = studio.getModel().getRunMethods(elementID); 
     if (atr != null)
     {
	     org.openl.rules.ui.AllTestsRunResult.Test[] tests = atr.getTests();
	     if (tests.length > 0)
	     {
	
      out.write("\r\n");
      out.write("\t\t\t<h1>Select one of the available runs by clicking on it:</h1>\r\n");
      out.write("\t");
    
		 
	     	for(int i = 0; i < tests.length; ++i)
	     	{
	     		for(int j = 0; j < tests[i].ntests(); ++j)
	     		{
	     			String tname = org.openl.rules.webtools.WebTool.encodeURL(tests[i].getTestName());
	     			String tdescrURL = org.openl.rules.webtools.WebTool.encodeURL(tests[i].getTestDescription(j));
	     			String tdescrBody = org.openl.rules.webtools.WebTool.encodeHTMLBody(tests[i].getTestDescription(j));
	     			
	
      out.write("\r\n");
      out.write("\t\t\t\t\t<p>&nbsp;<a href=\"runMethod.jsp?elementID=");
      out.print(s_id);
      out.write("&testName=");
      out.print(tname);
      out.write("&testID=");
      out.print(j);
      out.write("&testDescr=");
      out.print(tdescrURL);
      out.write('"');
      out.write('>');
      out.print(tdescrBody);
      out.write("</a>     \r\n");
      out.write("\t");
     	
				}
	     	}
	 	  } 	
	}	   
   

      out.write('\r');
      out.write('\n');
      out.write("\r\n");
      out.write("\r\n");
      out.write("\r\n");
      out.write("<!--  added by Kazimirski -->\r\n");
      out.write("\r\n");
      out.write("<br /><br /><br />\r\n");

FacesContext fc = FacesContext.getCurrentInstance();
TableWriter tw = new TableWriter(elementID,view,studio);
tw.render(out);

      out.write("\r\n");
      out.write("\r\n");
      out.write("<script type=\"text/javascript\">\r\n");
      out.write("initialRow = '");
      out.print(tw.getInitialRow());
      out.write("';\r\n");
      out.write("initialColumn = '");
      out.print(tw.getInitialColumn());
      out.write("';\r\n");
      out.write("</script>\r\n");
      out.write("\r\n");
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
