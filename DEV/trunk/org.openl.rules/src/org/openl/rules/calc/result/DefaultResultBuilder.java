package org.openl.rules.calc.result;


public class DefaultResultBuilder implements IResultBuilder {

    public Object makeResult(SpreadsheetResult result) {
        
        int height = result.getSpreadsheet().getHeight();
        int width = result.getSpreadsheet().getWidth();
        
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                result.getValue(row, col);
            }
        }
        
        return result;
    }
    
}
