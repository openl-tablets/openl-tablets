package org.apache.poi.xssf.usermodel;

import org.apache.poi.POIXMLDocumentPart;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.openxml4j.opc.PackageRelationship;
import org.apache.xmlbeans.XmlObject;
import org.openxmlformats.schemas.drawingml.x2006.main.CTColorScheme;
import org.openxmlformats.schemas.drawingml.x2006.main.ThemeDocument;
import org.openxmlformats.schemas.drawingml.x2006.main.CTColor;

/**
 * Class that represents theme of XLSX document. The theme includes specific
 * colors and fonts.
 * 
 * @author Petr Udalau(Petr.Udalau at exigenservices.com) - theme colors
 */
public class XSSFTheme extends POIXMLDocumentPart {
    private ThemeDocument theme;

    public XSSFTheme(PackagePart part, PackageRelationship rel) throws Exception {
        super(part, rel);
        theme = ThemeDocument.Factory.parse(part.getInputStream());
    }

    public XSSFTheme(ThemeDocument theme) {
        this.theme = theme;
    }

    public XSSFColor getThemeColor(int idx) {
        CTColorScheme colorScheme = theme.getTheme().getThemeElements().getClrScheme();
        CTColor ctColor = null;
        int cnt = 0;
        for (XmlObject obj : colorScheme.selectPath("./*")) {
            if (obj instanceof org.openxmlformats.schemas.drawingml.x2006.main.CTColor) {
                if (cnt == idx) {
                    ctColor = (org.openxmlformats.schemas.drawingml.x2006.main.CTColor) obj;
                    return new XSSFColor(ctColor.getSrgbClr().getVal());
                }
                cnt++;
            }
        }
        return null;
    }
}
