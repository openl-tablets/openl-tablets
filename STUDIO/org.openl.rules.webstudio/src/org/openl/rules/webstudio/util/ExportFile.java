package org.openl.rules.webstudio.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.util.FileUtils;
import org.openl.util.IOUtils;

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
            input.transferTo(res.getOutputStream());
        } catch (final IOException e) {
            String msg = "Failed to write content of '" + content.getAbsolutePath() + "' into response.";
            final Logger log = LoggerFactory.getLogger(ExportFile.class);
            log.error(msg, e);
            WebStudioUtils.addErrorMessage(msg, e.getMessage());
        } finally {
            IOUtils.closeQuietly(input);
        }
    }
}
