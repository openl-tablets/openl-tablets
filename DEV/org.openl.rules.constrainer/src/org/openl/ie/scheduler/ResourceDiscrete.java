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
import org.openl.ie.constrainer.IntExp;
import org.openl.ie.constrainer.IntExpArray;

/**
 * An interface for the representation of the different discrete Resources
 * (people, machines, materials).
 * <p>
 * Resource has diferent capacities in different times. There are
 * require-constraints between jobs and resources.
 *
 * Examples:
 *
 * <pre>
 * Job job = schedule.addJob(duration, &quot;job1&quot;);
 * Resource resource = schedule.addResourceDiscrete(capacity, &quot;truck1&quot;);
 *
 * job.requires(resource).post();
 * </pre>
 *
 * @see Job
 * @see ResourceUnary
 */
public class ResourceDiscrete implements Resource {

    private Object _object;
    private Schedule _schedule;
    private Constrainer _constrainer;

    private String _name;
    private IntExpArray _caps;
    private int _timeMin;
    private int _timeMax;
    private int _duration;

    /**
     * Constructs resource with specified initial capacity available diring all
     * the schedule
     *
     * @param sch Schedule to associate with
     * @param capacity Resource capacity
     */
    public ResourceDiscrete(Schedule sch, int capacity) {
        init(sch, sch.start(), sch.end(), capacity);
    }

    /**
     * Constructs resource with specified initial capacity and interval
     *
     * @param sch Schedule to associate with
     * @param timeMin Start time of resource availability
     * @param timeMax End time of resource availability
     * @param capacity Resource capacity
     */
    public ResourceDiscrete(Schedule sch, int timeMin, int timeMax, int capacity) {
        init(sch, timeMin, timeMax, capacity);
    }

    /**
     * Returns an array of capacity variables
     *
     * @return An array of capacity variables
     */
    public IntExpArray caps() {
        return _caps;
    }

    /**
     * Returns constrainer this resource associated with
     *
     * @return Constrainer this resource associated with
     */
    public Constrainer constrainer() {
        return _constrainer;
    }

    /**
     * Returns resource availability durarion
     *
     * @return Resource availability durarion
     */
    public int duration() {
        return _duration;
    }

    public String getAssignment() {
        return _schedule.getAssignments(this);
    }

    public int getCapacityMax(int time) throws Failure {
        return _caps.get(time - _timeMin).max();
    }

    public int getCapacityMin(int time) throws Failure {
        return _caps.get(time - _timeMin).min();
    }

    /**
     * Returns internal variable associated with capacity at specified moment of
     * time
     *
     * @param time Moment of time
     * @return Capacity variable
     * @throws Failure
     */
    public IntExp getCapacityVar(int time) throws Failure {
        return _caps.get(time - _timeMin);
    }

    /**
     * Returns resource name
     *
     * @return Resource name
     */
    public String getName() {
        return _name;
    }

    public Object getObject() {
        return _object;
    }

    /**
     * Initializes internal data structures
     *
     * @param sch Schedule
     * @param timeMin Available from
     * @param timeMax Available to
     * @param capacity Capacity
     */
    private void init(Schedule sch, int timeMin, int timeMax, int capacity) {
        _object = null;
        _schedule = sch;
        _constrainer = _schedule.constrainer();
        _timeMax = timeMax;
        _timeMin = timeMin;

        int i;
        _duration = _timeMax - _timeMin;
        _caps = new IntExpArray(_constrainer, _duration);
        for (i = 0; i < _duration; i++) {
            _caps.set(_constrainer.addIntVar(0, capacity), i);
        }
    }

    /**
     * Returns string representation of current state of resource
     *
     * @return String representation
     */
    public String mapString() {
        StringBuilder s = new StringBuilder();
        s.append("[");
        for (int i = 0; i < _duration; i++) {
            s.append(".").append(_caps.get(i).max());
        }
        s.append(".]");
        return s.toString();
    }

    public void setCapacityMax(int time, int capacity) throws Failure {
        _caps.get(time - _timeMin).setMax(capacity);
    }

    public void setCapacityMax(int time1, int time2, int capacity) throws Failure {
        for (int i = time1 - _timeMin; i < time2 - _timeMin; i++) {
            _caps.get(i).setMax(capacity);
        }
    }

    public void setCapacityMin(int time, int capacity) throws Failure {
        _caps.get(time - _timeMin).setMin(capacity);
    }

    public void setCapacityMin(int time1, int time2, int capacity) throws Failure {
        for (int i = time1 - _timeMin; i < time2 - _timeMin; i++) {
            _caps.get(i).setMin(capacity);
        }
    }

    /**
     * Sets resource name
     *
     * @param name New resource name
     */
    public void setName(String name) {
        _name = name;
        for (int i = 0; i < _duration; i++) {
            Integer v = new Integer(i);
            _caps.get(i).name(_name + "." + v);
        }
    }

    public void setObject(Object o) {
        _object = o;
    }

    /**
     * Returns resource availability end time
     *
     * @return Resource availability end time
     */
    public int timeMax() {
        return _timeMax;
    }

    /**
     * Returns resource availability start time
     *
     * @return Resource availability start time
     */
    public int timeMin() {
        return _timeMin;
    }

    @Override
    public String toString() {
        String assignments = _schedule.getAssignments(this);
        return getName() + (assignments.equalsIgnoreCase("") ? "" : " : " + assignments);
    }
}