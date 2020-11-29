package org.openl.rules.project.abstraction;

import java.io.InputStream;

import org.openl.rules.common.ProjectException;

public interface IProjectResource extends IProjectArtefact {

    InputStream getContent() throws ProjectException;

}
