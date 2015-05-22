package org.openl.extension.xmlrules.syntax;

/**
 * @author nsamatov.
 */
public class StringGridBuilder {
    private int startRow = 0;
    private int startColumn = 0;

    private int row = startRow;
    private int column = startColumn;

    private SimpleGrid.SimpleGridBuilder builder;

    public StringGridBuilder(String uri) {
        builder = new SimpleGrid.SimpleGridBuilder(uri);
    }

    public StringGridBuilder setStartRow(int row) {
        this.row = this.startRow = row;
        return this;
    }

    public StringGridBuilder setStartColumn(int column) {
        this.column = this.startColumn = column;
        return this;
    }

    public int getStartRow() {
        return startRow;
    }

    public int getStartColumn() {
        return startColumn;
    }

    public StringGridBuilder nextRow() {
        row++;
        column = startColumn;
        return this;
    }

    public StringGridBuilder setCell(int column, int row, int width, int height, String value) {
        builder.addCell(new SimpleCell(column, row, width, height, value));

        int newRow = row + height - 1;
        int newColumn = column + width - 1;
        this.row = Math.max(this.row, newRow);
        this.column = Math.max(this.column, newColumn);

        return this;
    }

    public StringGridBuilder addCell(String value) {
        return addCell(value, 1);
    }

    public StringGridBuilder addCell(String value, int width) {
        builder.addCell(new SimpleCell(column, row, width, 1, value));
        column += width;
        return this;
    }

    public SimpleGrid build() {
        return builder.build();
    }

    public int getRow() {
        return row;
    }

    public void setRow(int row) {
        this.row = row;
    }

    public int getColumn() {
        return column;
    }

    public void setColumn(int column) {
        this.column = column;
    }
}
