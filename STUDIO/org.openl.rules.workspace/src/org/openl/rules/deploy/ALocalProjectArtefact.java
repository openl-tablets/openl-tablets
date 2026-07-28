package org.openl.rules.deploy;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import org.openl.rules.project.abstraction.IProjectArtefact;

@RequiredArgsConstructor
public abstract class ALocalProjectArtefact implements IProjectArtefact {

    @Getter
    private final String name;
}
