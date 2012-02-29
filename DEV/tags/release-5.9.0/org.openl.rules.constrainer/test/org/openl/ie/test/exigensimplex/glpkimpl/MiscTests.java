package org.openl.ie.test.exigensimplex.glpkimpl;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: </p>
 * @author unascribed
 * @version 1.0
 */
import org.openl.ie.exigensimplex.LPProblem;
import org.openl.ie.exigensimplex.VarBounds;
import org.openl.ie.exigensimplex.VariableType;
import org.openl.ie.exigensimplex.glpkimpl.GLPKLPProblem;

public class MiscTests {
    static void assertion(boolean arg, String str) {
        if (!arg) {
            throw new RuntimeException(str + " - failed!");
        }
        System.out.println(str + " - passed!");
    }

    public static void main(String[] args) {
        LPProblem lp = new GLPKLPProblem();
        System.out.println(lp.getProblemName());
        lp.addRows(2);
        lp.setRowBounds(1, VariableType.DOUBLE_BOUNDED, 123, 578);
        lp.addColumns(7);
        lp.markColumnAsIntVar(1);
        assertion(lp.isColumnInteger(1), "lp.isColumnInteger");
        lp.markColumnAsBoolVar(2);
        assertion(lp.isColumnBoolean(2), "lp.isColumnBoolean");
        assertion(lp.getColumnBounds(2).getLb() == 0, "Boolean column has zero lower bound");
        assertion(lp.getColumnBounds(2).getUb() == 1, "Boolean column has unity upper bound");
        assertion(lp.getColumnBounds(2).getType() == VariableType.DOUBLE_BOUNDED,
                "Boolean column has correct bounds type");
        lp.setColumnBounds(2, VariableType.DOUBLE_BOUNDED, 43, 55);
        assertion(!lp.isColumnBoolean(2), "After bounds have been set the column is not boolean longer");
        assertion(lp.isColumnInteger(2), "It is integer");
        lp.setColumnBounds(5, VariableType.DOUBLE_BOUNDED, -3.5463, 3.432);
        VarBounds vb = lp.getColumnBounds(5);
        VarBounds vb1 = lp.getRowBounds(1);

        String[] names = { "row1", "row2", "row3", "row4", "row5" };
        double[] lbounds = { 1, 2, 3, 4, 5 };
        double[] ubounds = { 2, 3, 4, 5, 6 };
        int[] types = { VariableType.BOUNDED_ABOVE, VariableType.BOUNDED_BELOW, VariableType.DOUBLE_BOUNDED,
                VariableType.FIXED_VARIABLE, VariableType.FREE_VARIABLE };
        lp.addRows(5, names, types, lbounds, ubounds);
        int numr = lp.getNumRows();
        for (int i = 0; i < numr; i++) {
            System.out.println(lp.getRowName(i) + ": " + lp.getRowBounds(i));
        }

        lp.setMatrixRow(3, new int[] { 1, 3, 2 }, new double[] { 1, 54.34, 1e-04 });
        System.out.println();
        System.out.println(vb);
        System.out.println(vb1);
        MiscTests misc = new MiscTests();
        lp.deleteCurrentLP();
    }

    public MiscTests() {
    }
}