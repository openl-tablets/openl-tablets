package org.openl.rules.calc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.exception.OpenLRuntimeException;
import org.openl.rules.calc.trace.SpreadsheetTraceObject;
import org.openl.rules.table.DefaultInvokerWithTrace;
import org.openl.types.IDynamicObject;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.trace.DefaultTracePrinter;
import org.openl.vm.trace.TracePrinter;
import org.openl.vm.trace.Tracer;

/**
 * Invoker for {@link Spreadsheet}.
 * 
 * @author DLiauchuk
 *
 */
public class SpreadsheetInvoker extends DefaultInvokerWithTrace {

    private final Log LOG = LogFactory.getLog(SpreadsheetInvoker.class);

    private Spreadsheet spreadsheet;

    public SpreadsheetInvoker(Spreadsheet spreadsheet, Object target, Object[] params, IRuntimeEnv env) {
        super(target, params, env);
        this.spreadsheet = spreadsheet;
    }

    public boolean canInvoke() {        
        return spreadsheet.getResultBuilder() != null;
    }

    public SpreadsheetTraceObject createTraceObject() {       
        return new SpreadsheetTraceObject(spreadsheet, getParams());
    }    

    public OpenLRuntimeException getError() {        
        return new OpenLRuntimeException(spreadsheet.getSyntaxNode().getErrors()[0]);
    }

    public Object invokeSimple() {        
      SpreadsheetResultCalculator res = new SpreadsheetResultCalculator(spreadsheet, (IDynamicObject) getTarget(), getParams(), getEnv());
      return spreadsheet.getResultBuilder().makeResult(res);
    }

    public Object invokeTraced() {
        Tracer tracer = Tracer.getTracer();

        Object result = null;

        SpreadsheetTraceObject traceObject = createTraceObject();
        tracer.push(traceObject);

        try {
            SpreadsheetResultCalculator res = new SpreadsheetResultCalculator(spreadsheet, (IDynamicObject) getTarget(), getParams(),
                    getEnv(), traceObject);

            result = spreadsheet.getResultBuilder().makeResult(res);
            traceObject.setResult(result);
        } catch (RuntimeException e) {
           traceObject.setError(e);
           LOG.error("Error when tracing Spreadsheet table", e);
           throw e;
        } finally {
            tracer.pop();

            TracePrinter printer = new DefaultTracePrinter();
            Writer writer;
            try {
                writer = new PrintWriter(new File("D:/out.txt"));
                printer.print(tracer, writer);
                writer.close();
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } finally {
                
            }

        }
        return result;
    }
}
