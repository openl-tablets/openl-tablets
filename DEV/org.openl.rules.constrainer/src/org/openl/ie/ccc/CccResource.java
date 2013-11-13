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
import org.openl.ie.scheduler.Resource;

public class CccResource extends CccVariable {
    private Resource _constrainer_resource;
    private String _value;
    private String _assignment;

    public CccResource(CccCore core, Resource r) {
        super(core, TM_RESOURCE, r.getName());
        setType(TM_RESOURCE);
        constrainerResource(r);
    }

    public Resource constrainerResource() {
        return _constrainer_resource;
    }

    public void constrainerResource(Resource resource) {
        _constrainer_resource = resource;
        fetchConstrainerState();
    }

    @Override
    public String debugInfo() {
        return "ni";
    }

    @Override
    public void fetchConstrainerState() {
        value(_constrainer_resource.mapString());
        _assignment = _constrainer_resource.getAssignment();
        status(STATUS_YELLOW);
    }

    @Override
    public String getInfo(String infotype) {
        if (infotype.equalsIgnoreCase("assignment")) {
            return _assignment;
        }
        return super.getInfo(infotype);
    }

    @Override
    public CccGoal getMaximizeGoal() {
        return null; // we cannot maximize resources
    }

    @Override
    public CccGoal getMinimizeGoal() {
        return null; // we cannot minimize resources
    }

    @Override
    public String toString() {
        return value();
    }

    @Override
    public String value() {
        return _value;
    }

    public void value(String v) {
        _value = v;
    }

}
