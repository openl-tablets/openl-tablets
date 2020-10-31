package org.openl.rules.project.resolving;

import java.lang.reflect.InvocationTargetException;

import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.PathEntry;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.util.CollectionUtils;
import org.openl.util.FileUtils;

public interface PropertiesFileNameProcessor {

    @Deprecated
    default ITableProperties process(Module module, String fileNamePattern) throws NoMatchFileNameException,
                                                                            InvalidFileNamePatternException {
        try {
            PropertiesFileNameProcessor pfnp = getClass().getConstructor(String.class).newInstance(fileNamePattern);
            String path = module.getRulesRootPath().getPath();
            return pfnp.process(path);
        } catch (InvocationTargetException e) {
            Throwable targetException = e.getTargetException();
            if (targetException instanceof InvalidFileNamePatternException) {
                throw (InvalidFileNamePatternException) targetException;
            }
            if (targetException instanceof RuntimeException) {
                throw (RuntimeException) targetException;
            }
            throw new IllegalStateException(targetException);
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        } catch (NoSuchMethodException e) {
            return process(module, new String[] { fileNamePattern });
        }
    }

    @Deprecated
    default ITableProperties process(Module module, String... fileNamePatterns) throws NoMatchFileNameException,
                                                                                InvalidFileNamePatternException {
        if (CollectionUtils.isEmpty(fileNamePatterns)) {
            return null;
        }
        if (fileNamePatterns.length == 1) {
            return process(module, fileNamePatterns[0]);
        }
        // choose the suitable pattern
        NoMatchFileNameException error = null;
        for (String fileNamePattern1 : fileNamePatterns) {
            try {
                return process(module, fileNamePattern1);
            } catch (NoMatchFileNameException e) {
                if (error != null) {
                    e.addSuppressed(error);
                }
                error = e;
            }
        }

        throw error;

    }

    default ITableProperties process(String modulePath) throws NoMatchFileNameException {
        try {
            Module module = new Module();
            module.setRulesRootPath(new PathEntry(modulePath));
            module.setName(FileUtils.getBaseName(modulePath));
            return process(module, "");
        } catch (InvalidFileNamePatternException e) {
            throw new IllegalStateException(e);
        }
    }

}
