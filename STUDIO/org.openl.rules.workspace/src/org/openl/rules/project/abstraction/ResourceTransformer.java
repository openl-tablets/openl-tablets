package org.openl.rules.project.abstraction;

import java.io.InputStream;

import org.openl.rules.common.ProjectException;

/**
 * @author NSamatov.
 */
public interface ResourceTransformer {
    InputStream transform(AProjectResource resource) throws ProjectException;
}
