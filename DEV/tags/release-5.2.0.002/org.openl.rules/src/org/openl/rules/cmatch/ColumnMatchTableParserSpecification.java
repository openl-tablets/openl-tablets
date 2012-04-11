/**
 * 
 */
package org.openl.rules.cmatch;

/**
 * @author User
 * 
 */
@Deprecated
public class ColumnMatchTableParserSpecification {
    private String keyword;
    private String description;
    private String[] types;

    /**
     * @return the keyword
     */
    public String getKeyword() {
        return keyword;
    }

    /**
     * @param keyword
     *            the keyword to set
     */
    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description
     *            the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return the types
     */
    public String[] getTypes() {
        return types;
    }

    /**
     * @param types the types to set
     */
    public void setTypes(String[] types) {
        this.types = types;
    }

    public enum ValueNecessity {
        REQUIRED,
        OPTIONAL,
        PROHIBITED;
        
        public boolean isRequired(String value) {
            return value != null && value.equalsIgnoreCase(REQUIRED.name());
        }

        public boolean isOptional(String value) {
            return value != null && value.equalsIgnoreCase(OPTIONAL.name());
        }

        public boolean isProhibited(String value) {
            return value != null && value.equalsIgnoreCase(PROHIBITED.name());
        }
    }
}
