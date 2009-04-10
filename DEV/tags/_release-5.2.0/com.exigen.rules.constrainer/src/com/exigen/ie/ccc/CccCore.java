package com.exigen.ie.ccc;

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

import org.openl.util.Log;

import com.exigen.ie.constrainer.Constrainer;
import com.exigen.ie.constrainer.Constraint;
import com.exigen.ie.constrainer.Failure;
import com.exigen.ie.constrainer.FloatExp;
import com.exigen.ie.constrainer.Goal;
import com.exigen.ie.constrainer.GoalAnd;
import com.exigen.ie.constrainer.GoalCheckSolutionNumber;
import com.exigen.ie.constrainer.IntBoolExp;
import com.exigen.ie.constrainer.IntExp;
import com.exigen.ie.constrainer.IntExpArray;
import com.exigen.ie.constrainer.NonLinearExpression;
import com.exigen.ie.constrainer.lpsolver.ConstrainerLP;
import com.exigen.ie.scheduler.Job;
import com.exigen.ie.scheduler.Resource;
import com.exigen.ie.scheduler.Schedule;

public class CccCore implements CccConst
{
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

//	private int _constraints_num;
//	private int _goals_num;
//	private int _integers_num;
//	private int _floats_num;
//	private int _jobs_num;
//	private int _resources_num;

	private String _activated_goal_id;

	private int _solution_num;

	private String _status;
	private String _last_failed_id;

	private long _exec_time;
	// -----------------------------------------------------------------------------

	/**
	 * CccCore constructor.
	 */
	public CccCore()
	{
		super();
		_name = "null";
		_valid = false;
	}

