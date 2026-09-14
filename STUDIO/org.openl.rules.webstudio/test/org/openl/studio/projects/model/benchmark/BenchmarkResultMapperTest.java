package org.openl.studio.projects.model.benchmark;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.openl.rules.testmethod.ParameterWithValueDeclaration;
import org.openl.studio.projects.service.benchmark.BenchmarkMeasurement;
import org.openl.types.java.JavaOpenClass;

@ExtendWith(MockitoExtension.class)
class BenchmarkResultMapperTest {

    @Mock
    private SchemaGenerator schemaGenerator;

    private BenchmarkResultMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new BenchmarkResultMapper(new ObjectMapper(), schemaGenerator, null);
    }

    @Test
    void mapResult_wholeTestTable() {
        var measurement = new BenchmarkMeasurement("m1", "abc123", "PolicyTest", true, null, 3, 512,
                3_500_000_000L, List.of());

        var result = mapper.mapResult(measurement);

        assertEquals("m1", result.id());
        assertEquals("abc123", result.tableId());
        assertEquals("PolicyTest", result.name());
        assertTrue(result.testTable());
        assertNull(result.runTable());
        assertEquals(3, result.testCases());
        assertEquals(512, result.runs());
        assertEquals(3500.0, result.executionTimeMs(), 0.001);
        assertTrue(result.parameters().isEmpty());
    }

    @Test
    void mapResult_oneTestCaseWithItsInput() {
        var driver = new ParameterWithValueDeclaration("driver", "Sara", JavaOpenClass.STRING);
        var measurement = new BenchmarkMeasurement("m2", "abc123", "PolicyTest", true, Boolean.TRUE, 1, 8,
                3_000_000_000L, List.of(driver));

        var result = mapper.mapResult(measurement);

        assertEquals(Boolean.TRUE, result.runTable());
        assertEquals(1, result.parameters().size());
        var parameter = result.parameters().getFirst();
        assertEquals("driver", parameter.name());
        assertEquals("Sara", parameter.value().asText());
        // The values are read, never edited, so the type is not described.
        assertNull(parameter.schema());
    }
}
