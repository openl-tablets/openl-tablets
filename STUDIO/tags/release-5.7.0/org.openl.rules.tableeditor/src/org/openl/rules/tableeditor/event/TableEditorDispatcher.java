package org.openl.rules.tableeditor.event;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.commons.web.jsf.FacesUtils;
import org.openl.commons.web.util.WebTool;
import org.openl.rules.table.xls.XlsUrlParser;
import org.openl.rules.tableeditor.util.Constants;
import org.openl.util.exec.ExcelLauncher;

public class TableEditorDispatcher implements PhaseListener {
    private static final long serialVersionUID = 8617343432886373802L;

    private static final Log LOG = LogFactory.getLog(TableEditorDispatcher.class);

    private static final String AJAX_MATCH = "ajax/";
    private static final String EXCEL_MATCH = "excel/";

    public void afterPhase(PhaseEvent event) {
    }

    public void beforePhase(PhaseEvent event) {
        FacesContext context = event.getFacesContext();
        ExternalContext extContext = event.getFacesContext().getExternalContext();
        HttpServletRequest request = ((HttpServletRequest) extContext.getRequest());
        HttpServletResponse response = (HttpServletResponse) extContext.getResponse();

        String uri = request.getRequestURI();
        if (uri.indexOf(Constants.TABLE_EDITOR_PATTERN) > -1) {
            String path = uri.substring(uri
                    .indexOf(Constants.TABLE_EDITOR_PATTERN)
                    + Constants.TABLE_EDITOR_PATTERN.length());
            if (path.startsWith(AJAX_MATCH)) {
                handleAjaxRequest(context, response, path);
            } else if (path.startsWith(EXCEL_MATCH)) {
                handleExcelRequest(context, response, path);
            } else {
                handleResourceRequest(context, response, path);
            }
        }
    }

    private void handleAjaxRequest(FacesContext context, HttpServletResponse response, String path) {
        try {
            String methodExpressionString = makeMehtodExpressionString(path.replaceFirst(AJAX_MATCH, ""));
            String res = (String) FacesUtils.invokeMethodExpression(methodExpressionString);

            response.setHeader("Pragma", "no-cache");
            response.setHeader("Cache-Control", "no-cache");
            response.setDateHeader("Expires", 0);
            PrintWriter writer = response.getWriter();
            if (res != null) {
                writer.write(res);
            }
            writer.close();
            context.responseComplete();
        } catch (IOException e) {
            LOG.error("Could not handle Ajax request", e);
        }
    }

    private void handleResourceRequest(FacesContext context, HttpServletResponse response, String path) {
        try {
            ClassLoader cl = getClass().getClassLoader();
            InputStream is = cl.getResourceAsStream(path);
            if (is == null) {
                return;
            }
            OutputStream out = response.getOutputStream();
            byte buffer[] = new byte[2048];
            BufferedInputStream bis = new BufferedInputStream(is);
            int read = 0;
            for (read = bis.read(buffer); read != -1; read = bis.read(buffer)) {
                out.write(buffer, 0, read);
            }
            bis.close();
            out.flush();
            out.close();
            context.responseComplete();
        } catch (IOException e) {
            LOG.error("Could not handle Resource request", e);
        }
    }

    public void handleExcelRequest(FacesContext context, HttpServletResponse response, String path) {
        String uri = ((HttpServletRequest)context.getExternalContext().getRequest()).getParameter(
                Constants.REQUEST_PARAM_URI);
        if (uri != null && !uri.equals("")) {
            boolean local = WebTool.isLocalRequest((ServletRequest) context.getExternalContext().getRequest());
            XlsUrlParser parser = new XlsUrlParser();
            parser.parse(uri);
            try {
                if (local) { // open file
                    ExcelLauncher.launch("scripts/LaunchExcel.vbs", parser.wbPath,
                            parser.wbName, parser.wsName, parser.range);
                } else { // download file
                    File xlsFile = new File(parser.wbPath, parser.wbName);
                    if (xlsFile.isFile()) {
                        response.setContentType("application/vnd.ms-excel");
                        response.setHeader("Content-Disposition", "attachment;filename=\"" + xlsFile.getName() + "\"");
                        ServletOutputStream out = response.getOutputStream();
                        byte bytes[] = new byte[1 << 15];
                        FileInputStream fis = new FileInputStream(xlsFile);
                        try {
                            int len;
                            while ((len = fis.read(bytes)) != -1) {
                                out.write(bytes, 0, len);
                            }
                        } finally {
                            fis.close();
                        }
                        out.flush();
                        out.close();
                    }
                }
                context.responseComplete();
            } catch (Exception e) {
                LOG.error("Could not handle Excel request", e);
            }
        }
    }

    private String makeMehtodExpressionString(String request) {
        int pos = request.indexOf('?');
        if (pos >= 0) {
            request = request.substring(0, pos);
        }
        return new StringBuilder("#{").append(
                Constants.TABLE_EDITOR_CONTROLLER_NAME).append(".").append(
                request).append('}').toString();
    }

    public PhaseId getPhaseId() {
        return PhaseId.RENDER_RESPONSE;
    }
}
