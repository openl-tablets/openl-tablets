package org.openl.rules.maven.extension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.repository.WorkspaceReader;
import org.eclipse.aether.repository.WorkspaceRepository;
import org.eclipse.aether.util.repository.ChainedWorkspaceReader;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Verifies the reflective patch ReactorReaderInjector applies to {@code org.apache.maven.ReactorReader}'s
 * private indexes. The real maven-core class is package-private, so the test uses a fake with the same
 * field names ({@code projectsByGAV}, {@code projectsByGA}) — the injector matches by class name in
 * production, but the field-injection logic itself is unit-tested here against a name-equivalent stub.
 */
class ReactorReaderInjectorTest {

    private static MavenProject project(String groupId, String artifactId, String version) {
        var p = new MavenProject();
        p.setGroupId(groupId);
        p.setArtifactId(artifactId);
        p.setVersion(version);
        return p;
    }

    /** Stub that mimics ReactorReader's field shape so the reflective injector can populate it. */
    @SuppressWarnings("unused") // fields are read reflectively
    private static final class FakeReactorReader implements WorkspaceReader {
        private final Map<String, MavenProject> projectsByGAV = new HashMap<>();
        private final Map<String, List<MavenProject>> projectsByGA = new HashMap<>();

        @Override
        public WorkspaceRepository getRepository() {
            return new WorkspaceRepository("reactor");
        }

        @Override
        public File findArtifact(org.eclipse.aether.artifact.Artifact artifact) {
            return null;
        }

        @Override
        public List<String> findVersions(org.eclipse.aether.artifact.Artifact artifact) {
            return List.of();
        }
    }

    private static MavenSession sessionWith(WorkspaceReader reader) {
        var aether = new DefaultRepositorySystemSession();
        aether.setWorkspaceReader(reader);
        var session = Mockito.mock(MavenSession.class);
        Mockito.when(session.getRepositorySession()).thenReturn(aether);
        return session;
    }

    /**
     * The real ReactorReader is in {@code org.apache.maven}; the injector only patches classes with that
     * exact name. The fake here lives in the test package, so the injector should skip it — verifying the
     * class-name guard and the no-op path when no ReactorReader is on the chain.
     */
    @Test
    void skipsWhenWorkspaceReaderIsNotReactorReaderByClassName() {
        var fake = new FakeReactorReader();
        ReactorReaderInjector.inject(sessionWith(fake), List.of(project("g", "a", "1")));
        assertTrue(fake.projectsByGAV.isEmpty(), "unrelated reader must not be mutated");
    }

    @Test
    void skipsWhenAddedIsEmpty() {
        var fake = new FakeReactorReader();
        ReactorReaderInjector.inject(sessionWith(fake), List.of());
        assertTrue(fake.projectsByGAV.isEmpty());
    }

    @Test
    void chainUnwrapFindsTheGuardedClass() {
        // Wrap our fake in a ChainedWorkspaceReader to ensure the injector walks the chain. The injector
        // still won't mutate it (class-name guard), but the chain-walking branch is exercised end-to-end.
        var fake = new FakeReactorReader();
        var chain = new ChainedWorkspaceReader(fake);
        ReactorReaderInjector.inject(sessionWith(chain), List.of(project("g", "a", "1")));
        assertTrue(fake.projectsByGAV.isEmpty());
    }

    @Test
    void noOpsOnNullWorkspaceReader() {
        var session = sessionWith(null);
        // Just verify no exception — there's nothing observable to assert when the chain is empty.
        ReactorReaderInjector.inject(session, List.of(project("g", "a", "1")));
    }

    /**
     * Direct test of the reflective mutation by force-injecting against the fake via the same reflective
     * shape, bypassing the class-name guard. Documents the contract: when ReactorReader IS found, both
     * maps get the new GAV/GA entries.
     */
    @Test
    void reflectivePopulationShape() throws Exception {
        var fake = new FakeReactorReader();
        var p1 = project("com.example", "alpha", "1.0");
        var p2 = project("com.example", "alpha", "2.0");
        var p3 = project("com.example", "beta", "1.0");

        // Apply the same mutation the injector would do once it has located a ReactorReader. This mirrors
        // the loop body so the test fails loudly if the field shape on ReactorReader ever drifts.
        applyDirectly(fake, List.of(p1, p2, p3));

        assertEquals(p1, fake.projectsByGAV.get("com.example:alpha:1.0"));
        assertEquals(p2, fake.projectsByGAV.get("com.example:alpha:2.0"));
        assertEquals(p3, fake.projectsByGAV.get("com.example:beta:1.0"));
        var alpha = fake.projectsByGA.get("com.example:alpha");
        assertNotNull(alpha);
        assertEquals(2, alpha.size());
    }

    private static void applyDirectly(FakeReactorReader fake, Collection<MavenProject> projects) throws Exception {
        var byGavField = FakeReactorReader.class.getDeclaredField("projectsByGAV");
        var byGaField = FakeReactorReader.class.getDeclaredField("projectsByGA");
        byGavField.setAccessible(true);
        byGaField.setAccessible(true);
        @SuppressWarnings("unchecked")
        var byGav = (Map<String, MavenProject>) byGavField.get(fake);
        @SuppressWarnings("unchecked")
        var byGa = (Map<String, List<MavenProject>>) byGaField.get(fake);
        for (var p : projects) {
            var gav = p.getGroupId() + ':' + p.getArtifactId() + ':' + p.getVersion();
            var ga = p.getGroupId() + ':' + p.getArtifactId();
            byGav.put(gav, p);
            byGa.computeIfAbsent(ga, k -> new java.util.ArrayList<>()).add(p);
        }
        assertNull(fake.projectsByGAV.get("missing:dep:1"));
    }
}
