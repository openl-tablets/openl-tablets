package org.openl.rules.ui.tablewizard.jsf;

/**
 * @author Aliaksandr Antonik.
 */
public abstract class BaseWizardBean {
    private int step;
    private int maxVisitedStep;

    public String getName() {
        return "page";
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

    public void setStep(int step) {
        this.step = step;
        maxVisitedStep = Math.max(step, maxVisitedStep);
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

    public void cancel() {
        onFinish(true);
    }
}
