package org.openl.engine;

import org.openl.IOpenSourceCodeModule;
import org.openl.OpenL;
import org.openl.ProcessedCode;
import org.openl.SourceType;
import org.openl.binding.IBindingContextDelegator;
import org.openl.binding.IBoundCode;
import org.openl.syntax.IParsedCode;
import org.openl.syntax.ISyntaxError;
import org.openl.syntax.SyntaxErrorException;

/**
 * Class that defines OpenL engine manager implementation for source processing
 * operations.
 * 
 */
public class OpenlSourceManager extends BaseOpenlManager {

    private OpenlParseManager parseManager;
    private OpenlBindManager bindManager;

    /**
     * Create new instance of OpenL engine manager.
     * 
     * @param openl {@link OpenL} instance
     */
    public OpenlSourceManager(OpenL openl) {
        super(openl);

        bindManager = new OpenlBindManager(openl);
        parseManager = new OpenlParseManager(openl);

    }

    /**
     * Parses and binds source.
     * 
     * @param source source
     * @param sourceType type of source
     * @return processed code descriptor
     */
    public ProcessedCode processSource(IOpenSourceCodeModule source, SourceType sourceType) {

        return processSource(source, sourceType, null, false);
    }

    /**
     * Parses and binds source.
     * 
     * @param source source
     * @param sourceType type of source
     * @param bindingContextDelegator binding context
     * @param ignoreErrors define a flag that indicates to suppress errors or
     *            break source processing when an error has occurred
     * @return processed code descriptor
     */
    public ProcessedCode processSource(IOpenSourceCodeModule source, SourceType sourceType,
            IBindingContextDelegator bindingContextDelegator, boolean ignoreErrors) {

        IParsedCode parsedCode = parseManager.parseSource(source, sourceType);

        ISyntaxError[] parsingErrors = parsedCode.getErrors();

        if (!ignoreErrors && parsingErrors.length > 0) {
            throw new SyntaxErrorException("Parsing Error:", parsingErrors);
        }

        IBoundCode boundCode = bindManager.bindCode(bindingContextDelegator, parsedCode);

        ISyntaxError[] bindingErrors = boundCode.getErrors();

        if (!ignoreErrors && bindingErrors.length > 0) {
            throw new SyntaxErrorException("Binding Error:", bindingErrors);
        }

        ProcessedCode processedCode = new ProcessedCode();
        processedCode.setParsedCode(parsedCode);
        processedCode.setBoundCode(boundCode);

        return processedCode;
    }

}
