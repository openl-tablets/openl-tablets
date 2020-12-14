package org.open.rules.model.scaffolding;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;
import org.openl.rules.model.scaffolding.DatatypeModel;
import org.openl.rules.model.scaffolding.PathInfo;
import org.openl.rules.model.scaffolding.TypeInfo;
import org.openl.rules.model.scaffolding.data.DataModel;

public class DataModelTest {

    public static final String APPLICATION_JSON = "application/json";

    @Test
    public void testDataModelCreation() {
        PathInfo pi = new PathInfo("a/b/c", "abc", "GET", new TypeInfo("Bank", true), APPLICATION_JSON, APPLICATION_JSON);
        PathInfo qualityInfo = new PathInfo("/qualityI",
            "/qI",
            "GET",
            new TypeInfo("QualityIndicators", true),
            APPLICATION_JSON,
            APPLICATION_JSON);
        DatatypeModel dtm = new DatatypeModel("test");
        DataModel dm = new DataModel("bankData", "Bank", pi, dtm);
        DataModel theSameDm = new DataModel("bankData", "Bank", pi, dtm);
        assertEquals(dm, dm);
        assertEquals(dm.hashCode(), dm.hashCode());
        assertNotEquals(dm, null);
        assertEquals(dm, theSameDm);
        assertEquals(dm.hashCode(), theSameDm.hashCode());

        DataModel qualityIndicators = new DataModel("qualityIndicators", "Bank", pi, dtm);
        assertNotEquals(dm, qualityIndicators);
        assertNotEquals(dm.hashCode(), qualityIndicators.hashCode());

        DataModel qualityIndicatorsCorrectType = new DataModel("qualityIndicators", "QualityIndicator", pi, dtm);
        assertNotEquals(qualityIndicators, qualityIndicatorsCorrectType);
        assertNotEquals(qualityIndicators.hashCode(), qualityIndicatorsCorrectType.hashCode());

        DataModel qualityIndicatorsCorrectPathInfo = new DataModel("qualityIndicators",
            "QualityIndicator",
            qualityInfo,
            dtm);
        assertNotEquals(qualityIndicatorsCorrectType, qualityIndicatorsCorrectPathInfo);
        assertNotEquals(qualityIndicatorsCorrectType.hashCode(), qualityIndicatorsCorrectPathInfo.hashCode());

        assertEquals(qualityIndicatorsCorrectPathInfo.getName(), "qualityIndicators");
        assertEquals(qualityIndicatorsCorrectPathInfo.getType(), "QualityIndicator");
        assertEquals(qualityIndicatorsCorrectPathInfo.getDatatypeModel(), dtm);
        assertEquals(qualityIndicatorsCorrectPathInfo.getInfo(), qualityInfo);
    }
}
