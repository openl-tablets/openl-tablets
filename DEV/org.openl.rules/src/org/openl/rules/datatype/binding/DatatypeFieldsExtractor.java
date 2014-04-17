package org.openl.rules.datatype.binding;

import org.apache.commons.lang.StringUtils;
import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.exception.OpenLCompilationException;
import org.openl.rules.datatype.gen.FieldDescription;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.rules.table.properties.PropertiesHelper;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.syntax.impl.Tokenizer;

import java.util.*;

/**
 * Almost everything is copied from org.openl.rules.datatype.binding.DatatypeTableBoundNode
 * FIXME: refactor to avoid duplicates
 */
public class DatatypeFieldsExtractor {

    private Set<String> POCJavaTypes = new HashSet<String>() {
        {
            add("String");
            add("Double");
            add("Date");
            add("Boolean");

        }
    };

    public Set<String> extract(ILogicalTable datatypeTable, OpenL openl, IBindingContext cxt)
        throws OpenLCompilationException {
        /**
         * FiXME: commented, consider all the datatype tables are vertical
         */
//        ILogicalTable dataTable = DatatypeHelper.getNormalizedDataPartTable(datatypeTable, openl, cxt);

        ILogicalTable dataPart = null;
        if (PropertiesHelper.getPropertiesTableSection(datatypeTable) != null) {
            dataPart = datatypeTable.getRows(2);
        } else {
            dataPart = datatypeTable.getRows(1);
        }
        ILogicalTable dataTable = dataPart;
        // end

        int tableHeight = 0;

        if (dataTable != null) {
            tableHeight = dataTable.getHeight();
        }

        Set<String> datatypes = new LinkedHashSet<String>();

        for (int i = 0; i < tableHeight; i++) {
            ILogicalTable row = dataTable.getRow(i);
            processSimple(row, cxt, datatypes);
        }
        return datatypes;
    }

    private void processSimple(ILogicalTable row, IBindingContext cxt, Set<String> datatypes)
            throws OpenLCompilationException {
        GridCellSourceCodeModule rowSrc = new GridCellSourceCodeModule(row.getSource(), cxt);
        if (canProcessRow(rowSrc)) {
            String typeName = getType(row, cxt);
            if (StringUtils.isNotBlank(typeName)) {
                datatypes.add(typeName);
            }
        }
    }

    private boolean canProcessRow(GridCellSourceCodeModule rowSrc) {
        String srcCode = rowSrc.getCode().trim();

        if (srcCode.length() == 0 || DatatypeHelper.isCommented(srcCode)) {
            return false;
        }
        return true;
    }

    private String getName(ILogicalTable row, IBindingContext cxt) throws OpenLCompilationException {
        GridCellSourceCodeModule name = getCellSource(row, cxt, 1);
        IdentifierNode[] idn = getIdentifierNode(name);
        if (idn.length != 1) {
            String errorMessage = String.format("Bad field name: %s", name.getCode());
            throw SyntaxNodeExceptionUtils.createError(errorMessage, null, null, name);
        } else {
            return idn[0].getIdentifier();
        }
    }

    private String getType(ILogicalTable row, IBindingContext cxt) throws OpenLCompilationException {
        GridCellSourceCodeModule type = getCellSource(row, cxt, 0);
        IdentifierNode[] idn = getIdentifierNode(type);
        if (idn.length != 1) {
            String errorMessage = String.format("Bad field type: %s", type.getCode());
            throw SyntaxNodeExceptionUtils.createError(errorMessage, null, null, type);
        } else {
            String typeName = idn[0].getIdentifier();
            if (POCJavaTypes.contains(typeName)) {
                return null;
            }

            return typeName;
        }
    }

    private GridCellSourceCodeModule getCellSource(ILogicalTable row, IBindingContext cxt, int columnIndex) {
        return new GridCellSourceCodeModule(row.getColumn(columnIndex).getSource(), cxt);
    }

    private IdentifierNode[] getIdentifierNode(GridCellSourceCodeModule cellSrc)
            throws OpenLCompilationException {

        IdentifierNode[] idn = Tokenizer.tokenize(cellSrc, " \r\n");

        return idn;
    }

    public static class Field {
        private String type;
        private String name;

        public Field(String type, String name) {
            this.type = type;
            this.name = name;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
