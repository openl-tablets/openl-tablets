package org.openl.rules;

public enum RulesCommons {
    
    COMMENT_SYMBOLS("//");
    
    private String description;
    
    private RulesCommons(String description) {
        this.description = description;
    }
    
    @Override
    public String toString() {
        return description;
    }

}
