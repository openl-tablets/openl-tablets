package org.openl.rules.lang.xls;

import java.net.MalformedURLException;
import java.net.URL;

import org.openl.binding.IBoundCode;
import org.openl.conf.UserContext;
import org.openl.rules.lang.xls.binding.XlsMetaInfo;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.FileSourceCodeModule;
import org.openl.source.impl.URLSourceCodeModule;
import org.openl.syntax.code.IParsedCode;
import org.openl.types.IOpenClass;
import org.openl.util.PropertiesLocator;
import org.openl.util.RuntimeExceptionWrapper;

public abstract class XlsHelper {

    public static XlsMetaInfo getXlsMetaInfo(String srcFile) {
        UserContext ucxt = new UserContext(Thread.currentThread().getContextClassLoader(), ".");
        IOpenSourceCodeModule src = null;
        String fileOrURL = PropertiesLocator.locateFileOrURL(srcFile, ucxt.getUserClassLoader(),
                new String[] { ucxt.getUserHome() });
        if (fileOrURL == null) {
            throw new RuntimeException("File " + srcFile + " is not found");
        }
        try {
            if (fileOrURL.indexOf(':') < 2) {
                src = new FileSourceCodeModule(fileOrURL, null);
            } else {
                src = new URLSourceCodeModule(new URL(fileOrURL));
            }
        } catch (MalformedURLException e) {
            throw RuntimeExceptionWrapper.wrap(e);
        }
        IParsedCode pc = new XlsParser(ucxt).parseAsModule(src);
        IBoundCode bc = new XlsBinder(ucxt).bind(pc);
        IOpenClass ioc = bc.getTopNode().getType();
        return (XlsMetaInfo) ioc.getMetaInfo();
    }
}
