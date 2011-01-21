package org.openl.ie.ccc;

import java.util.Vector;

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
public class CccView {
    CccCore _core;
    // boolean _active;
    // String _status;
    // String _lname = "";
    // boolean _lfail = false;
    // int _lnum = 0;

    public String[] _obj_names = {};

    public CccView(CccCore c) {
        _core = c;
        _obj_names = getGroups();

        // _active = false;
    }

    public boolean activateAllConstraints() {
        return core().activateAllConstraints();
    }

    // public void activate() { _active = true; }
    // public void deactivate() { _active = false; }
    // public boolean active() { return _active; }
    public CccCore core() {
        return _core;
    }

    // public String getLastFailedId() { return core().getLastFailedId(); }

    public void deactivateAllConstraints() {
        core().deactivateAllConstraints();
    }

    public boolean exist(String obj) {
        return core().getObject(obj, 0) == null ? false : true;
    }

    public int getColor(String obj, int i) {
        CccObject o = core().getObject(obj, i);
        // System.out.println("STATUS of "+o+": "+o.status());
        if (o == null) {
            core().traceln("TRYING TO GET COLOR OF EMTPY: " + obj + "." + i);
        }

        return o == null ? -1 : o.status();
    }

    public String[] getDataGroups() {
        Vector s = new Vector();
        Vector gr = core().getGroups();
        for (int i = 0; i < gr.size(); i++) {
            CccGroup g = (CccGroup) gr.get(i);
            if ("objective".equals(g.getName())) {
                continue;
            }
            if (g.size() > 0) {
                CccObject o = g.getObject(0);
                if (o instanceof CccVariable) {
                    s.add(g.getName());
                }
            }
        }
        return (String[]) s.toArray(new String[s.size()]);
    }

    public String[] getGroups() {
        Vector s = new Vector();
        Vector gr = core().getGroups();
        for (int i = 0; i < gr.size(); i++) {
            CccGroup g = (CccGroup) gr.get(i);
            s.add(g.getName());
        }
        return (String[]) s.toArray(new String[s.size()]);
    }

    public String getHtml(String obj, int i) {
        CccObject o = core().getObject(obj, i);
        if (o == null) {
            core().traceln("TRYING TO GET NAME OF EMTPY: " + obj + "." + i);
            return "";
        }
        return o.getHtml();
    }

    public String getHtmlForId(String id) {
        CccObject o = core().getObject(id);
        if (o == null) {
            core().traceln("TRYING TO GET NAME OF EMTPY: " + id);
            return "";
        }
        return o.getHtml();
    }

    public String getInfo(String obj, int i, String infotype) {
        CccObject o = core().getObject(obj, i);
        if (o == null) {
            core().traceln("TRYING TO GET INFO OF EMTPY: " + obj + "." + i);
            return "";
        }
        return o.getInfo(infotype);
    }

    // public void setLastName(String s) { _lname = s; }
    // public String getLastName() { return core().getObject(
    // core().getLastFailedId() ).name(); }
    public boolean getLastFailed() {
        return core().getLastFailed();
    }

    public int getMax(String obj, int i) {
        CccObject o = core().getObject(obj, i);
        if (o == null) {
            core().traceln("TRYING TO GET MAX OF EMTPY: " + obj + "." + i);
            return 0;
        }
        if (o instanceof CccInteger) {
            return ((CccInteger) o).max();
        }
        if (o instanceof CccFloat) {
            return (int) ((CccFloat) o).max();
        }
        if (o instanceof CccJob) {
            return ((CccJob) o).endMax();
        }
        if (o instanceof CccResource) {
            return ((CccResource) o).constrainerResource().timeMax();
        }
        return 0;
    }

    public int getMaxSolutionNumber() {
        return core().getMaxSolutionNum();
    }

    public int getMin(String obj, int i) {
        CccObject o = core().getObject(obj, i);
        if (o == null) {
            core().traceln("TRYING TO GET MIN OF EMTPY: " + obj + "." + i);
            return 0;
        }
        if (o instanceof CccInteger) {
            return ((CccInteger) o).min();
        }
        if (o instanceof CccFloat) {
            return (int) ((CccFloat) o).min();
        }
        if (o instanceof CccJob) {
            return ((CccJob) o).startMin();
        }
        if (o instanceof CccResource) {
            return ((CccResource) o).constrainerResource().timeMin();
        }
        return 0;

    }

