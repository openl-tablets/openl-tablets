package org.openl.ie.scheduler;

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
import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.IntBoolExp;
import org.openl.ie.constrainer.IntVar;

/**
 * An implementation of interval job. This is the type of activity which has two
 * independent parameters: start time and duration.
 */
public class JobInterval implements Job {

    private Constrainer _constrainer;
    private Schedule _schedule;

    private IntVar _start;
    private IntVar _end;

    private int _duration;
    private int _s_min;
    private int _e_max;
    private String _name;

    // //////////////////////////////////////////////////////////////////////////////

    public JobInterval(Schedule sch, int duration) throws Failure {
        _duration = duration;
        _schedule = sch;
        _constrainer = _schedule.constrainer();
        _start = _constrainer.addIntVar(_schedule.start(), _schedule.end());
        _end = _constrainer.addIntVar(_schedule.start(), _schedule.end());
        _constrainer.postConstraint(_end.eq(_start.add(_duration)));
        saveAssignmentInfo();
    }

    public boolean bound() {
        return _start.bound();
    }

    public Constrainer constrainer() {
        return _constrainer;
    }

    public int duration() {
        return _duration;
    }

    public int endMax() {
        return _end.max();
    }

    public int endMaxA() {
        return _e_max;
    }

    public IntBoolExp endsAfterEnd(Job job) // +?
    {
        return job.getEndVariable().le(_end);
    }

    public IntBoolExp endsAfterStart(Job job) // +?
    {
        return job.getStartVariable().le(_end);
    }

    public String getAssignment() {
        return _schedule.getAssignments(this);
    }

    public IntVar getEndVariable() {
        return _end;
    }

    public String getName() {
        return _name;
    }

    public IntVar getStartVariable() {
        return _start;
    }

    public AlternativeResourceConstraint requires(AlternativeResourceSet res, int capacity) {
        return new AlternativeResourceConstraint(this, res, capacity);
    }

    public AlternativeResourceConstraint requires(AlternativeResourceSet res, IntVar capacity) {
        return new AlternativeResourceConstraint(this, res, capacity);
    }

    public AlternativeResourceConstraint requires(Resource res, int capacity) {
        AlternativeResourceSet set = new AlternativeResourceSet();
        set.add(res);
        return requires(set, capacity);
    }

    public AlternativeResourceConstraint requires(Resource res, IntVar capacity) {
        AlternativeResourceSet set = new AlternativeResourceSet();
        set.add(res);
        return requires(set, capacity);
    }

    public void saveAssignmentInfo() {
        _s_min = _start.min();
        _e_max = _end.max();
    }

    public Schedule schedule() {
        return _schedule;
    }

    public void setName(String name) {
        _name = name;
        _start.name(_name + ".start");
        _end.name(_name + ".end");
    }

    public int startMin() {
        return _start.min();
    }

    public int startMinA() {
        return _s_min;
    }

    public IntBoolExp startsAfterEnd(Job job) // +
    {
        return job.getEndVariable().le(_start);
    }

    public IntBoolExp startsAfterStart(Job job) // +?
    {
        return job.getStartVariable().le(_start);
    }

    public IntBoolExp startsAtStart(Job job) // +?
    {
        return job.getStartVariable().eq(_start);
    }

    @Override
    public String toString() {
        return getName() + value();
    }

    public String value() {
        try {
            String assignments = _schedule.getAssignments(this);
            return "[" + getStartVariable().value() + " --" + _duration + "--> " + getEndVariable().value() + ")"
                    + (assignments.equalsIgnoreCase("") ? "" : " => " + assignments);
        } catch (Failure f) {
            return "[" + getStartVariable().domainToString() + " --" + _duration + "--> "
                    + getEndVariable().domainToString() + ")";
        }
    }
}
