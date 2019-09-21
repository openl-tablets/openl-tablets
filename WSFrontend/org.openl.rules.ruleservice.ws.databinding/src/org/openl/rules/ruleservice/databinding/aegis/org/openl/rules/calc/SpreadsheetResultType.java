package org.openl.rules.ruleservice.databinding.aegis.org.openl.rules.calc;

/*
 * #%L
 * OpenL - RuleService - RuleService - Web Services Databinding
 * %%
 * Copyright (C) 2013 OpenL Tablets
 * %%
 * See the file LICENSE.txt for copying permission.
 * #L%
 */

import java.util.Arrays;

import javax.xml.namespace.QName;

import org.apache.cxf.aegis.Context;
import org.apache.cxf.aegis.DatabindingException;
import org.apache.cxf.aegis.type.AegisType;
import org.apache.cxf.aegis.type.TypeUtil;
import org.apache.cxf.aegis.type.basic.BeanType;
import org.apache.cxf.aegis.type.basic.BeanTypeInfo;
import org.apache.cxf.aegis.type.java5.Java5TypeCreator;
import org.apache.cxf.aegis.xml.MessageReader;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.ruleservice.databinding.aegis.WrapperBeanTypeInfo;

/**
 * Custom mapping for {@link SpreadSheetResult} due to it is not usual bean all results should be registered using the
 * special methods.
 *
 * This class uses Java Generics and causes one problems that is described in {@link OpenLTypeMapping}.
 *
 * @author Marat Kamalov
 */
public class SpreadsheetResultType extends BeanType {
    public static final Class<?> TYPE_CLASS = SpreadsheetResult.class;

    public static final QName QNAME = new Java5TypeCreator().createQName(TYPE_CLASS);

    public SpreadsheetResultType() {
        super(new WrapperBeanTypeInfo(TYPE_CLASS,
            QNAME.getNamespaceURI(),
            Arrays.asList("rowTitles",
                "columTitles",
                "height",
                "width",
                "logicalTable",
                "customSpreadsheetResultOpenClass",
                "rowNamesMarkedWithAsterisk",
                "columnNamesMarkedWithAsterisk",
                "detailedPlainModel")));
        setTypeClass(TYPE_CLASS);
        setSchemaType(QNAME);
    }

    @Override
    public Object readObject(MessageReader reader, Context context) {
        BeanTypeInfo inf = getTypeInfo();

        try {
            Object[][] results = null;
            String[] columnNames = null;
            String[] rowNames = null;

            // Read child elements
            while (reader.hasMoreElementReaders()) {
                MessageReader childReader = reader.getNextElementReader();
                if (childReader.isXsiNil()) {
                    childReader.readToEnd();
                    continue;
                }
                QName qName = childReader.getName();
                AegisType defaultType = inf.getType(qName);
                AegisType type = TypeUtil
                    .getReadType(childReader.getXMLStreamReader(), context.getGlobalContext(), defaultType);
                if (type != null && qName.getLocalPart().equals("columnNames")) {
                    columnNames = (String[]) type.readObject(childReader, context);
                } else if (type != null && qName.getLocalPart().equals("rowNames")) {
                    rowNames = (String[]) type.readObject(childReader, context);
                } else if (type != null && qName.getLocalPart().equals("results")) {
                    results = (Object[][]) type.readObject(childReader, context);
                } else {
                    childReader.readToEnd();
                }
            }

            return new SpreadsheetResult(results, rowNames, columnNames);
        } catch (IllegalArgumentException e) {
            throw new DatabindingException("Illegal argument. " + e.getMessage(), e);
        }
    }
}
