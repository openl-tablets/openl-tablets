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
import java.util.HashSet;

public class AlternativeResourceSet {

    private HashSet _resources;

    public AlternativeResourceSet() {
        _resources = new HashSet();
    }

    public void add(Resource r) {
        _resources.add(r);
    }

    public int size() {
        return _resources.size();
    }

    public Resource[] toArray() {
        Resource[] r = new Resource[_resources.size()];
        Object[] o = _resources.toArray();
        for (int i = 0; i < o.length; i++) {
            r[i] = (Resource) o[i];
        }
        return r;
    }

}