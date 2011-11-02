package org.openl.rules.ui.tablewizard;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

import org.openl.rules.ui.EnumValuesUIHelper;
import org.openl.rules.ui.copy.NewDimensionalVersionTableCopier;
import org.openl.rules.ui.copy.NewVersionTableCopier;
import org.openl.rules.ui.copy.TableNamesCopier;

@ManagedBean
@SessionScoped
public class TableCopierWizardManager extends TableWizard {

    static enum CopyType {
        CHANGE_NAMES,
        CHANGE_DIMENSION,
        CHANGE_VERSION
    }

    private CopyType copyType;

    private EnumValuesUIHelper enumHelper = new EnumValuesUIHelper();

    public TableCopierWizardManager () {
    }

    public String getCopyType() {
        return copyType.name();
    }

    public void setCopyType(String copyType) {
        this.copyType = CopyType.valueOf(copyType);
        startWizard();
    }

    @Override
    public String startWizard() {
        reload();
        switch (copyType) {
            case CHANGE_NAMES:
                wizard = new TableNamesCopier(getTableUri()); 
                break;
            case CHANGE_VERSION:
                wizard = new NewVersionTableCopier(getTableUri());
                break;
            case CHANGE_DIMENSION:
                wizard = new NewDimensionalVersionTableCopier(getTableUri());
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
        return null;
    }

    @Override
    public String start() {
        copyType = CopyType.CHANGE_NAMES;
        startWizard();
        return null;
    }

    public EnumValuesUIHelper getEnumHelper() {
        return enumHelper;
    }

}
