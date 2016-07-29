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
import org.openl.ie.tools.FastVector;

/**
 * An implementation of Schedule, general container and manager for all
 * scheduling objects.
 */

public class Schedule {

    // local variables

    private String _name;

    private FastVector _jobs;
    private FastVector _resources;
    private FastVector _requirements;

    private int _scheduleStart;
    private int _scheduleEnd;

    private Constrainer _constrainer;

    /**
     * Constructs new schedule using existing Constrainer
     *
     * @param c Constrainer to use
     * @param start Schedule starting time
     * @param end Schedule ending time
     */
    public Schedule(Constrainer c, int start, int end) {
        _constrainer = c;
        _scheduleStart = start;
        _scheduleEnd = end;
        _jobs = new FastVector();
        _resources = new FastVector();
        _requirements = new FastVector();
    }

    /**
     * Add job with specified duration to schedule
     *
     * @param duration Job duration
     * @param name Job name
     * @return Created job
     * @throws Failure
     */
    public Job addJob(int duration, String name) throws Failure {
        JobInterval job = new JobInterval(this, duration);
        job.setName(name);
        _jobs.addElement(job);
        return job;
    }

    /**
     * Add predefined job to schedule
     *
     * @param job Job
     * @return The same job
     * @throws Failure
     */
    public Job addJob(Job job) throws Failure {
        _jobs.addElement(job);
        return job;
    }

    public void addRequirement(AlternativeResourceConstraint c) {
        _requirements.addElement(c);
    }

    /**
     * Add predefined resource to schedule
     *
     * @param resource Resource
     * @return The same resource
     */
    public Resource addResource(Resource resource) {
        _resources.addElement(resource);
        return resource;
    }

    /**
     * Add discrete resource with specified capacity to schedule
     *
     * @param capacity Resource capacity
     * @param avmin Resource availability start time
     * @param avmax Resource availability end time
     * @param name Resource name
     * @return Created resource
     */
    public Resource addResourceDiscrete(int capacity, int avmin, int avmax, String name) {
        Resource resource = new ResourceDiscrete(this, avmin, avmax, capacity);
        resource.setName(name);
        _resources.addElement(resource);
        return resource;
    }

    /**
     * Add discrete resource with specified capacity to schedule Resource is
     * available during all shedule period
     *
     * @param capacity Resource capacity
     * @param name Resource name
     * @return Created resource
     */
    public Resource addResourceDiscrete(int capacity, String name) {
        Resource resource = new ResourceDiscrete(this, capacity);
        resource.setName(name);
        _resources.addElement(resource);
        return resource;
    }

    /**
     * Add unary resource to schedule
     *
     * @param avmin Resource availability start time
     * @param avmax Resource availability end time
     * @param name Resource name
     * @return Created resource
     */
    public Resource addResourceUnary(int avmin, int avmax, String name) {
        Resource resource = new ResourceUnary(this, avmin, avmax);
        resource.setName(name);
        _resources.addElement(resource);
        return resource;
    }

    /**
     * Add unary resource to schedule Resource is available during all shedule
     * period
     *
     * @param name Resource name
     * @return Created resource
     */
    public Resource addResourceUnary(String name) {
        Resource resource = new ResourceUnary(this);
        resource.setName(name);
        _resources.addElement(resource);
        return resource;
    }

    /**
     * Returns schedule's constrainer
     *
     * @return Schedule's constrainer
     */
    public Constrainer constrainer() {
        return _constrainer;
    }

    /**
     * Returns schedule ending time
     *
     * @return Schedule ending time
     */
    public int end() {
        return _scheduleEnd;
    }

    /**
     * Query assigned jobs
     *
     * @param res Resource to query jobs assigned
     * @return An array of job objects
     */
    public Job[] getAssignedJobs(Resource res) {
        int i;
        FastVector v = new FastVector(_requirements.size());
        for (i = 0; i < _requirements.size(); i++) {
            AlternativeResourceConstraint c = (AlternativeResourceConstraint) _requirements.elementAt(i);
            if (res.equals(c.getResource())) {
                v.add(c.getJob());
            }
        }
        Job[] jobs = new Job[v.size()];
        for (i = 0; i < _requirements.size(); i++) {
            jobs[i] = (Job) v.elementAt(i);
        }
        return jobs;
    }

    /**
     * Query assigned resources
     *
     * @param job Job to query assigned resources
     * @return An array of resource objects
     */
    public Resource[] getAssignedResources(Job job) {
        int i;
        FastVector v = new FastVector(_requirements.size());
        for (i = 0; i < _requirements.size(); i++) {
            AlternativeResourceConstraint c = (AlternativeResourceConstraint) _requirements.elementAt(i);
            if (job.equals(c.getJob())) {
                Resource r = c.getResource();
                if (r != null) {
                    v.add(r);
                }
            }
        }
        Resource[] res = new Resource[v.size()];
        for (i = 0; i < _requirements.size(); i++) {
            res[i] = (Resource) v.elementAt(i);
        }
        return res;
    }

    /**
     * Query assigned resources
     *
     * @param job Job to query assigned resources
     * @return List of assigned resources
     */
    public String getAssignments(Job job) {
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < _requirements.size(); i++) {
            AlternativeResourceConstraint c = (AlternativeResourceConstraint) _requirements.elementAt(i);
            if (job.equals(c.getJob())) {
                Resource r = c.getResource();
                if (r != null) {
                    str.append(r.getName()).append("(").append(c.getCapacity()).append(") ");
                }
            }
        }
        return str.toString();
    }

    /**
     * Query jobs assigned to a resource
     *
     * @param res Resource to query assigned jobs
     * @return List of jobs assigned to the resource
     */
    public String getAssignments(Resource res) {
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < _requirements.size(); i++) {
            AlternativeResourceConstraint c = (AlternativeResourceConstraint) _requirements.elementAt(i);
            if (res.equals(c.getResource())) {
                str.append(c.getJob().getName()).append(" ");
            }
        }
        return str.toString();
    }

    /**
     * Returns schedule's name
     *
     * @return Schedule's name
     */
    public String getName() {
        return _name;
    }

    /**
     * Returns vector of schedule's jobs
     *
     * @return Vector of jobs
     */
    public FastVector jobs() {
        return _jobs;
    }

    /**
     * Returns vector of schedule's resources
     *
     * @return Vector of resources
     */
    public FastVector resources() {
        return _resources;
    }

    /**
     * Sets schedule's name
     *
     * @param name Schedule's new name
     */
    public void setName(String name) {
        _name = name;
    }

    /**
     * Returns schedule size (duration)
     *
     * @return Schedule size
     */
    public int size() {
        return _scheduleEnd - _scheduleStart;
    }

    /**
     * Returns schedule starting time
     *
     * @return Schedule starting time
     */
    public int start() {
        return _scheduleStart;
    }

    @Override
    public String toString() {
        return "Schedule: " + _name + " [" + _scheduleStart + ";" + _scheduleEnd + ")" + "\nJobs: " + _jobs.size()
                + "\nResources: " + _resources.size() + "\nRequirements: " + _requirements.size();
    }
}