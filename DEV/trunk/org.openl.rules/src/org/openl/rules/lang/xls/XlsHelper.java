package org.openl.rules.lang.xls;

import java.net.MalformedURLException;
import java.net.URL;

import org.openl.binding.IBoundCode;
import org.openl.conf.UserContext;
import org.openl.exception.OpenLRuntimeException;
import org.openl.rules.lang.xls.binding.XlsMetaInfo;
import org.openl.rules.lang.xls.syntax.XlsModuleSyntaxNode;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.FileSourceCodeModule;
import org.openl.source.impl.URLSourceCodeModule;
import org.openl.syntax.code.IParsedCode;
import org.openl.types.IOpenClass;
import org.openl.util.Log;
import org.openl.util.PropertiesLocator;
import org.openl.util.StringTool;

public abstract class XlsHelper {

    public static XlsMetaInfo getXlsMetaInfo(String srcFile) {

    	UserContext ucxt = new UserContext(Thread.currentThread().getContextClassLoader(), ".");
        String fileOrURL = PropertiesLocator.locateFileOrURL(srcFile, ucxt.getUserClassLoader(), new String[] { ucxt.getUserHome() });
        
        if (fileOrURL == null) {
            throw new RuntimeException("File " + srcFile + " is not found");
        }
        
        IOpenSourceCodeModule src = null;
        
        try {
            if (fileOrURL.indexOf(':') < 2) {
                src = new FileSourceCodeModule(fileOrURL, null);
            } else {
                src = new URLSourceCodeModule(new URL(fileOrURL));
            }
        } catch (MalformedURLException e) {
            throw new OpenLRuntimeException(e);
        }
        
        IParsedCode pc = new XlsParser(ucxt).parseAsModule(src);
        IBoundCode bc = new XlsBinder(ucxt).bind(pc);
        IOpenClass ioc = bc.getTopNode().getType();
        
        return (XlsMetaInfo) ioc.getMetaInfo();
    }
    
    public static String getModuleName(XlsModuleSyntaxNode node) {

        String uri = node.getModule().getUri(0);

        try {
            URL url = new URL(uri);
            String file = url.getFile();
            int index = file.lastIndexOf('/');

            file = index < 0 ? file : file.substring(index + 1);

            index = file.lastIndexOf('.');

            if (index > 0) {
                file = file.substring(0, index);
            }

            return StringTool.makeJavaIdentifier(file);

        } catch (MalformedURLException e) {

            Log.error("Error URI to name conversion", e);

            return "UndefinedXlsType";
        }
    }
}
