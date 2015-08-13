package org.openl.extension.xmlrules;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.reflection.PureJavaReflectionProvider;
import com.thoughtworks.xstream.io.xml.DomDriver;
import org.apache.commons.io.IOUtils;
import org.openl.extension.Deserializer;
import org.openl.extension.xmlrules.model.*;
import org.openl.extension.xmlrules.model.single.ExtensionModuleInfo;
import org.openl.extension.xmlrules.model.lazy.LazyExtensionModule;
import org.openl.extension.xmlrules.model.single.SheetInfo;
import org.openl.extension.xmlrules.model.single.*;

public class ZipFileXmlDeserializer implements Deserializer<ExtensionModule> {
    private static final String MODULE_TAG = "module";
    private static final String TABLE_GROUP_TAG = "sheet";
    private static final String TYPE_TAG = "type";
    private static final String DATA_INSTANCE_TAG = "data-instance";
    private static final String FIELD_TAG = "field";
    private static final String TABLE_TAG = "table";
    private static final String FUNCTION_TAG = "function";
    private static final String CONDITION_TAG = "condition";
    private static final String RETURN_TAG = "return";
    private static final String PARAMETER_TAG = "parameter";
    private static final String XLS_REGION_TAG = "region";
    private static final String SEGMENT_TAG = "segment";
    public static final String EXPRESSION = "expression";
    public static final String FUNCTION_EXPRESSION = "functionExpression";
    public static final String ENTRY_POINT = "module.xml";

    private final XStream xstream;

    private final File file;

    public ZipFileXmlDeserializer(String uri) {
        File sourceFile;
        try {
            sourceFile = new File(new URI(uri));
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
        if (!sourceFile.exists()) {
            throw new IllegalArgumentException("File " + uri + " doesn't exist");
        }
        file = sourceFile;

        xstream = new XStream(new PureJavaReflectionProvider(), new DomDriver());
        xstream.aliasType(MODULE_TAG, ExtensionModuleInfo.class);
        xstream.aliasType(TABLE_GROUP_TAG, SheetInfo.class);

        xstream.aliasField("format-version", ExtensionModuleInfo.class, "formatVersion");
        xstream.aliasField("xls-file", ExtensionModuleInfo.class, "xlsFileName");
        xstream.aliasField("sheet-entries", ExtensionModuleInfo.class, "sheetEntries");
        xstream.aliasField("type-entries", SheetInfo.class, "typeEntries");
        xstream.aliasField("data-instance-entries", SheetInfo.class, "dataInstanceEntries");
        xstream.aliasField("table-entries", SheetInfo.class, "tableEntries");
        xstream.aliasField("function-entries", SheetInfo.class, "functionEntries");
        xstream.aliasField("data-instance", Reference.class, "dataInstance");

        xstream.aliasType(TYPE_TAG, TypeImpl.class);
        xstream.aliasType(DATA_INSTANCE_TAG, DataInstanceImpl.class);
        xstream.aliasType("reference", Reference.class);
        xstream.aliasType(FIELD_TAG, FieldImpl.class);
        xstream.aliasType(TABLE_TAG, TableImpl.class);
        xstream.aliasType(FUNCTION_TAG, FunctionImpl.class);
        xstream.aliasType(CONDITION_TAG, ConditionImpl.class);
        xstream.aliasType(EXPRESSION, ExpressionImpl.class);
        xstream.aliasType(FUNCTION_EXPRESSION, FunctionExpressionImpl.class);
        xstream.aliasType(RETURN_TAG, ReturnValueImpl.class);
        xstream.aliasType(XLS_REGION_TAG, XlsRegionImpl.class);
        xstream.aliasType(PARAMETER_TAG, ParameterImpl.class);
        xstream.aliasType("single-value", SingleValue.class);
        xstream.aliasType("array-value", ArrayValue.class);

        xstream.useAttributeFor(ExpressionImpl.class, "value");
        xstream.useAttributeFor(ExpressionImpl.class, "width");
        xstream.useAttributeFor(ExpressionImpl.class, "height");
        xstream.useAttributeFor(ReturnValueImpl.class, "value");
        xstream.useAttributeFor(SingleValue.class, "value");

        xstream.aliasType(SEGMENT_TAG, SegmentImpl.class);
    }

    @Override
    public ExtensionModule deserialize(InputStream source) {
        IOUtils.closeQuietly(source); // TODO remove it
        return new LazyExtensionModule(file, ENTRY_POINT);
    }

}
