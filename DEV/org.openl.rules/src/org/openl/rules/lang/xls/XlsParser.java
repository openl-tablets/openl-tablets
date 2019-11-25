/*
 * Created on Oct 2, 2003 Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.lang.xls;

import org.openl.IOpenParser;
import org.openl.conf.ConfigurableResourceContext;
import org.openl.conf.IUserContext;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.code.IParsedCode;

/**
 * Implements {@link IOpenParser} abstraction for Excel files.
 *
 * @author snshor
 */
public class XlsParser extends BaseParser {

    private IUserContext userContext;

    public XlsParser(IUserContext userContext) {
        this.userContext = userContext;
    }

    @Override
    public IParsedCode parseAsModule(IOpenSourceCodeModule source) {

        String[] roots = { userContext.getUserHome() };
        ClassLoader classLoader = userContext.getUserClassLoader();
        IncludeSearcher includeSeeker = new IncludeSearcher(new ConfigurableResourceContext(classLoader, roots));

        XlsLoader xlsLoader = new XlsLoader(includeSeeker);

        return xlsLoader.parse(source);
    }

    @Override
    protected IParsedCode getNotSupportedCode(IOpenSourceCodeModule source, String sourceType) {
        String message = String.format(".xls files cannot be parsed as %s.", sourceType);
        return getInvalidCode(message, source);
    }
}