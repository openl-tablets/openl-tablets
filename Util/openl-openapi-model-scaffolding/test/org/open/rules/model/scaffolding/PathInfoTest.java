package org.open.rules.model.scaffolding;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;
import org.openl.rules.model.scaffolding.PathInfo;
import org.openl.rules.model.scaffolding.TypeInfo;

public class PathInfoTest {

    public static final String APPLICATION_JSON = "application/json";
    public static final String TEXT_PLAIN = "text/plain";
    public static final String POST = "POST";
    public static final String TEXT_JAVASCRIPT = "text/javascript";

    @Test
    public void testPathInfoCreation() {
        PathInfo xyzPath = new PathInfo("/x/y/z",
            "xyz",
            POST,
            new TypeInfo(Double.class),
            APPLICATION_JSON,
            TEXT_PLAIN);
        PathInfo xyzPathText = new PathInfo("/x/y/z",
            "xyz",
            POST,
            new TypeInfo(Double.class),
            TEXT_PLAIN,
            TEXT_PLAIN);
        PathInfo xyzPathJSON = new PathInfo("/x/y/z",
            "xyz",
            POST,
            new TypeInfo(Double.class),
            APPLICATION_JSON,
            APPLICATION_JSON);
        PathInfo oneMoreXyzPath = new PathInfo("/x/y/z",
            "xyz",
            POST,
            new TypeInfo(Double.class),
            APPLICATION_JSON,
            TEXT_PLAIN);
        PathInfo xyzStringPath = new PathInfo("/x/y/z",
            "xyz",
            POST,
            new TypeInfo(String.class),
            APPLICATION_JSON,
            TEXT_PLAIN);
        PathInfo xyFormattedPath = new PathInfo("/x/y/z",
            "xy",
            POST,
            new TypeInfo(Double.class),
            TEXT_JAVASCRIPT,
            TEXT_PLAIN);
        PathInfo xyOriginalPath = new PathInfo("/x/y",
            "xyz",
            POST,
            new TypeInfo(Double.class),
            TEXT_JAVASCRIPT,
            TEXT_JAVASCRIPT);
        PathInfo xyzPUT = new PathInfo("/x/y/z",
            "xyz",
            "PUT",
            new TypeInfo(Double.class),
            TEXT_PLAIN,
            APPLICATION_JSON);

        assertEquals(xyzPath, xyzPath);
        assertEquals(xyzPath, oneMoreXyzPath);
        assertEquals(xyzPath.hashCode(), oneMoreXyzPath.hashCode());
        assertNotEquals(xyzPath, null);

        assertNotEquals(xyzPath, xyzPathText);
        assertNotEquals(xyzPath, xyzPathJSON);

        assertNotEquals(xyzPath, xyzStringPath);
        assertNotEquals(xyzPath.hashCode(), xyzStringPath.hashCode());

        assertNotEquals(xyzPath, xyFormattedPath);
        assertNotEquals(xyzPath.hashCode(), xyFormattedPath.hashCode());

        assertNotEquals(xyzPath, xyOriginalPath);
        assertNotEquals(xyzPath.hashCode(), xyOriginalPath.hashCode());

        assertNotEquals(xyzPath, xyzPUT);
        assertNotEquals(xyzPath.hashCode(), xyzPUT.hashCode());

        PathInfo bankRatingPath = new PathInfo();
        bankRatingPath.setOriginalPath("/bankRating");
        bankRatingPath.setFormattedPath("bankRating");
        bankRatingPath.setOperation("POST");
        bankRatingPath.setReturnType(new TypeInfo(Double.class));
        bankRatingPath.setProduces(APPLICATION_JSON);
        bankRatingPath.setConsumes(TEXT_PLAIN);

        assertEquals("/bankRating", bankRatingPath.getOriginalPath());
        assertEquals("bankRating", bankRatingPath.getFormattedPath());
        assertEquals("POST", bankRatingPath.getOperation());
        assertEquals("Double", bankRatingPath.getReturnType().getSimpleName());
        assertEquals(APPLICATION_JSON, bankRatingPath.getProduces());
        assertEquals(TEXT_PLAIN, bankRatingPath.getConsumes());
    }
}
