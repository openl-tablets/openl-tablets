package org.openl.rules.ui.tablewizard.jsf;

import java.util.HashSet;
import java.util.Set;

import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;
import org.openl.rules.ui.tablewizard.TableWizard;

/**
 * @author Aliaksandr Antonik.
 */
public abstract class BaseWizardBean {

    private int step;
    private int maxVisitedStep;
    private int stepsCount;

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
        setStep(getStep() + 1);
        if (getStep() == getMaxVisitedStep()) {
            onStepFirstVisit(getStep() + 1);
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

    protected void doSave() throws Exception {
        for(XlsWorkbookSourceCodeModule workbook : modifiedWorkbooks){
            workbook.save();
        }
    }

    protected void onStepFirstVisit(int step) {
    }

    public void setStep(int step) {
        this.step = step;
        maxVisitedStep = Math.max(step, maxVisitedStep);
    }

    public int getStepsCount() {
        return stepsCount;
    }

    public void setStepsCount(int stepsCount) {
        this.stepsCount = stepsCount;
    }

    public String start() {
        maxVisitedStep = step = 0;
        try {
            onStart();
            return getName();
        } catch (IllegalArgumentException e) {
            // Process the error during starting.
            FacesUtils.addErrorMessage("Can`t create wizard for this kind of table.", e.getMessage());
            return TableWizard.ERROR;
        }
   
    }

    public String finish() throws Exception {
        onFinish();
        return null;
    }

}
