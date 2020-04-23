package org.openl.rules.lang.xls.prebind;

import java.util.Set;

import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.IMemberBoundNode;
import org.openl.conf.IUserContext;
import org.openl.dependency.CompiledDependency;
import org.openl.rules.binding.RulesModuleBindingContext;
import org.openl.rules.calc.SpreadsheetBoundNode;
import org.openl.rules.constants.ConstantsTableBoundNode;
import org.openl.rules.data.IDataBase;
import org.openl.rules.datatype.binding.AliasDatatypeBoundNode;
import org.openl.rules.datatype.binding.DatatypeTableBoundNode;
import org.openl.rules.dt.ADtColumnsDefinitionTableBoundNode;
import org.openl.rules.lang.xls.XlsBinder;
import org.openl.rules.lang.xls.XlsHelper;
import org.openl.rules.lang.xls.binding.XlsMetaInfo;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.XlsModuleSyntaxNode;
import org.openl.rules.property.PropertyTableBoundNode;
import org.openl.syntax.exception.CompositeSyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;

/**
 * Serves to prebind code. Prebind means to process all datatypes, properties and headers for another table types.
 *
 * @author pudalau
 */
public class XlsPreBinder extends XlsBinder {
    private final IPrebindHandler prebindHandler;

    public XlsPreBinder(IUserContext userContext, IPrebindHandler prebindHandler) {
        super(userContext);
        this.prebindHandler = prebindHandler;
    }

    @Override
    protected void finalizeBind(IMemberBoundNode memberBoundNode,
            TableSyntaxNode tableSyntaxNode,
            RulesModuleBindingContext rulesModuleBindingContext) {
        if (memberBoundNode instanceof DatatypeTableBoundNode || memberBoundNode instanceof AliasDatatypeBoundNode || memberBoundNode instanceof PropertyTableBoundNode || memberBoundNode instanceof ConstantsTableBoundNode || memberBoundNode instanceof ADtColumnsDefinitionTableBoundNode || memberBoundNode instanceof SpreadsheetBoundNode) {
            try {
                memberBoundNode.finalizeBind(rulesModuleBindingContext);
            } catch (SyntaxNodeException error) {
                processError(error, tableSyntaxNode, rulesModuleBindingContext);
            } catch (CompositeSyntaxNodeException ex) {
                for (SyntaxNodeException error : ex.getErrors()) {
                    processError(error, tableSyntaxNode, rulesModuleBindingContext);
                }
            } catch (Exception | LinkageError t) {
                SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(t, tableSyntaxNode);
                processError(error, tableSyntaxNode, rulesModuleBindingContext);
            }
        }
    }

    @Override
    protected XlsModuleOpenClass createModuleOpenClass(XlsModuleSyntaxNode moduleNode,
            OpenL openl,
            IDataBase dbase,
            Set<CompiledDependency> moduleDependencies,
            IBindingContext bindingContext) {
        if (prebindHandler != null) {
            return new XlsLazyModuleOpenClass(XlsHelper.getModuleName(moduleNode),
                new XlsMetaInfo(moduleNode),
                openl,
                dbase,
                moduleDependencies,
                Thread.currentThread().getContextClassLoader(),
                bindingContext,
                prebindHandler);
        } else {
            return new XlsModuleOpenClass(XlsHelper.getModuleName(moduleNode),
                new XlsMetaInfo(moduleNode),
                openl,
                dbase,
                moduleDependencies,
                Thread.currentThread().getContextClassLoader(),
                bindingContext);
        }
    }
}
