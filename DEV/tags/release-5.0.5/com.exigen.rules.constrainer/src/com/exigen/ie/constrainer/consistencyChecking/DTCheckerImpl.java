package com.exigen.ie.constrainer.consistencyChecking;


/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: </p>
 * @author unascribed
 * @version 1.0
 */
import java.util.Vector;

import com.exigen.ie.constrainer.Constrainer;
import com.exigen.ie.constrainer.Constraint;
import com.exigen.ie.constrainer.Failure;
import com.exigen.ie.constrainer.Goal;
import com.exigen.ie.constrainer.GoalAnd;
import com.exigen.ie.constrainer.GoalGenerate;
import com.exigen.ie.constrainer.GoalImpl;
import com.exigen.ie.constrainer.IntBoolExp;
import com.exigen.ie.constrainer.IntBoolExpConst;
import com.exigen.ie.constrainer.IntExpArray;
import com.exigen.ie.constrainer.IntVar;

public class DTCheckerImpl implements DTChecker
{
  private CDecisionTable _dt = null;
  private CompletenessChecker _cpChecker = new CompletenessCheckerImpl();
  private OverlappingChecker _opChecker= new OverlappingCheckerImpl();
  private Vector _uncoveredRegions = new Vector();
  private Vector _overlappingRules = new Vector();



  private class CompletenessCheckerImpl implements CompletenessChecker{
    private Constrainer C = null;

    private class GoalSaveSolutions extends GoalImpl{
      public GoalSaveSolutions(Constrainer c){super(c);}
      public Goal execute() throws Failure{
        _uncoveredRegions.add(new Uncovered(_dt.getVars()));
        return null;
      }
    }

    public Vector check() {
      IntBoolExp[] rules = _dt.getRules();
      C = rules[0].constrainer();
      IntExpArray ruleArray = new IntExpArray(C, rules.length);
      for (int i=0;i<rules.length; i++){
        ruleArray.set(rules[i], i);
      }
      Constraint incompleteness = ruleArray.sum().equals(0);
      Goal save = new GoalSaveSolutions(C);
      Goal generate = new GoalGenerate(_dt.getVars());
      Goal target = new GoalAnd(new GoalAnd(incompleteness, generate), save);
      boolean flag = C.execute(target, true);
      return _uncoveredRegions;
    }
  }

  private class OverlappingCheckerImpl implements OverlappingChecker{
    private Constrainer C = null;

    private class GoalSaveSolutions extends GoalImpl{
      public GoalSaveSolutions(Constrainer c){super(c);}
      public Goal execute() throws Failure{
        Overlapping over = new Overlapping(_dt.getVars());
        for (int i = 0; i<_dt.getRules().length; i++){
          IntBoolExp rule = _dt.getRule(i);
          if (rule.bound() && (rule.max() == 1))
            over.addRule(i);
        }
        if (over.amount() > 0 )
          _overlappingRules.add(over);
        return null;
      }
    }

    public Vector check(){
      IntBoolExp[] rules = _dt.getRules();
      C = rules[0].constrainer();
      IntExpArray ruleArray = new IntExpArray(C, rules.length);
      for (int i=0;i<rules.length; i++){
        ruleArray.set(rules[i], i);
      }
      Constraint overlapping = (ruleArray.sum().gt(1)).asConstraint();
      Goal save = new GoalSaveSolutions(C);
      Goal generate = new GoalGenerate(_dt.getVars());
      Goal target = new GoalAnd(new GoalAnd(overlapping, generate), save);
      boolean flag = C.execute(target, true);
      return _overlappingRules;
    }
  }

  static public class CDecisionTableImpl implements CDecisionTable{
    private IntBoolExp[][] _data = null;
    private IntBoolExp[] _rules = null;
    private IntExpArray _vars = null;

    public CDecisionTableImpl(IntBoolExp[][] data, IntExpArray vars){
      if (data == null)
        throw new IllegalArgumentException("DecisionTableImpl(IntBoolExp[][] _data, IntExpArray vars) : can't be created based on null data array");
      _data = data;
      _vars = vars;
      int nbRules = _data.length;
      _rules = new IntBoolExp[nbRules];
      java.util.Arrays.fill(_rules, new IntBoolExpConst(_vars.constrainer(), true));
      for (int i=0; i<_data.length; i++){
        int nbVars = _data[i].length;
        for (int j=0;j<nbVars; j++){
          _rules[i] = _rules[i].and(_data[i][j]);
        }
      }
    }

    public IntBoolExp[] getRules(){
      return _rules;
    };


    public IntBoolExp getRule(int i){return _rules[i];}
    public IntExpArray getVars(){return _vars;}
    public IntVar getVar(int i){return (IntVar)_vars.get(i);}
    public IntBoolExp getEntry(int i, int j){return _data[i][j];}
  }

  public DTCheckerImpl(CDecisionTable dtable){
    _dt = dtable;
  }

  public void setDT(CDecisionTable dtable){_dt = dtable;}
  public CDecisionTable getDT(){return _dt;}
  public Vector checkCompleteness(){
    return _cpChecker.check();
  }
  public Vector checkOverlappings(){
    return _opChecker.check();
  }

  public void setOverlappingChecker(OverlappingChecker chk){
    _opChecker = chk;
  }

  public OverlappingChecker getOverlappingChecker(){
    return _opChecker;
  }

  public void setCompletenessChecker(CompletenessChecker chk){
    _cpChecker = chk;
  }

  public CompletenessChecker getCompletenessChecker(){
    return _cpChecker;
  }
}