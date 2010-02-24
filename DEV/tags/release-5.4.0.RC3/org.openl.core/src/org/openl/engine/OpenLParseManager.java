package org.openl.engine;

import org.openl.IOpenParser;
import org.openl.IOpenSourceCodeModule;
import org.openl.OpenL;
import org.openl.SourceType;
import org.openl.syntax.IParsedCode;

/**
 * Class that defines OpenL engine manager implementation for parsing
 * operations.
 * 
 */
public class OpenLParseManager extends OpenLHolder {

    /**
     * Creates new instance of OpenL engine manager.
     * 
     * @param openl {@link OpenL} instance
     */
    public OpenLParseManager(OpenL openl) {
        super(openl);
    }

    /**
     * Parses source.
     * 
     * @param source source
     * @param sourceType type that describes parsing algorithm
     * @return {@link IParsedCode} instance
     */
    public IParsedCode parseSource(IOpenSourceCodeModule source, SourceType sourceType) {

        IOpenParser parser = getParser();

        switch (sourceType) {
            case MODULE:
                return parser.parseAsModule(source);
            case METHOD_HEADER:
                return parser.parseAsMethodHeader(source);
            case METHOD_BODY:
                return parser.parseAsMethodBody(source);
            case TYPE:
                return parser.parseAsType(source);
            case INT_RANGE:
                return parser.parseAsIntegerRange(source);
            case DOUBLE_RANGE:
                return parser.parseAsFloatRange(source);

            default:
                throw new IllegalArgumentException("Invalid source type.");

        }
    }

    /**
     * Gets the parser.
     * 
     * @return parser
     */
    private IOpenParser getParser() {
        return getOpenL().getParser();
    }

}
