package org.openl.rules.project.abstraction;

import org.openl.rules.common.ProjectException;

import java.io.InputStream;

/**
 * @author NSamatov.
 */
public interface ResourceTransformer {
    InputStream tranform(AProjectResource resource) throws ProjectException;
}
