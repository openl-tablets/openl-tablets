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
import org.openl.syntax.code.impl.ParsedCode;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.util.PropertiesLocator;

/**
 * Implements {@link IOpenParser} abstraction for Excel files.
 * 
 * @author snshor
 */
public class XlsParser implements IOpenParser {

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

    public IParsedCode parseAsMethodBody(IOpenSourceCodeModule source) {

        String message = ".xls files can not be parsed as a Method Body";

        return getInvalidCode(message, source);
    }

    public IParsedCode parseAsMethodHeader(IOpenSourceCodeModule source) {

        String message = ".xls files can not be parsed as a Method Header";

        return getInvalidCode(message, source);
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
        
        IncludeSearcher includeSeeker = new IncludeSearcher(resourceContext, searchPath);
        return includeSeeker;
    }

    public IParsedCode parseAsType(IOpenSourceCodeModule source) {

        String message = ".xls files can not be parsed as a Type";

        return getInvalidCode(message, source);
    }

    public IParsedCode parseAsFloatRange(IOpenSourceCodeModule source) {

        String message = ".xls files can not be parsed as a float range";

        return getInvalidCode(message, source);
    }

    public IParsedCode parseAsIntegerRange(IOpenSourceCodeModule source) {

        String message = ".xls files can not be parsed as a integer range";

        return getInvalidCode(message, source);
    }

    private IParsedCode getInvalidCode(String message, IOpenSourceCodeModule source) {

        SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(message, source);
        OpenLMessagesUtils.addError(error);

        SyntaxNodeException[] errors = new SyntaxNodeException[] { error };

        return new ParsedCode(null, source, errors);
    }

    public IUserContext getUserContect() {        
        return userContext;
    }
}