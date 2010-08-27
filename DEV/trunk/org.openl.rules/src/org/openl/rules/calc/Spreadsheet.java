package org.openl.rules.calc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.binding.BindingDependencies;
import org.openl.rules.annotations.Executable;
import org.openl.rules.calc.element.SpreadsheetCell;
import org.openl.rules.calc.result.IResultBuilder;
import org.openl.rules.calc.trace.SpreadsheetTraceObject;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IDynamicObject;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.impl.AMethod;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.trace.DefaultTracePrinter;
import org.openl.vm.trace.TracePrinter;
import org.openl.vm.trace.Tracer;

@Executable
public class Spreadsheet extends AMethod implements IMemberMetaInfo {

    private final Log LOG = LogFactory.getLog(Spreadsheet.class);

    private SpreadsheetBoundNode node;

    protected IResultBuilder resultBuilder;

    private SpreadsheetCell[][] cells;
    private String[] rowNames;
    private String[] columnNames;

    private SpreadsheetOpenClass spreadsheetType;

    public Spreadsheet(IOpenMethodHeader header, SpreadsheetBoundNode boundNode) {
        super(header);

        this.node = boundNode;
    }

    public SpreadsheetBoundNode getBoundNode() {
        return node;
    }

    public SpreadsheetCell[][] getCells() {
        return cells;
    }

    public BindingDependencies getDependencies() {
        return null;
    }

    @Override
    public IMemberMetaInfo getInfo() {
        return this;
    }

    public IResultBuilder getResultBuilder() {
        return resultBuilder;
    }

    public String getSourceUrl() {
        return ((TableSyntaxNode) node.getSyntaxNode()).getUri();
    }

    public SpreadsheetOpenClass getSpreadsheetType() {
        return spreadsheetType;
    }

    public ISyntaxNode getSyntaxNode() {
        return node.getSyntaxNode();
    }

    public int getHeight() {
        return cells.length;
    }

    public void setCells(SpreadsheetCell[][] cells) {
        this.cells = cells;
    }

    public void setColumnNames(String[] colNames) {
        this.columnNames = colNames;
    }

    public void setResultBuilder(IResultBuilder resultBuilder) {
        this.resultBuilder = resultBuilder;
    }

    public void setRowNames(String[] rowNames) {
        this.rowNames = rowNames;
    }

    public void setSpreadsheetType(SpreadsheetOpenClass spreadsheetType) {
        this.spreadsheetType = spreadsheetType;
    }

    public int getWidth() {
        return cells[0].length;
    }
    
    public String[] getRowNames() {
        return rowNames;
    }

    public String[] getColumnNames() {
        return columnNames;
    }

    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        if (Tracer.isTracerOn()) {
            return invokeTraced(target, params, env);
        }

        SpreadsheetResultCalculator res = new SpreadsheetResultCalculator(this, (IDynamicObject) target, params, env);
        return resultBuilder.makeResult(res);
    }
    
    public Object invokeTraced(Object target, Object[] params, IRuntimeEnv env) {
        Tracer tracer = Tracer.getTracer();

        Object result = null;

        SpreadsheetTraceObject traceObject = new SpreadsheetTraceObject(this, params);
        tracer.push(traceObject);

        try {
            SpreadsheetResultCalculator res = new SpreadsheetResultCalculator(this, (IDynamicObject) target, params,
                    env, traceObject);

            result = resultBuilder.makeResult(res);
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
    
    public List<SpreadsheetCell> listNonEmptyCells(SpreadsheetHeaderDefinition definition) {
        
        List<SpreadsheetCell> list = new ArrayList<SpreadsheetCell>();
        
        int row = definition.getRow();
        int col = definition.getColumn();

        if (row >= 0) {
            for (int i = 0; i < getWidth(); ++i) {
                if (!cells[row][i].isEmpty()) {
                    list.add(cells[row][i]);
                }
            }
        } else {
            for (int i = 0; i < getHeight(); ++i) {
                if (!cells[i][col].isEmpty()) {
                    list.add(cells[i][col]);
                }
            }
        }

        return list;
    }
    
    @Deprecated 
    public int height()
    {
        return getHeight();
    }
    
    @Deprecated
    public int width()
    {
        return getWidth();
    }

}
