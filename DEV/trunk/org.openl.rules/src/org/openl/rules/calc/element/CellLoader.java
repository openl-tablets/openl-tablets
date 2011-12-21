package org.openl.rules.calc.element;

import org.apache.commons.lang.StringUtils;
import org.openl.binding.IBindingContext;
import org.openl.engine.OpenLCellExpressionsCompiler;
import org.openl.meta.IMetaHolder;
import org.openl.meta.IMetaInfo;
import org.openl.rules.convertor.IString2DataConvertor;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.SubTextSourceCodeModule;
import org.openl.syntax.exception.CompositeSyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.types.IOpenMethodHeader;

public class CellLoader {

    private IBindingContext bindingContext;
    private IOpenMethodHeader header;
    private IString2DataConvertor convertor;

    public CellLoader(IBindingContext bindingContext, IOpenMethodHeader header, IString2DataConvertor convertor) {
        this.bindingContext = bindingContext;
        this.header = header;
        this.convertor = convertor;
    }

    public Object loadSingleParam(IOpenSourceCodeModule source, IMetaInfo meta) throws SyntaxNodeException {

        String code = source.getCode();

        if (code == null || (code = code.trim()).length() == 0) {
            return null;
        }

        if (bindingContext != null) {

            if (isFormula(code)) {

                int end = 0;

                if (code.startsWith(SpreadsheetExpressionMarker.OPEN_CURLY_BRACKET.getSymbol())) {
                    end = -1;
                }

                IOpenSourceCodeModule srcCode = new SubTextSourceCodeModule(source, 1, end);
                Object method = null;
                try {
                    method = OpenLCellExpressionsCompiler.makeMethod(bindingContext.getOpenL(), srcCode, 
                        header, bindingContext);
                    return method;
                } catch (CompositeSyntaxNodeException e) {
                    // catch the error of making method and wrap it to SyntaxNodeException.
                    //
                    throw SyntaxNodeExceptionUtils.createError("Error loading cell value", e, null, source);                    
                }
            }
        }

        try {
            Object result = convertor.parse(code, null, bindingContext);

            if (result instanceof IMetaHolder) {
                ((IMetaHolder) result).setMetaInfo(meta);
            }

            return result;
        } catch (Throwable t) {
            String message = String.format("Cannot parse cell value: [%s] to the necessary type", code);
            throw SyntaxNodeExceptionUtils.createError(message, t, null, source);
        }
    }

    public static boolean isFormula(String src) {

        if (StringUtils.isBlank(src)) {
            return false;
        }
        
        if (src.startsWith(SpreadsheetExpressionMarker.OPEN_CURLY_BRACKET.getSymbol()) 
                && src.endsWith(SpreadsheetExpressionMarker.CLOSED_CURLY_BRACKET.getSymbol())) {
            return true;
        }

        if (src.startsWith(SpreadsheetExpressionMarker.EQUALS_SIGN.getSymbol()) 
                && (src.length() > 2 || src.length() == 2 && Character.isLetterOrDigit(src.charAt(1)))) {
            return true;
        }

        return false;
    }

}
