package org.openl.rules.ui.tablewizard;

import javax.faces.component.html.HtmlInputHidden;

import org.openl.rules.ui.tablewizard.jsf.BaseWizardBean;

public abstract class TableWizard {
    
    private HtmlInputHidden hiddenStep;
    
    protected BaseWizardBean wizard;
    
    public abstract String cancel();
    
    public abstract String startWizard();

    public abstract String start();
    
    public BaseWizardBean getWizard() {
        return wizard;
    }

    public String next() {
        return wizard.next();
    }

    public String prev() {
        return wizard.prev();
    }
    
    public HtmlInputHidden getHiddenStep() {
        return hiddenStep;
    }
    
    public void setHiddenStep(HtmlInputHidden hidden) {
        hiddenStep = hidden;
        try {
            int step = Integer.parseInt((String) hidden.getValue());
            wizard.setStep(step);
        } catch (NumberFormatException nfe) {
        }
    }
}
