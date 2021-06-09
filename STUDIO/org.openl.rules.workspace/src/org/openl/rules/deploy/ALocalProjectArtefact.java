package org.openl.rules.deploy;

import org.openl.rules.project.abstraction.IProjectArtefact;

public abstract class ALocalProjectArtefact implements IProjectArtefact {

    private final String name;

    public ALocalProjectArtefact(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
