package org.openl.ie.ccc;

///////////////////////////////////////////////////////////////////////////////
/*
 * Copyright Exigen Group 1998, 1999, 2000, 2002
 * 320 Amboy Ave., Metuchen, NJ, 08840, USA, www.exigengroup.com
 *
 * The copyright to the computer program(s) herein
 * is the property of Exigen Group, USA. All rights reserved.
 * The program(s) may be used and/or copied only with
 * the written permission of Exigen Group
 * or in accordance with the terms and conditions
 * stipulated in the agreement/contract under which
 * the program(s) have been supplied.
 */
///////////////////////////////////////////////////////////////////////////////
import java.util.HashMap;
import java.util.Vector;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Constraint;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.FloatExp;
import org.openl.ie.constrainer.Goal;
import org.openl.ie.constrainer.GoalAnd;
import org.openl.ie.constrainer.GoalCheckSolutionNumber;
import org.openl.ie.constrainer.IntBoolExp;
import org.openl.ie.constrainer.IntExp;
import org.openl.ie.constrainer.IntExpArray;
import org.openl.ie.constrainer.NonLinearExpression;
import org.openl.ie.constrainer.lpsolver.ConstrainerLP;
import org.openl.ie.scheduler.Job;
import org.openl.ie.scheduler.Resource;
import org.openl.ie.scheduler.Schedule;
import org.openl.util.Log;


public class CccCore implements CccConst {
    String _name;
    private Vector _integers;
    private Vector _floats;
    private Vector _constraints;
    private Vector _c_active;
    private Vector _c_contr;
    private Vector _goals;
    private Vector _jobs;
    private Vector _resources;
    private Vector groups;

    private Constrainer _constrainer;
    private Schedule _schedule;
    private CccVariable _objective;
    private int _max_solution_num = 99;
    private int _timeout = 30;

    private CccProblem _problem;
    private HashMap _objects;

    private boolean _valid;
    private boolean _ok;
    private boolean _failed;
    private boolean _initializing;

    // private int _constraints_num;
    // private int _goals_num;
    // private int _integers_num;
    // private int _floats_num;
    // private int _jobs_num;
    // private int _resources_num;

    private String _activated_goal_id;

    private int _solution_num;

    private String _status;
    private String _last_failed_id;

    private long _exec_time;

    // -----------------------------------------------------------------------------

    /**
     * CccCore constructor.
     */
    public CccCore() {
        super();
        _name = "null";
        _valid = false;
    }

    synchronized public boolean activateAllConstraints() {
        // System.out.println("activateAllConstraints()");

        for (int i = 0; i < constraints().size(); i++) {
            String id = (String) constraints().elementAt(i);
            if (!isActivated(id)) {
                addToActiveList(id);
            }
        }
        if (isInitializing()) {
            return true;
        }
        return solve();
    }

    synchronized public boolean activateConstraint(String id) {
        addToActiveList(id);

        CccConstraint c = getConstraintById(id);

        traceln("activateConstraint(" + id + ") :" + c);

        if (isInitializing()) {
            return true;
        }
        boolean result = solve();
        setStatus("Activated constraint " + c.name() + ", time: " + getExecutionTime() + "ms");
        if (result == false) {
            c.status(STATUS_INCOMPATIBLE);
            setLastFailedId(id);
            removeFromActiveList(id);
            addToContradictionList(id);
        }

        return result;
    }

    public boolean activateGoal(String goal) {
        return activateGoal(goal, 1);
    }

    synchronized public boolean activateGoal(String goal, int solution) {
        // System.out.println("activateGoal("+goal+", "+solution+")!");
        for (int i = 0; i < _goals.size(); i++) {
            getGoalByNum(i).status(STATUS_INACTIVE);
        }

        _activated_goal_id = goal;
        setSolutionNumber(solution);
        if (isInitializing()) {
            return true;
        }
        return solve();
    }

    public boolean activateSolution(int n) {
        return activateGoal(getGoalSolutionId(), n);
    }

    public CccConstraint addConstraint(CccConstraint c) {
        CccGroup g = getGroup(getConstraintId());
        if (isInitializing()) {
            _constraints.addElement(getConstraintId(_constraints.size()));
        }
        String id = g.add(c);
        putObject(id, c);
        if (isActivated(id)) {
            postConstraint(id);
            try {
                if (c.getSimplex() != null) {
                    c.getSimplex().addConstraint(c.getBoolExp(), false);
                }
            } catch (NonLinearExpression e) {
                Log.error(e);
            }
            fetchConstrainerState(false);
        }
        return c;
    }

