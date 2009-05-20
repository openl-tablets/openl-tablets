package org.apache.poi.hssf.record.formula.function;

import java.util.List;

import org.apache.poi.hssf.record.formula.functions.FreeRefFunction;
import org.apache.poi.hssf.usermodel.HSSFCell;

public abstract class LiveExcelFunction implements FreeRefFunction{
    private List<HSSFCell> inputCells;
    private HSSFCell outputCell;
    private String name;

    public LiveExcelFunction(String name, HSSFCell outputCell, List<HSSFCell> inputCells) {
        this.inputCells = inputCells;
        this.outputCell = outputCell;
        this.name = name;
    }

    public List<HSSFCell> getInputCells() {
        return inputCells;
    }

    public HSSFCell getOutputCell() {
        return outputCell;
    }

    public String getName() {
        return name;
    }
}
