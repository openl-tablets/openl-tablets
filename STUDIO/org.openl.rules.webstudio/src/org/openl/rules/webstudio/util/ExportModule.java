package org.openl.rules.webstudio.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.commons.web.jsf.FacesUtils;
import org.openl.commons.web.util.WebTool;

public class ExportModule {
    private final static Log log = LogFactory.getLog(ExportModule.class);
    
    public static void writeOutContent(final HttpServletResponse res, final File content, final String filename, final String type) {
        if (content == null) {
            return;
        }
        FileInputStream input = null;
        try {
            res.setHeader("Pragma", "no-cache");
            res.setDateHeader("Expires", 0);
            res.setContentType("application/" + type);
            WebTool.setContentDisposition(res, filename);

            input = new FileInputStream(content);
            IOUtils.copy(input, res.getOutputStream());
        } catch (final IOException e) {
            String msg = "Failed to write content of '" + content.getAbsolutePath() + "' into response!";
            log.error(msg, e);
            FacesUtils.addErrorMessage(msg, e.getMessage());
        } finally {
            IOUtils.closeQuietly(input);
        }
    }
}