    public CccConstraint addConstraint(Constraint c, String name) throws Exception {
        CccConstraint cc = new CccConstraint(this, name);
        cc.executable(c);
        return addConstraint(cc);
    }

    public CccConstraint addConstraint(IntBoolExp e, String name) throws Exception {
        return addConstraint(e.asConstraint(), name);
    }

    public CccConstraint addConstraint(IntBoolExp e, String name, ConstrainerLP s) throws Exception {
        CccConstraint cc = new CccConstraint(this, name);
        cc.executable(e.asConstraint());
        cc.setBoolExp(e);
        cc.setSimplex(s);
        return addConstraint(cc);
    }

    public CccFloat addFloat(CccFloat f) {
        CccGroup g = getGroup(getFloatId());
        if (isInitializing()) {
            String id = g.add(f);
            _floats.addElement(id);
        }
        putObject(g.add(f), f);
        return f;
    }

    public CccGoal addGoal(CccGoal g) {
        CccGroup grp = getGroup(getGoalId());
        if (isInitializing()) {
            _goals.addElement(getGoalId(_goals.size()));
        }
        putObject(grp.add(g), g);
        return g;
    }

    public CccGoal addGoalMaximize() {
        return addGoal(objective().getMaximizeGoal());
    }

    public CccGoal addGoalMinimize() {
        return addGoal(objective().getMinimizeGoal());
    }

    public CccGoal addGoalSolution(Goal solv) {
        CccGoal gs = new CccGoalSolution(this);
        gs.executable(solv);
        return addGoal(gs);
    }

    public CccInteger addInteger(CccInteger i) {
        CccGroup g = getGroup(getIntegerId());
        if (isInitializing()) {
            String id = g.add(i);
            _integers.addElement(id);
        }
        putObject(g.add(i), i);
        return i;
    }

    public CccInteger addInteger(IntExp i) {
        CccInteger ci = new CccInteger(this, i);
        return addInteger(ci);
    }

    public CccInteger addInteger(IntExp i, String name) {
        CccInteger ci = new CccInteger(this, i, name);
        return addInteger(ci);
    }

    // -----------------------------------------------------------------------------

    public void addIntExpArray(IntExpArray a, String name) {
        CccGroup g = getGroup(name);
        for (int i = 0; i < a.size(); i++) {
            IntExp v = a.get(i);
            CccInteger ci = new CccInteger(this, v);
            g.add(ci);
            if (isInitializing()) {
                String id = g.add(ci);
                // _integers.addElement(id);
            }
            putObject(g.add(ci), ci);
        }
    }

    public CccJob addJob(Job job) {
        CccGroup g = getGroup(getJobId());
        CccJob j = new CccJob(this, job);

        if (isInitializing()) {
            String id = getJobId(_jobs.size());
            // trace("addjob "+id);
            _jobs.addElement(id);
        }
        putObject(g.add(j), j);

        return j;
    }

    public void addObjective(CccVariable obj) {
        CccGroup g = getGroup(getObjectiveId());
        _objective = obj;
        _objective.setType(TM_OBJECTIVE);
        putObject(g.add(_objective), _objective);
    }

    public void addObjective(FloatExp e) {
        addObjective(new CccFloat(this, e));
    }

    public void addObjective(FloatExp e, String name) {
        addObjective(new CccFloat(this, e, name));
    }

    public void addObjective(IntExp e) {
        addObjective(new CccInteger(this, e));
    }

    public void addObjective(IntExp e, String name) {
        addObjective(new CccInteger(this, e, name));
    }

    // -----------------------------------------------------------------------------

    public CccResource addResource(Resource resource) {
        CccGroup g = getGroup(getResourceId());
        CccResource r = new CccResource(this, resource);

        if (isInitializing()) {
            String id = getResourceId(_resources.size());
            _resources.addElement(id);
        }
        putObject(g.add(r), r);

        return r;
    }

    protected void addToActiveList(String id) {
        _c_active.add(id);
    }

    protected void addToContradictionList(String id) {
        if (!_c_contr.contains(id)) {
            _c_contr.add(id);
        }
    }

    synchronized public void assignProblem(CccProblem p) {
        _problem = p;
        flush();
        _status = "Initialized new problem";
        setInitializing(true);
        reset();
        try {
            _problem.main(this);
        } catch (Exception e) {
            Log.error("ASSIGN: EXCEPTION DURING _problem.run()", e);
        }
        setInitializing(false);
    }

