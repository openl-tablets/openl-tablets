package org.openl.rules.binding;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.types.IOpenMethod;
import org.openl.types.impl.OpenMethodHeader;

public interface RecursiveOpenMethodPreBinder extends IOpenMethod {

    OpenMethodHeader getHeader();

    TableSyntaxNode getTableSyntaxNode();

    boolean isReturnsCustomSpreadsheetResult();

    void preBind();

    void startPreBind();

    void finishPreBind();

    boolean isPreBindStarted();

    boolean isCompleted();

}