	public void flush()
	{
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

	private void reset()
	{
		_constrainer = new Constrainer("");
		_constrainer.setTimeLimit(getTimeout());

//		_constraints_num = 0;
//		_goals_num = 0;
//		_floats_num = 0;
//		_jobs_num = 0;
//		_resources_num = 0;
		for (int i=0; i<groups.size(); i++)
		{
			CccGroup g = (CccGroup)groups.get(i);
			g.reset();
		}

		setLastFailedId(null);
		setLastFailed(false);
	}

	synchronized public void assignProblem(CccProblem p)
	{
		_problem = p;
		flush();
		_status = "Initialized new problem";
		setInitializing(true);
		reset();
		try
		{
			_problem.main(this);
		}
		catch (Exception e)
		{
			Log.error("ASSIGN: EXCEPTION DURING _problem.run()", e);
		}
		setInitializing(false);
	}

	synchronized protected boolean solve()
	{
		traceln("[--------------------------------------] > solve()");
		// reset
		reset();

		long time1 = System.currentTimeMillis();

		try
		{
			// run
			try
			{
				traceln("RUNNIG PROBLEM");
				_problem.main(this);
			}
			catch (Exception e)
			{
				Log.error("EXCEPTION DURING _problem.run()", e);
				throw e;
			}

			// activate constraints
			//    traceln("ACTIVATION CONTRADICTIONS");
			//    executeActivatedConstraints();

			traceln("EXECUTION GOAL");
			// activate goals
			if (getActivatedGoalId() != null)
			{
				boolean res = false;
				if (getSolutionNumber() > 1)
				{
					int n = getSolutionNumber();
					traceln("Looking for solution #"+n);
					GoalCheckSolutionNumber chk =
						new GoalCheckSolutionNumber(constrainer(), n);
					Goal goal =
						new GoalAnd(getGoalById(getActivatedGoalId()).executable(), chk);
					//          System.out.print("EXECUTING SOLUTION("+n+"): "+goal);
					res = constrainer().execute(goal);
					setSolutionNumber(chk.getCurrentSolutionNumber());
					//          if (!res) {
					//            System.out.print("FAILED ON SOLUTION("+n+")");
					//          }

				}
				else
				{
					Goal goal = getGoalById(getActivatedGoalId()).executable();
					//          System.out.print("EXECUTING GOAL: "+goal);
					res = constrainer().execute(goal);
				}
				//        System.out.println(" => "+res);

				getGoalById(getActivatedGoalId()).status(STATUS_ACTIVE);

				traceVars();
				if (!res)
					throw new Exception("Goal exec failed");

			}
			else
			{
				//      System.out.println("NO ACTIVE GOALS");
			}

			if (isOk())
			{
				setOk(!getLastFailed());
			}
			else
			{
				setOk(!getLastFailed());
				setLastFailed(false);
			}
			traceln("+getLastFailed() = " + getLastFailed());
			traceln("+isOk() = " + isOk());

		}
		catch (Exception e)
		{
			Log.debug("Failure during execution.");
			//      e.printStackTrace();
			if (getActivatedGoalId() != null)
				getGoalById(getActivatedGoalId()).status(STATUS_INCOMPATIBLE);
			if (isOk())
			{
				Log.debug("ACTIVATION FAILURE");
				setLastFailed(true);
			}
			setOk(false);
		}
		setExecutionTime(System.currentTimeMillis() - time1);
		Log.debug("Execution time: " + getExecutionTime() + "ms // of "+getTimeout()*1000);

		if (getExecutionTime() > getTimeout() * 1000)
		{
			Log.debug("Execution timeout - extra: " + (getExecutionTime() - getTimeout() * 1000) + "ms");
			setStatus("Timeout during execution: " + getExecutionTime() + "ms");
			setOk(false);
		}

		// fetch?
		fetchConstrainerState(true);

		traceln("[--------------------------------------] solve() >");
		return isOk();
	}

	// -----------------------------------------------------------------------------
	protected void setInitializing(boolean f)
	{
		_initializing = f;
	}
	protected boolean isInitializing()
	{
		return _initializing;
	}

	protected void setOk(boolean f)
	{
		_ok = f;
	}
	protected boolean isOk()
	{
		return _ok;
	}

	public void setLastFailed(boolean f)
	{
		_failed = f;
	}
	public boolean getLastFailed()
	{
		return _failed;
	}

	public void setExecutionTime(long t)
	{
		_exec_time = t;
	}
	public long getExecutionTime()
	{
		return _exec_time;
	}

	public void setStatus(String s)
	{
		traceln("STATUS: " + s);
		_status = s;
	}

	public String getStatus()
	{
		return _status;
	}

	protected void putObject(String id, CccObject o)
	{
		//    if (isInitializing())
		//      System.out.println("ADD: "+id+" \t= "+o);
		o.setId(id);
		_objects.put(id, o);
	}

	protected CccObject getObject(String id, int i)
	{
		//    System.out.println("? "+id+"("+i+") \t: "+_objects.get( id+getDivider()+i ) );
		return (CccObject) _objects.get(id + getDivider() + i);
	}

	protected CccObject getObject(String id)
	{
		//    System.out.println("? "+id+" \t: "+_objects.get( id ) );
		return (CccObject) _objects.get(id);
	}

	// -----------------------------------------------------------------------------

	protected CccGoal getGoalById(String id)
	{
		return (CccGoal) _objects.get(id);
	}

	protected CccConstraint getConstraintById(String id)
	{
		return (CccConstraint) _objects.get(id);
	}

	protected CccInteger getIntegerById(String id)
	{
		return (CccInteger) _objects.get(id);
	}

	protected CccFloat getFloatById(String id)
	{
		return (CccFloat) _objects.get(id);
	}

	protected CccJob getJobById(String id)
	{
		return (CccJob) _objects.get(id);
	}

	protected CccResource getResourceById(String id)
	{
		return (CccResource) _objects.get(id);
	}

	protected CccVariable getObjectiveById(String id)
	{
		return (CccVariable) _objects.get(id);
	}

	// -----------------------------------------------------------------------------

	protected CccGoal getGoalByNum(int i)
	{
		return getGoalById(getGoalId(i));
	}

	protected CccConstraint getConstraintByNum(int i)
	{
		return getConstraintById(getConstraintId(i));
	}

	protected CccInteger getIntegerByNum(int i)
	{
		return getIntegerById(getIntegerId(i));
	}

	protected CccFloat getFloatByNum(int i)
	{
		return getFloatById(getFloatId(i));
	}

	protected CccJob getJobByNum(int i)
	{
		return getJobById(getJobId(i));
	}

	protected CccResource getResourceByNum(int i)
	{
		return getResourceById(getResourceId(i));
	}

	protected CccVariable getObjectiveByNum(int i)
	{
		return getObjectiveById(getObjectiveId(i));
	}

	// ---ids-----------------------------------------------------------------------

	protected String getDivider()
	{
		return ",";
	}

	protected String getConstraintId()
	{
		return "constraint";
	}
	protected String getConstraintId(int i)
	{
		return getConstraintId() + getDivider() + i;
	}

	protected String getGoalId()
	{
		return "goal";
	}
	protected String getGoalId(int i)
	{
		return getGoalId() + getDivider() + i;
	}

	protected String getIntegerId()
	{
		return "integer";
	}
	protected String getIntegerId(int i)
	{
		return getIntegerId() + getDivider() + i;
	}

	protected String getFloatId()
	{
		return "float";
	}
	protected String getFloatId(int i)
	{
		return getFloatId() + getDivider() + i;
	}

	protected String getJobId()
	{
		return "job";
	}
	protected String getJobId(int i)
	{
		return getJobId() + getDivider() + i;
	}

	protected String getResourceId()
	{
		return "resource";
	}
	protected String getResourceId(int i)
	{
		return getResourceId() + getDivider() + i;
	}

	protected String getObjectiveId()
	{
		return "objective";
	}
	protected String getObjectiveId(int i)
	{
		return getObjectiveId() + getDivider() + i;
	}

	protected String getByGroupId(CccGroup g, int i)
	{
		return g.getName() + getDivider() + i;
	}

	// ---addConstraint-------------------------------------------------------------

	public CccConstraint addConstraint(CccConstraint c)
	{
		CccGroup g = getGroup(getConstraintId());
		if (isInitializing())
		{
			_constraints.addElement(getConstraintId(_constraints.size()));
		}
		String id = g.add(c);
		putObject(id, c);
		if (isActivated(id))
		{
			postConstraint(id);
			try
			{
				if (c.getSimplex() != null)
					c.getSimplex().addConstraint(c.getBoolExp(),false);
			}
			catch (NonLinearExpression e)
			{
				Log.error(e);
			}
			fetchConstrainerState(false);
		}
		return c;
	}

	public CccConstraint addConstraint(Constraint c, String name)
		throws Exception
	{
		CccConstraint cc = new CccConstraint(this, name);
		cc.executable(c);
		return addConstraint(cc);
	}

//	public CccConstraint addConstraint(String c, String name) throws Exception
//	{
//		CccConstraint cc = new CccConstraint(this, name);
//		cc.executable(constrainer().evaluateConstraint(c));
//		return addConstraint(cc);
//	}

//	public CccConstraint addConstraint(String c) throws Exception
//	{
//		return addConstraint(c, c);
//	}

	public CccConstraint addConstraint(IntBoolExp e, String name)
		throws Exception
	{
		return addConstraint(e.asConstraint(), name);
	}

	public CccConstraint addConstraint(IntBoolExp e, String name, ConstrainerLP s)
		throws Exception
	{
		CccConstraint cc = new CccConstraint(this, name);
		cc.executable(e.asConstraint());
		cc.setBoolExp(e);
		cc.setSimplex(s);
		return addConstraint(cc);
	}

	// ---addGoal-------------------------------------------------------------

	public CccGoal addGoal(CccGoal g)
	{
		CccGroup grp = getGroup(getGoalId());
		if (isInitializing())
		{
			_goals.addElement(getGoalId(_goals.size()));
		}
		putObject(grp.add(g), g);
		return g;
	}

	public CccGoal addGoalMinimize()
	{
		return addGoal(objective().getMinimizeGoal());
	}

	public CccGoal addGoalMaximize()
	{
		return addGoal(objective().getMaximizeGoal());
	}

	public CccGoal addGoalSolution(Goal solv)
	{
		CccGoal gs = new CccGoalSolution(this);
		gs.executable(solv);
		return addGoal(gs);
	}

	// ---addInteger----------------------------------------------------------------

	public CccInteger addInteger(CccInteger i)
	{
		CccGroup g = getGroup(getIntegerId());
		if (isInitializing())
		{
			String id = g.add(i);
			_integers.addElement(id);
		}
		putObject(g.add(i), i);
		return i;
	}

	public CccInteger addInteger(IntExp i)
	{
		CccInteger ci = new CccInteger(this, i);
		return addInteger(ci);
	}

	public CccInteger addInteger(IntExp i, String name)
	{
		CccInteger ci = new CccInteger(this, i, name);
		return addInteger(ci);
	}

	public void addIntExpArray(IntExpArray a, String name)
	{
		CccGroup g = getGroup(name);
		for (int i=0; i<a.size(); i++)
		{
			IntExp v = a.get(i);
			CccInteger ci = new CccInteger(this, v);
			g.add(ci);
			if (isInitializing())
			{
				String id = g.add(ci);
//				_integers.addElement(id);
			}
			putObject(g.add(ci), ci);
		}
	}

	// ---addFloat----------------------------------------------------------------

	public CccFloat addFloat(CccFloat f)
	{
		CccGroup g = getGroup(getFloatId());
		if (isInitializing())
		{
			String id = g.add(f);
			_floats.addElement(id);
		}
		putObject(g.add(f), f);
		return f;
	}

	// ---addJob/Resource-----------------------------------------------------------

	public CccJob addJob(Job job)
	{
		CccGroup g = getGroup(getJobId());
		CccJob j = new CccJob(this, job);

		if (isInitializing())
		{
			String id = getJobId(_jobs.size());
			//      trace("addjob "+id);
			_jobs.addElement(id);
		}
		putObject(g.add(j), j);

		return j;
	}

	public CccResource addResource(Resource resource)
	{
		CccGroup g = getGroup(getResourceId());
		CccResource r = new CccResource(this, resource);

		if (isInitializing())
		{
			String id = getResourceId(_resources.size());
			_resources.addElement(id);
		}
		putObject(g.add(r), r);

		return r;
	}

	// ---addObjective--------------------------------------------------------------

	public void addObjective(CccVariable obj)
	{
		CccGroup g = getGroup(getObjectiveId());
		_objective = obj;
		_objective.setType(TM_OBJECTIVE);
		putObject(g.add(_objective), _objective);
	}

	public void addObjective(IntExp e)
	{
		addObjective(new CccInteger(this, e));
	}

	public void addObjective(IntExp e, String name)
	{
		addObjective(new CccInteger(this, e, name));
	}

	public void addObjective(FloatExp e)
	{
		addObjective(new CccFloat(this, e));
	}

	public void addObjective(FloatExp e, String name)
	{
		addObjective(new CccFloat(this, e, name));
	}

	public CccVariable objective()
	{
		return _objective;
	}

	// ---shedule----------------------------------------------------------------

	public Schedule createSchedule(int start, int end)
	{
		_schedule = new Schedule(_constrainer, start, end);
		_schedule.setName("schedule for " + _name);
		return _schedule;
	}

	// ---active lists--------------------------------------------------------------

	protected void addToActiveList(String id)
	{
		_c_active.add(id);
	}

	public Vector getActiveList()
	{
		return _c_active;
	}

	protected void addToContradictionList(String id)
	{
		if (!_c_contr.contains(id))
			_c_contr.add(id);
	}
	protected boolean removeFromActiveList(String id)
	{
		return _c_active.remove(id);
	}
	protected boolean removeFromContradictionList(String id)
	{

		return _c_contr.remove(id);
	}

	public Constrainer constrainer()
	{
		return _constrainer;
	}
	public void constrainer(Constrainer c)
	{
		_constrainer = c;
	}

	public Schedule schedule()
	{
		return _schedule;
	}

	public Vector constraints()
	{
		return _constraints;
	}
	/*
	  public boolean executeActivatedConstraints()
	  {
	    CccConstraint ct = null;
	    // trying to activate contradictory constraints
	    for(int i = 0; i < _c_contr.size(); i++)
	    {
	      try {
	        ct = getConstraintById( (String)_c_contr.elementAt(i));
	        Constraint constraint = (Constraint)ct.executable();
	        constraint.execute();
	        traceln("contr "+ct+" activated!");
	        ct.status(STATUS_ACTIVE);
	        removeFromContradictionList(ct.getId());
	        i--;
	        addToActiveList(ct.getId());
	        traceln("OK:Contradiction eliminated "+ct);
	      }
	      catch(Failure f)
	      {
	        if (ct!=null)
	        {
	          traceln("OK:Failure to activate contr "+ct);
	          ct.status(STATUS_INCOMPATIBLE);
	        }
	        else
	          traceln("Impossible Failure in executeActivatedConstraints()");
	//        success = false;
	      }
	      catch(Exception e)
	      {
	        traceln("executeActivatedConstraints Exception");
	        e.printStackTrace();
	        System.exit(0);
	      }
	    }
	
	//    traceVars();
	    return true;
	  }
	*/

	protected boolean postConstraint(String id)
	{
		CccConstraint ct = null;
		try
		{
			ct = getConstraintById(id);
			Constraint constraint = (Constraint) ct.executable();
			traceln("Activating C: " + ct);
			constrainer().postConstraint(constraint);
			traceln("C:" + ct + " activated!");
			ct.status(STATUS_ACTIVE);
			if (removeFromContradictionList(id))
				addToActiveList(id);
		}
		catch (Failure f)
		{
			if (ct != null)
			{
				traceln(
					" WRONG: Failure to activate " + ct + ", " + isActivated(ct.getId()));
				//                ct.activated(true);
				removeFromActiveList(id);
				addToContradictionList(id);
				setLastFailedId(id);
				ct.status(STATUS_INCOMPATIBLE);
				setLastFailed(true);
			}
			else
				traceln("Impossible Failure in executeActivatedConstraints()");
			//        success = false;
		}
		catch (Exception e)
		{
			Log.debug("!postConstraint() exception", e);
			System.exit(0);
		}
		return true;
	}

	public CccGoalSolution getGoalSolution()
	{
		CccGoalSolution s = null;
		for (int i = 0; i < goals().size(); i++)
		{
			CccGoal g = getGoalByNum(i);
			if (g instanceof CccGoalSolution)
			{
				s = (CccGoalSolution) g;
				break;
			}
		}
		return s;
	}

	public String getGoalSolutionId()
	{
		for (int i = 0; i < goals().size(); i++)
			if (getGoalByNum(i) instanceof CccGoalSolution)
				return getGoalId(i);
		return null;
	}

	// ---goal activation----------------------------------------------------------

	synchronized public boolean activateGoal(String goal, int solution)
	{
		//    System.out.println("activateGoal("+goal+", "+solution+")!");
		for (int i = 0; i < _goals.size(); i++)
			getGoalByNum(i).status(STATUS_INACTIVE);

		_activated_goal_id = goal;
		setSolutionNumber(solution);
		if (isInitializing())
			return true;
		return solve();
	}

	public boolean activateGoal(String goal)
	{
		return activateGoal(goal, 1);
	}

	public boolean activateSolution(int n)
	{
		return activateGoal(getGoalSolutionId(), n);
	}

	synchronized public void deactivateGoal()
	{
		activateGoal(null, 1);
	}

	public String getActivatedGoalId()
	{
		return _activated_goal_id;
	}

	protected CccExecutable getActivatedGoal()
	{
		return (CccExecutable) getObject(_activated_goal_id);
	}

	// ---constraint activation-----------------------------------------------------

	synchronized public boolean activateConstraint(String id)
	{
		addToActiveList(id);

		CccConstraint c = getConstraintById(id);

		traceln("activateConstraint(" + id + ") :" + c);

		if (isInitializing())
			return true;
		boolean result = solve();
		setStatus(
			"Activated constraint "
				+ c.name()
				+ ", time: "
				+ getExecutionTime()
				+ "ms");
		if (result == false)
		{
			c.status(STATUS_INCOMPATIBLE);
			setLastFailedId(id);
			removeFromActiveList(id);
			addToContradictionList(id);
		}

		return result;
	}

	synchronized public boolean activateAllConstraints()
	{
		//    System.out.println("activateAllConstraints()");

		for (int i = 0; i < constraints().size(); i++)
		{
			String id = (String) constraints().elementAt(i);
			if (!isActivated(id))
				addToActiveList(id);
		}
		if (isInitializing())
			return true;
		return solve();
	}

	synchronized public void deactivateConstraint(String id)
	{
		//    System.out.println("deactivateConstraint(String id)");
		removeFromActiveList(id);
		removeFromContradictionList(id);

		CccConstraint c = getConstraintById(id);
		c.status(STATUS_INACTIVE);

		if (isInitializing())
			return;
		solve();
	}

	synchronized public void deactivateAllConstraints()
	{
		//    System.out.println("deactivateAllConstraints()");

		_c_active.clear();
		_c_contr.clear();

		if (isInitializing())
			return;
		solve();
	}

	public boolean isActivated(String s)
	{
		if (s.equals(_activated_goal_id))
			return true;

		if (_c_active.contains(s))
			return true;
		if (_c_contr.contains(s))
			return true;

		return false;
	}

	public boolean isIncompatible(String s)
	{
		if (_c_contr.contains(s))
			return true;
		return false;
	}

	// -----------------------------------------------------------------------------

	public void setSolutionNumber(int n)
	{
		_solution_num = n;
	}

	public int getSolutionNumber()
	{
		return _solution_num;
	}

	public Vector goals()
	{
		return _goals;
	}

	public Vector integers()
	{
		return _integers;
	}

	public Vector floats()
	{
		return _floats;
	}

	public Vector jobs()
	{
		return _jobs;
	}

	public Vector resources()
	{
		return _resources;
	}

	public String name()
	{
		return _name;
	}

	public String stat()
	{
		return _constrainer.toString()
			+ (_schedule != null ? _schedule.toString() : "");
	}

	/*  public void removeConstraint(CccConstraint c)
	  {
	    _constraints.removeElement(c);
	  }
	*/
	protected void fetchConstrainerState(boolean jj)
	{
		for (int i = 0; i < groups.size(); i++)
		{
			CccGroup g = (CccGroup)groups.get(i);
			for (int j = 0; j < g.size(); j++)
			{
				CccObject o = g.getObject(j);
				if (o instanceof CccInteger)
				{
					CccInteger v = (CccInteger)o;
					v.fetchConstrainerState();
				}
				if (o instanceof CccFloat)
				{
					CccFloat v = (CccFloat)o;
					v.fetchConstrainerState();
				}
				if (o instanceof CccJob)
				{
					CccJob v = (CccJob)o;
					v.fetchConstrainerState();
				}
				if (o instanceof CccResource)
				{
					CccResource v = (CccResource)o;
					v.fetchConstrainerState();
				}
			}
		}

		for (int i = 0; i < _integers.size(); i++)
			getIntegerByNum(i).fetchConstrainerState();

		for (int i = 0; i < _floats.size(); i++)
			getFloatByNum(i).fetchConstrainerState();

		for (int i = 0; i < _constraints.size(); i++)
			getConstraintByNum(i).fetchConstrainerState();

		for (int i = 0; i < _goals.size(); i++)
			getGoalByNum(i).fetchConstrainerState();

		Vector jon = new Vector();
		//    System.out.println("JOBS: "+_jobs.size());
		for (int i = 0; i < _jobs.size(); i++)
		{
			CccJob job = getJobByNum(i);
			if (job.isConstrained())
				jon.add(getJobId(i));
			else
				job.fetchConstrainerState();
		}

		if (jon.size() > 0)
		{
			//      System.out.println("+ACTIVATING SOLVER");
			Goal ex = getGoalSolution().executable();
			if (jj)
				constrainer().execute(ex, true);
			//      System.out.println("SOLVER DONE");

			//      System.out.println("BINDED JOBS: "+jon.size());
			for (int i = 0; i < jon.size(); i++)
			{
				CccJob job = getJobById((String) jon.elementAt(i));
				job.fetchConstrainerState();
			}
		}

		for (int i = 0; i < _resources.size(); i++)
			getResourceByNum(i).fetchConstrainerState();

		if (objective() != null)
			objective().fetchConstrainerState();
	}

	public void setMaxSolutionNum(int n)
	{
		_max_solution_num = n;
	}
	public int getMaxSolutionNum()
	{
		return _max_solution_num;
	}

	public void setTimeout(int n)
	{
		_timeout = n;
	}

	public int getTimeout()
	{
		return _timeout;
	}

	public void setLastFailedId(String id)
	{
		_last_failed_id = id;
	}

	public String getLastFailedId()
	{
		return _last_failed_id;
	}

	public Vector getGroups()
	{
		return groups;
	}

	public CccGroup getGroup(String name)
	{
		for (int i=0; i<groups.size(); i++)
		{
			CccGroup g = (CccGroup)groups.get(i);
			if (name.equals(g.getName()))
				return g;
		}
		CccGroup g = new CccGroup(this,name);
		groups.add(g);
		return g;
	}

	public boolean existsGroup(String name)
	{
		for (int i=0; i<groups.size(); i++)
		{
			CccGroup g = (CccGroup)groups.get(i);
			if (name.equals(g.getName()))
				return true;
		}
		return false;
	}

	// ---debug---------------------------------------------------------------------

	public void traceVars()
	{
		//    System.out.println("@INT: "+constrainer().integers());
		//    System.out.println("@FLT: "+constrainer().floats());
		//    if (_objective!=null)
		//    System.out.println("@OBJ: "+_objective+", "+_objective.debugInfo());
	}

	/*  public void trace(String s)
	  {
	  	Log.debug(s);
	  }
	*/
	public void traceln(String s)
	{
		//  	Log.debug(s);
	}

	public void traceError(String s)
	{
		Log.error(s);
	}

}
