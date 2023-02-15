package org.openl.rules.dt;

import org.openl.binding.IBoundNode;
import org.openl.types.java.JavaOpenClass;

public class Expr {

    public static final JavaOpenClass EXPR_JAVA_OPEN_CLASS = JavaOpenClass.getOpenClass(Expr.class);

    public static final Expr NULL_EXPR = new Expr(null);

    private final AST ast;

    public Expr(IBoundNode boundNode) {
        this.ast = new AST(boundNode);
    }

    public String getTextValue() {
        return ast.getCode(ast.getBoundNode());
    }

    public AST getAST() {
        return ast;
    }
}
