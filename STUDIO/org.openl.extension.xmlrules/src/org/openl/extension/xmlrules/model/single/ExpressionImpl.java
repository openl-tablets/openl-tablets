package org.openl.extension.xmlrules.model.single;

import org.openl.extension.xmlrules.model.Expression;

public class ExpressionImpl implements Expression {
    private String value;
    private int width = 1;
    private int height = 1;

    @Override
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }
}
