/*
 * Created on May 30, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl;

import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.code.IParsedCode;

/**
 * The <code>IOpenParser</code> interface is designed to provide a common protocol for parsers what can be used in OpenL
 * engine.
 *
 * @author snshor
 *
 */
public interface IOpenParser {

    /**
     * Parse source as method body.
     *
     * @param source source code
     * @return {@link IParsedCode} instance
     */
    IParsedCode parseAsMethodBody(IOpenSourceCodeModule source);

    /**
     * Parse source as method header.
     *
     * @param source source code
     * @return {@link IParsedCode} instance
     */
    IParsedCode parseAsMethodHeader(IOpenSourceCodeModule source);

    /**
     * Parse source as rules module.
     *
     * @param source source code
     * @return {@link IParsedCode} instance
     */
    IParsedCode parseAsModule(IOpenSourceCodeModule source);

    /**
     * Parse source as type.
     *
     * @param source source code
     * @return {@link IParsedCode} instance
     */
    IParsedCode parseAsType(IOpenSourceCodeModule source);
}
