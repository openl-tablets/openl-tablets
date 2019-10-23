package org.openl.rules.tableeditor.event;

import java.io.*;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.tableeditor.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
            String res = (String) FacesUtils.invokeMethodExpression(methodExpressionString);

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
        return PhaseId.RENDER_RESPONSE;
    }
}
