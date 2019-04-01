package org.openl.rules.ui;

/**
 * @author Aliaksandr Antonik.
 */
public abstract class BaseWizard {

    private int step;
    private int maxVisitedStep;
    private int stepsCount;

    public void cancel() {
        onCancel();
    }

    public int getMaxVisitedStep() {
        return maxVisitedStep;
    }

    public String getName() {
        return "page";
    }

    public int getStep() {
        return step;
    }

    public String next() {
        setStep(getStep() + 1);
        if (getStep() > getMaxVisitedStep()) {
            onStepFirstVisit(getStep());
            maxVisitedStep = Math.max(step, maxVisitedStep);
        }
        return "next";
    }

    public String prev() {
        step--;
        return "prev";
    }

    protected abstract void onCancel();

    protected abstract void onStart();

    protected void onFinish() throws Exception {
    }

    protected void onStepFirstVisit(int step) {
    }

    public void setStep(int step) {
        this.step = step;
    }

    public int getStepsCount() {
        return stepsCount;
    }

    public void setStepsCount(int stepsCount) {
        this.stepsCount = stepsCount;
    }

    public String start() {
        maxVisitedStep = step = 0;
        onStart();
        return getName();
    }

    public String finish() throws Exception {
        onFinish();
        return null;
    }

}
