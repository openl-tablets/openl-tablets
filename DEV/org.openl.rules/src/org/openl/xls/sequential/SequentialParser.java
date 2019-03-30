package org.openl.xls.sequential;

import org.openl.conf.ConfigurableResourceContext;
import org.openl.conf.IUserContext;
import org.openl.excel.grid.SequentialXlsLoader;
import org.openl.rules.lang.xls.BaseParser;
import org.openl.rules.lang.xls.IncludeSearcher;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.code.IParsedCode;

public class SequentialParser extends BaseParser {
    private final IUserContext userContext;

    public SequentialParser(IUserContext userContext) {
        this.userContext = userContext;
    }

    @Override
    public IParsedCode parseAsModule(IOpenSourceCodeModule source) {
        String[] roots = { userContext.getUserHome() };
        ClassLoader classLoader = userContext.getUserClassLoader();
        IncludeSearcher includeSeeker = new IncludeSearcher(new ConfigurableResourceContext(classLoader, roots));

        return new SequentialXlsLoader(includeSeeker).parse(source);
    }
}
