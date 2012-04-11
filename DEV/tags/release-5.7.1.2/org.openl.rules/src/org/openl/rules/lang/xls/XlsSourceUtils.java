package org.openl.rules.lang.xls;

import java.net.MalformedURLException;
import java.net.URL;

import org.openl.rules.lang.xls.syntax.XlsModuleSyntaxNode;
import org.openl.util.Log;
import org.openl.util.StringTool;

public abstract class XlsSourceUtils {

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
