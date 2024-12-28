package org.openl.xls;

import org.openl.conf.IUserContext;
import org.openl.excel.grid.SequentialXlsLoader;
import org.openl.rules.lang.xls.BaseParser;
import org.openl.rules.lang.xls.IncludeSearcher;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.code.IParsedCode;

public class Parser extends BaseParser {
    private final IUserContext userContext;

    public Parser(IUserContext userContext) {
        this.userContext = userContext;
    }

    @Override
    public IParsedCode parseAsModule(IOpenSourceCodeModule source) {
        IncludeSearcher includeSeeker = new IncludeSearcher(userContext.getUserHome(), userContext.getUserClassLoader());

        return new SequentialXlsLoader(includeSeeker).parse(source);
    }
}
