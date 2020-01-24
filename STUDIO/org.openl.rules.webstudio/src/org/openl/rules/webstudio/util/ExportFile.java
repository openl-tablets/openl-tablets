package org.openl.rules.webstudio.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.openl.commons.web.jsf.FacesUtils;
import org.openl.util.FileUtils;
import org.openl.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ExportFile {
    private ExportFile() {
    }

    public static void writeOutContent(final HttpServletResponse res, final File content) {
        writeOutContent(res, content, content.getName());
    }

    public static void writeOutContent(final HttpServletResponse res, final File content, final String filename) {
        if (content == null) {
            return;
        }
        FileInputStream input = null;
        try {
            res.setHeader("Pragma", "no-cache");
            res.setDateHeader("Expires", 0);
            res.setContentType("application/" + FileUtils.getExtension(filename));
            res.setHeader("Content-Disposition", WebTool.getContentDispositionValue(filename));

            input = new FileInputStream(content);
            IOUtils.copy(input, res.getOutputStream());
        } catch (final IOException e) {
            String msg = "Failed to write content of '" + content.getAbsolutePath() + "' into response.";
            final Logger log = LoggerFactory.getLogger(ExportFile.class);
            log.error(msg, e);
            FacesUtils.addErrorMessage(msg, e.getMessage());
        } finally {
            IOUtils.closeQuietly(input);
        }
    }
}
