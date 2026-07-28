package org.openl.rules.deploy;

import lombok.RequiredArgsConstructor;

import org.openl.rules.project.abstraction.IProjectArtefact;

@RequiredArgsConstructor
public abstract class ALocalProjectArtefact implements IProjectArtefact {

    private final String name;

    @Override
    public String getName() {
        return name;
    }
}
