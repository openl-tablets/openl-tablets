package org.openl.ie.constrainer.lpsolver;

/**
 * <p>Title: </p>
 * <p>Description: An implementation of <code>interface Goal</code> being responsible for solving
 * LP problems</p>
 * <p>Copyright: Copyright (c) 2002 </p>
 * <p>Company: ExigenGroup</p>
 * @author unascribed
 * @version 1.0
 */
import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.FloatVar;
import org.openl.ie.constrainer.Goal;
import org.openl.ie.constrainer.GoalImpl;
import org.openl.ie.exigensimplex.LPProblem;
import org.openl.ie.exigensimplex.MatrixRow;
import org.openl.ie.exigensimplex.NoSolutionException;
import org.openl.ie.exigensimplex.SearchDirection;
import org.openl.ie.exigensimplex.VarBounds;
import org.openl.ie.exigensimplex.VariableType;
import org.openl.ie.exigensimplex.glpkimpl.GLPKLPProblem;
import org.openl.ie.exigensimplex.glpkimpl.Parameters;

public class GoalSimplexSolve extends GoalImpl {

    private ConstrainerLP _smpl = null;
    private LPProblem lpx = null;

    public GoalSimplexSolve(Constrainer c, ConstrainerLP smpl) {
        super(c, "");
        _smpl = smpl;
    }

    public Goal execute() throws org.openl.ie.constrainer.Failure {
        lpx = new GLPKLPProblem();
        lpx.setIntParam(Parameters.MSG_LEV, 0);
        int nbVars = _smpl.nbVars();
        int nbConstr = _smpl.nbConstraints();
        if (nbVars == 0) {
            throw new Failure("there is no need to solve empty problem");
        }
        lpx.addColumns(nbVars);
        if (nbConstr > 0) {
            lpx.addRows(nbConstr);
        } else {
            lpx.addRows(1);
        }
        // setting up constraints
        for (int i = 0; i < nbConstr; i++) {
            LPConstraint lpc = _smpl.getLPConstraint(i);
            lpx.setRowName(i, "Row" + i);
            lpx.setRowBounds(i, lpc.getType(), lpc.getLb(), lpc.getUb());
            lpx.setMatrixRow(i, new MatrixRow(lpc.getLocations(), lpc.getValues()));
        }
        // setting up variables
        for (int i = 0; i < nbVars; i++) {
            FloatVar var = _smpl.getVar(i);
            VarBounds vt = new VarBounds(VariableType.DOUBLE_BOUNDED, var.min(), var.max());
            lpx.setColumnName(i, "var" + i);
            lpx.setColumnCoeff(i, _smpl.getCostCoeff(i));
            lpx.setColumnBounds(i, vt.getType(), vt.getLb(), vt.getUb());
        }

        lpx.setObjConst(_smpl.getFreeTerm());
        int status = lpx.solveLP(_smpl.toBeMaximized() ? SearchDirection.MAXIMIZATION : SearchDirection.MINIMIZATION);
        if (status != 0) {
            throw new Failure(lpx.errorAsString(status));
        }
        try {
            for (int i = 0; i < nbVars; i++) {
                _smpl.getVar(i).setValue(lpx.getColumnValue(i));
            }
        } catch (NoSolutionException ex) {
            throw new Failure(lpx.errorAsString(ex.getErrorCode()));
        }

        lpx.deleteCurrentLP();

        return null;
    }
}