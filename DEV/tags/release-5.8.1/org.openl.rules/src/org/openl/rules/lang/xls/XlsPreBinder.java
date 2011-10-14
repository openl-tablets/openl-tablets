package org.openl.rules.lang.xls;

import org.openl.binding.IMemberBoundNode;
import org.openl.conf.IUserContext;
import org.openl.rules.binding.RulesModuleBindingContext;
import org.openl.rules.datatype.binding.AliasDatatypeBoundNode;
import org.openl.rules.datatype.binding.DatatypeTableBoundNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.property.PropertyTableBoundNode;
import org.openl.syntax.exception.CompositeSyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;

/**
 * Serves to prebind code. Prebind means to process all datatypes, properties
 * and headers for another table types.
 * 
 * @author pudalau
 */
public class XlsPreBinder extends XlsBinder {
    public XlsPreBinder(IUserContext userContext) {
        super(userContext);
    }

    protected void finilizeBind(IMemberBoundNode memberBoundNode, TableSyntaxNode tableSyntaxNode,
            RulesModuleBindingContext moduleContext) {
        if (memberBoundNode instanceof DatatypeTableBoundNode || memberBoundNode instanceof AliasDatatypeBoundNode
                || memberBoundNode instanceof PropertyTableBoundNode) {
            try {
                memberBoundNode.finalizeBind(moduleContext);

            } catch (SyntaxNodeException error) {
                processError(error, tableSyntaxNode, moduleContext);

            } catch (CompositeSyntaxNodeException ex) {

                for (SyntaxNodeException error : ex.getErrors()) {
                    processError(error, tableSyntaxNode, moduleContext);
                }

            } catch (Throwable t) {

                SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(t, tableSyntaxNode);
                processError(error, tableSyntaxNode, moduleContext);
            }
        }
    }
}
