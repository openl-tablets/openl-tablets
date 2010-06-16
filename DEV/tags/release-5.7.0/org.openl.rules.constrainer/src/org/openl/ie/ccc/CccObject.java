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
/**
 * Base class for all CCC objects
 */

public class CccObject implements CccConst {
    private CccCore _core;
    private String _id;
    private String _name;
    private String html;
    private int _status;
    private int _typeMask;
    private boolean _visible;
    private boolean _bound;

    /**
     * CccObject constructor comment.
     */
    public CccObject(CccCore core) {
        this(core, "");
    }

    /**
     * CccObject constructor comment.
     */
    public CccObject(CccCore core, String name) {
        super();

        _typeMask = TYPE_UNKNOWN;
        _core = core;
        _name = name;
        _status = STATUS_UNKNOWN;
        _bound = false;
        _visible = true;
    }

    public boolean bound() {
        return _bound;
    }

    public void bound(boolean b) {
        _bound = b;
    }

    public CccCore core() {
        return _core;
    }

    public String getHtml() {
        return html;
    }

    public String getId() {
        return _id;
    }

    public String getInfo(String type) {
        // System.out.println("??? CccObject.getInfo(\""+type+"\") - unknown!");
        return "";
    }

    public int getTypeMask() {
        return _typeMask;
    }

    public boolean hasType(int mask) {
        return ((mask & _typeMask) != 0);
    }

    public String name() {
        return _name;
    }

    /**
     * @param string
     */
    public void setHtml(String string) {
        html = string;
    }

    public void setId(String id) {
        _id = id;
    }

    public void setType(int mask) {
        _typeMask |= mask;
    }

    public void setTypeMask(int t) {
        _typeMask = t;
    }

    public int status() {
        return _status;
    }

    public void status(int s) {
        _status = s;
    }

    @Override
    public String toString() {
        return _name;
    }

    public void unsetType(int mask) {
        _typeMask |= ~mask;
    }

    public boolean visible() {
        return _visible;
    }

    public void visible(boolean b) {
        _visible = b;
    }

}
