package org.openl.rules.data;

import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

public class DatatypeArrayMultiRowElementContext {
    private final Map<String, Map<Integer, Element>> arrayIndexes = new HashMap<>();
    private final Set<Object> valuesOfRows = Collections.newSetFromMap(new IdentityHashMap<>());

    /**
     * Remembers an array or a collection that the rows set to a field.
     */
    public void addValueOfRows(Object values) {
        valuesOfRows.add(values);
    }

    /**
     * Checks whether the rows set the array or the collection, as opposed to a default value of the field.
     */
    public boolean isValueOfRows(@Nullable Object values) {
        return values != null && valuesOfRows.contains(values);
    }

    @Getter
    @Setter
    private int row;

    @Getter
    @Setter
    private boolean rowValueIsTheSameAsPrevious;

    /**
     * Whether the current row has blank cells for the collection element and the objects nested in it.
     */
    @Getter
    @Setter
    private boolean rowValueIsEmpty;

    /**
     * Returns the index of the collection element that the current row defines.
     *
     * <p>A row continues the element of the previous row when its values are the same. Otherwise, it defines the
     * next element of the same collection.
     *
     * <p>Rows without values before the first element of a collection define no element. The first row with values
     * defines the first element, so the collection does not start with an empty element. After the first element, an
     * empty row is compared with the previous row as any other row.
     *
     * @param array  the path of the collection from the root object
     * @param target the object that holds the collection
     */
    public int getIndex(String array, Object target) {
        var elements = arrayIndexes.computeIfAbsent(array, k -> new HashMap<>());
        var element = elements.get(row);
        if (element == null) {
            var prev = row > 0 ? elements.get(row - 1) : null;
            var defined = !rowValueIsEmpty;
            if (prev == null || prev.target() != target) {
                element = new Element(0, target, defined);
            } else if (rowValueIsTheSameAsPrevious || isBeforeFirstElement(prev)) {
                element = new Element(prev.index(), target, prev.defined() || defined);
            } else {
                element = new Element(prev.index() + 1, target, defined);
            }
            elements.put(row, element);
        }
        return element.index();
    }

    private static boolean isBeforeFirstElement(Element element) {
        return element.index() == 0 && !element.defined();
    }

    /**
     * The element of a collection that a row defines.
     *
     * @param index   the index of the element
     * @param target  the object that holds the collection
     * @param defined whether a row of the element has values
     */
    private record Element(int index, Object target, boolean defined) {
    }
}