    public Constrainer constrainer() {
        return _constrainer;
    }

    public void constrainer(Constrainer c) {
        _constrainer = c;
    }

    public Vector constraints() {
        return _constraints;
    }

    // ---ids-----------------------------------------------------------------------

    public Schedule createSchedule(int start, int end) {
        _schedule = new Schedule(_constrainer, start, end);
        _schedule.setName("schedule for " + _name);
        return _schedule;
    }

    synchronized public void deactivateAllConstraints() {
        // System.out.println("deactivateAllConstraints()");

        _c_active.clear();
        _c_contr.clear();

        if (isInitializing()) {
            return;
        }
        solve();
    }

    synchronized public void deactivateConstraint(String id) {
        // System.out.println("deactivateConstraint(String id)");
        removeFromActiveList(id);
        removeFromContradictionList(id);

        CccConstraint c = getConstraintById(id);
        c.status(STATUS_INACTIVE);

        if (isInitializing()) {
            return;
        }
        solve();
    }

    synchronized public void deactivateGoal() {
        activateGoal(null, 1);
    }

    public boolean existsGroup(String name) {
        for (int i = 0; i < groups.size(); i++) {
            CccGroup g = (CccGroup) groups.get(i);
            if (name.equals(g.getName())) {
                return true;
            }
        }
        return false;
    }

    /*
     * public void removeConstraint(CccConstraint c) {
     * _constraints.removeElement(c); }
     */
    protected void fetchConstrainerState(boolean jj) {
        for (int i = 0; i < groups.size(); i++) {
            CccGroup g = (CccGroup) groups.get(i);
            for (int j = 0; j < g.size(); j++) {
                CccObject o = g.getObject(j);
                if (o instanceof CccInteger) {
                    CccInteger v = (CccInteger) o;
                    v.fetchConstrainerState();
                }
                if (o instanceof CccFloat) {
                    CccFloat v = (CccFloat) o;
                    v.fetchConstrainerState();
                }
                if (o instanceof CccJob) {
                    CccJob v = (CccJob) o;
                    v.fetchConstrainerState();
                }
                if (o instanceof CccResource) {
                    CccResource v = (CccResource) o;
                    v.fetchConstrainerState();
                }
            }
        }

        for (int i = 0; i < _integers.size(); i++) {
            getIntegerByNum(i).fetchConstrainerState();
        }

        for (int i = 0; i < _floats.size(); i++) {
            getFloatByNum(i).fetchConstrainerState();
        }

        for (int i = 0; i < _constraints.size(); i++) {
            getConstraintByNum(i).fetchConstrainerState();
        }

        for (int i = 0; i < _goals.size(); i++) {
            getGoalByNum(i).fetchConstrainerState();
        }

        Vector jon = new Vector();
        // System.out.println("JOBS: "+_jobs.size());
        for (int i = 0; i < _jobs.size(); i++) {
            CccJob job = getJobByNum(i);
            if (job.isConstrained()) {
                jon.add(getJobId(i));
            } else {
                job.fetchConstrainerState();
            }
        }

        if (jon.size() > 0) {
            // System.out.println("+ACTIVATING SOLVER");
            Goal ex = getGoalSolution().executable();
            if (jj) {
                constrainer().execute(ex, true);
            // System.out.println("SOLVER DONE");
            }

            // System.out.println("BINDED JOBS: "+jon.size());
            for (int i = 0; i < jon.size(); i++) {
                CccJob job = getJobById((String) jon.elementAt(i));
                job.fetchConstrainerState();
            }
        }

        for (int i = 0; i < _resources.size(); i++) {
            getResourceByNum(i).fetchConstrainerState();
        }

        if (objective() != null) {
            objective().fetchConstrainerState();
        }
    }

    public Vector floats() {
        return _floats;
    }

    public void flush() {
        _objects = new HashMap();

        _schedule = null;
        _integers = new Vector(10, 5);
        _floats = new Vector(10, 5);
        _constraints = new Vector(10, 5);
        _c_active = new Vector(10, 5);
        _c_contr = new Vector(10, 5);
        _jobs = new Vector(10, 5);
        _resources = new Vector(10, 5);
        _goals = new Vector(1, 1);
        groups = new Vector(10, 5);
        _activated_goal_id = null;
        _objective = null;
        setSolutionNumber(1);
        setOk(true);
    }

    protected CccExecutable getActivatedGoal() {
        return (CccExecutable) getObject(_activated_goal_id);
    }

    public String getActivatedGoalId() {
        return _activated_goal_id;
    }

