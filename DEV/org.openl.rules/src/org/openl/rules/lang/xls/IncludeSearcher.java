package org.openl.rules.lang.xls;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.openl.conf.IConfigurableResourceContext;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.URLSourceCodeModule;
import org.openl.util.PathTool;

/**
 * Searches for includes.
 *
 */
public class IncludeSearcher {

    private static final String INCLUDE = "include/";
    private IConfigurableResourceContext ucxt;

    public IncludeSearcher(IConfigurableResourceContext ucxt) {
        this.ucxt = ucxt;
    }

    public IOpenSourceCodeModule findInclude(String include) throws IOException {
        String p = PathTool.mergePath(INCLUDE, include);
        URL url = ucxt.findClassPathResource(p);

        if (url != null) {
            return new URLSourceCodeModule(url);
        }

        File f = ucxt.findFileSystemResource(p);

        if (f != null) {
            try {
                return new URLSourceCodeModule(f.toURI().toURL());
            } catch (MalformedURLException ex) {
                // ignore
            }
        }

        // let's try simple concat and use url
        String u2 = INCLUDE + include;
        URL xurl = new URL(u2);

        // URLConnection uc;
        InputStream is = null;

        try {
            is = xurl.openStream();
        } catch (IOException iox) {
            return null;
        } finally {
            if (is != null) {
                is.close();
            }
        }

        return new URLSourceCodeModule(xurl);
    }
}
