package org.openl.rules.ui.tablewizard;

import org.openl.rules.ui.WebStudio;
import org.openl.rules.ui.tablewizard.jsf.BaseWizardBean;
import org.openl.rules.webstudio.web.util.WebStudioUtils;

public abstract class TableWizard {
    
    public static final String ERROR = "error";
    
    private String elementUri;
    
    protected BaseWizardBean wizard;

    public abstract String startWizard();

    public abstract String start();

    public abstract String cancel();

    public BaseWizardBean getWizard() {
        return wizard;
    }

    public String next() {
        return wizard.next();
    }

    public String prev() {
        return wizard.prev();
    }
    
    public String getElementUri() {
        return elementUri;
    }

    public void setElementUri(String elementUri) {
        this.elementUri = elementUri;
    }
    
    protected void reload() {
        elementUri = null;
        init();        
    }

    protected void init() {
        WebStudio studio = WebStudioUtils.getWebStudio();
        elementUri = studio.getTableUri();
    }

}