    public Vector getActiveList() {
        return _c_active;
    }

    protected String getByGroupId(CccGroup g, int i) {
        return g.getName() + getDivider() + i;
    }

    protected CccConstraint getConstraintById(String id) {
        return (CccConstraint) _objects.get(id);
    }

    protected CccConstraint getConstraintByNum(int i) {
        return getConstraintById(getConstraintId(i));
    }

    protected String getConstraintId() {
        return "constraint";
    }

    protected String getConstraintId(int i) {
        return getConstraintId() + getDivider() + i;
    }

    // ---addConstraint-------------------------------------------------------------

    protected String getDivider() {
        return ",";
    }

    public long getExecutionTime() {
        return _exec_time;
    }

    // public CccConstraint addConstraint(String c, String name) throws
    // Exception
    // {
    // CccConstraint cc = new CccConstraint(this, name);
    // cc.executable(constrainer().evaluateConstraint(c));
    // return addConstraint(cc);
    // }

    // public CccConstraint addConstraint(String c) throws Exception
    // {
    // return addConstraint(c, c);
    // }

    protected CccFloat getFloatById(String id) {
        return (CccFloat) _objects.get(id);
    }

    protected CccFloat getFloatByNum(int i) {
        return getFloatById(getFloatId(i));
    }

    // ---addGoal-------------------------------------------------------------

    protected String getFloatId() {
        return "float";
    }

    protected String getFloatId(int i) {
        return getFloatId() + getDivider() + i;
    }

    protected CccGoal getGoalById(String id) {
        return (CccGoal) _objects.get(id);
    }

    protected CccGoal getGoalByNum(int i) {
        return getGoalById(getGoalId(i));
    }

    // ---addInteger----------------------------------------------------------------

    protected String getGoalId() {
        return "goal";
    }

    protected String getGoalId(int i) {
        return getGoalId() + getDivider() + i;
    }

    public CccGoalSolution getGoalSolution() {
        CccGoalSolution s = null;
        for (int i = 0; i < goals().size(); i++) {
            CccGoal g = getGoalByNum(i);
            if (g instanceof CccGoalSolution) {
                s = (CccGoalSolution) g;
                break;
            }
        }
        return s;
    }

    public String getGoalSolutionId() {
        for (int i = 0; i < goals().size(); i++) {
            if (getGoalByNum(i) instanceof CccGoalSolution) {
                return getGoalId(i);
            }
        }
        return null;
    }

    // ---addFloat----------------------------------------------------------------

    public CccGroup getGroup(String name) {
        for (int i = 0; i < groups.size(); i++) {
            CccGroup g = (CccGroup) groups.get(i);
            if (name.equals(g.getName())) {
                return g;
            }
        }
        CccGroup g = new CccGroup(this, name);
        groups.add(g);
        return g;
    }

    // ---addJob/Resource-----------------------------------------------------------

    public Vector getGroups() {
        return groups;
    }

    protected CccInteger getIntegerById(String id) {
        return (CccInteger) _objects.get(id);
    }

    // ---addObjective--------------------------------------------------------------

    protected CccInteger getIntegerByNum(int i) {
        return getIntegerById(getIntegerId(i));
    }

    protected String getIntegerId() {
        return "integer";
    }

    protected String getIntegerId(int i) {
        return getIntegerId() + getDivider() + i;
    }

    protected CccJob getJobById(String id) {
        return (CccJob) _objects.get(id);
    }

    protected CccJob getJobByNum(int i) {
        return getJobById(getJobId(i));
    }

    protected String getJobId() {
        return "job";
    }

    // ---shedule----------------------------------------------------------------

    protected String getJobId(int i) {
        return getJobId() + getDivider() + i;
    }

    // ---active
    // lists--------------------------------------------------------------

    public boolean getLastFailed() {
        return _failed;
    }

    public String getLastFailedId() {
        return _last_failed_id;
    }

    public int getMaxSolutionNum() {
        return _max_solution_num;
    }

    protected CccObject getObject(String id) {
        // System.out.println("? "+id+" \t: "+_objects.get( id ) );
        return (CccObject) _objects.get(id);
    }

    protected CccObject getObject(String id, int i) {
        // System.out.println("? "+id+"("+i+") \t: "+_objects.get(
        // id+getDivider()+i ) );
        return (CccObject) _objects.get(id + getDivider() + i);
    }

    protected CccVariable getObjectiveById(String id) {
        return (CccVariable) _objects.get(id);
    }

