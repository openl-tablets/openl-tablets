package org.openl.rules.webstudio.web.servlet;

import java.io.IOException;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openl.rules.table.properties.def.TablePropertyDefinition;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;
import org.openl.util.EnumUtils;
import org.openl.util.StringUtils;

public class TablePropertyValues extends HttpServlet {

    private static final long serialVersionUID = -2074749648149949900L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException,
                                                                                   IOException {
        doPost(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException,
                                                                                    IOException {
        String propName = request.getParameter("propName");

        TablePropertyDefinition propDefinition = TablePropertyDefinitionUtils.getPropertyByName(propName);
        ServletOutputStream outputStream = response.getOutputStream();

        try {
            if (propDefinition.getType() != null && propDefinition.getType().isArray()) {
                String[] values = EnumUtils.getNames(propDefinition.getType().getInstanceClass().getComponentType());
                String[] displayValues = EnumUtils
                    .getValues(propDefinition.getType().getInstanceClass().getComponentType());

                String choisesString = "\"" + StringUtils.join(values, "\", \"") + "\"";
                String displayValuesString = "\"" + StringUtils.join(displayValues, "\", \"") + "\"";

                String params = String.format(
                    "{\"type\" : \"MULTI\", \"choices\" : [%s], \"displayValues\" : [%s], \"separator\" : \",\", \"separatorEscaper\" : \"&#92;&#92;&#92;&#92;\"}",
                    choisesString,
                    displayValuesString);

                outputStream.println(params);
            } else if (propDefinition.getType().getInstanceClass().isEnum()) {
                String[] values = EnumUtils.getNames(propDefinition.getType().getInstanceClass());
                String[] displayValues = EnumUtils.getValues(propDefinition.getType().getInstanceClass());

                String choisesString = "\"" + StringUtils.join(values, "\", \"") + "\"";
                String displayValuesString = "\"" + StringUtils.join(displayValues, "\", \"") + "\"";

                String params = String.format(
                    "{\"type\" : \"SINGLE\", \"param\" : {\"choices\" : [%s], \"displayValues\" : [%s]}, \"separator\" : \",\", \"separatorEscaper\" : \"&#92;&#92;&#92;&#92;\"}",
                    choisesString,
                    displayValuesString);

                outputStream.println(params);
            } else if (Date.class.equals(propDefinition.getType().getInstanceClass())) {
                outputStream.println("{\"type\" : \"DATE\"}");
            } else if (Boolean.class.equals(propDefinition.getType().getInstanceClass())) {
                outputStream.println("{\"type\" : \"BOOLEAN\"}");
            } else {
                outputStream.println("{\"type\" : \"TEXT\"}");
            }
        } catch (Exception e) {
            outputStream.println("{\"type\" : \"TEXT\"}");
        }

        outputStream.flush();
        outputStream.close();
    }

}
