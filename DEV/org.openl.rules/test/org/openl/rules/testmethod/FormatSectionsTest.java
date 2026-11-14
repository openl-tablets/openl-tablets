package org.openl.rules.testmethod;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/**
 * @author Yury Molchan
 */
class FormatSectionsTest {

    @Test
    void oneSectionShowsEveryNumber() {
        assertEquals("0.00", FormatSections.sectionFor("0.00", 5));
        assertEquals("0.00", FormatSections.sectionFor("0.00", -5));
        assertEquals("0.00", FormatSections.sectionFor("0.00", 0));
    }

    @Test
    void sectionsBySign() {
        assertEquals("0.00", FormatSections.sectionFor("0.00;(0.0)", 5));
        assertEquals("(0.0)", FormatSections.sectionFor("0.00;(0.0)", -5));
        assertEquals("0.00", FormatSections.sectionFor("0.00;(0.0)", 0));
        assertEquals("0.00", FormatSections.sectionFor("0.00;(0.0);0;@", 5));
        assertEquals("(0.0)", FormatSections.sectionFor("0.00;(0.0);0;@", -5));
        assertEquals("0", FormatSections.sectionFor("0.00;(0.0);0;@", 0));
        // a negative zero is a zero
        assertEquals("0", FormatSections.sectionFor("0.00;(0.0);0", -0.0));
    }

    @Test
    void sectionsByCondition() {
        assertEquals("0.00,", FormatSections.sectionFor("[<1]0.00,;0.00", 0));
        assertEquals("0.00", FormatSections.sectionFor("[<1]0.00,;0.00", 1));
        assertEquals("0.0,", FormatSections.sectionFor("[>=1000]0.0,;0.00", 1000));
        assertEquals("[Red]0.0", FormatSections.sectionFor("[Red][>=1000]0.0;[<=-1]0.00", 2000));
        assertEquals("0.00", FormatSections.sectionFor("[>=1000]0.0;[<=-1]0.00", -1));
        assertEquals("0", FormatSections.sectionFor("[>=1000]0.0;[<=-1]0.00;0", 5));
        assertEquals("0", FormatSections.sectionFor("[=1]0;[<>1]0.0", 1));
        assertEquals("0.0", FormatSections.sectionFor("[=1]0;[<>1]0.0", 2));
        assertEquals("0.0", FormatSections.sectionFor("[>1]0;[<=1]0.0", 1));
        assertEquals("0", FormatSections.sectionFor("[<0.5]0;0.0", 0.25));
        assertEquals("0,", FormatSections.sectionFor("[<.5]0,;0.00", 0));
        assertEquals("0.00", FormatSections.sectionFor("[ >= -1.5 ]0.00;0", -1));
        assertEquals("0", FormatSections.sectionFor("[==1]0;[!=1]0.0", 1));
        assertEquals("0.0", FormatSections.sectionFor("[==1]0;[!=1]0.0", 2));
        // the place of a section without a condition keeps its sign
        assertEquals("0.00", FormatSections.sectionFor("[>=1000]0.0,;0.00,;0.00", 0));
        assertEquals("0.00,", FormatSections.sectionFor("[>=1000]0.0,;0.00,;0.00", -5));
        assertEquals("0.0,", FormatSections.sectionFor("0.0,;[<-5]0.00,;0.00", 5));
        assertEquals("0.00", FormatSections.sectionFor("0.0,;[<-5]0.00,;0.00", -1));
        // no section takes the value
        assertNull(FormatSections.sectionFor("[>=1000]0.0;[<=-1]0.00", 5));
        assertNull(FormatSections.sectionFor("[<1]0.00", 5));
        // a bracket that is no condition
        assertEquals("[<.]0", FormatSections.sectionFor("[<.]0", 5));
    }

    @Test
    void separatorsThatDoNotSplit() {
        assertEquals("0.00\" a;b\"", FormatSections.sectionFor("0.00\" a;b\";(0.00)", 1));
        assertEquals("0.00\\;", FormatSections.sectionFor("0.00\\;;(0.00)", 1));
        assertEquals("(0.00)", FormatSections.sectionFor("0.00\\;;(0.00)", -1));
        // a condition in quotes or after a backslash is text
        assertEquals("0.00,", FormatSections.sectionFor("\"[<1]x\"0.00;0.00;0.00,", 0));
        assertEquals("\"[<1]x\"0.00", FormatSections.sectionFor("\"[<1]x\"0.00;0.00;0.00,", 0.5));
        assertEquals("0.00,", FormatSections.sectionFor("\\[<1]0.00;0.00;0.00,", 0));
        // a bracket that is no condition stays in the section
        assertEquals("[$€-x]0.00", FormatSections.sectionFor("[$€-x]0.00;(0.00)", 1));
    }
}
