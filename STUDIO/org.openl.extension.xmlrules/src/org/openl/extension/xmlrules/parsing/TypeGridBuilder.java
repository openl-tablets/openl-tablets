package org.openl.extension.xmlrules.parsing;

import org.openl.util.StringUtils;
import org.openl.extension.xmlrules.ProjectData;
import org.openl.extension.xmlrules.model.Field;
import org.openl.extension.xmlrules.model.Sheet;
import org.openl.extension.xmlrules.model.Type;
import org.openl.extension.xmlrules.syntax.StringGridBuilder;
import org.openl.extension.xmlrules.utils.HelperFunctions;
import org.openl.message.OpenLMessagesUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TypeGridBuilder {
    private TypeGridBuilder() {
    }

    public static void build(StringGridBuilder gridBuilder, Sheet sheet) {
        try {
            if (sheet.getTypes() == null) {
                return;
            }
            for (Type type : sheet.getTypes()) {
                ProjectData.getCurrentInstance().addType(type);
                gridBuilder.addCell("Datatype " + type.getName(), 2).nextRow();

                for (Field field : type.getFields()) {
                    String typeName = HelperFunctions.getOpenLType(field.getTypeName());
                    if (StringUtils.isBlank(typeName)) {
                        typeName = "String";
                    }
                    if (field.getIsArray()) {
                        typeName += "[]";
                    }

                    gridBuilder.addCell(typeName).addCell(field.getName()).nextRow();
                }

                gridBuilder.nextRow();
            }
        } catch (RuntimeException e) {
            Logger log = LoggerFactory.getLogger(TypeGridBuilder.class);
            log.error(e.getMessage(), e);
            OpenLMessagesUtils.addError(e);
            gridBuilder.nextRow();
        }
    }

}
