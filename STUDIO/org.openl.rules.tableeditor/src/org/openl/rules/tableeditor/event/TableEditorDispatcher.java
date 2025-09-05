package org.openl.rules.tableeditor.event;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.faces.event.PhaseEvent;
import jakarta.faces.event.PhaseId;
import jakarta.faces.event.PhaseListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openl.rules.tableeditor.util.Constants;

public class TableEditorDispatcher implements PhaseListener {

    private static final long serialVersionUID = 8617343432886373802L;

    private final Logger log = LoggerFactory.getLogger(TableEditorDispatcher.class);

    private static final String AJAX_MATCH = "ajax/";

    @Override
    public void afterPhase(PhaseEvent event) {
    }

    @Override
    public void beforePhase(PhaseEvent event) {
        FacesContext context = event.getFacesContext();
        ExternalContext extContext = event.getFacesContext().getExternalContext();
        HttpServletRequest request = (HttpServletRequest) extContext.getRequest();
        HttpServletResponse response = (HttpServletResponse) extContext.getResponse();

        String uri = request.getRequestURI();
        if (uri.contains(Constants.TABLE_EDITOR_PATTERN)) {
            String path = uri
                    .substring(uri.indexOf(Constants.TABLE_EDITOR_PATTERN) + Constants.TABLE_EDITOR_PATTERN.length());
            if (path.startsWith(AJAX_MATCH)) {
                handleAjaxRequest(context, response, path);
            } else {
                handleResourceRequest(context, response, path);
            }
        }
    }

    private void handleAjaxRequest(FacesContext context, HttpServletResponse response, String path) {
        try {
            String methodExpressionString = makeMethodExpressionString(path.replaceFirst(AJAX_MATCH, ""));
            String res = (String) TableEditorController.invokeMethodExpression(methodExpressionString);

            response.setHeader("Pragma", "no-cache");
            response.setHeader("Cache-Control", "no-cache");
            response.setDateHeader("Expires", 0);
            response.setCharacterEncoding("UTF-8");
            PrintWriter writer = response.getWriter();
            if (res != null) {
                writer.write(res);
            }
            writer.close();
            context.responseComplete();
        } catch (IOException e) {
            log.error("Could not handle Ajax request", e);
        }
    }

    private void handleResourceRequest(FacesContext context, HttpServletResponse response, String path) {
        ClassLoader cl = getClass().getClassLoader();
        InputStream is = null;
        try {
            is = cl.getResourceAsStream(path);
        } catch (Exception e) {
            log.error("Could not handle Resource request for path '{}'. Error: {}", path, e.getMessage(), e);
        }
        if (is == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            context.responseComplete();
            return;
        }
        BufferedInputStream bis = new BufferedInputStream(is);

        try {
            // IE 9 fix
            if (path.endsWith(".css")) {
                response.setContentType("text/css");
            }
            OutputStream out = response.getOutputStream();
            byte buffer[] = new byte[2048];
            int read;
            for (read = bis.read(buffer); read != -1; read = bis.read(buffer)) {
                out.write(buffer, 0, read);
            }
            out.flush();
            out.close();
            context.responseComplete();
        } catch (Exception e) {
            log.error("Could not handle Resource request for path '{}'. Error: {}", path, e.getMessage(), e);
        } finally {
            try {
                bis.close();
            } catch (IOException e) {
                log.error("Could not close input stream", e);
            }
        }
    }

    private String makeMethodExpressionString(String request) {
        int pos = request.indexOf('?');
        if (pos >= 0) {
            request = request.substring(0, pos);
        }
        return "#{" + Constants.TABLE_EDITOR_CONTROLLER_NAME + "." + request + '}';
    }

    @Override
    public PhaseId getPhaseId() {
        return PhaseId.RESTORE_VIEW;
    }
}
