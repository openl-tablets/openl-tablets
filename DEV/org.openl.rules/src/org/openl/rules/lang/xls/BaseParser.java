package org.openl.rules.lang.xls;

import org.openl.IOpenParser;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.code.IParsedCode;
import org.openl.syntax.code.impl.ParsedCode;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;

public abstract class BaseParser implements IOpenParser {

    @Override
    public IParsedCode parseAsMethodBody(IOpenSourceCodeModule source) {
        return getNotSupportedCode(source, "a Method Body");
    }

    @Override
    public IParsedCode parseAsMethodHeader(IOpenSourceCodeModule source) {
        return getNotSupportedCode(source, "a Method Header");
    }

    @Override
    public IParsedCode parseAsModule(IOpenSourceCodeModule source) {
        return getNotSupportedCode(source, "a Module");
    }

    @Override
    public IParsedCode parseAsType(IOpenSourceCodeModule source) {
        return getNotSupportedCode(source, "a Type");
    }

    @Override
    public IParsedCode parseAsIntegerRange(IOpenSourceCodeModule source) {
        return getNotSupportedCode(source, "an integer range");
    }

    @Override
    public IParsedCode parseAsFloatRange(IOpenSourceCodeModule source) {
        return getNotSupportedCode(source, "a float range");
    }

    protected IParsedCode getNotSupportedCode(IOpenSourceCodeModule source, String sourceType) {
        String message = String.format("The source can't be parsed as %s", sourceType);
        return getInvalidCode(message, source);
    }

    protected IParsedCode getInvalidCode(String message, IOpenSourceCodeModule source) {
        SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(message, source);
        SyntaxNodeException[] errors = new SyntaxNodeException[] { error };

        return new ParsedCode(null, source, errors, null);
    }
}
