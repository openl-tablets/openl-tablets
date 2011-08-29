package org.openl.rules.webstudio.web.servlet;

import java.io.File;
import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.commons.web.util.WebTool;
import org.openl.main.SourceCodeURLConstants;
import org.openl.main.SourceCodeURLTool;
import org.openl.rules.table.word.WordUrlParser;
import org.openl.rules.table.xls.XlsUrlParser;
import org.openl.rules.webstudio.util.ExcelLauncher;
import org.openl.rules.webstudio.util.WordLauncher;
import org.openl.util.FileTypeHelper;
import org.openl.util.StringTool;

public class LaunchFileServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final Log LOG = LogFactory.getLog(LaunchFileServlet.class);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        doPost(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String excelScriptPath = getServletContext().getRealPath("scripts/LaunchExcel.vbs");
        String wordScriptPath = getServletContext().getRealPath("scripts/LaunchWord.vbs");

        boolean local = WebTool.isLocalRequest(request);

        String uri = request.getParameter("uri");

        String wbPath = null;
        String wbName = null;
        String wsName = null;
        String range = null;
        
        String wdPath = null;
        String wdName = null;
        String wdParStart = null;
        String wdParEnd = null;

        boolean isExcel = false;
        boolean isWord = false;

        if (uri != null) { // by uri
            String file = SourceCodeURLTool.parseUrl(uri).get(SourceCodeURLConstants.FILE);

            try {
                if (FileTypeHelper.isExcelFile(file)) { // Excel
                    XlsUrlParser parser = new XlsUrlParser();
                    parser.parse(uri);
                    wbPath = StringTool.decodeURL(parser.wbPath);
                    wbName = StringTool.decodeURL(parser.wbName); 
                    wsName = StringTool.decodeURL(parser.wsName); 
                    range = StringTool.decodeURL(parser.range);
                    isExcel = true;
    
                } else if (FileTypeHelper.isWordFile(file)) { // Word
                    WordUrlParser parser = new WordUrlParser();
                    parser.parse(uri);
                    wdPath = StringTool.decodeURL(parser.wdPath);
                    wdName = StringTool.decodeURL(parser.wdName); 
                    wdParStart = StringTool.decodeURL(parser.wdParStart); 
                    wdParEnd = StringTool.decodeURL(parser.wdParEnd);
                    isWord = true;
                }
            } catch (Exception e) {
                LOG.error("Can't parse file uri", e);
                return;
            }

        } else { // by params @Deprecated
            wbName = StringTool.decodeURL(request.getParameter("wbName"));

            if (wbName != null) { // Excel
                wbPath = StringTool.decodeURL(request.getParameter("wbPath"));
                wsName = StringTool.decodeURL(request.getParameter("wsName"));
                range = StringTool.decodeURL(request.getParameter("range"));
                isExcel = true;
            } else {
                wdName = StringTool.decodeURL(request.getParameter("wdName"));

                if (wdName != null) {  // Word
                    wdPath = StringTool.decodeURL(request.getParameter("wdPath"));
                    wdParStart = StringTool.decodeURL(request.getParameter("wdParStart"));
                    wdParEnd = StringTool.decodeURL(request.getParameter("wdParEnd"));
                    isWord = true;
                }
            }
        }

        if (local) { // local mode
            try {
                if (isExcel) {
                    ExcelLauncher.launch(excelScriptPath, wbPath, wbName, wsName, range);
                    return;
                } else if (isWord) {
                    WordLauncher.launch(wordScriptPath, wdPath, wdName, wdParStart, wdParEnd);
                    return;
                }
            } catch (Exception e) {
                LOG.error("Can't launch file", e);
            }

        } else { // remote mode
            String fileName = null;
            String path = null;

            if (isExcel) {
                fileName = wbName;
                path = wbPath;
            } else if (isWord) {
                fileName = wdName;
                path = wdPath;
            }
            String filePath = new File(path, fileName).getAbsolutePath();

            String query = "filename=" + filePath;
            RequestDispatcher dispatcher = getServletContext().getRequestDispatcher("/action/download?" + query);
            dispatcher.forward(request,response);
        }
    }

}
