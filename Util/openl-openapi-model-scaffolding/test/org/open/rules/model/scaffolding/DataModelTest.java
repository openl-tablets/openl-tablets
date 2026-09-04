package org.open.rules.model.scaffolding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

import org.openl.rules.model.scaffolding.DatatypeModel;
import org.openl.rules.model.scaffolding.PathInfo;
import org.openl.rules.model.scaffolding.TypeInfo;
import org.openl.rules.model.scaffolding.data.DataModel;

class DataModelTest {

    private static final String APPLICATION_JSON = "application/json";

    @Test
    void testDataModelCreation() {
        var pi = new PathInfo("a/b/c", "abc", PathInfo.Operation.GET,
                new TypeInfo("Bank", "Bank", TypeInfo.Type.DATATYPE),
                APPLICATION_JSON,
                APPLICATION_JSON);
        var qualityInfo = new PathInfo("/qualityI",
                "/qI",
                PathInfo.Operation.GET,
                new TypeInfo("QualityIndicators", "QualityIndicators", TypeInfo.Type.DATATYPE),
                APPLICATION_JSON,
                APPLICATION_JSON);
        var dtm = new DatatypeModel("test");
        var dm = new DataModel("bankData", "Bank", pi, dtm);
        var theSameDm = new DataModel("bankData", "Bank", pi, dtm);
        assertEquals(dm, dm);
        assertEquals(dm.hashCode(), dm.hashCode());
        assertNotEquals(dm, null);
        assertEquals(dm, theSameDm);
        assertEquals(dm.hashCode(), theSameDm.hashCode());

        var qualityIndicators = new DataModel("qualityIndicators", "Bank", pi, dtm);
        assertNotEquals(dm, qualityIndicators);
        assertNotEquals(dm.hashCode(), qualityIndicators.hashCode());

        var qualityIndicatorsCorrectType = new DataModel("qualityIndicators", "QualityIndicator", pi, dtm);
        assertNotEquals(qualityIndicators, qualityIndicatorsCorrectType);
        assertNotEquals(qualityIndicators.hashCode(), qualityIndicatorsCorrectType.hashCode());

        var qualityIndicatorsCorrectPathInfo = new DataModel("qualityIndicators",
                "QualityIndicator",
                qualityInfo,
                dtm);
        assertNotEquals(qualityIndicatorsCorrectType, qualityIndicatorsCorrectPathInfo);
        assertNotEquals(qualityIndicatorsCorrectType.hashCode(), qualityIndicatorsCorrectPathInfo.hashCode());

        assertEquals(qualityIndicatorsCorrectPathInfo.getName(), "qualityIndicators");
        assertEquals(qualityIndicatorsCorrectPathInfo.getType(), "QualityIndicator");
        assertEquals(qualityIndicatorsCorrectPathInfo.getDatatypeModel(), dtm);
        assertEquals(qualityIndicatorsCorrectPathInfo.getPathInfo(), qualityInfo);
    }
}
