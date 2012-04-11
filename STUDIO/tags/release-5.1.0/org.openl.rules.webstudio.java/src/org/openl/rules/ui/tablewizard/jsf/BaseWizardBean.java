package org.openl.rules.ui.tablewizard.jsf;

import javax.faces.component.html.HtmlInputHidden;

/**
 * @author Aliaksandr Antonik.
 */
public abstract class BaseWizardBean {
    private int step = -1; //not started
    private int maxVisitedStep;
    private HtmlInputHidden hiddenStep;

    protected String getName() {
        return "page";
    }

    public synchronized void finish() {
        step = -1;
        onFinish(false);
    }

    public String start() {
        maxVisitedStep = step = 0;
        onStart();
        return getName();
    }

    protected abstract void onFinish(boolean cancelled);

    protected abstract void onStart();

    public int getStep() {
        return step;
    }

    public HtmlInputHidden getHiddenStep() {
        return hiddenStep;
    }

    public void setHiddenStep(HtmlInputHidden hidden) {
        this.hiddenStep = hidden;
        try{
            step = Integer.parseInt((String) hidden.getValue());
            maxVisitedStep = Math.max(step, maxVisitedStep);
        } catch (NumberFormatException nfe) {}
    }

    public int getMaxVisitedStep() {
        return maxVisitedStep;
    }

    public String next() {
        if (getStep() == getMaxVisitedStep()) {
            onStepFirstVisit(getStep() + 1);
        }
        return "next";
    }

    protected void onStepFirstVisit(int step) {}

    public String prev() {
        return "prev";
    }

    public String cancel() {
        onFinish(true);
        return getName() +"Cancel";
    }
}
