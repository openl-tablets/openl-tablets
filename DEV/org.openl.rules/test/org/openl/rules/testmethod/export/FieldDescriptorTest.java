package org.openl.rules.testmethod.export;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.Test;

import org.openl.types.java.JavaOpenClass;

/**
 * Checks which fields of the values of a parameter are written to a results workbook.
 *
 * @author Yury Molchan
 */
class FieldDescriptorTest {

    /** The elements of an array of arrays are described by their own fields, as the elements of an array are. */
    @Test
    void describesTheElementsOfAnArrayOfArrays() {
        var grid = new Grid();
        grid.setName("first");
        grid.setCells(new Cell[][]{{cell("a"), cell("b")}, {cell("c")}});

        var fields = FieldDescriptor.nonEmptyFields(JavaOpenClass.getOpenClass(Grid.class), List.of(grid), false);

        assertEquals(List.of("name", "cells"), fields.stream().map(field -> field.getField().getName()).toList());
        assertEquals(List.of("code"),
                fields.getLast().getChildren().stream().map(field -> field.getField().getName()).toList());
    }

    private static Cell cell(String code) {
        var cell = new Cell();
        cell.setCode(code);
        return cell;
    }

    @Getter
    @Setter
    public static class Grid {
        private String name;
        private Cell[][] cells;
    }

    @Getter
    @Setter
    public static class Cell {
        private String code;
    }
}
