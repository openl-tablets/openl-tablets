package org.openl.rules.tutorial4;

public enum DriverAgeType {

    YOUNG("Young Driver"),
    SENIOR("Senior Driver"),
    STANDARD("Standard Driver");

    private final String displayName;

    private DriverAgeType(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
    
    public static DriverAgeType parse(String displayName){
        if(displayName.equals(YOUNG.toString())){
            return YOUNG;
        }else if (displayName.equals(SENIOR.toString())){
            return SENIOR;
        }else if (displayName.equals(STANDARD.toString())){
            return STANDARD;
        }else{
            return null;
        }
    }
}
