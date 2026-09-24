package org.openl.studio.projects.model.tests;

import com.fasterxml.jackson.annotation.JsonProperty;

/** A value of a test case that is read a level at a time. */
public enum TestCaseValue {

    /** The whole value the tested rule returned. */
    @JsonProperty("result")
    RESULT,

    /** A value the case was given, by its position among the values the test table passes to the rule. */
    @JsonProperty("parameter")
    PARAMETER,

    /** What came out for a compared value, by the position of the comparison. */
    @JsonProperty("assertion")
    ASSERTION
}
