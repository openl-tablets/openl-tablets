package org.openl.rules.workspace.dtr;

import java.util.EventListener;

import org.openl.rules.project.abstraction.AProjectArtefact;

public interface DesignTimeRepositoryListener extends EventListener {
    public static class DTRepositoryEvent {
        private AProjectArtefact artefact;
        private int type;

        public DTRepositoryEvent(AProjectArtefact artefact, int type) {
            this.artefact = artefact;
            this.type = type;
        }

        protected AProjectArtefact getArtefact() {
            return artefact;
        }

        protected void setArtefact(AProjectArtefact artefact) {
            this.artefact = artefact;
        }

        protected int getType() {
            return type;
        }

        protected void setType(int type) {
            this.type = type;
        }
    }

    void onArtefactModified(DTRepositoryEvent event);
}
