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
import org.openl.util.PropertiesLocator;

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

    protected String getSearchPath(IConfigurableResourceContext resourceContext) {

        if (searchPath == null) {
            searchPath = PropertiesLocator.findPropertyValue(SEARCH_PROPERTY_NAME, SEARCH_FILE_NAME, resourceContext);
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