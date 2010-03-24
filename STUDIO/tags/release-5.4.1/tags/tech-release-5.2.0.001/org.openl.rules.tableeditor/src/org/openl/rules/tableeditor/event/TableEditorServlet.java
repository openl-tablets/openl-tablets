package org.openl.rules.tableeditor.event;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

import javax.faces.FactoryFinder;
import javax.faces.context.FacesContext;
import javax.faces.context.FacesContextFactory;
import javax.faces.el.MethodBinding;
import javax.faces.lifecycle.Lifecycle;
import javax.faces.lifecycle.LifecycleFactory;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openl.rules.tableeditor.util.Constants;

public class TableEditorServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static String AJAX = "ajax/";

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String uri = request.getRequestURI();
        String path = uri.substring(uri.indexOf(Constants.TABLE_EDITOR_PATTERN)
                + Constants.TABLE_EDITOR_PATTERN.length());

        if (path.startsWith(AJAX)) {
            FacesContext context = getFacesContext(request, response);

            MethodBinding methodBinding = context
                    .getApplication()
                    .createMethodBinding(
                            makeMehtodBindingString(path.replaceFirst(AJAX, "")),
                            new Class[0]);
            String res = (String) methodBinding.invoke(context, new Object[0]);
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Cache-Control", "no-cache");
            response.setDateHeader("Expires", 0);
            PrintWriter writer = response.getWriter();
            if (res != null) {
                writer.write(res);
            }
            writer.close();
        } else {
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
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response);
    }

    private FacesContext getFacesContext(HttpServletRequest request,
            HttpServletResponse response) {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext == null) {
            FacesContextFactory contextFactory = (FacesContextFactory) FactoryFinder
                    .getFactory(FactoryFinder.FACES_CONTEXT_FACTORY);
            LifecycleFactory lifecycleFactory = (LifecycleFactory) FactoryFinder
                    .getFactory(FactoryFinder.LIFECYCLE_FACTORY);
            Lifecycle lifecycle = lifecycleFactory
                    .getLifecycle(LifecycleFactory.DEFAULT_LIFECYCLE);

            facesContext = contextFactory.getFacesContext(request.getSession()
                    .getServletContext(), request, response, lifecycle);
        }
        return facesContext;
    }

    private String makeMehtodBindingString(String request) {
        int pos = request.indexOf('?');
        if (pos >= 0)
            request = request.substring(0, pos);
        return new StringBuilder("#{_tableEditorController.").append(request)
                .append('}').toString();
    }

}