    public String getName(String obj, int i) {
        CccObject o = core().getObject(obj, i);
        if (o == null) {
            core().traceln("TRYING TO GET NAME OF EMTPY: " + obj + "." + i);
            return "";
        }
        return o.name();
    }

    public String getNameForId(String id) {
        CccObject o = core().getObject(id);
        if (o == null) {
            core().traceln("TRYING TO GET NAME OF EMTPY: " + id);
            return "";
        }
        return o.name();
    }

    public int getSolutionNumber() {
        return core().getSolutionNumber();
        // CccGoalSolution s = core().getGoalSolution();
        // return s!=null ? s.getTotalSolutions(s.solutionNumber()) : 0;
    }

    public String getStatus() {
        return core().getStatus();
    }

    public int getTotal(String obj) {
        int r = -1;
        if ("all".equals(obj)) {
            r = 2;
        }
        if (obj.equalsIgnoreCase("goal")) {
            r = core().goals().size();
        }
        if (obj.equalsIgnoreCase("constraint")) {
            r = core().constraints().size();
        }
        if (obj.equalsIgnoreCase("job")) {
            r = core().jobs().size();
        }
        if (obj.equalsIgnoreCase("resource")) {
            r = core().resources().size();
        }
        if (obj.equalsIgnoreCase("integer")) {
            r = core().integers().size();
        }
        if (obj.equalsIgnoreCase("float")) {
            r = core().floats().size();
        }
        if (obj.equalsIgnoreCase("objective")) {
            r = core().objective() == null ? 0 : 1;
        }
        if (r == -1 && core().existsGroup(obj)) {
            // System.out.println("getTotal("+obj+")="+r);
            CccGroup g = core().getGroup(obj);
            r = g.size();
        }
        // System.out.println("getTotal("+obj+")="+r);
        return r;
    }

    public String getValue(String obj, int i) {
        CccObject o = core().getObject(obj, i);
        if (o == null) {
            core().traceln("TRYING TO GET VALUE OF EMTPY: " + obj + "." + i);
            return "";
        }
        if (o instanceof CccVariable) {
            return ((CccVariable) o).value();
        }
        return "";
    }

    public boolean isSolution(String obj, int i) {
        CccObject o = core().getObject(obj, i);
        return o instanceof CccGoalSolution;
    }

    // returns false if timeout reached
    public boolean process(String obj, int i) {
        // _lfail = false;
        // _lnum = i;
        // System.out.println("CccView::process("+obj+") i="+i);

        if (obj.equalsIgnoreCase("all")) {
            if (i == 1) {
                return activateAllConstraints();
            }
            if (i == 0) {
                deactivateAllConstraints();
            }
            return true;
        }

        if (obj.equalsIgnoreCase("goal")) {
            CccGoal g = core().getGoalByNum(i);
            // setLastName(g.name());
            if (core().isActivated(g.getId())) {
                core().deactivateGoal();
                setStatus("Deactivated goal: " + g.name());
            } else {
                core().activateGoal(g.getId());
                // System.out.println("CccView::process(goal)
                // ms="+(time2-time1));
                setStatus("Activated goal: " + g.name() + ", time: " + core().getExecutionTime() + "ms");
            }
            return true;
        }

        if (obj.equalsIgnoreCase("constraint")) {
            CccExecutable ex = core().getConstraintByNum(i);
            // setLastName(ex.name());
            if (core().isActivated(ex.getId())) {
                // System.out.println("+deact");
                ex.deactivate();
                setStatus("Deactivated constraint: " + ex.name());
            } else {
                ex.activate();
            }
        }

        return true;
    }

    public boolean setSolutionNumber(int i) {
        boolean res;
        boolean ok = core().isOk();
        if (!(res = core().activateSolution(i))) {
            core().traceln("FAILED solution " + i + ", trying autosolution: " + core().getSolutionNumber());
            core().setOk(ok);
            res = core().activateSolution(core().getSolutionNumber());
        }
        return res;
    }

    public void setStatus(String s) {
        core().setStatus(s);
    }
}
