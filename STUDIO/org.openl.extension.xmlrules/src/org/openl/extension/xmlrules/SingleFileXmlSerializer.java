package org.openl.extension.xmlrules;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import org.openl.extension.Serializer;
import org.openl.extension.xmlrules.model.*;
import org.openl.extension.xmlrules.model.single.*;

public class SingleFileXmlSerializer implements Serializer<ExtensionModule> {
    private static final String MODULE_TAG = "module";
    private static final String TABLE_GROUP_TAG = "tableGroup";
    private static final String XLS_FILE_NAME_TAG = "xls-file";
    private static final String TYPE_TAG = "type";
    private static final String DATA_INSTANCE_TAG = "dataInstance";
    private static final String FIELD_TAG = "field";
    private static final String TABLE_TAG = "table";
    private static final String FUNCTION_TAG = "function";
    private static final String CONDITION_TAG = "condition";
    private static final String RETURN_TAG = "return";
    private static final String PARAMETER_TAG = "parameter";
    private static final String XLS_REGION_TAG = "region";
    private static final String SEGMENT_TAG = "segment";
    public static final String CONDITION_EXPRESSION = "conditionExpression";
    public static final String FUNCTION_EXPRESSION = "functionExpression";

    private final XStream xstream;

    public SingleFileXmlSerializer() {
        xstream = new XStream(new DomDriver());
        xstream.aliasType(MODULE_TAG, ExtensionModuleImpl.class);
        xstream.aliasType(TABLE_GROUP_TAG, TableGroupImpl.class);
        xstream.aliasField(XLS_FILE_NAME_TAG, ExtensionModuleImpl.class, "xlsFileName");

        xstream.aliasType(TYPE_TAG, TypeImpl.class);
        xstream.aliasType(DATA_INSTANCE_TAG, DataInstanceImpl.class);
        xstream.aliasType(FIELD_TAG, FieldImpl.class);
        xstream.aliasType(TABLE_TAG, TableImpl.class);
        xstream.aliasType(FUNCTION_TAG, FunctionImpl.class);
        xstream.aliasType(CONDITION_TAG, ConditionImpl.class);
        xstream.aliasType(CONDITION_EXPRESSION, ConditionExpressionImpl.class);
        xstream.aliasType(FUNCTION_EXPRESSION, FunctionExpressionImpl.class);
        xstream.aliasType(RETURN_TAG, ReturnValueImpl.class);
        xstream.aliasType(XLS_REGION_TAG, XlsRegionImpl.class);
        xstream.aliasType(PARAMETER_TAG, ParameterImpl.class);

        xstream.aliasType(SEGMENT_TAG, SegmentImpl.class);
    }

    @Override
    public String serialize(ExtensionModule extensionModule) {
        ExtensionModuleImpl module = (ExtensionModuleImpl) extensionModule;
        for (TableGroup group : module.getTableGroups()) {
            preProcess(group);
        }
        return xstream.toXML(module);
    }

    @Override
    public ExtensionModule deserialize(InputStream source) {
        ExtensionModuleImpl extensionModule = (ExtensionModuleImpl) xstream.fromXML(source);
        for (TableGroup group : extensionModule.getTableGroups()) {
            postProcess(group);
        }
        return extensionModule;
    }

    private void postProcess(TableGroup tableGroup) {
        if (tableGroup.getTypes() != null) {
            for (Type type : tableGroup.getTypes()) {
                postProcess((XlsRegionImpl) type.getRegion());

                for (Field field : type.getFields()) {
                    postProcess((XlsRegionImpl) field.getRegion());
                }
            }
        }

        if (tableGroup.getTables() != null) {
            for (Table table : tableGroup.getTables()) {
                TableImpl t = (TableImpl) table;
                postProcess(t.getRegion());

                List<Condition> verticalConditions = t.getVerticalConditions();
                if (verticalConditions != null) {
                    for (Condition condition : verticalConditions) {
                        for (ConditionExpression expression : condition.getExpressions()) {
                            postProcess((XlsRegionImpl) expression.getRegion());
                        }
                    }
                } else {
                    t.setVerticalConditions(Collections.<Condition>emptyList());
                }
                List<Condition> horizontalConditions = table.getHorizontalConditions();
                if (horizontalConditions != null) {
                    for (Condition condition : horizontalConditions) {
                        for (ConditionExpression expression : condition.getExpressions()) {
                            postProcess((XlsRegionImpl) expression.getRegion());
                        }
                    }
                } else {
                    t.setHorizontalConditions(Collections.<Condition>emptyList());
                }

                for (List<ReturnValue> row : table.getReturnValues()) {
                    for (ReturnValue returnValue : row) {
                        postProcess((XlsRegionImpl) returnValue.getRegion());
                    }
                }
            }
        }

        if (tableGroup.getFunctions() != null) {
            for (Function function : tableGroup.getFunctions()) {
                postProcess((XlsRegionImpl) function.getRegion());
            }
        }
    }

    private void postProcess(XlsRegionImpl path) {
        if (path == null) {
            return;
        }
        if (path.getWidth() == null) {
            path.setWidth(1);
        }
        if (path.getHeight() == null) {
            path.setHeight(1);
        }
    }
    private void preProcess(TableGroup tableGroup) {
        for (Type type : tableGroup.getTypes()) {
            preProcess((XlsRegionImpl) type.getRegion());

            for (Field field : type.getFields()) {
                preProcess((XlsRegionImpl) field.getRegion());
            }
        }

        for (Table table : tableGroup.getTables()) {
            TableImpl t = (TableImpl) table;
            preProcess(t.getRegion());

            for (Condition condition : t.getHorizontalConditions()) {
                for (ConditionExpression expression : condition.getExpressions()) {
                    preProcess((XlsRegionImpl) expression.getRegion());
                }
            }
            for (Condition condition : t.getVerticalConditions()) {
                for (ConditionExpression expression : condition.getExpressions()) {
                    preProcess((XlsRegionImpl) expression.getRegion());
                }
            }

            for (List<ReturnValue> row : t.getReturnValues()) {
                for (ReturnValue returnValue : row) {
                    preProcess((XlsRegionImpl) returnValue.getRegion());
                }
            }
        }

        for (Function function : tableGroup.getFunctions()) {
            preProcess((XlsRegionImpl) function.getRegion());
        }
    }

    private void preProcess(XlsRegionImpl path) {
        if (path == null) {
            return;
        }
        if (path.getWidth() == 1) {
            path.setWidth(null);
        }
        if (path.getHeight() == 1) {
            path.setHeight(null);
        }
    }
}
