package org.openl.rules.binding;

import java.util.Arrays;
import java.util.Objects;

import org.openl.rules.table.properties.ITableProperties;

public class TableProperties {
    private ITableProperties tableProperties;

    public TableProperties(ITableProperties tableProperties) {
        this.tableProperties = Objects.requireNonNull(tableProperties, "tableProperties cannot be null");
    }

    // <<< INSERT >>>
    public java.lang.String getName() {
        return tableProperties.getName();
    }

    public java.lang.String getCategory() {
        return tableProperties.getCategory();
    }

    public java.lang.String getCreatedBy() {
        return tableProperties.getCreatedBy();
    }

    public java.util.Date getCreatedOn() {
        return tableProperties.getCreatedOn();
    }

    public java.lang.String getModifiedBy() {
        return tableProperties.getModifiedBy();
    }

    public java.util.Date getModifiedOn() {
        return tableProperties.getModifiedOn();
    }

    public java.lang.String getDescription() {
        return tableProperties.getDescription();
    }

    public java.lang.String[] getTags() {
        return tableProperties.getTags();
    }

    public java.util.Date getEffectiveDate() {
        return tableProperties.getEffectiveDate();
    }

    public java.util.Date getExpirationDate() {
        return tableProperties.getExpirationDate();
    }

    public java.util.Date getStartRequestDate() {
        return tableProperties.getStartRequestDate();
    }

    public java.util.Date getEndRequestDate() {
        return tableProperties.getEndRequestDate();
    }

    public org.openl.rules.enumeration.CaRegionsEnum[] getCaRegions() {
        return tableProperties.getCaRegions();
    }

    public org.openl.rules.enumeration.CaProvincesEnum[] getCaProvinces() {
        return tableProperties.getCaProvinces();
    }

    public org.openl.rules.enumeration.CountriesEnum[] getCountry() {
        return tableProperties.getCountry();
    }

    public org.openl.rules.enumeration.RegionsEnum[] getRegion() {
        return tableProperties.getRegion();
    }

    public org.openl.rules.enumeration.CurrenciesEnum[] getCurrency() {
        return tableProperties.getCurrency();
    }

    public org.openl.rules.enumeration.LanguagesEnum[] getLang() {
        return tableProperties.getLang();
    }

    public java.lang.String[] getLob() {
        return tableProperties.getLob();
    }

    public org.openl.rules.enumeration.OriginsEnum getOrigin() {
        return tableProperties.getOrigin();
    }

    public org.openl.rules.enumeration.UsRegionsEnum[] getUsregion() {
        return tableProperties.getUsregion();
    }

    public org.openl.rules.enumeration.UsStatesEnum[] getState() {
        return tableProperties.getState();
    }

    public java.lang.String getVersion() {
        return tableProperties.getVersion();
    }

    public java.lang.Boolean getActive() {
        return tableProperties.getActive();
    }

    public java.lang.String getId() {
        return tableProperties.getId();
    }

    public java.lang.String getBuildPhase() {
        return tableProperties.getBuildPhase();
    }

    public org.openl.rules.enumeration.ValidateDTEnum getValidateDT() {
        return tableProperties.getValidateDT();
    }

    public java.lang.Boolean getFailOnMiss() {
        return tableProperties.getFailOnMiss();
    }

    public java.lang.String getScope() {
        return tableProperties.getScope();
    }

    public java.lang.Integer getPriority() {
        return tableProperties.getPriority();
    }

    public java.lang.String getDatatypePackage() {
        return tableProperties.getDatatypePackage();
    }

    public java.lang.String getSpreadsheetResultPackage() {
        return tableProperties.getSpreadsheetResultPackage();
    }

    public java.lang.Boolean getCacheable() {
        return tableProperties.getCacheable();
    }

    public org.openl.rules.enumeration.RecalculateEnum getRecalculate() {
        return tableProperties.getRecalculate();
    }

    public org.openl.rules.enumeration.DTEmptyResultProcessingEnum getEmptyResultProcessing() {
        return tableProperties.getEmptyResultProcessing();
    }

