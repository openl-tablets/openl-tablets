package org.openl.extension.xmlrules.model.single.node.expression;

public enum Operator {
    Equal("=", "=="),
    Greater(">"),
    Less("<"),
    GreaterOrEqual(">="),
    LessOrEqual("<="),
    NotEqual("<>", "!="),
    Concatenate("&", "+"), // TODO In OpenL concatenation operator will be changed
    Addition("+"), // TODO In OpenL addition operator will cast strings to numbers
    Subtraction("-"),
    Multiplication("*"),
    Division("/"),
    Exponentiation("^", "**"),
    Percent("%", "* 0.01");
    //    Range(":"); // Not supported
    //    CombinePath("!"), // Not Supported
    //    Union(","), // Not Supported
    //    Intersection(" "), // Not Supported

    private final String xmlRulesOperator;
    private final String openLOperator;

    Operator(String xmlRulesOperator) {
        this(xmlRulesOperator, xmlRulesOperator);
    }

    Operator(String xmlRulesOperator, String openLOperator) {
        this.xmlRulesOperator = xmlRulesOperator;
        this.openLOperator = openLOperator;
    }

    public String getXmlRulesOperator() {
        return xmlRulesOperator;
    }

    public String getOpenLOperator() {
        return openLOperator;
    }

    /**
     * Searches predefined operator
     *
     * @param xmlRulesOperator searching operator
     * @return returns predefined operator or null if operator isn't supported
     */
    public static Operator findOperator(String xmlRulesOperator) {
        for (Operator operator : values()) {
            if (operator.getXmlRulesOperator().equals(xmlRulesOperator)) {
                return operator;
            }
        }

        return null;
    }
}