/*
 * Created on May 30, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding;

import java.util.Collection;

import org.openl.message.OpenLMessage;
import org.openl.syntax.code.IParsedCode;
import org.openl.syntax.exception.SyntaxNodeException;

/**
 * The <code>IBoundCode</code> interface is designed to provide a common protocol for objects what describes bound code.
 * 
 * @author snshor
 * 
 */
public interface IBoundCode {

    /**
     * Gets errors what was found during binding process.
     * 
     * @return syntax errors
     */
    SyntaxNodeException[] getErrors();

    Collection<OpenLMessage> getMessages();

    /**
     * Gets link to parsed code that was used in binding process.
     * 
     * @return source code
     */
    IParsedCode getParsedCode();

    /**
     * Gets link to top node of bound code objects hierarchy. Bound code represented as a tree of bound code objects
     * (nodes).
     * 
     * @return top node
     */
    IBoundNode getTopNode();
}
