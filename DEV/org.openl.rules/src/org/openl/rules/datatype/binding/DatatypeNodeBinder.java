/*
 * Created on Oct 3, 2003 Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.datatype.binding;

import org.openl.OpenL;
import org.openl.binding.IMemberBoundNode;
import org.openl.domain.EnumDomain;
import org.openl.domain.IDomain;
import org.openl.engine.OpenLManager;
import org.openl.rules.OpenlToolAdaptor;
import org.openl.rules.binding.RuleRowHelper;
import org.openl.rules.binding.RulesModuleBindingContext;
import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.lang.xls.binding.AXlsTableBinder;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.types.DatatypeMetaInfo;
import org.openl.rules.lang.xls.types.DatatypeOpenClass;
import org.openl.rules.table.ILogicalTable;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.StringSourceCodeModule;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.IOpenClass;
import org.openl.types.impl.DomainOpenClass;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.ArrayTool;

/**
 * @author snshor
 *
 */
public class DatatypeNodeBinder extends AXlsTableBinder {

    public static final int PARENT_TYPE_INDEX = 3;
    public static final int TYPE_INDEX = 1;

    @Override
    public IMemberBoundNode preBind(TableSyntaxNode tsn,
            OpenL openl,
            RulesModuleBindingContext bindingContext,
            XlsModuleOpenClass module) throws Exception {

        ILogicalTable table = tsn.getTable();
        IOpenSourceCodeModule tableSource = tsn.getHeader().getModule();

        IdentifierNode[] parsedHeader = DatatypeHelper.tokenizeHeader(tableSource);

        String typeName = parsedHeader[TYPE_INDEX].getIdentifier();

        IOpenClass openClass = bindingContext.findType(ISyntaxConstants.THIS_NAMESPACE, typeName);
        String packageName = tsn.getTableProperties().getPropertyValueAsString("datatypePackage");
        // Additional condition that it is not loaded class from classloader
        if (openClass != null && !(openClass instanceof JavaOpenClass) && openClass.getPackageName()
            .equals(packageName)) {
            String message = "Duplicate type definition: " + typeName;
            throw SyntaxNodeExceptionUtils.createError(message, null, parsedHeader[TYPE_INDEX]);
        }

        // Put sub table without header and properties section for business view.
        //
        putSubTableForBusinessView(tsn);

        // Check the datatype table that is alias data type.
        //
        if (parsedHeader.length == 3 && parsedHeader[2] != null && parsedHeader[2].getIdentifier()
            .startsWith("<") && parsedHeader[2].getIdentifier().endsWith(">")) {

            int beginIndex = 1;
            int endIndex = parsedHeader[2].getIdentifier().length() - 1;

            // Load data part of table (part where domain values are defined).
            //
            ILogicalTable dataPart = DatatypeHelper.getNormalizedDataPartTable(table, openl, bindingContext);

            // Get type name.
            //
            String type = parsedHeader[2].getIdentifier().substring(beginIndex, endIndex).trim();

            // Create appropriate domain object.
            //
            Object[] res = {};
            if (dataPart != null) {
                IOpenSourceCodeModule arrayAliasTypeSource = new StringSourceCodeModule(type + "[]",
                    tableSource.getUri());
                IOpenClass arrayOpenClass = OpenLManager.makeType(openl, arrayAliasTypeSource, bindingContext);

                OpenlToolAdaptor openlAdaptor = new OpenlToolAdaptor(openl, bindingContext, tsn);

                Object values = RuleRowHelper.loadParam(dataPart, arrayOpenClass, "Values", "", openlAdaptor, true);

                if (values != null) {
                    res = ArrayTool.toArray(values);
                }
            }

            IDomain<?> domain = new EnumDomain<>(res);

            // Create source code module for type definition.
            // Domain values are loaded as elements of array. We are create one
            // more type for it - array with appropriate type of elements.
            //
            IOpenSourceCodeModule aliasTypeSource = new StringSourceCodeModule(type, tableSource.getUri());

            // Create appropriate OpenL class for type definition.
            //
            IOpenClass baseOpenClass = OpenLManager.makeType(openl, aliasTypeSource, bindingContext);

            // Create domain class definition which will be used by OpenL engine at runtime.
            //
            DomainOpenClass tableType = new DomainOpenClass(typeName,
                baseOpenClass,
                domain,
                new DatatypeMetaInfo(tableSource.getCode(), tsn.getUri()));

            // Add domain class definition to biding context as internal type.
            //
            bindingContext.addType(ISyntaxConstants.THIS_NAMESPACE, tableType);

            // Return bound node.
            //
            return new AliasDatatypeBoundNode(tsn, tableType, module);
        } else {
            if (parsedHeader.length != 2 && parsedHeader.length != 4 || parsedHeader.length == 4 && !parsedHeader[2]
                .getIdentifier()
                .equals("extends")) {

                String message = "Datatype table formats: [Datatype %typename%] or [Datatype %typename% extends %parentTypeName%] or [Datatype %typename% %<aliastype>%] ";
                throw SyntaxNodeExceptionUtils.createError(message, null, null, tableSource);
            }

            DatatypeOpenClass tableType = new DatatypeOpenClass(typeName, packageName);

            // set meta info with uri to the DatatypeOpenClass for indicating the source of the datatype table
            //
            tableType.setMetaInfo(new DatatypeMetaInfo(tableSource.getCode(), tsn.getUri()));

            // Add domain class definition to biding context as internal type.
            //
            bindingContext.addType(ISyntaxConstants.THIS_NAMESPACE, tableType);

            if (parsedHeader.length == 4) {
                return new DatatypeTableBoundNode(tsn,
                    tableType,
                    module,
                    table,
                    openl,
                    parsedHeader[PARENT_TYPE_INDEX]);
            } else {
                return new DatatypeTableBoundNode(tsn, tableType, module, table, openl);
            }
        }
    }

    private void putSubTableForBusinessView(TableSyntaxNode tsn) {
        tsn.getSubTables().put(IXlsTableNames.VIEW_BUSINESS, tsn.getTableBody());
    }
}
