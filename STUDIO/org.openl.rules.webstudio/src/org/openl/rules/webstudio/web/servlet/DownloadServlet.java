package org.openl.rules.webstudio.web.servlet;

import org.openl.commons.web.util.WebTool;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

public class DownloadServlet extends HttpServlet {
    private static final long serialVersionUID = -5102656998760586960L;

    private static void dumpFile(File file, OutputStream out) throws IOException {
        byte bytes[] = new byte[1 << 15];
        FileInputStream fis = new FileInputStream(file);
        try {
            int len;
            while ((len = fis.read(bytes)) != -1) {
                out.write(bytes, 0, len);
            }
        } finally {
            fis.close();
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException,
            IOException {
        boolean found = false;
        String filename = request.getParameter("filename");

        if (filename != null) {
            File file = new File(filename);
            if (file.isFile()) {
                found = true;

                response.setContentType("application/octet-stream");
                WebTool.setContentDisposition(response, file.getName());

                ServletOutputStream outputStream = response.getOutputStream();
                dumpFile(file, outputStream);
                outputStream.flush();
                outputStream.close();
            }
        }

        if (!found) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }

}
