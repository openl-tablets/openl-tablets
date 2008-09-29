package org.openl.rules.ui.tablewizard;

/**
 * @author Aliaksandr Antonik.
 */
public class ConditionClauseVariant {
    private String displayValue;
    private String code;
    private final long id;
    private static long idUsed; 

    public ConditionClauseVariant(String displayValue, String code) {
        this.displayValue = displayValue;
        this.code = code;
        synchronized (ConditionClauseVariant.class) {
            id =  idUsed++;
        }
    }

    public String getDisplayValue() {
        return displayValue;
    }

    public String toCode(String p1, String p2) {
        return code.replaceAll("\\{0}", p1).replaceAll("\\{1}", p2);
    }

    public long getId() {
        return id;
    }
}
