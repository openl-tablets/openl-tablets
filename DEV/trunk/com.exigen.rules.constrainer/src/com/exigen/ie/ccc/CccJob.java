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

/**
 * class CccJob
 */
import java.util.Vector;

import com.exigen.ie.scheduler.AlternativeResourceConstraint;
import com.exigen.ie.scheduler.Job;

public class CccJob extends CccVariable
{
	private int _start_min;
	private int _end_max;
	private int _duration;
	private Job _constrainer_job;
	private String _value;
	private String _assignment;

	/**
	 * CccJob constructor comment.
	 */
	public CccJob(CccCore core, Job j)
	{
		super(core, TM_JOB, j.getName());
		setType(TM_JOB);
		constrainerJob(j);
	}

	public Job constrainerJob()
	{
		return _constrainer_job;
	}

	public void constrainerJob(Job job)
	{
		_constrainer_job = job;
		fetchConstrainerState();
	}

	public int startMin()
	{
		return _start_min;
	}

	public int endMax()
	{
		return _end_max;
	}

	public void fetchConstrainerState()
	{
		//    System.out.println("fetching job state: "+this);
		value(_constrainer_job.value());
		bound(_constrainer_job.bound());
		if (isConstrained())
		{
			//      core().traceln("+BIND  "+_constrainer_job.getStartVariable()+" "+_constrainer_job.getEndVariable());
			_start_min = _constrainer_job.startMinA();
			_end_max = _constrainer_job.endMaxA();
		}
		else
		{
			//     core().traceln("-EMPTY "+_constrainer_job.getStartVariable()+" "+_constrainer_job.getEndVariable());
			_start_min = _constrainer_job.startMin();
			_end_max = _constrainer_job.endMax();
		}
		_assignment = _constrainer_job.getAssignment();
		if (!bound())
		{
			status(STATUS_UNKNOWN);
		}
		else
		{
			status(STATUS_GREEN);
		}
	}

	public boolean isConstrained()
	{
		//     System.out.println("?constrained");
		Vector a = core().getActiveList();
		for (int i = 0; i < a.size(); i++)
		{
			String id = (String) a.elementAt(i);
			CccConstraint o = core().getConstraintById(id);
			//      if (o==null) {
			//        System.out.println("!");
			//      }
			if (o.executable() instanceof AlternativeResourceConstraint)
			{
				AlternativeResourceConstraint arc =
					(AlternativeResourceConstraint) o.executable();
				//           System.out.println("?chk: "+arc);
				if (arc.getJob().equals(_constrainer_job))
					return true;
			}
		}
		return false;
	}

	public String toString()
	{
		return value();
	}
	public String value()
	{
		return _value;
	}
	public void value(String v)
	{
		_value = v;
	}

	public CccGoal getMinimizeGoal()
	{
		return null;
	}
	public CccGoal getMaximizeGoal()
	{
		return null;
	}

	public String getInfo(String infotype)
	{
		if (infotype.equalsIgnoreCase("assignment"))
			return _assignment;
		return super.getInfo(infotype);
	}

	public String debugInfo()
	{
		return "ni";
	}

}
