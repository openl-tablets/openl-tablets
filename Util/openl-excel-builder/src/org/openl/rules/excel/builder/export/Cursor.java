package org.openl.rules.excel.builder.export;

import java.util.Objects;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class Cursor {

    @Getter
    private final int column;
    @Getter
    private final int row;

    public Cursor moveLeft(int x) {
        return new Cursor(column - x, row);
    }

    public Cursor moveRight(int x) {
        return new Cursor(column + x, row);
    }

    public Cursor moveDown(int y) {
        return new Cursor(column, row + y);
    }

    public Cursor moveUp(int y) {
        return new Cursor(column, row - y);
    }

    public Cursor setColumn(int x) {
        return new Cursor(x, row);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Cursor cursor)) {
            return false;
        }
        return column == cursor.column && row == cursor.row;
    }

    @Override
    public int hashCode() {
        return Objects.hash(column, row);
    }
}
