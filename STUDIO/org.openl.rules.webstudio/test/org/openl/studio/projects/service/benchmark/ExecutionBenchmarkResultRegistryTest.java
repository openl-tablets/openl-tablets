package org.openl.studio.projects.service.benchmark;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.openl.studio.projects.model.ProjectIdModel;

class ExecutionBenchmarkResultRegistryTest {

    private ExecutionBenchmarkResultRegistry registry;
    private ProjectIdModel projectId;
    private ProjectIdModel otherProjectId;

    private static BenchmarkMeasurement measurement(String id) {
        return new BenchmarkMeasurement(id, "table1", "Main", "Table", true, null, 1, 10, 3_000_000_000L, List.of());
    }

    private static List<String> idsOf(List<BenchmarkMeasurement> measurements) {
        return measurements.stream().map(BenchmarkMeasurement::id).toList();
    }

    @BeforeEach
    void setUp() {
        registry = new ExecutionBenchmarkResultRegistry();
        projectId = ProjectIdModel.builder()
                .repository("design")
                .projectName("TestProject")
                .build();
        otherProjectId = ProjectIdModel.builder()
                .repository("design")
                .projectName("OtherProject")
                .build();
    }

    @Test
    void collect_nothingTaken() {
        assertTrue(registry.collect(projectId).isEmpty());
    }

    @Test
    void collect_takesCompletedMeasurementsOnce() {
        registry.setTask(projectId, "t1", CompletableFuture.completedFuture(List.of(measurement("m1"))));

        assertIterableEquals(List.of("m1"), idsOf(registry.collect(projectId)));
        assertIterableEquals(List.of("m1"), idsOf(registry.collect(projectId)));
    }

    @Test
    void collect_newestFirst() {
        registry.setTask(projectId, "t1", CompletableFuture.completedFuture(List.of(measurement("m1"))));
        registry.collect(projectId);
        registry.setTask(projectId, "t2", CompletableFuture.completedFuture(List.of(measurement("m2"), measurement("m3"))));

        assertIterableEquals(List.of("m2", "m3", "m1"), idsOf(registry.collect(projectId)));
    }

    @Test
    void collect_runningBenchmarkAddsNothing() {
        registry.setTask(projectId, "t1", new CompletableFuture<>());

        assertTrue(registry.collect(projectId).isEmpty());
    }

    @Test
    void collect_failedBenchmarkAddsNothing() {
        registry.setTask(projectId, "t1", CompletableFuture.failedFuture(new IllegalStateException("boom")));

        assertTrue(registry.collect(projectId).isEmpty());
    }

    @Test
    void collect_cancelledBenchmarkAddsNothing() {
        var task = new CompletableFuture<List<BenchmarkMeasurement>>();
        registry.setTask(projectId, "t1", task);
        task.cancel(true);

        assertTrue(registry.collect(projectId).isEmpty());
    }

    @Test
    void setTask_anotherProjectStartsANewList() {
        registry.setTask(projectId, "t1", CompletableFuture.completedFuture(List.of(measurement("m1"))));
        registry.collect(projectId);

        registry.setTask(otherProjectId, "t2", CompletableFuture.completedFuture(List.of(measurement("m2"))));

        assertIterableEquals(List.of("m2"), idsOf(registry.collect(otherProjectId)));
    }

    @Test
    void collect_anotherProjectHasNoMeasurementsOfItsOwn() {
        registry.setTask(projectId, "t1", CompletableFuture.completedFuture(List.of(measurement("m1"))));

        assertTrue(registry.collect(otherProjectId).isEmpty());
        // The benchmark of the measured project is still there to be read.
        assertIterableEquals(List.of("m1"), idsOf(registry.collect(projectId)));
    }

    @Test
    void forget_forgetsNamedMeasurements() {
        registry.setTask(projectId, "t1",
                CompletableFuture.completedFuture(List.of(measurement("m1"), measurement("m2"))));
        registry.collect(projectId);

        registry.forget(projectId, List.of("m1"));

        assertIterableEquals(List.of("m2"), idsOf(registry.collect(projectId)));
    }

    @Test
    void forget_withoutIdentifiersForgetsEveryMeasurementOfTheProject() {
        registry.setTask(projectId, "t1",
                CompletableFuture.completedFuture(List.of(measurement("m1"), measurement("m2"))));
        registry.collect(projectId);

        registry.forget(projectId, List.of());

        assertTrue(registry.collect(projectId).isEmpty());
    }

    @Test
    void forget_leavesTheMeasurementsOfAnotherProjectAlone() {
        registry.setTask(projectId, "t1", CompletableFuture.completedFuture(List.of(measurement("m1"))));
        registry.collect(projectId);

        registry.forget(otherProjectId, List.of());

        assertIterableEquals(List.of("m1"), idsOf(registry.collect(projectId)));
    }

    @Test
    void clear_forgetsEveryMeasurementAndCancelsTheBenchmark() {
        registry.setTask(projectId, "t1", CompletableFuture.completedFuture(List.of(measurement("m1"))));
        registry.collect(projectId);
        var running = new CompletableFuture<List<BenchmarkMeasurement>>();
        registry.setTask(projectId, "t2", running);

        registry.clear();

        assertTrue(running.isCancelled());
        assertFalse(registry.hasTask(projectId));
        assertEquals(List.of(), registry.collect(projectId));
    }
}
