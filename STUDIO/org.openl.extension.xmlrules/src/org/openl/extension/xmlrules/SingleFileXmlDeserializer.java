package org.openl.extension.xmlrules;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.reflection.PureJavaReflectionProvider;
import com.thoughtworks.xstream.io.xml.DomDriver;
import org.openl.extension.Deserializer;
import org.openl.extension.xmlrules.model.*;
import org.openl.extension.xmlrules.model.lazy.LazyWorkbook;
import org.openl.extension.xmlrules.model.single.*;

public class SingleFileXmlDeserializer implements Deserializer<ExtensionModule> {
    private static final String MODULE_TAG = "module";
    private static final String TABLE_GROUP_TAG = "sheet";
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

    public SingleFileXmlDeserializer() {
        xstream = new XStream(new PureJavaReflectionProvider(), new DomDriver());
        xstream.aliasType(MODULE_TAG, ExtensionModuleImpl.class);
        xstream.aliasType(TABLE_GROUP_TAG, SheetImpl.class);
        xstream.aliasField(XLS_FILE_NAME_TAG, ExtensionModuleImpl.class, "xlsFileName");

        xstream.aliasType(TYPE_TAG, TypeImpl.class);
        xstream.aliasType(DATA_INSTANCE_TAG, DataInstanceImpl.class);
        xstream.aliasType(FIELD_TAG, FieldImpl.class);
        xstream.aliasType(TABLE_TAG, TableImpl.class);
        xstream.aliasType(FUNCTION_TAG, FunctionImpl.class);
        xstream.aliasType(CONDITION_TAG, ConditionImpl.class);
        xstream.aliasType(CONDITION_EXPRESSION, ExpressionImpl.class);
        xstream.aliasType(FUNCTION_EXPRESSION, FunctionExpressionImpl.class);
        xstream.aliasType(RETURN_TAG, ReturnValueImpl.class);
        xstream.aliasType(XLS_REGION_TAG, XlsRegionImpl.class);
        xstream.aliasType(PARAMETER_TAG, ParameterImpl.class);

        xstream.aliasType(SEGMENT_TAG, SegmentImpl.class);
    }

    @Override
    public ExtensionModule deserialize(InputStream source) {
        ExtensionModuleImpl extensionModule = (ExtensionModuleImpl) xstream.fromXML(source);
        for (LazyWorkbook workbook : extensionModule.getWorkbooks()) {
            for (Sheet group : workbook.getSheets()) {
                postProcess(group);
            }
        }
        return extensionModule;
    }

    private void postProcess(Sheet sheet) {
        if (sheet.getTables() != null) {
            for (Table table : sheet.getTables()) {
                TableImpl t = (TableImpl) table;

                List<ConditionImpl> verticalConditions = t.getVerticalConditions();
                if (verticalConditions == null) {
                    t.setVerticalConditions(Collections.<ConditionImpl>emptyList());
                }
                List<ConditionImpl> horizontalConditions = table.getHorizontalConditions();
                if (horizontalConditions == null) {
                    t.setHorizontalConditions(Collections.<ConditionImpl>emptyList());
                }
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

}
