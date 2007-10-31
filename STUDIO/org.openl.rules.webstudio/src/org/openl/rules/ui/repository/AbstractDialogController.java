package org.openl.rules.ui.repository;

public abstract class AbstractDialogController {
    private Context context;
    
    public void setContext(Context context) {
        this.context = context;
    }
    
    protected Context getContext() {
        return context;
    }

    /**
     * Returns string for outcome.
     * 
     * @param success whether operation was successful or not.
     * @return outcome: "success" or "fail"
     */
    protected String outcome(boolean success) {
        return (success ? "success" : "fail");
    }

    protected void refresh() {
        context.refresh();
    }
}
