/*
 * Created on Oct 2, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.lang.xls;

import org.openl.IOpenParser;
import org.openl.IOpenSourceCodeModule;
import org.openl.conf.ConfigurableResourceContext;
import org.openl.conf.IConfigurableResourceContext;
import org.openl.conf.IUserContext;
import org.openl.syntax.IParsedCode;
import org.openl.util.PropertiesLocator;

/**
 * @author snshor
 * 
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
        throw new UnsupportedOperationException(".xls files can not be parsed as a Method Body");
    }

    public IParsedCode parseAsMethodHeader(IOpenSourceCodeModule source) {
        throw new UnsupportedOperationException(".xls files can not be parsed as a Method Header");
    }

    public IParsedCode parseAsModule(IOpenSourceCodeModule source) {

        return new XlsLoader(resourceContext, getSearchPath()).parse(source);
    }

    public IParsedCode parseAsType(IOpenSourceCodeModule source) {
        throw new UnsupportedOperationException(".xls files can not be parsed as a Type");
    }

    public IParsedCode parseAsFloatRange(IOpenSourceCodeModule source) {
        throw new UnsupportedOperationException(".xls files can not be parsed as a float range");
    }

    public IParsedCode parseAsIntegerRange(IOpenSourceCodeModule source) {
        throw new UnsupportedOperationException(".xls files can not be parsed as a integer range");
    }

}