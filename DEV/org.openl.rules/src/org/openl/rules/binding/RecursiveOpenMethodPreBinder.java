package org.openl.rules.binding;

import org.openl.types.IOpenMethod;
import org.openl.types.impl.OpenMethodHeader;

public interface RecursiveOpenMethodPreBinder extends IOpenMethod {

    OpenMethodHeader getHeader();

    void preBind();

    boolean isPreBinding();

    boolean isCompleted();

}
