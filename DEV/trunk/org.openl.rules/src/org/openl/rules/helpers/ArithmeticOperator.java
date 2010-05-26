package org.openl.rules.helpers;

/**
 *
 * @author snshor
 *
 */

public class ArithmeticOperator implements IArithmeticOperator

{

    static abstract class AArithmeticOperator implements IArithmeticOperator {
        public double accumulate(IDoubleHolder acc, IDoubleHolder param, RoundingOperator rop, double precision) {
            double res = op(acc.getValue(), param.getValue());
            if (rop != null) {
                res = rop.op(res, precision);
            }
            acc.setValue(res);
            return res;
        }

        @Override
        public String toString() {
            return getOperatorSymbol();
        }

    }

    static class OpAdd extends AArithmeticOperator {

        public String[] getNames() {
            return new String[] { "ADD" };
        }

        public String getOperatorSymbol() {
            return "+";
        }

        public double op(double op1, double op2) {
            return op1 + op2;
        }

    }

    static class OpAssign implements IArithmeticOperator {

        public double accumulate(IDoubleHolder acc, IDoubleHolder param, RoundingOperator rop, double precision) {
            double res = op(acc.getValue(), param.getValue());
            if (rop != null) {
                res = rop.op(res, precision);
            }
            param.setValue(res);
            acc.setValue(res);
            return res;
        }

        public String[] getNames() {
            return new String[] { "ASSIGN" };
        }

        public String getOperatorSymbol() {
            return "=";
        }

        public double op(double op1, double op2) {
            return op1;
        }

    }

    static class OpDiv extends AArithmeticOperator {

        public String[] getNames() {
            return new String[] { "DIVIDE", "DIV" };
        }

        public String getOperatorSymbol() {
            return "/";
        }

        public double op(double op1, double op2) {
            return op1 / op2;
        }
    }

    static class OpMul extends AArithmeticOperator {

        public String[] getNames() {
            return new String[] { "MULTIPLY", "MUL" };
        }

        public String getOperatorSymbol() {
            return "*";
        }

        public double op(double op1, double op2) {
            return op1 * op2;
        }
    }

    static class OpSet extends AArithmeticOperator {

        public String[] getNames() {
            return new String[] { "SET" };
        }

        public String getOperatorSymbol() {
            return "=>";
        }

        public double op(double op1, double op2) {
            return op2;
        }

        // public void accumulate(IDoubleHolder acc, IDoubleHolder param)
        // {
        // acc.setValue(param.getValue());
        //
        // }

    }

    static class OpSub extends AArithmeticOperator {

        public String[] getNames() {
            return new String[] { "SUBTRACT", "SUB" };
        }

        public String getOperatorSymbol() {
            return "-";
        }

        public double op(double op1, double op2) {
            return op1 - op2;
        }
    }

    static final OpAdd OP_ADD = new OpAdd();

    static final OpSub OP_SUB = new OpSub();

    static final OpMul OP_MUL = new OpMul();

    static final OpDiv OP_DIV = new OpDiv();

    static final OpAssign OP_ASSIGN = new OpAssign();

    static final OpSet OP_SET = new OpSet();

    static public IArithmeticOperator[] operators = { OP_ADD, OP_SUB, OP_MUL, OP_DIV, OP_ASSIGN, OP_SET };

    IArithmeticOperator operator;

    static IArithmeticOperator findOperator(String op) {
        if (op == null) {
            throw new RuntimeException("ArithmeticOperator's name must not be null");
        }

        for (int i = 0; i < operators.length; i++) {

            if (operators[i].getOperatorSymbol().equals(op)) {
                return operators[i];
            }

            for (int j = 0; j < operators[i].getNames().length; j++) {
                if (operators[i].getNames()[j].equalsIgnoreCase(op)) {
                    return operators[i];
                }
            }
        }

        throw new RuntimeException("Unknown ArithmeticOperator name: " + op);

    }

    static public ArithmeticOperator fromString(String op) {

        return new ArithmeticOperator(op);
    }

    public ArithmeticOperator(String op) {

        operator = findOperator(op);
    }

    public double accumulate(IDoubleHolder acc, IDoubleHolder param, RoundingOperator rop, double precision) {
        return operator.accumulate(acc, param, rop, precision);
    }

    public String[] getNames() {
        return operator.getNames();
    }

    public String getOperatorSymbol() {
        return operator.getOperatorSymbol();
    }

    public double op(double op1, double op2) {
        return operator.op(op1, op2);
    }

    @Override
    public String toString() {
        return operator.getNames()[0];
    }

}
