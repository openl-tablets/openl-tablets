package org.openl.rules.maven.migration;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.openl.rules.project.model.ExposedMethods;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.util.CollectionUtils;

/**
 * {@code rules.xml} migration: replaces module-level {@code <method-filter>} with project-level
 * {@code <exposed-methods>} populated from the actual methods of the OpenL-generated interface. The supplier
 * is invoked at most once and only when there is a non-empty filter and no {@code <exposed-methods>} block
 * yet — populated {@code <exposed-methods>} is left untouched.
 * <p>
 * Migrator id: {@code config.project.method-filter}.
 *
 * @author Yury Molchan
 */
public final class ConfigProjectMethodFilterMigrator implements Migrator {

    /**
     * Clears module-level {@code <method-filter>} elements. If at least one had content and no populated
     * {@code <exposed-methods>} exists, queries the supplier and writes the OpenL interface's method names
     * into a fresh {@code <exposed-methods>}. Package-private for direct unit testing.
     */
    static void transform(ProjectDescriptor descriptor, Supplier<Class<?>> generatedInterface) {
        var modules = descriptor.getModules();
        if (CollectionUtils.isEmpty(modules)) {
            return;
        }

        var anyFilter = false;
        for (var module : modules) {
            var mf = module.getMethodFilter();
            if (mf != null && (CollectionUtils.isNotEmpty(mf.getIncludes())
                    || CollectionUtils.isNotEmpty(mf.getExcludes()))) {
                anyFilter = true;
                module.setMethodFilter(null);
            }
        }

        if (!anyFilter) {
            return;
        }

        // Existing populated <exposed-methods> wins — the user has already declared what to expose. We just
        // dropped the legacy filters, no further action.
        var existing = descriptor.getExposedMethods();
        if (existing != null && (CollectionUtils.isNotEmpty(existing.getIncludes())
                || CollectionUtils.isNotEmpty(existing.getExcludes()))) {
            return;
        }

        if (generatedInterface == null) {
            return;
        }
        var interfaceClass = generatedInterface.get();
        if (interfaceClass == null) {
            return;
        }

        var methodNames = Arrays.stream(interfaceClass.getMethods())
                .map(Method::getName)
                .collect(Collectors.toCollection(TreeSet::new));

        if (methodNames.isEmpty()) {
            return;
        }

        var em = existing != null ? existing : new ExposedMethods();
        em.setIncludes(new TreeSet<>(methodNames));
        descriptor.setExposedMethods(em);
    }

    @Override
    public String getId() {
        return "config.project.method-filter";
    }

    @Override
    public String getCommitMessage() {
        return "method-filter to exposed-methods";
    }

    @Override
    public List<Path> migrate(Path sourceFolder, Supplier<Class<?>> generatedInterface)
            throws IOException {
        return ConfigProjectIO.roundtrip(this, sourceFolder,
                descriptor -> transform(descriptor, generatedInterface));
    }
}
