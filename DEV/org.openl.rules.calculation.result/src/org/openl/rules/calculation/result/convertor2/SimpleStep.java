package org.openl.rules.calculation.result.convertor2;

import javax.xml.bind.annotation.XmlRootElement;

/*
 * #%L
 * OpenL - DEV - Rules - Calculation Result
 * %%
 * Copyright (C) 2013 OpenL Tablets
 * %%
 * See the file LICENSE.txt for copying permission.
 * #L%
 */

/**
 * Spreadsheet row(step) that has formula and value as column values.
 *
 * @author DLiauchuk
 *
 */

@XmlRootElement
public class SimpleStep extends CodeStep {

    private static final long serialVersionUID = 3906469763279262117L;

    private String text;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

}
