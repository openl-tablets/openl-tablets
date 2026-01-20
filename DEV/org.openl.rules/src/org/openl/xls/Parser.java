package org.openl.xls;

import org.openl.IOpenParser;
import org.openl.excel.grid.SequentialXlsLoader;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.code.IParsedCode;
import org.openl.syntax.code.impl.ParsedCode;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;

public class Parser implements IOpenParser {

    @Override
    public IParsedCode parseAsMethodBody(IOpenSourceCodeModule source) {
        return getNotSupportedCode(source, "a Method Body");
    }

    @Override
    public IParsedCode parseAsMethodHeader(IOpenSourceCodeModule source) {
        return getNotSupportedCode(source, "a Method Header");
    }

    @Override
    public IParsedCode parseAsType(IOpenSourceCodeModule source) {
        return getNotSupportedCode(source, "a Type");
    }

    @Override
    public IParsedCode parseAsParameterDeclaration(IOpenSourceCodeModule source) {
        return getNotSupportedCode(source, "a param declaration");
    }

    protected IParsedCode getNotSupportedCode(IOpenSourceCodeModule source, String sourceType) {
        String message = String.format("The source cannot be parsed as %s", sourceType);
        return getInvalidCode(message, source);
    }

    protected IParsedCode getInvalidCode(String message, IOpenSourceCodeModule source) {
        SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(message, source);
        SyntaxNodeException[] errors = new SyntaxNodeException[]{error};

        return new ParsedCode(null, source, errors, null);
    }

    @Override
    public IParsedCode parseAsModule(IOpenSourceCodeModule source) {
        return new SequentialXlsLoader().parse(source);
    }
}
