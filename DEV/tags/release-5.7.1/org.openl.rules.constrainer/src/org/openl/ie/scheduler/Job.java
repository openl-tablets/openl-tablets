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
import org.openl.ie.constrainer.IntBoolExp;
import org.openl.ie.constrainer.IntVar;

/**
 * An interface for the representation of the different jobs (activities,
 * tasks).
 * <p>
 * Job has a known duration and an unknown start/end (constrained int
 * variables). There are precedence constraints between jobs, and
 * require-constraints between jobs and resources.
 * <p>
 * Examples:
 *
 * <pre>
 * Job job1 = schedule.addJob(duration1, name1);
 * Job job2 = schedule.addJob(duration2, name2);
 *
 * job2.startsAfterStart(job1).asConstraint().post();
 * </pre>
 *
 * @see Resource
 */
public interface Job {

    /**
     * Returns boolean reflecting whether job is scheduled (variables are bound)
     *
     * @return True if job is scheduled to specified time
     */
    public boolean bound();

    /**
     * Returns constrainer this job associated with
     *
     * @return Ñonstrainer this job associated with
     */
    public Constrainer constrainer();

    /**
     * Returns job duration
     *
     * @return Job duration
     */
    public int duration();

    /**
     * Returns maximum possible job end time
     *
     * @return Maximum possible job end time
     */
    public int endMax();

    public int endMaxA();

    /**
     * Returns boolean expression depends on job relationship
     *
     * @param job Job for compare
     * @return True if this job ends after end of parameter job
     */
    public IntBoolExp endsAfterEnd(Job job);

    /**
     * Returns boolean expression depends on job relationship
     *
     * @param job Job for compare
     * @return True if this job ends after start of parameter job
     */
    public IntBoolExp endsAfterStart(Job job);

    /**
     * Returns resourse assignment (if any)
     *
     * @return String representation of assignment
     */
    public String getAssignment();

    /**
     * Returns an end variable associated with this Job.
     *
     * @return an end variable associated with this Job.
     */
    public IntVar getEndVariable();

    /**
     * Returns job name
     *
     * @return Job name
     */
    public String getName();

    /**
     * Creates resource requirement constraint
     *
     * @param res Required resoruce
     * @param flag Expression (variable)to associate with the constraint, if
     *            it's satisfied expresson will be bound to 1, otherwise 0
     * @param capacity Required resource capacity
     * @return Resource requirement constraint
     */
    // public ConstraintRequires requires(Resource res, IntExp flag, int
    // capacity);
    /**
     * Creates resource requirement constraint with capacity = 1
     *
     * @param res Required resoruce
     * @param flag Expression (variable)to associate with the constraint, if
     *            it's satisfied expresson will be bound to 1, otherwise 0
     * @return Resource requirement constraint
     */
    // public ConstraintRequires requires(Resource res, IntExp flag); //
    // duration = 1
    /**
     * Returns a start variable associated with this Job.
     *
     * @return a start variable associated with this Job.
     */
    public IntVar getStartVariable();

    /**
     * Creates resource requirement constraint with alternative resources
     *
     * @param res Required resoruce set
     * @param capacity Required resource capacity
     * @return Resource requirement constraint
     */
    public AlternativeResourceConstraint requires(AlternativeResourceSet res, int capacity);

    /**
     * Creates resource requirement constraint with alternative resources
     *
     * @param res Required resoruce set
     * @param capacity Required resource capacity (constrained variable)
     * @return Resource requirement constraint
     */
    public AlternativeResourceConstraint requires(AlternativeResourceSet res, IntVar capacity);

    /**
     * Creates resource requirement constraint
     *
     * @param res Required resoruce
     * @param capacity Required resource capacity
     * @return Resource requirement constraint
     */
    public AlternativeResourceConstraint requires(Resource res, int capacity);

    /**
     * Creates resource requirement constraint
     *
     * @param res Required resoruce
     * @param capacity Required resource capacity (constrained variable)
     * @return Resource requirement constraint
     */
    public AlternativeResourceConstraint requires(Resource res, IntVar capacity);

    public void saveAssignmentInfo();

    /**
     * Return parent schedule
     *
     * @return Parent schedule
     */
    public Schedule schedule();

    /**
     * Sets job name
     *
     * @param name New job name
     */
    public void setName(String name);

    /**
     * Returns minium possible job start time
     *
     * @return Minium possible job start time
     */
    public int startMin();

    public int startMinA();

    /**
     * Returns boolean expression depends on job relationship
     *
     * @param job Job for compare
     * @return True if this job starts after end of parameter job
     */
    public IntBoolExp startsAfterEnd(Job job);

    /**
     * Returns boolean expression depends on job relationship
     *
     * @param job Job for compare
     * @return True if this job starts after start of parameter job
     */
    public IntBoolExp startsAfterStart(Job job);

    /**
     * Returns boolean expression depends on job relationship
     *
     * @param job Job for compare
     * @return True if this job starts simltaneously with parameter job
     */
    public IntBoolExp startsAtStart(Job job);

    public String toString();

    /**
     * Similar to toString() except adding job name
     *
     * @return String representing job value
     */
    public String value();
}