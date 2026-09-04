package org.openl.rules.datatype;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.openl.rules.runtime.RulesEngineFactory;
import org.openl.types.IOpenClass;

/**
 * A datatype marks the fields its bean keeps either way round: {@code name~} marks a single field as transient, and
 * {@code name*} marks the fields to keep and turns every other field of that datatype transient.
 *
 * <p>Which cell of a row holds the name is up to the table, so the same marking must be read from a titled table
 * that puts {@code Name} first as from a legacy table that puts it second.
 */
class TransientFieldsTest {

    private static final String SRC = "test/rules/datatype/TransientFields.xlsx";

    private static IOpenClass moduleOpenClass;

    @BeforeAll
    static void compile() {
        moduleOpenClass = new RulesEngineFactory<>(SRC).getCompiledOpenClass().getOpenClassWithErrors();
    }

    // Name is titled into the first column, so a table read by position would look for the marking in Type.
    @Test
    void aMarkedFieldIsFoundInATitledTableThatReordersItsColumns() {
        assertTransient("TitledReordered", "dropped");
        assertNotTransient("TitledReordered", "kept");
    }

    @Test
    void aMarkedFieldIsFoundInATitledTable() {
        assertTransient("TitledCanonical", "dropped");
        assertNotTransient("TitledCanonical", "kept");
    }

    @Test
    void aMarkedFieldIsFoundInALegacyTable() {
        assertTransient("LegacyMarked", "dropped");
        assertNotTransient("LegacyMarked", "kept");
    }

    // Without a marked field the other marking applies: only the field carrying '~' is transient.
    @Test
    void anUnmarkedTableKeepsEveryFieldButTheOneMarkedTransient() {
        assertTransient("LegacyDefault", "dropped");
        assertNotTransient("LegacyDefault", "kept");
    }

    // A commented row declares no field, so commenting one out must not turn the whole datatype transient.
    @Test
    void aCommentedRowDoesNotMarkAnything() {
        assertNotTransient("CommentedMarker", "kept");
        assertThrows(NoSuchFieldException.class,
                () -> findType("CommentedMarker").getInstanceClass().getDeclaredField("dropped"));
    }

    private static void assertTransient(String datatype, String fieldName) {
        assertTrue(isTransient(datatype, fieldName),
                "Field '%s' of '%s' is expected to be transient".formatted(fieldName, datatype));
    }

    private static void assertNotTransient(String datatype, String fieldName) {
        assertFalse(isTransient(datatype, fieldName),
                "Field '%s' of '%s' is not expected to be transient".formatted(fieldName, datatype));
    }

    private static boolean isTransient(String datatype, String fieldName) {
        try {
            return Modifier.isTransient(findType(datatype).getInstanceClass().getDeclaredField(fieldName).getModifiers());
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException(e);
        }
    }

    private static IOpenClass findType(String datatype) {
        var type = moduleOpenClass.findType(datatype);
        assertNotNull(type, "There is '%s' datatype".formatted(datatype));
        return type;
    }
}
