package org.openl.rules.table.xls.builder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openl.base.INamedThing;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;

/**
 * A class, containing description of Data Table's field columns (or variables)
 *
 * @author NSamatov
 *
 */
public class DataTableUserDefinedTypeField extends DataTableField {
    private IOpenClass type;

    private List<DataTableField> availableFields;

    private final PredefinedTypeChecker predefinedChecker;

    /**
     * Create a field with a given generalized abstraction of a class and a field name
     *
     * @param type generalized abstraction of a class
     * @param name name technical name of a field
     * @param predefinedChecker object that checks if a "type" is a predefined OpenL type such as IntRange etc
     */
    public DataTableUserDefinedTypeField(IOpenClass type, String name, PredefinedTypeChecker predefinedChecker) {
        this(type, name, name.toUpperCase(), predefinedChecker);
    }

    /**
     * Create a field with a given generalized abstraction of a class and a field name
     *
     * @param type generalized abstraction of a class
     * @param name name technical name of a field
     * @param businessName business name of a field
     * @param predefinedChecker object that checks if a "type" is a predefined OpenL type such as IntRange etc
     */
    public DataTableUserDefinedTypeField(IOpenClass type,
            String name,
            String businessName,
            PredefinedTypeChecker predefinedChecker) {
        super(name, businessName);
        this.type = type;
        this.predefinedChecker = predefinedChecker;
    }

    /**
     * Get a generalized abstraction of a class
     *
     * @return generalized abstraction of a class
     */
    public IOpenClass getType() {
        return type;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected List<DataTableField> getAvailableFields() {
        if (availableFields == null) {
            List<DataTableField> list = new ArrayList<>();

            for (IOpenField field : getType().getFields()) {
                if (field.isConst() || !field.isWritable()) {
                    continue;
                }

                list.add(new DataTableUserDefinedTypeField(field.getType(), field.getName(), predefinedChecker));
            }

            availableFields = Collections.unmodifiableList(list);
        }

        return availableFields;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getTypeName() {
        return getType().getDisplayName(INamedThing.SHORT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isComplex() {
        return !getType().isSimple() && !getType().isArray() && !predefinedChecker.isPredefined(getType());
    }

    /**
     * Utility interface that checks if a "type" is a predefined OpenL type such as IntRange etc
     *
     * @author NSamatov
     *
     */
    public interface PredefinedTypeChecker {
        /**
         * Check if a "type" is a predefined OpenL type
         *
         * @param type checking type
         * @return true if a "type" is a predefined OpenL type such as IntRange etc, false otherwise
         */
        boolean isPredefined(IOpenClass type);
    }
}
