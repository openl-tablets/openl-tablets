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
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.IntExp;

/**
 * An interface for the representation of the unary Resources (people, machines,
 * materials).
 * <p>
 * Resource has diferent capacities (1 or 0, ie available or not) in different
 * times. There are require-constraints between jobs and resources.
 * <p>
 * Examples:
 *
 * <pre>
 * Job job = schedule.addJob(duration, &quot;job1&quot;);
 * Resource resource = schedule.addUnaryResource(&quot;truck1&quot;);
 *
 * job.requires(resource).post();
 * </pre>
 *
 * @see Job
 * @see ResourceDiscrete
 */
public class ResourceUnary extends ResourceDiscrete {

    private IntExp _used;

    public ResourceUnary(Schedule sch) {
        super(sch, 1);
        _used = null;
    }

    public ResourceUnary(Schedule sch, int timeMin, int timeMax) {
        super(sch, timeMin, timeMax, 1);
        _used = null;
    }

    /**
     * Returns expression reflecting usage of resource It equals to number of
     * moments when capacity = 0
     *
     * @return Expression reflecting usage of resource
     * @throws Failure
     */
    public IntExp getUsed() throws Failure {
        if (_used == null) {
            _used = new IntExpEmployed(this);
        }
        return _used;
    }
}