    protected CccVariable getObjectiveByNum(int i) {
        return getObjectiveById(getObjectiveId(i));
    }

    protected String getObjectiveId() {
        return "objective";
    }

    protected String getObjectiveId(int i) {
        return getObjectiveId() + getDivider() + i;
    }

    /*
     * public boolean executeActivatedConstraints() { CccConstraint ct = null; //
     * trying to activate contradictory constraints for(int i = 0; i <
     * _c_contr.size(); i++) { try { ct = getConstraintById(
     * (String)_c_contr.elementAt(i)); Constraint constraint =
     * (Constraint)ct.executable(); constraint.execute(); traceln("contr "+ct+"
     * activated!"); ct.status(STATUS_ACTIVE);
     * removeFromContradictionList(ct.getId()); i--;
     * addToActiveList(ct.getId()); traceln("OK:Contradiction eliminated "+ct); }
     * catch(Failure f) { if (ct!=null) { traceln("OK:Failure to activate contr
     * "+ct); ct.status(STATUS_INCOMPATIBLE); } else traceln("Impossible Failure
     * in executeActivatedConstraints()"); // success = false; } catch(Exception
     * e) { traceln("executeActivatedConstraints Exception");
     * e.printStackTrace(); System.exit(0); } }
     *  // traceVars(); return true; }
     */

    protected CccResource getResourceById(String id) {
        return (CccResource) _objects.get(id);
    }

    protected CccResource getResourceByNum(int i) {
        return getResourceById(getResourceId(i));
    }

    protected String getResourceId() {
        return "resource";
    }

    // ---goal
    // activation----------------------------------------------------------

    protected String getResourceId(int i) {
        return getResourceId() + getDivider() + i;
    }

    public int getSolutionNumber() {
        return _solution_num;
    }

    public String getStatus() {
        return _status;
    }

    public int getTimeout() {
        return _timeout;
    }

    public Vector goals() {
        return _goals;
    }

    public Vector integers() {
        return _integers;
    }

    // ---constraint
    // activation-----------------------------------------------------

    public boolean isActivated(String s) {
        if (s.equals(_activated_goal_id)) {
            return true;
        }

        if (_c_active.contains(s)) {
            return true;
        }
        if (_c_contr.contains(s)) {
            return true;
        }

        return false;
    }

    public boolean isIncompatible(String s) {
        if (_c_contr.contains(s)) {
            return true;
        }
        return false;
    }

    protected boolean isInitializing() {
        return _initializing;
    }

    protected boolean isOk() {
        return _ok;
    }

    public Vector jobs() {
        return _jobs;
    }

    public String name() {
        return _name;
    }

    // -----------------------------------------------------------------------------

    public CccVariable objective() {
        return _objective;
    }

    protected boolean postConstraint(String id) {
        CccConstraint ct = null;
        try {
            ct = getConstraintById(id);
            Constraint constraint = (Constraint) ct.executable();
            traceln("Activating C: " + ct);
            constrainer().postConstraint(constraint);
            traceln("C:" + ct + " activated!");
            ct.status(STATUS_ACTIVE);
            if (removeFromContradictionList(id)) {
                addToActiveList(id);
            }
        } catch (Failure f) {
            if (ct != null) {
                traceln(" WRONG: Failure to activate " + ct + ", " + isActivated(ct.getId()));
                // ct.activated(true);
                removeFromActiveList(id);
                addToContradictionList(id);
                setLastFailedId(id);
                ct.status(STATUS_INCOMPATIBLE);
                setLastFailed(true);
            } else {
                traceln("Impossible Failure in executeActivatedConstraints()");
            // success = false;
            }
        } catch (Exception e) {
            Log.error("!postConstraint() exception", e);
        }
        return true;
    }

    protected void putObject(String id, CccObject o) {
        // if (isInitializing())
        // System.out.println("ADD: "+id+" \t= "+o);
        o.setId(id);
        _objects.put(id, o);
    }

    protected boolean removeFromActiveList(String id) {
        return _c_active.remove(id);
    }

    protected boolean removeFromContradictionList(String id) {

        return _c_contr.remove(id);
    }

    private void reset() {
        _constrainer = new Constrainer("");
        _constrainer.setTimeLimit(getTimeout());

        // _constraints_num = 0;
        // _goals_num = 0;
        // _floats_num = 0;
        // _jobs_num = 0;
        // _resources_num = 0;
        for (int i = 0; i < groups.size(); i++) {
            CccGroup g = (CccGroup) groups.get(i);
            g.reset();
        }

        setLastFailedId(null);
        setLastFailed(false);
    }

