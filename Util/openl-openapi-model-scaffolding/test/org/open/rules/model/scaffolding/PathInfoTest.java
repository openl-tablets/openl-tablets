package org.open.rules.model.scaffolding;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;
import org.openl.rules.model.scaffolding.PathInfo;

public class PathInfoTest {

    @Test
    public void testPathInfoCreation() {
        PathInfo xyzPath = new PathInfo("/x/y/z", "xyz", "POST", "Double");
        PathInfo oneMoreXyzPath = new PathInfo("/x/y/z", "xyz", "POST", "Double");
        PathInfo xyzStringPath = new PathInfo("/x/y/z", "xyz", "POST", "String");
        PathInfo xyFormattedPath = new PathInfo("/x/y/z", "xy", "POST", "Double");
        PathInfo xyOriginalPath = new PathInfo("/x/y", "xyz", "POST", "Double");
        PathInfo xyzPUT = new PathInfo("/x/y/z", "xyz", "PUT", "Double");

        assertEquals(xyzPath, xyzPath);
        assertEquals(xyzPath, oneMoreXyzPath);
        assertEquals(xyzPath.hashCode(), oneMoreXyzPath.hashCode());
        assertNotEquals(xyzPath, null);

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
        bankRatingPath.setReturnType("Double");

        assertEquals("/bankRating", bankRatingPath.getOriginalPath());
        assertEquals("bankRating", bankRatingPath.getFormattedPath());
        assertEquals("POST", bankRatingPath.getOperation());
        assertEquals("Double", bankRatingPath.getReturnType());
    }
}
