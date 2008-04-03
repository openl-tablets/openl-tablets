package com.exigen.ie.constrainer.lpsolver;

/**
 * <p>Title: </p>
 * <p>Description: An implementation of <code>interface Goal</code> being responsible for solving
 * integer-valued LP problems</p>
 * <p>Copyright: Copyright (c) 2002 </p>
 * <p>Company: ExigenGroup</p>
 * @author unascribed
 * @version 1.0
 */
import com.exigen.ie.constrainer.Constrainer;
import com.exigen.ie.constrainer.Failure;
import com.exigen.ie.constrainer.Goal;
import com.exigen.ie.constrainer.GoalImpl;
import com.exigen.ie.constrainer.IntVar;
import com.exigen.ie.exigensimplex.LPProblem;
import com.exigen.ie.exigensimplex.MatrixRow;
import com.exigen.ie.exigensimplex.NoSolutionException;
import com.exigen.ie.exigensimplex.SearchDirection;
import com.exigen.ie.exigensimplex.VarBounds;
import com.exigen.ie.exigensimplex.VariableType;
import com.exigen.ie.exigensimplex.glpkimpl.GLPKLPProblem;
import com.exigen.ie.exigensimplex.glpkimpl.Parameters;

public class GoalIntSimplexSolve extends GoalImpl {

  private ConstrainerMIP _smpl = null;
  private LPProblem lpx = null;

  public GoalIntSimplexSolve(Constrainer c, ConstrainerMIP smpl) {
    super(c,"");
    _smpl = smpl;
  }

  public Goal execute() throws com.exigen.ie.constrainer.Failure {
    lpx = new GLPKLPProblem();
    lpx.setIntParam(Parameters.MSG_LEV, 0);
    int nbVars = _smpl.nbVars();
    int nbConstr = _smpl.nbConstraints();
    if (nbVars == 0)
      throw new Failure("there is no need to solve empty problem");
    lpx.addColumns(nbVars);
    if (nbConstr >0)
      lpx.addRows(nbConstr);
    else
      lpx.addRows(1);
    lpx.ascribeMIPStatus();
// setting up constraints
    for (int i=0;i<nbConstr;i++){
      LPConstraint lpc = _smpl.getLPConstraint(i);
      lpx.setRowName(i, "Row"+i);
      lpx.setRowBounds(i, lpc.getType(), lpc.getLb(), lpc.getUb());
      lpx.setMatrixRow(i, new MatrixRow(lpc.getLocations(), lpc.getValues()));
    }
// setting up variables
    for (int i=0; i<nbVars;i++){
      IntVar var = _smpl.getVar(i);
      VarBounds vt = new VarBounds(VariableType.DOUBLE_BOUNDED, var.min(), var.max());
      lpx.setColumnName(i, "var"+i);
      lpx.setColumnCoeff (i, _smpl.getCostCoeff(i));
      lpx.setColumnBounds(i, vt.getType(), vt.getLb(), vt.getUb());
      lpx.markColumnAsIntVar(i);
    }

    lpx.setObjConst(_smpl.getFreeTerm());
    //lpx.setObjDir(_smpl.toBeMaximized() ? Direction.MAX : Direction.MIN);
    int status = lpx.solveMIP(_smpl.toBeMaximized()
                               ? SearchDirection.MAXIMIZATION
                               : SearchDirection.MINIMIZATION);
    if (status != 0)
      throw new Failure(lpx.errorAsString(status));

    try{
      for (int i=0;i<nbVars;i++)
          _smpl.getVar(i).setValue((int)lpx.getMIPColumnValue(i));
    }
    catch(NoSolutionException ex){
      throw new Failure(ex.getMessage());
    }

    lpx.deleteCurrentLP();
    return null;
  }
}