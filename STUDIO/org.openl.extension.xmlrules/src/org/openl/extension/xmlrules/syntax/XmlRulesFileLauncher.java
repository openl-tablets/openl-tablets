package org.openl.extension.xmlrules.syntax;

import java.awt.*;
import java.io.File;

import org.openl.extension.FileLauncher;
import org.openl.rules.table.xls.XlsUrlParser;
import org.openl.util.StringTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XmlRulesFileLauncher implements FileLauncher {
    private transient final Logger log = LoggerFactory.getLogger(XmlRulesFileLauncher.class);
    private final String uri;
    private final String sourceFileName;

    public XmlRulesFileLauncher(String uri, String sourceFileName) {
        this.uri = uri;
        this.sourceFileName = sourceFileName;
    }

    public static boolean isLaunchSupported() {
        return Desktop.isDesktopSupported();
    }

    @Override
    public void launch() {
        // Parse url
        try {
            XlsUrlParser parser = new XlsUrlParser();
            parser.parse(StringTool.decodeURL(uri));
            Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.OPEN)) {
                desktop.open(new File(parser.getWbPath(), sourceFileName));
            } else {
                log.error("File open is not supported");
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}
