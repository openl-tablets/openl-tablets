package org.openl.rules.excel.builder.template.row;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.CellStyle;

@RequiredArgsConstructor
public class NameValueRowStyleImpl implements NameValueRowStyle {

    @Getter
    private final CellStyle nameStyle;
    @Getter
    private final CellStyle valueStyle;
}
