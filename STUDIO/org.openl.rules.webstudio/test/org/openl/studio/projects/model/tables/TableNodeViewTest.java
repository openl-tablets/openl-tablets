package org.openl.studio.projects.model.tables;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

/**
 * Verifies the two shapes of a graph node and the kind that tells them apart.
 */
class TableNodeViewTest {

    private final ObjectMapper mapper = new ObjectMapper();

    /** Every kind a node can carry, mapped to the node shape that stands for it. */
    private static Stream<String> mappedKinds() {
        return Arrays.stream(TableNodeView.class.getAnnotation(JsonSubTypes.class).value())
                .flatMap(type -> Stream.concat(Stream.of(type.name()), Arrays.stream(type.names())))
                .filter(name -> !name.isEmpty());
    }

    @Test
    void everyNodeKindIsMappedToANodeShape() {
        var mapped = mappedKinds().collect(Collectors.toSet());
        var declared = Arrays.stream(TableGraphNodeKind.values())
                .map(TableGraphNodeKind::value)
                .collect(Collectors.toSet());
        assertEquals(declared, mapped, "a new graph node kind must be mapped to a node shape");
    }

    @Test
    void aDatatypeNodeCarriesItsDataModel() throws Exception {
        var node = DatatypeNodeView.builder()
                .extendz("vehicle")
                .fields(List.of(new DatatypeNodeFieldView("drivers", "Driver[]", "driver", Boolean.TRUE),
                        new DatatypeNodeFieldView("number", "String", null, null)))
                .id("policy")
                .name("Policy")
                .kind(TableGraphNodeKind.DATATYPE)
                .dependencies(Set.of("vehicle", "driver"))
                .build();

        var body = mapper.writeValueAsString(node);
        var json = mapper.readTree(body);

        // the kind doubles as the discriminator, so it is written once, by the node itself
        assertEquals("Datatype", json.get("kind").asText());
        assertEquals(1, body.split("\"kind\"", -1).length - 1, "the discriminator is not written twice");
        assertEquals("vehicle", json.get("extends").asText());
        assertEquals("Driver[]", json.get("fields").get(0).get("type").asText());
        assertTrue(json.get("fields").get(0).get("collection").asBoolean());
        // a field of a simple type is listed without a reference and without the collection flag
        assertNull(json.get("fields").get(1).get("ref"));
        assertNull(json.get("fields").get(1).get("collection"));
        // the calling contract of a rules table has no place on a datatype
        assertNull(json.get("signature"));
    }

    @Test
    void anExecutableNodeCarriesItsCallingContract() throws Exception {
        var node = ExecutableNodeView.builder()
                .id("rule")
                .name("calcPremium")
                .kind(TableGraphNodeKind.SPREADSHEET)
                .build();

        var json = mapper.readTree(mapper.writeValueAsString(node));

        assertEquals("Spreadsheet", json.get("kind").asText());
        // the data model belongs to a datatype, not to a callable table
        assertNull(json.get("fields"));
        assertNull(json.get("extends"));
    }
}
