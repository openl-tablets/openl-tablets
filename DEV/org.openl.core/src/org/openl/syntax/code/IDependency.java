package org.openl.syntax.code;

import org.openl.syntax.impl.IdentifierNode;

public interface IDependency {
    DependencyType getType();

    IdentifierNode getNode();
}