    public java.lang.String getPrecision() {
        return tableProperties.getPrecision();
    }

    public java.lang.Boolean getTableStructureDetails() {
        return tableProperties.getTableStructureDetails();
    }

    public java.lang.Boolean getAutoType() {
        return tableProperties.getAutoType();
    }

    public java.lang.Boolean getCalculateAllCells() {
        return tableProperties.getCalculateAllCells();
    }

    public java.lang.Boolean getParallel() {
        return tableProperties.getParallel();
    }

    public java.lang.String getNature() {
        return tableProperties.getNature();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\r\n");
        if (tableProperties.getName() != null) {
            sb.append("Name").append(" = ").append(toString(tableProperties.getName())).append("\r\n");
        }
        if (tableProperties.getCategory() != null) {
            sb.append("Category").append(" = ").append(toString(tableProperties.getCategory())).append("\r\n");
        }
        if (tableProperties.getCreatedBy() != null) {
            sb.append("CreatedBy").append(" = ").append(toString(tableProperties.getCreatedBy())).append("\r\n");
        }
        if (tableProperties.getCreatedOn() != null) {
            sb.append("CreatedOn").append(" = ").append(toString(tableProperties.getCreatedOn())).append("\r\n");
        }
        if (tableProperties.getModifiedBy() != null) {
            sb.append("ModifiedBy").append(" = ").append(toString(tableProperties.getModifiedBy())).append("\r\n");
        }
        if (tableProperties.getModifiedOn() != null) {
            sb.append("ModifiedOn").append(" = ").append(toString(tableProperties.getModifiedOn())).append("\r\n");
        }
        if (tableProperties.getDescription() != null) {
            sb.append("Description").append(" = ").append(toString(tableProperties.getDescription())).append("\r\n");
        }
        if (tableProperties.getTags() != null) {
            sb.append("Tags").append(" = ").append(toString(tableProperties.getTags())).append("\r\n");
        }
        if (tableProperties.getEffectiveDate() != null) {
            sb.append("EffectiveDate")
                    .append(" = ")
                    .append(toString(tableProperties.getEffectiveDate()))
                    .append("\r\n");
        }
        if (tableProperties.getExpirationDate() != null) {
            sb.append("ExpirationDate")
                    .append(" = ")
                    .append(toString(tableProperties.getExpirationDate()))
                    .append("\r\n");
        }
        if (tableProperties.getStartRequestDate() != null) {
            sb.append("StartRequestDate")
                    .append(" = ")
                    .append(toString(tableProperties.getStartRequestDate()))
                    .append("\r\n");
        }
        if (tableProperties.getEndRequestDate() != null) {
            sb.append("EndRequestDate")
                    .append(" = ")
                    .append(toString(tableProperties.getEndRequestDate()))
                    .append("\r\n");
        }
        if (tableProperties.getCaRegions() != null) {
            sb.append("CaRegions").append(" = ").append(toString(tableProperties.getCaRegions())).append("\r\n");
        }
        if (tableProperties.getCaProvinces() != null) {
            sb.append("CaProvinces").append(" = ").append(toString(tableProperties.getCaProvinces())).append("\r\n");
        }
        if (tableProperties.getCountry() != null) {
            sb.append("Country").append(" = ").append(toString(tableProperties.getCountry())).append("\r\n");
        }
        if (tableProperties.getRegion() != null) {
            sb.append("Region").append(" = ").append(toString(tableProperties.getRegion())).append("\r\n");
        }
        if (tableProperties.getCurrency() != null) {
            sb.append("Currency").append(" = ").append(toString(tableProperties.getCurrency())).append("\r\n");
        }
        if (tableProperties.getLang() != null) {
            sb.append("Lang").append(" = ").append(toString(tableProperties.getLang())).append("\r\n");
        }
        if (tableProperties.getLob() != null) {
            sb.append("Lob").append(" = ").append(toString(tableProperties.getLob())).append("\r\n");
        }
        if (tableProperties.getOrigin() != null) {
            sb.append("Origin").append(" = ").append(toString(tableProperties.getOrigin())).append("\r\n");
        }
        if (tableProperties.getUsregion() != null) {
            sb.append("Usregion").append(" = ").append(toString(tableProperties.getUsregion())).append("\r\n");
        }
        if (tableProperties.getState() != null) {
            sb.append("State").append(" = ").append(toString(tableProperties.getState())).append("\r\n");
        }
        if (tableProperties.getVersion() != null) {
            sb.append("Version").append(" = ").append(toString(tableProperties.getVersion())).append("\r\n");
        }
        if (tableProperties.getActive() != null) {
            sb.append("Active").append(" = ").append(toString(tableProperties.getActive())).append("\r\n");
        }
        if (tableProperties.getId() != null) {
            sb.append("Id").append(" = ").append(toString(tableProperties.getId())).append("\r\n");
        }
        if (tableProperties.getBuildPhase() != null) {
            sb.append("BuildPhase").append(" = ").append(toString(tableProperties.getBuildPhase())).append("\r\n");
        }
        if (tableProperties.getValidateDT() != null) {
            sb.append("ValidateDT").append(" = ").append(toString(tableProperties.getValidateDT())).append("\r\n");
        }
        if (tableProperties.getFailOnMiss() != null) {
            sb.append("FailOnMiss").append(" = ").append(toString(tableProperties.getFailOnMiss())).append("\r\n");
        }
        if (tableProperties.getScope() != null) {
            sb.append("Scope").append(" = ").append(toString(tableProperties.getScope())).append("\r\n");
        }
        if (tableProperties.getPriority() != null) {
            sb.append("Priority").append(" = ").append(toString(tableProperties.getPriority())).append("\r\n");
        }
        if (tableProperties.getDatatypePackage() != null) {
            sb.append("DatatypePackage")
                    .append(" = ")
                    .append(toString(tableProperties.getDatatypePackage()))
                    .append("\r\n");
        }
        if (tableProperties.getSpreadsheetResultPackage() != null) {
            sb.append("SpreadsheetResultPackage")
                    .append(" = ")
                    .append(toString(tableProperties.getSpreadsheetResultPackage()))
                    .append("\r\n");
        }
        if (tableProperties.getCacheable() != null) {
            sb.append("Cacheable").append(" = ").append(toString(tableProperties.getCacheable())).append("\r\n");
        }
        if (tableProperties.getRecalculate() != null) {
            sb.append("Recalculate").append(" = ").append(toString(tableProperties.getRecalculate())).append("\r\n");
        }
        if (tableProperties.getEmptyResultProcessing() != null) {
            sb.append("EmptyResultProcessing")
                    .append(" = ")
                    .append(toString(tableProperties.getEmptyResultProcessing()))
                    .append("\r\n");
        }
        if (tableProperties.getPrecision() != null) {
            sb.append("Precision").append(" = ").append(toString(tableProperties.getPrecision())).append("\r\n");
        }
        if (tableProperties.getTableStructureDetails() != null) {
            sb.append("TableStructureDetails")
                    .append(" = ")
                    .append(toString(tableProperties.getTableStructureDetails()))
                    .append("\r\n");
        }
        if (tableProperties.getAutoType() != null) {
            sb.append("AutoType").append(" = ").append(toString(tableProperties.getAutoType())).append("\r\n");
        }
        if (tableProperties.getCalculateAllCells() != null) {
            sb.append("CalculateAllCells")
                    .append(" = ")
                    .append(toString(tableProperties.getCalculateAllCells()))
                    .append("\r\n");
        }
        if (tableProperties.getParallel() != null) {
            sb.append("Parallel").append(" = ").append(toString(tableProperties.getParallel())).append("\r\n");
        }
        if (tableProperties.getNature() != null) {
            sb.append("Nature").append(" = ").append(toString(tableProperties.getNature())).append("\r\n");
        }
        sb.append("}\r\n");
        return sb.toString();
    }
    // <<< END INSERT >>>

    private String toString(Object o) {
        if (o != null && o.getClass().isArray()) {
            return Arrays.deepToString((Object[]) o);
        }
        return o != null ? o.toString() : "null";
    }
}
