package org.openl.rules.ui.tablewizard;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

import org.openl.rules.ui.copy.DimensionalPropertiesTableCopier;
import org.openl.rules.ui.copy.TableNamesCopier;
import org.openl.rules.ui.copy.VersionPropertyTableCopier;

@ManagedBean
@SessionScoped
public class TableCopierWizardManager extends BaseTableWizardManager {

    enum CopyType {
        CHANGE_NAMES,
        CHANGE_DIMENSION,
        CHANGE_VERSION
    }

    private CopyType copyType;

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
                wizard = new TableNamesCopier(getTable());
                break;
            case CHANGE_VERSION:
                wizard = new VersionPropertyTableCopier(getTable());
                break;
            case CHANGE_DIMENSION:
                wizard = new DimensionalPropertiesTableCopier(getTable());
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

}
