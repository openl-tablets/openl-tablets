package org.openl.rules.ui.tablewizard.jsf;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;

/**
 * @author Aliaksandr Antonik.
 */
public abstract class BaseWizardBean {
    private int step;
    private int maxVisitedStep;
    private Set<XlsWorkbookSourceCodeModule> modifiedWorkbooks = new HashSet<XlsWorkbookSourceCodeModule>();

    public void cancel() {
        onCancel();
    }

    public Set<XlsWorkbookSourceCodeModule> getModifiedWorkbooks() {
        return modifiedWorkbooks;
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
        if (getStep() == getMaxVisitedStep()) {
            onStepFirstVisit(getStep() + 1);
        }
        return "next";
    }

    protected abstract void onCancel();

    protected abstract void onStart();

    protected void onFinish() throws Exception {
        doSave();
    }

    protected void doSave() throws Exception {
        for(XlsWorkbookSourceCodeModule workbook : modifiedWorkbooks){
            workbook.save();
        }
    }

    protected void onStepFirstVisit(int step) {
    }

    public String prev() {
        return "prev";
    }

    public void setStep(int step) {
        this.step = step;
        maxVisitedStep = Math.max(step, maxVisitedStep);
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
