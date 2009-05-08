/*
 * Created on Aug 25, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.syntax.impl;

/**
 * @author snshor
 *
 */
public interface ISyntaxConstants {
    public static final String
    // The namespace for regular openl types
            THIS_NAMESPACE = "org.openl.this",
            // The namespace for operator methods - this way they do not mix
            // with
            // THIS namespace
            OPERATORS_NAMESPACE = "org.openl.operators";
}
