package org.openl.extension.xmlrules;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import org.openl.extension.Serializer;
import org.openl.extension.xmlrules.model.*;
import org.openl.extension.xmlrules.model.single.*;

public class SingleFileXmlSerializer implements Serializer<Project> {
    private static final String LIVE_EXCEL_PROJECT_TAG = "project";
    private static final String XLS_FILE_NAME_TAG = "xls-file";
    private static final String LIVE_EXCEL_TYPE_TAG = "type";
    private static final String LIVE_EXCEL_DATA_INSTANCE_TAG = "dataInstance";
    private static final String LIVE_EXCEL_FIELD_TAG = "field";
    private static final String LIVE_EXCEL_TABLE_TAG = "table";
    private static final String LIVE_EXCEL_CONDITION_TAG = "condition";
    private static final String LIVE_EXCEL_RETURN_TAG = "return";
    private static final String XLS_REGION_TAG = "region";
    public static final String CONDITION_EXPRESSION = "conditionExpression";

    private final XStream xstream;

    public SingleFileXmlSerializer() {
        xstream = new XStream(new DomDriver());
        xstream.aliasType(LIVE_EXCEL_PROJECT_TAG, ProjectImpl.class);
        xstream.aliasField(XLS_FILE_NAME_TAG, ProjectImpl.class, "xlsFileName");

        xstream.aliasType(LIVE_EXCEL_TYPE_TAG, TypeImpl.class);
        xstream.aliasType(LIVE_EXCEL_DATA_INSTANCE_TAG, DataInstanceImpl.class);
        xstream.aliasType(LIVE_EXCEL_FIELD_TAG, FieldImpl.class);
        xstream.aliasType(LIVE_EXCEL_TABLE_TAG, TableImpl.class);
        xstream.aliasType(LIVE_EXCEL_CONDITION_TAG, ConditionImpl.class);
        xstream.aliasType(CONDITION_EXPRESSION, ConditionExpressionImpl.class);
        xstream.aliasType(LIVE_EXCEL_RETURN_TAG, ReturnValueImpl.class);
        xstream.aliasType(XLS_REGION_TAG, XlsRegionImpl.class);
    }

    @Override
    public String serialize(Project project) {
        preProcess((ProjectImpl) project);
        return xstream.toXML(project);
    }

    @Override
    public Project deserialize(InputStream source) {
        Project project = (Project) xstream.fromXML(source);
        postProcess((ProjectImpl) project);
        return project;
    }

    private void postProcess(ProjectImpl project) {
        for (Type type : project.getTypes()) {
            postProcess((XlsRegionImpl) type.getRegion());

            for (Field field : type.getFields()) {
                postProcess((XlsRegionImpl) field.getRegion());
            }
        }

        for (Table table : project.getTables()) {
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
    private void preProcess(ProjectImpl project) {
        for (Type type : project.getTypes()) {
            preProcess((XlsRegionImpl) type.getRegion());

            for (Field field : type.getFields()) {
                preProcess((XlsRegionImpl) field.getRegion());
            }
        }

        for (Table table : project.getTables()) {
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
