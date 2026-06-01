package org.openl.rules.maven.extension;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.repository.WorkspaceReader;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Patches {@code org.apache.maven.ReactorReader}'s internal indexes via reflection so synthesised pom-less
 * OpenL projects are discoverable by dependency resolution.
 * <p>
 * Maven 3.9.x {@code ReactorReader} snapshots {@code session.getProjects()} once in its constructor and never
 * refreshes. The participant's {@code afterProjectsRead} mutates the session too late — the workspace reader
 * already holds the pre-extension snapshot, so a sibling looking up a pom-less GA via
 * {@link WorkspaceReader#findArtifact} sees {@code null} and Aether falls through to the remote repositories.
 * This injector pokes the missing projects into the two private (but mutable) index maps,
 * {@code projectsByGAV} and {@code projectsByGA}.
 * <p>
 * No-ops on distributions that wire a different workspace reader (Maven 4, some mvnd modes), where the bug
 * doesn't apply.
 *
 * @author Yury Molchan
 */
final class ReactorReaderInjector {

    private static final Logger LOG = LoggerFactory.getLogger(ReactorReaderInjector.class);

    private static final String REACTOR_READER_CLASS = "org.apache.maven.ReactorReader";
    private static final String CHAINED_READERS_FIELD = "readers";
    private static final String PROJECTS_BY_GAV_FIELD = "projectsByGAV";
    private static final String PROJECTS_BY_GA_FIELD = "projectsByGA";

    private ReactorReaderInjector() {
    }

    /**
     * Registers each project in {@code added} with the session's {@code ReactorReader} so a downstream
     * {@link WorkspaceReader#findArtifact} resolves it from the reactor. No-ops when no {@code ReactorReader}
     * is on the workspace reader chain.
     */
    static void inject(MavenSession session, Collection<MavenProject> added) {
        if (added.isEmpty()) {
            return;
        }
        var workspaceReader = session.getRepositorySession().getWorkspaceReader();
        var reactorReader = findReactorReader(workspaceReader);
        if (reactorReader == null) {
            LOG.debug("No ReactorReader on the workspace reader chain; skipping pom-less index injection.");
            return;
        }
        try {
            var byGav = readMapField(reactorReader, PROJECTS_BY_GAV_FIELD);
            var byGa = readMapField(reactorReader, PROJECTS_BY_GA_FIELD);
            for (var p : added) {
                var gavKey = ArtifactUtils.key(p.getGroupId(), p.getArtifactId(), p.getVersion());
                var gaKey = ArtifactUtils.versionlessKey(p.getGroupId(), p.getArtifactId());
                @SuppressWarnings("unchecked")
                var typedByGav = (Map<String, MavenProject>) byGav;
                @SuppressWarnings("unchecked")
                var typedByGa = (Map<String, List<MavenProject>>) byGa;
                typedByGav.put(gavKey, p);
                typedByGa.computeIfAbsent(gaKey, k -> new ArrayList<>()).add(p);
            }
        } catch (ReflectiveOperationException e) {
            LOG.warn("Failed to patch ReactorReader for pom-less projects; sibling resolution will fall back to repositories.", e);
        }
    }

    private static Map<?, ?> readMapField(Object target, String fieldName) throws ReflectiveOperationException {
        var field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return (Map<?, ?>) field.get(target);
    }

    private static @Nullable Object findReactorReader(@Nullable WorkspaceReader reader) {
        if (reader == null) {
            return null;
        }
        if (REACTOR_READER_CLASS.equals(reader.getClass().getName())) {
            return reader;
        }
        return unwrapChain(reader);
    }

    /**
     * Walks Aether's {@code ChainedWorkspaceReader} (and any other workspace reader exposing a
     * {@code readers} list field) so we still find a wrapped {@code ReactorReader}. Returns
     * {@code null} when the field is absent or empty.
     */
    private static @Nullable Object unwrapChain(WorkspaceReader reader) {
        try {
            var field = reader.getClass().getDeclaredField(CHAINED_READERS_FIELD);
            field.setAccessible(true);
            var nested = field.get(reader);
            if (!(nested instanceof List<?> readers)) {
                return null;
            }
            for (var sub : readers) {
                if (sub instanceof WorkspaceReader wr) {
                    var found = findReactorReader(wr);
                    if (found != null) {
                        return found;
                    }
                }
            }
        } catch (NoSuchFieldException ignored) {
            // Workspace reader exposes no readers list — leaf reader, nothing to unwrap.
        } catch (ReflectiveOperationException e) {
            LOG.debug("Failed to introspect workspace reader chain on {}.", reader.getClass(), e);
        }
        return null;
    }
}
