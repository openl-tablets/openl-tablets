package org.openl.extension.xmlrules.parsing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.openl.extension.xmlrules.model.DataInstance;
import org.openl.extension.xmlrules.model.ExtensionModule;
import org.openl.extension.xmlrules.model.Sheet;
import org.openl.extension.xmlrules.model.Type;
import org.openl.extension.xmlrules.model.lazy.LazyWorkbook;
import org.openl.extension.xmlrules.model.single.ArrayValue;
import org.openl.extension.xmlrules.model.single.FieldImpl;
import org.openl.extension.xmlrules.model.single.Reference;
import org.openl.extension.xmlrules.model.single.ValuesRow;
import org.openl.extension.xmlrules.syntax.StringGridBuilder;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DataInstanceGridBuilder {
    private DataInstanceGridBuilder() {
    }

    public static void build(StringGridBuilder gridBuilder, ExtensionModule module, Sheet sheet, Collection<OpenLMessage> messages) {
        try {
            if (sheet.getDataInstances() == null) {
                return;
            }
            for (DataInstance dataInstance : sheet.getDataInstances()) {
                List<String> fields = dataInstance.getFields();
                Type t = getType(module, dataInstance);
                if (t == null) {
                    throw new IllegalArgumentException("Can't find type " + dataInstance.getType());
                }
                List<FieldImpl> actualFields = t.getFields();
                if (fields == null) {
                    fields = new ArrayList<String>();
                    for (FieldImpl field : actualFields) {
                        fields.add(field.getName());
                    }
                }

                gridBuilder.addCell("Data " + dataInstance.getType() + " " + dataInstance.getName(),
                        fields.size()).nextRow();
                // Fields
                boolean hasReferences = false;
                for (int fieldIndex = 0; fieldIndex < fields.size(); fieldIndex++) {
                    String field = fields.get(fieldIndex);

                    FieldImpl actualField = getField(actualFields, field);

                    if (actualField != null && actualField.getIsArray()) {
                        int maximumArrayLength = getMaximumArrayLength(dataInstance, fieldIndex);
                        for (int i = 0; i < maximumArrayLength; i++) {
                            gridBuilder.addCell(field + "[" + i + "]");
                        }
                    } else {
                        gridBuilder.addCell(field);
                    }

                    if (getReference(dataInstance, field) != null) {
                        hasReferences = true;
                    }
                }
                gridBuilder.nextRow();

                // References
                if (hasReferences) {
                    for (int fieldIndex = 0; fieldIndex < fields.size(); fieldIndex++) {
                        String field = fields.get(fieldIndex);
                        Reference reference = getReference(dataInstance, field);

                        if (reference != null) {
                            int maximumArrayLength = getMaximumArrayLength(dataInstance, fieldIndex);
                            if (maximumArrayLength > 0) {
                                for (int i = 0; i < maximumArrayLength; i++) {
                                    gridBuilder.addCell(">" + reference.getDataInstance());
                                }
                            } else {
                                gridBuilder.addCell(">" + reference.getDataInstance());
                            }
                        } else {
                            gridBuilder.addCell(null);
                        }
                    }
                    gridBuilder.nextRow();
                }

                // Business names
                for (String field : fields) {
                    gridBuilder.addCell(field.toUpperCase());
                }
                gridBuilder.nextRow();

                for (ValuesRow row : dataInstance.getValues()) {
                    for (ArrayValue value : row.getList()) {
                        List<String> arrayValues = value.getValues();
                        for (String arrayValue : arrayValues) {
                            gridBuilder.addCell(arrayValue);
                        }
                    }
                    gridBuilder.nextRow();
                }

                gridBuilder.nextRow();
            }
        } catch (RuntimeException e) {
            Logger log = LoggerFactory.getLogger(DataInstanceGridBuilder.class);
            log.error(e.getMessage(), e);
            messages.addAll(OpenLMessagesUtils.newErrorMessages(e));
            gridBuilder.nextRow();
        }
    }

    private static FieldImpl getField(List<FieldImpl> actualFields, String field) {
        FieldImpl actualField = null;
        for (FieldImpl f : actualFields) {
            if (f.getName().equals(field)) {
                actualField = f;
                break;
            }
        }
        return actualField;
    }

    private static Type getType(ExtensionModule module, DataInstance dataInstance) {
        for (LazyWorkbook workbook : module.getInternalWorkbooks()) {
            for (Sheet s : workbook.getSheets()) {
                for (Type type : s.getTypes()) {
                    if (dataInstance.getType().equals(type.getName())) {
                        return type;
                    }
                }
            }
        }

        return null;
    }

    private static Reference getReference(DataInstance dataInstance, String field) {
        List<Reference> references = dataInstance.getReferences();
        if (references != null) {
            for (Reference reference : references) {
                if (reference.getField().equals(field)) {
                    return reference;
                }
            }
        }

        return null;
    }

    private static int getMaximumArrayLength(DataInstance dataInstance, int fieldIndex) {
        int maximumArrayLength = -1;
        for (ValuesRow row : dataInstance.getValues()) {
            List rowList = row.getList();
            if (fieldIndex >= rowList.size()) {
                continue;
            }
            Object value = rowList.get(fieldIndex);
            if (value instanceof ArrayValue) {
                int arraySize = ((ArrayValue) value).getValues().size();
                if (arraySize > maximumArrayLength) {
                    maximumArrayLength = arraySize;
                }
            }
        }
        return maximumArrayLength;
    }

}
