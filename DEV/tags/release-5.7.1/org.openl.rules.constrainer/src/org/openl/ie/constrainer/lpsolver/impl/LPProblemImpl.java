package org.openl.ie.constrainer.lpsolver.impl;

/**
 * <p>Title: LPProblemImpl</p>
 * <p>Description: The implementation of LPProblem interface</p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: Exigengroup</p>
 * @author Tseitlin
 * @version 1.0
 */

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.ConstrainerObjectImpl;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.FloatExp;
import org.openl.ie.constrainer.FloatExpArray;
import org.openl.ie.constrainer.FloatVar;
import org.openl.ie.constrainer.IntBoolExp;
import org.openl.ie.constrainer.NonLinearExpression;
import org.openl.ie.constrainer.lpsolver.ConstrainerLP;
import org.openl.ie.constrainer.lpsolver.LPConstraint;
import org.openl.ie.constrainer.lpsolver.UnexpectedVariable;
import org.openl.ie.exigensimplex.MatrixRow;
import org.openl.ie.exigensimplex.VarBounds;
import org.openl.ie.exigensimplex.VariableType;


public class LPProblemImpl extends ConstrainerObjectImpl implements ConstrainerLP {
    private class LPConstraintImpl implements LPConstraint {
        private MatrixRow _mtrow = null;
        private VarBounds _bnds = null;

        public LPConstraintImpl(int[] locations, double[] values, int type, double lb) {
            _mtrow = new MatrixRow(locations, values);
            _bnds = new VarBounds(type, lb, (type == VariableType.FIXED_VARIABLE) ? lb : Double.MAX_VALUE);
        }

        public double getLb() {
            return _bnds.getLb();
        }

        public int[] getLocations() {
            return _mtrow.getLocations();
        }

        public int getType() {
            return _bnds.getType();
        }

        public double getUb() {
            return _bnds.getUb();
        }

        public double[] getValues() {
            return _mtrow.getValues();
        }

        @Override
        public String toString() {
            return "[" + _bnds.toString() + "," + _mtrow.toString() + "]";
        }
    }

    private Vector _cost = new Vector();
    private Vector _ctType = new Vector();
    // private Vector _isConstraintValid = new Vector();
    private Vector _constraints = new Vector();
    private HashMap _vars_locs = new HashMap();
    private HashMap _locs_vars = null;
    private double _freeTerm = 0;
    private boolean _maximiz = false;

    public LPProblemImpl(FloatExp exp, boolean maximization) throws NonLinearExpression {
        this(exp, maximization, exp.constrainer(), "");
    }

    public LPProblemImpl(FloatExp exp, boolean maximization, Constrainer c, String name) throws NonLinearExpression {
        super(c, name);
        if (!exp.isLinear()) {
            throw new NonLinearExpression(exp);
        }
        HashMap map = new HashMap();
        _freeTerm = exp.calcCoeffs(map);
        Iterator iter = map.keySet().iterator();
        int varCounter = 0;
        while (iter.hasNext()) {
            FloatExp curExp = (FloatExp) iter.next();
            _vars_locs.put(curExp, new Integer(varCounter));
            _cost.add(map.get(curExp));
            // _isConstraintValid.add(new Boolean(true));
            varCounter++;
        }
        _maximiz = maximization;
    }

    public LPProblemImpl(FloatExpArray array, double[] costCoeffs, boolean maximization) throws NonLinearExpression {
        this(array, costCoeffs, maximization, array.constrainer(), "");
    }

    public LPProblemImpl(FloatExpArray array, double[] costCoeffs, boolean maximization, Constrainer c, String name)
            throws NonLinearExpression {
        super(c, name);
        if (array.size() != costCoeffs.length) {
            throw new IllegalArgumentException(
                    "Array of variables and array of their coefficients must be of the same size");
        }
        for (int i = 0; i < array.size(); i++) {
            FloatExp curExp = array.get(i);
            if (!curExp.isLinear()) {
                throw new NonLinearExpression(curExp);
            }
            _vars_locs.put(curExp, new Integer(i));
            _cost.set(i, new Double(costCoeffs[i]));
            // _isConstraintValid.set(i, new Boolean(true));
        }
        _maximiz = maximization;
    }

    public void addConstraint(IntBoolExp exp, boolean isEquality) throws NonLinearExpression {
        if (!exp.isLinear()) {
            throw new NonLinearExpression(exp);
        }
        _constraints.add(exp);
        _ctType.add(new Boolean(isEquality));
        // _isConstraintValid.add(new Boolean(true));
    }

    public void addConstraints(Collection exps, boolean isEquality) throws NonLinearExpression {
        Iterator iter = exps.iterator();
        while (iter.hasNext()) {
            addConstraint(((IntBoolExp) (iter.next())), isEquality);
        }
    }

    public void addVar(FloatVar var) {
        int num = _vars_locs.size();
        _vars_locs.put(var, new Integer(num));
        if (_locs_vars != null) {
            _locs_vars.put(new Integer(num), var);
        }
        _cost.add(new Double(0));
    }

