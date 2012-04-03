package org.openl.engine;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.impl.MethodUsagesSearcher;
import org.openl.binding.impl.MethodUsagesSearcher.MethodUsage;
import org.openl.rules.lang.xls.types.CellMetaInfo;
import org.openl.rules.lang.xls.types.CellMetaInfo.Type;
import org.openl.rules.table.ICell;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.CompositeSourceCodeModule;
import org.openl.source.impl.SubTextSourceCodeModule;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.impl.CompositeMethod;

/**
 * Compiles OpenL expressions from the cells and sets meta info about used
 * methods.
 * 
 * @author PUdalau
 */
public class OpenLCellExpressionsCompiler {

    /**
     * Compiles a method and sets meta info to the cells.
     * 
     * @param opel OpenL engine context
     * @param source method source
     * @param compositeMethod {@link CompositeMethod} instance
     * @param bindingContext binding context
     */
    public static void compileMethod(OpenL openl, IOpenSourceCodeModule source, CompositeMethod compositeMethod,
            IBindingContext bindingContext) {

        OpenLCompileManager compileManager = new OpenLCompileManager(openl);

        compileManager.compileMethod(source, compositeMethod, bindingContext);
        if (!bindingContext.isExecutionMode()) {
            gatherMetaInfo(source, compositeMethod);
        }
    }

    /**
     * Makes a method from source using method header descriptor and sets meta
     * info to the cells.
     * 
     * @param opel OpenL engine context
     * @param source source
     * @param methodHeader method header descriptor
     * @param bindingContext binding context
     * @return {@link CompositeMethod} instance
     */
    public static CompositeMethod makeMethod(OpenL openl, IOpenSourceCodeModule source, IOpenMethodHeader methodHeader,
            IBindingContext bindingContext) {

        OpenLCodeManager codeManager = new OpenLCodeManager(openl);

        CompositeMethod method = codeManager.makeMethod(source, methodHeader, bindingContext);
        if (!bindingContext.isExecutionMode()) {
            gatherMetaInfo(source, method);
        }
        return method;
    }

    /**
     * Makes method with unknown return type from source using method name and
     * method signature. This method used to create open class that hasn't
     * information of return type at compile time. Return type can be recognized
     * at runtime time. Sets meta info to the cells.
     * 
     * @param opel OpenL engine context
     * @param source source
     * @param methodName method name
     * @param signature method signature
     * @param declaringClass open class that declare method
     * @param bindingContext binding context
     * @return {@link IOpenMethodHeader} instance
     */
    public static CompositeMethod makeMethodWithUnknownType(OpenL openl, IOpenSourceCodeModule source,
            String methodName, IMethodSignature signature, IOpenClass declaringClass, IBindingContext bindingContext) {

        OpenLCodeManager codeManager = new OpenLCodeManager(openl);

        CompositeMethod method = codeManager.makeMethodWithUnknownType(source, methodName, signature, declaringClass,
                bindingContext);
        if (!bindingContext.isExecutionMode()) {
            gatherMetaInfo(source, method);
        }
        return method;
    }

    public static void gatherMetaInfo(IOpenSourceCodeModule source, CompositeMethod method) {
        int startIndex = 0;
        if (source instanceof CompositeSourceCodeModule) {
            setMetaInfoForCompositeSource(method, (CompositeSourceCodeModule) source, startIndex);
        } else {
            IOpenSourceCodeModule src = source;
            // extract original cell source
            while (src instanceof SubTextSourceCodeModule) {
                startIndex += ((SubTextSourceCodeModule) src).getStartPosition();
                src = ((SubTextSourceCodeModule) src).getBaseModule();
            }
            if (src instanceof GridCellSourceCodeModule) {
                List<MethodUsage> methodUsages = MethodUsagesSearcher.findAllMethods(method.getMethodBodyBoundNode(),
                        source.getCode(), startIndex);
                setCellMetaInfo(method, (GridCellSourceCodeModule) src, methodUsages);
            }
        }
    }

    private static void setMetaInfoForCompositeSource(CompositeMethod method, CompositeSourceCodeModule source,
            int startIndex) {
        List<MethodUsage> methodUsages = MethodUsagesSearcher.findAllMethods(method.getMethodBodyBoundNode(),
                source.getCode(), startIndex);
        IOpenSourceCodeModule[] modules = source.getModules();
        int moduleStart = 0;
        for (int i = 0; i < modules.length; i++) {
            int moduleEnd = moduleStart + modules[i].getCode().length();
            if (modules[i] instanceof GridCellSourceCodeModule) {
                GridCellSourceCodeModule cellSource = (GridCellSourceCodeModule) modules[i];
                // find all methods used in current cell
                List<MethodUsage> currentCellMethodUsages = new ArrayList<MethodUsage>();
                for (MethodUsage usage : methodUsages) {
                    if (usage.getStart() >= moduleStart && usage.getEnd() <= moduleEnd) {
                        currentCellMethodUsages.add(new MethodUsage(usage.getStart() - moduleStart, usage.getEnd()
                                - moduleStart, usage.getMethod()));
                    }
                }
                setCellMetaInfo(method, cellSource, currentCellMethodUsages);
            }
            moduleStart = moduleEnd + 1;
        }
    }

    private static void setCellMetaInfo(CompositeMethod method, GridCellSourceCodeModule src,
            List<MethodUsage> methodUsages) {
        ICell cell = src.getCell();
        if (!CollectionUtils.isEmpty(methodUsages) && cell != null) {
            cell.setMetaInfo(new CellMetaInfo(Type.DT_CA_CODE, null, method.getType(), false, methodUsages));
        }
    }

}
