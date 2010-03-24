/*
 * Created on Oct 2, 2003 Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.lang.xls;

import org.openl.IOpenParser;
import org.openl.conf.ConfigurableResourceContext;
import org.openl.conf.IConfigurableResourceContext;
import org.openl.conf.IUserContext;
import org.openl.message.OpenLMessagesUtils;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.code.IParsedCode;
import org.openl.util.PropertiesLocator;

/**
 * Implements {@link IOpenParser} abstraction for Excel files.
 * 
 * @author snshor
 */
public class XlsParser implements IOpenParser {

    private static final String SEARCH_PROPERTY_NAME = "org.openl.rules.include";
    private static final String SEARCH_FILE_NAME = "org/openl/rules/org.openl.rules.include.properties";

    private IConfigurableResourceContext resourceContext;

    private String searchPath;

    public XlsParser(IUserContext userContext) {

        this.resourceContext = new ConfigurableResourceContext(userContext.getUserClassLoader(),
            new String[] { userContext.getUserHome() });
    }

    protected String getSearchPath() {

        if (searchPath != null) {
            return searchPath;
        }

        searchPath = PropertiesLocator.findPropertyValue(SEARCH_PROPERTY_NAME, SEARCH_FILE_NAME, resourceContext);

        return searchPath;
    }

    public IParsedCode parseAsMethodBody(IOpenSourceCodeModule source) {

        String message = ".xls files can not be parsed as a Method Body";
        OpenLMessagesUtils.addError(message);
        
        throw new UnsupportedOperationException(message);
    }

    public IParsedCode parseAsMethodHeader(IOpenSourceCodeModule source) {

        String message = ".xls files can not be parsed as a Method Header";
        OpenLMessagesUtils.addError(message);
        
        throw new UnsupportedOperationException(message);
    }

    public IParsedCode parseAsModule(IOpenSourceCodeModule source) {

        XlsLoader xlsLoader = new XlsLoader(resourceContext, getSearchPath());

        return xlsLoader.parse(source);
    }

    public IParsedCode parseAsType(IOpenSourceCodeModule source) {

        String message = ".xls files can not be parsed as a Type";
        OpenLMessagesUtils.addError(message);
        
        throw new UnsupportedOperationException(message);
    }

    public IParsedCode parseAsFloatRange(IOpenSourceCodeModule source) {

        String message = ".xls files can not be parsed as a float range";
        OpenLMessagesUtils.addError(message);
        
        throw new UnsupportedOperationException(message);
    }

    public IParsedCode parseAsIntegerRange(IOpenSourceCodeModule source) {

        String message = ".xls files can not be parsed as a integer range";
        OpenLMessagesUtils.addError(message);
        
        throw new UnsupportedOperationException(message);
    }

}