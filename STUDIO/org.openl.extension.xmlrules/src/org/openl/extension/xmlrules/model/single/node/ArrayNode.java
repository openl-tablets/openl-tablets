package org.openl.extension.xmlrules.model.single.node;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "array-node")
public class ArrayNode extends Node {
    private String value;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @XmlTransient
    public String[][] getArray() {
        List<String[]> rows = new ArrayList<String[]>();
        String[] rowString = value.replaceAll("[{}]", "").split(";");

        for (String s : rowString) {
            rows.add(s.split(","));
        }

        return rows.toArray(new String[rows.size()][]);
    }

    @Override
    public String toOpenLString() {
        StringBuilder sb = new StringBuilder();
        String[][] array = getArray();
        if (array.length <= 1) {
            sb.append("new Object[] {");
            for (String[] row : array) {
                boolean firstInRow = true;
                for (String s : row) {
                    if (!firstInRow) {
                        sb.append(", ");
                    } else {
                        firstInRow = false;
                    }

                    sb.append(s);
                }
            }
            sb.append("}");
        } else {
            sb.append("new Object[][] {");
            boolean firstRow = true;
            for (String[] row : array) {
                if (!firstRow) {
                    sb.append(", ");
                } else {
                    firstRow = false;
                }

                boolean firstColumn = true;
                sb.append("{");
                for (String s : row) {
                    if (!firstColumn) {
                        sb.append(", ");
                    } else {
                        firstColumn = false;
                    }

                    sb.append(s);
                }
                sb.append("}");
            }
            sb.append("}");
        }

        return sb.toString();
    }
}
