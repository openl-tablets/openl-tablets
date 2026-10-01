package org.openl.studio.docs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class OpenLTableTest {

    @Test
    void acceptsRectangularMerges() {
        assertEquals(List.of(), OpenLTable.issues("""
                Rules String greeting(Integer hour, Boolean weekend)
                Rule,C1,C2,RET1
                ,hour,weekend,greeting
                ---
                R10,0-12,false,Good Morning
                R20,^,true,Lazy Morning
                R30,12-24,,Good Day
                Total,<,<,"<"
                Sum,"^",x,y
                """));
    }

    @Test
    void acceptsAreasSpanningRowsAndColumns() {
        assertEquals(List.of(), OpenLTable.issues("""

                Header, with a comma
                a,<,b
                ^,^,c
                ^,<,d
                """));
    }

    @Test
    void reportsALeftMarkerInTheFirstColumn() {
        assertEquals(List.of(new Issue(2, "A `<` in the first column has no cell on its left to join.")),
                OpenLTable.issues("Header\nx,y\n<,z\n"));
    }

    @Test
    void reportsMergesThatAreNoRectangles() {
        assertEquals(List.of(new Issue(1, "The merged cells do not form a rectangle.")), OpenLTable.issues("""
                Header
                ^,x
                """));
        assertEquals(List.of(new Issue(2, "The merged cells do not form a rectangle.")), OpenLTable.issues("""
                Header
                a,b,c
                ^,<,d
                """));
        assertEquals(List.of(new Issue(2, "The merged cells do not form a rectangle.")), OpenLTable.issues("""
                Header
                a,<,b
                ^,x,c
                """));
    }

    @Test
    void reportsMisplacedSeparators() {
        assertEquals(List.of(new Issue(0, "The table starts with `---`. Its first line is the table header.")),
                OpenLTable.issues("---\nx\n"));
        assertEquals(List.of(new Issue(3, "A second `---` line. One line ends the column headers.")),
                OpenLTable.issues("Header\n---\nx\n---\ny\n"));
        assertEquals(List.of(new Issue(0, "The table has no rows.")), OpenLTable.issues(" \n"));
    }

    @Test
    void reportsBrokenRecords() {
        assertEquals(List.of(new Issue(1, "A quoted value is not closed.")), OpenLTable.issues("Header\n\"open\n"));
    }
}
