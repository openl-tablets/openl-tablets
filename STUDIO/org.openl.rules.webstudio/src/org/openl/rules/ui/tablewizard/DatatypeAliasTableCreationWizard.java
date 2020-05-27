package org.openl.rules.ui.tablewizard;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;
import javax.validation.GroupSequence;
import javax.validation.Valid;

import org.hibernate.validator.constraints.NotBlank;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.rules.table.xls.builder.CreateTableException;
import org.openl.rules.table.xls.builder.DatatypeAliasTableBuilder;
import org.openl.rules.table.xls.builder.TableBuilder;
import org.openl.rules.ui.validation.StringPresentedGroup;
import org.openl.rules.ui.validation.StringValidGroup;
import org.openl.rules.ui.validation.TableNameConstraint;
import org.openl.util.StringUtils;

/**
 * @author Andrei Astrouski
 */
@GroupSequence({ DatatypeAliasTableCreationWizard.class, StringPresentedGroup.class, StringValidGroup.class })
public class DatatypeAliasTableCreationWizard extends TableCreationWizard {

    @NotBlank(message = "Cannot be empty", groups = StringPresentedGroup.class)
    @TableNameConstraint(groups = StringValidGroup.class)
    private String technicalName;

    private String aliasType;

    @Valid
    private List<AliasValue> values = new ArrayList<>();

    private final List<String> domainTypes = WizardUtils.predefinedTypes();

    public String getTechnicalName() {
        return technicalName;
    }

    public void setTechnicalName(String technicalName) {
        this.technicalName = technicalName;
    }

    public String getAliasType() {
        return aliasType;
    }

    public void setAliasType(String aliasType) {
        this.aliasType = aliasType;
    }

    public List<AliasValue> getValues() {
        return values;
    }

    public void setValues(List<AliasValue> values) {
        this.values = values;
    }

    public List<String> getDomainTypes() {
        return domainTypes;
    }

    @Override
    public String getName() {
        return "newDatatypeAliasTable";
    }

    @Override
    protected void onStart() {
        reset();

        setAliasType("String");

        addValue();
    }

    @Override
    protected void onCancel() {
        reset();
    }

    protected String buildTable(XlsSheetSourceCodeModule sourceCodeModule) throws CreateTableException {
        XlsSheetGridModel gridModel = new XlsSheetGridModel(sourceCodeModule);
        DatatypeAliasTableBuilder builder = new DatatypeAliasTableBuilder(gridModel);

        Map<String, Object> properties = buildProperties();

        int width = DatatypeAliasTableBuilder.MIN_WIDTH;
        if (!properties.isEmpty()) {
            width = TableBuilder.PROPERTIES_MIN_WIDTH;
        }
        int height = TableBuilder.HEADER_HEIGHT + properties.size() + values.size();

        builder.beginTable(width, height);

        builder.writeHeader(technicalName, aliasType);
        builder.writeProperties(properties, null);

        for (AliasValue value : values) {
            builder.writeValue(value.getValue());
        }

        String uri = gridModel.getRangeUri(builder.getTableRegion());

        builder.endTable();

        return uri;
    }

    @Override
    protected void onStepFirstVisit(int step) {
        if (step == 3) {
            initWorkbooks();
        }
    }

    public void addValue() {
        values.add(new AliasValue());
    }

    public void valueValidator(FacesContext context, UIComponent toValidate, Object value) {
        String text = (String) value;
        if (StringUtils.isBlank(text)) {
            throw new ValidatorException(new FacesMessage("Cannot be empty"));
        }

        String[] idParts = toValidate.getClientId().split(":");
        int inputNum = Integer.parseInt(idParts[idParts.length - 2]);
        values.get(inputNum).setSubmittedValue(text);

        for (int i = 0; i < inputNum; i++) {
            if (text.equals(values.get(i).getSubmittedValue())) {
                throw new ValidatorException(new FacesMessage("Value '" + text + "' already exists"));
            }
        }
    }

    public void removeValue(AliasValue value) {
        values.remove(value);
    }

    @Override
    protected void reset() {
        technicalName = null;
        values = new ArrayList<>();

        super.reset();
    }

    @Override
    protected void onFinish() throws Exception {
        XlsSheetSourceCodeModule sheetSourceModule = getDestinationSheet();
        String newTableUri = buildTable(sheetSourceModule);
        setNewTableId(newTableUri);
        getModifiedWorkbooks().add(sheetSourceModule.getWorkbookSource());
        super.onFinish();
    }

}