    public Vector resources() {
        return _resources;
    }

    public Schedule schedule() {
        return _schedule;
    }

    public void setExecutionTime(long t) {
        _exec_time = t;
    }

    // -----------------------------------------------------------------------------
    protected void setInitializing(boolean f) {
        _initializing = f;
    }

    public void setLastFailed(boolean f) {
        _failed = f;
    }

    public void setLastFailedId(String id) {
        _last_failed_id = id;
    }

    public void setMaxSolutionNum(int n) {
        _max_solution_num = n;
    }

    protected void setOk(boolean f) {
        _ok = f;
    }

    public void setSolutionNumber(int n) {
        _solution_num = n;
    }

    public void setStatus(String s) {
        traceln("STATUS: " + s);
        _status = s;
    }

    public void setTimeout(int n) {
        _timeout = n;
    }

    synchronized protected boolean solve() {
        traceln("[--------------------------------------] > solve()");
        // reset
        reset();

        long time1 = System.currentTimeMillis();

        try {
            // run
            try {
                traceln("RUNNIG PROBLEM");
                _problem.main(this);
            } catch (Exception e) {
                Log.error("EXCEPTION DURING _problem.run()", e);
                throw e;
            }

            // activate constraints
            // traceln("ACTIVATION CONTRADICTIONS");
            // executeActivatedConstraints();

            traceln("EXECUTION GOAL");
            // activate goals
            if (getActivatedGoalId() != null) {
                boolean res = false;
                if (getSolutionNumber() > 1) {
                    int n = getSolutionNumber();
                    traceln("Looking for solution #" + n);
                    GoalCheckSolutionNumber chk = new GoalCheckSolutionNumber(constrainer(), n);
                    Goal goal = new GoalAnd(getGoalById(getActivatedGoalId()).executable(), chk);
                    // System.out.print("EXECUTING SOLUTION("+n+"): "+goal);
                    res = constrainer().execute(goal);
                    setSolutionNumber(chk.getCurrentSolutionNumber());
                    // if (!res) {
                    // System.out.print("FAILED ON SOLUTION("+n+")");
                    // }

                } else {
                    Goal goal = getGoalById(getActivatedGoalId()).executable();
                    // System.out.print("EXECUTING GOAL: "+goal);
                    res = constrainer().execute(goal);
                }
                // System.out.println(" => "+res);

                getGoalById(getActivatedGoalId()).status(STATUS_ACTIVE);

                traceVars();
                if (!res) {
                    throw new Exception("Goal exec failed");
                }

            } else {
                // System.out.println("NO ACTIVE GOALS");
            }

            if (isOk()) {
                setOk(!getLastFailed());
            } else {
                setOk(!getLastFailed());
                setLastFailed(false);
            }
            traceln("+getLastFailed() = " + getLastFailed());
            traceln("+isOk() = " + isOk());

        } catch (Exception e) {
            Log.debug("Failure during execution.");
            // e.printStackTrace();
            if (getActivatedGoalId() != null) {
                getGoalById(getActivatedGoalId()).status(STATUS_INCOMPATIBLE);
            }
            if (isOk()) {
                Log.debug("ACTIVATION FAILURE");
                setLastFailed(true);
            }
            setOk(false);
        }
        setExecutionTime(System.currentTimeMillis() - time1);
        Log.debug("Execution time: " + getExecutionTime() + "ms // of " + getTimeout() * 1000);

        if (getExecutionTime() > getTimeout() * 1000) {
            Log.debug("Execution timeout - extra: " + (getExecutionTime() - getTimeout() * 1000) + "ms");
            setStatus("Timeout during execution: " + getExecutionTime() + "ms");
            setOk(false);
        }

        // fetch?
        fetchConstrainerState(true);

        traceln("[--------------------------------------] solve() >");
        return isOk();
    }

    public String stat() {
        return _constrainer.toString() + (_schedule != null ? _schedule.toString() : "");
    }

    // ---debug---------------------------------------------------------------------

    public void traceError(String s) {
        Log.error(s);
    }

    /*
     * public void trace(String s) { Log.debug(s); }
     */
    public void traceln(String s) {
        // Log.debug(s);
    }

    public void traceVars() {
        // System.out.println("@INT: "+constrainer().integers());
        // System.out.println("@FLT: "+constrainer().floats());
        // if (_objective!=null)
        // System.out.println("@OBJ: "+_objective+", "+_objective.debugInfo());
    }

}
