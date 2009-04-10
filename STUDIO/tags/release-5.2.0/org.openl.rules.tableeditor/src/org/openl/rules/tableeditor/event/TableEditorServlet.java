package org.openl.rules.tableeditor.event;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

import javax.faces.context.FacesContext;
import javax.faces.el.MethodBinding;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openl.rules.tableeditor.util.Constants;
import org.openl.rules.web.jsf.util.FacesUtils;

public class TableEditorServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static String AJAX_MATCH = "ajax/";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String uri = request.getRequestURI();
        String path = uri.substring(uri.indexOf(Constants.TABLE_EDITOR_PATTERN)
                + Constants.TABLE_EDITOR_PATTERN.length());
        if (path.startsWith(AJAX_MATCH)) {
            handleAjaxRequest(request, response, path);
        } else {
            handleResourceRequest(request, response, path);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException,
            IOException {
        doGet(request, response);
    }

    private void handleAjaxRequest(HttpServletRequest request, HttpServletResponse response, String path)
            throws IOException {
        FacesContext context = FacesUtils.getFacesContext(request, response);
        MethodBinding methodBinding = context.getApplication().createMethodBinding(
                makeMehtodBindingString(path.replaceFirst(AJAX_MATCH, "")), new Class[0]);
        String res = (String) methodBinding.invoke(context, new Object[0]);
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Cache-Control", "no-cache");
        response.setDateHeader("Expires", 0);
        PrintWriter writer = response.getWriter();
        if (res != null) {
            writer.write(res);
        }
        writer.close();
    }

    private void handleResourceRequest(HttpServletRequest request, HttpServletResponse response, String path)
            throws IOException {
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
    }

    private String makeMehtodBindingString(String request) {
        int pos = request.indexOf('?');
        if (pos >= 0) {
            request = request.substring(0, pos);
        }
        return new StringBuilder("#{").append(Constants.TABLE_EDITOR_CONTROLLER_NAME).append(".").append(request)
                .append('}').toString();
    }

}
