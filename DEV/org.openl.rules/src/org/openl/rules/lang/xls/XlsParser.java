/*
 * Created on Oct 2, 2003 Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.lang.xls;

import org.openl.IOpenParser;
import org.openl.conf.ConfigurableResourceContext;
import org.openl.conf.IConfigurableResourceContext;
import org.openl.conf.IUserContext;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.code.IParsedCode;
import org.openl.util.Log;
import org.openl.util.RuntimeExceptionWrapper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

/**
 * Implements {@link IOpenParser} abstraction for Excel files.
 * 
 * @author snshor
 */
public class XlsParser extends BaseParser {

    private static final String SEARCH_PROPERTY_NAME = "org.openl.rules.include";
    private static final String SEARCH_FILE_NAME = "org/openl/rules/org.openl.rules.include.properties";
    
    private IUserContext userContext;

    private String searchPath;

    public XlsParser(IUserContext userContext) {
        this.userContext = userContext;
    }

    private String findPropertyValue(String propertyName, String propertyFileName,
                                            IConfigurableResourceContext ucxt) {

        URL url = ucxt.findClassPathResource(propertyFileName);
        if (url != null) {
            InputStream is = null;
            try {
                is = url.openStream();
                Properties p = new Properties();
                p.load(is);
                return p.getProperty(propertyName);
            } catch (IOException e) {
                throw RuntimeExceptionWrapper.wrap(e);
            } finally {
                try {
                    if (is != null) {
                        is.close();
                    }
                } catch (Throwable t) {
                    Log.error("Error closing stream", t);
                }
            }
        }

        File f = ucxt.findFileSystemResource(propertyFileName);
        if (f != null) {
            InputStream is = null;
            try {
                is = new FileInputStream(f);
                Properties p = new Properties();
                p.load(is);
                return p.getProperty(propertyName);
            } catch (IOException e) {
                throw RuntimeExceptionWrapper.wrap(e);
            } finally {
                try {
                    if (is != null) {
                        is.close();
                    }
                } catch (Throwable t) {
                    Log.error("Error closing stream", t);
                }
            }
        }

        return ucxt.findProperty(propertyName);

    }

    protected String getSearchPath(IConfigurableResourceContext resourceContext) {

        if (searchPath == null) {
            searchPath = findPropertyValue(SEARCH_PROPERTY_NAME, SEARCH_FILE_NAME, resourceContext);
        }

        return searchPath;
    }

    public IParsedCode parseAsModule(IOpenSourceCodeModule source) {
        
        IncludeSearcher includeSeeker = getIncludeSeeker();
        
        XlsLoader xlsLoader = new XlsLoader(includeSeeker, userContext);

        return xlsLoader.parse(source);
    }

    private IncludeSearcher getIncludeSeeker() {
        IConfigurableResourceContext resourceContext = new ConfigurableResourceContext(userContext.getUserClassLoader(),
            new String[] { userContext.getUserHome() });
        String searchPath = getSearchPath(resourceContext);

        return new IncludeSearcher(resourceContext, searchPath);
    }

    @Override
    protected IParsedCode getNotSupportedCode(IOpenSourceCodeModule source, String sourceType) {
        String message = String.format(".xls files can not be parsed as %s", sourceType);
        return getInvalidCode(message, source);
    }
}