    public IntBoolExp[] constraints() {
        IntBoolExp[] constrs = new IntBoolExp[_constraints.size()];
        _constraints.toArray(constrs);
        return constrs;
    }

    public IntBoolExp getConstraint(int idx) {
        return (IntBoolExp) _constraints.get(idx);
    }

    public double getCostCoeff(int idx) {
        return ((Double) _cost.get(idx)).doubleValue();
    }

    public double getFreeTerm() {
        return _freeTerm;
    }

    public LPConstraint getLPConstraint(int i) throws UnexpectedVariable {
        return parseBoolExp((IntBoolExp) _constraints.get(i), i);
    }

    public FloatVar getVar(int idx) {
        if (_locs_vars == null) {
            _locs_vars = new HashMap();
            Iterator iter = _vars_locs.keySet().iterator();
            while (iter.hasNext()) {
                FloatVar var = (FloatVar) iter.next();
                Integer num = (Integer) _vars_locs.get(var);
                _locs_vars.put(num, var);
            }
        }
        return (FloatVar) _locs_vars.get(new Integer(idx));
    }

    public boolean isEquality(int idx) {
        return ((Boolean) _ctType.get(idx)).booleanValue();
    }

    public LPConstraint[] lpConstraints() throws UnexpectedVariable {
        LPConstraint[] constrs = new LPConstraint[_constraints.size()];
        for (int i = 0; i < _constraints.size(); i++) {
            constrs[i] = parseBoolExp((IntBoolExp) _constraints.get(i), i);
        }
        return constrs;
    }

    public int nbConstraints() {
        return _constraints.size();
    }

    public int nbVars() {
        return _vars_locs.size();
    }

    private LPConstraint parseBoolExp(IntBoolExp exp, int idx) throws UnexpectedVariable {
        if (exp == null) {
            return null;
        }
        HashMap map = new HashMap();
        double frTrm;
        try {
            frTrm = exp.calcCoeffs(map);
        } catch (Exception e) {
            return null;
        }

        int[] pre_pos = new int[map.size()];
        double[] pre_vals = new double[map.size()];

        Iterator iter = map.keySet().iterator();
        Set varSet = _vars_locs.keySet(); // set of LP variables
        int varCounter = 0;
        while (iter.hasNext()) {
            FloatExp curExp = (FloatExp) iter.next();
            double factor = ((Double) map.get(curExp)).doubleValue();
            if (factor == 0) {
                continue;
            }
            if (!varSet.contains(curExp)) { // If there is no such a variable
                if (!curExp.bound()) {
                    throw new UnexpectedVariable(curExp); // throw exception
                }
                try {
                    frTrm += curExp.value() * factor;
                } // otherwise it will contribute to the free term
                catch (Failure f) {
                }// this will never happen
            } else {
                int varPos = ((Integer) _vars_locs.get(curExp)).intValue();
                pre_pos[varCounter] = varPos;
                pre_vals[varCounter] = factor;
                varCounter++;
            }
        }

        boolean isEquality = ((Boolean) _ctType.get(idx)).booleanValue();
        int type;
        if (isEquality) {
            type = VariableType.FIXED_VARIABLE;
        } else {
            type = VariableType.BOUNDED_BELOW;
        }

        LPConstraint constraint = null;

        if (varCounter < pre_pos.length) { // There were variables missing in
                                            // LP's variables set
            int[] pos = new int[varCounter];
            double[] vals = new double[varCounter];
            System.arraycopy(pre_pos, 0, pos, 0, varCounter);
            System.arraycopy(pre_vals, 0, vals, 0, varCounter);
            constraint = new LPConstraintImpl(pos, vals, type, -frTrm);
        } else {
            constraint = new LPConstraintImpl(pre_pos, pre_vals, type, -frTrm);
        }
        return constraint;
    }

    public void removeConstraint(int idx) {
        // _isConstraintValid.remove(idx);
        _ctType.remove(idx);
        _constraints.remove(idx);
    }

    public void removeVar(FloatVar var) {
        int pos = ((Integer) _vars_locs.get(var)).intValue();
        _vars_locs.remove(var);
        if (_locs_vars != null) {
            _locs_vars.remove(new Integer(pos));
        }
        _cost.remove(pos);
    }

    public void setCostCoeff(int idx, double coeff) {
        _cost.set(idx, new Double(coeff));
    }

    public void setFreeTerm(double frTrm) {
        _freeTerm = frTrm;
    }

    public boolean toBeMaximized() {
        return _maximiz;
    }

    @Override
    public String toString() {
        String str = "[" + name() + "\n";
        str += "vars:" + _vars_locs.size() + ", constraints:" + _constraints.size() + "\n";
        str += "vars:\n" + _vars_locs + "\n";
        str += "cost_function:\n" + _cost + " free term:" + _freeTerm + "\n";
        str += "constraints:\n";

        for (int i = 0; i < _constraints.size(); i++) {
            str += i + ". ";
            try {
                str += parseBoolExp((IntBoolExp) _constraints.get(i), i) + "\n";
            } catch (UnexpectedVariable uv) {
                str += "invalid constraint!";
            }
        }
        str += "];";
        return str;
    }

}