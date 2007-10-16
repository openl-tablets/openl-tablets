package org.openl.rules.ui.repository;

import javax.faces.context.FacesContext;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;

public class JsfPhaseListener implements PhaseListener {
    private static final long serialVersionUID = 8884716766558552223L;
    
    private static final PhaseId RESET_MESSAGES_ON = PhaseId.RESTORE_VIEW;

    public void afterPhase(PhaseEvent phaseEvent) {
        if (canReset(phaseEvent.getPhaseId())) {
            resetMessages();
        }
    }

    public void beforePhase(PhaseEvent phaseEvent) {
        // Do nothing
    }

    public PhaseId getPhaseId() {
        return RESET_MESSAGES_ON;
    }

    private boolean canReset(PhaseId phaseId) {
        return RESET_MESSAGES_ON.equals(phaseId);
    }
    
    private void resetMessages() {
        System.out.println("-clear-");
        FacesContext context = FacesContext.getCurrentInstance();
        Object obj = context.getApplication().createValueBinding("#{userSession}").getValue(context);

        UserSessionBean userSession = (UserSessionBean) obj;
        if (userSession != null) {
            userSession.clearMessages();
        }
    }
}
