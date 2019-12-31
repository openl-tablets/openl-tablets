package org.openl.rules.ruleservice.publish.lazy;

import java.io.File;
import java.nio.file.Paths;
import java.util.Collection;

import org.openl.exception.OpenlNotCheckedException;
import org.openl.rules.project.model.Module;
import org.openl.types.IOpenMember;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModuleUtils {

    private static final Logger LOG = LoggerFactory.getLogger(ModuleUtils.class);

    public static Module getModuleForMember(IOpenMember member, Collection<Module> modules) {
        String sourceUrl = member.getDeclaringClass().getMetaInfo().getSourceUrl();
        Module module = getModuleForSourceUrl(sourceUrl, modules);
        if (module != null) {
            return module;
        }
        throw new OpenlNotCheckedException("Module is not found. This shoud not happen.");
    }

    private static Module getModuleForSourceUrl(String sourceUrl, Collection<Module> modules) {
        if (modules.size() == 1) {
            return modules.iterator().next();
        }
        for (Module module : modules) {
            String modulePath = module.getRulesRootPath().getPath();
            try {
                if (Paths.get(sourceUrl).normalize()
                    .equals(Paths.get(new File(modulePath).getCanonicalFile().toURI().toURL().toExternalForm()).normalize())) {
                    return module;
                }
            } catch (Exception e) {
                LOG.warn("Failed to build url for module '{}' with path: {}", module.getName(), modulePath, e);
            }
        }
        return null;
    }
}