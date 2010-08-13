/*
 * Created on Oct 3, 2003 Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.datatype.binding;

import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.IBindingContextDelegator;
import org.openl.binding.IMemberBoundNode;
import org.openl.domain.IDomain;
import org.openl.engine.OpenLManager;
import org.openl.rules.lang.xls.binding.AXlsTableBinder;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.types.DatatypeOpenClass;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.LogicalTableHelper;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.StringSourceCodeModule;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.syntax.impl.Tokenizer;
import org.openl.types.IOpenClass;
import org.openl.types.impl.DomainOpenClass;

/**
 * @author snshor
 * 
 */
public class DatatypeNodeBinder extends AXlsTableBinder {

	public static final int PARENT_TYPE_INDEX = 3;
	public static final int TYPE_INDEX = 1;

	@Override
	public IMemberBoundNode preBind(TableSyntaxNode tsn, OpenL openl, IBindingContext cxt, XlsModuleOpenClass module) throws Exception {

		ILogicalTable table = LogicalTableHelper.logicalTable(tsn.getTable());
		IOpenSourceCodeModule tableSource = new GridCellSourceCodeModule(table.getGridTable());
		IdentifierNode[] parsedHeader = Tokenizer.tokenize(tableSource, " \n\r");

		 if (parsedHeader.length < 2) {
			String message = "Datatype table format: Datatype <typename>";
			throw SyntaxNodeExceptionUtils.createError(message, null, null, tableSource);
		}

		String typeName = parsedHeader[TYPE_INDEX].getIdentifier();

		if (cxt.findType(ISyntaxConstants.THIS_NAMESPACE, typeName) != null) {
			String message = "Duplicated Type Definition: " + typeName;
			throw SyntaxNodeExceptionUtils.createError(message, null, parsedHeader[TYPE_INDEX]);
		}

		if (DatatypeHelper.isAliasDatatype(table, openl, cxt)) {

			String type = "String";

			if (parsedHeader.length > 2 && parsedHeader[2] != null) {
				int beginIndex = 1;
				int endIndex = parsedHeader[2].getIdentifier().length() - 1;

				type = parsedHeader[2].getIdentifier().substring(beginIndex, endIndex).trim();
			}

			IOpenSourceCodeModule aliasTypeSource = new StringSourceCodeModule(type, tableSource.getUri(0));
			IOpenSourceCodeModule arrayAliasTypeSource = new StringSourceCodeModule(type + "[]", tableSource.getUri(0));
			IOpenClass baseOpenClass = OpenLManager.makeType(openl, aliasTypeSource, (IBindingContextDelegator) cxt);
			IOpenClass arrayOpenClass = OpenLManager.makeType(openl, arrayAliasTypeSource, (IBindingContextDelegator) cxt);

			ILogicalTable dataPart = DatatypeHelper.getNormalizedDataPartTable(table, openl, cxt);
			IDomain<?> domain = DatatypeHelper.getTypeDomain(dataPart, arrayOpenClass, openl, cxt);

			DomainOpenClass tableType = new DomainOpenClass(typeName, baseOpenClass, domain, null);
			cxt.addType(ISyntaxConstants.THIS_NAMESPACE, tableType);
			module.addType(ISyntaxConstants.THIS_NAMESPACE, tableType);

			return new AliasDatatypeBoundNode(tsn, tableType, table, openl);
		} else {

			if (parsedHeader.length != 2
					&& parsedHeader.length != 4
					|| (parsedHeader.length == 4 && !parsedHeader[2]
							.getIdentifier().equals("extends"))) {
				
				String message = "Datatype table formats: Datatype <typename>] or [Datatype <typename> extends <parentTypeName>]";
				throw SyntaxNodeExceptionUtils.createError(message, null, null, tableSource);
			}
			
			DatatypeOpenClass tableType = new DatatypeOpenClass(module.getSchema(), typeName);
			cxt.addType(ISyntaxConstants.THIS_NAMESPACE, tableType);

			// Add new type to internal types of module.
			//
			module.addType(ISyntaxConstants.THIS_NAMESPACE, tableType);

			if (parsedHeader.length == 4) {
				return new DatatypeTableMethodBoundNode(tsn, tableType, table, openl, parsedHeader[PARENT_TYPE_INDEX].getIdentifier());
			} else {
				return new DatatypeTableMethodBoundNode(tsn, tableType, table, openl);
			}
		}
	}

}
