package org.openl.rules.ui.tablewizard;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

import org.openl.rules.ui.copy.NewVersionTableCopier;
import org.openl.rules.ui.copy.TableNamesCopier;
import org.openl.rules.ui.copy.TablePropertyCopier;

@ManagedBean
@SessionScoped
public class TableCopierWizardManager extends TableWizard {    

    static enum CopyType {
        CHANGE_NAMES,
        CHANGE_PROPERTIES,
        CHANGE_VERSION
    }    
    
    private CopyType copyType = CopyType.CHANGE_NAMES;
    
    public TableCopierWizardManager () {
        init();
    }    
    
    public String getCopyType() {
        return copyType.name();
    }
    
    public void setCopyType(String copyType) {
        try {
            this.copyType = CopyType.valueOf(copyType);
        } catch (IllegalArgumentException e) {
            this.copyType = CopyType.CHANGE_NAMES;
        }
    }

    @Override
    public String startWizard() {
        reload();
        switch (copyType) {
            case CHANGE_NAMES:
                wizard = new TableNamesCopier(getElementUri()); 
                break;
            case CHANGE_PROPERTIES:
                wizard = new TablePropertyCopier(getElementUri());
                break;
            case CHANGE_VERSION:
                wizard = new NewVersionTableCopier(getElementUri());
                break;
            default:
                return null;
        }
        wizard.next();
        return wizard.getName();
    }    

    @Override
    public String cancel() {
        if (wizard != null) {
            wizard.cancel();
        }
        return "newTableCancel";
    }
    
    @Override
    public String start() {
        copyType = CopyType.CHANGE_NAMES;
        return "wizardSelect";       
    }

}
