package org.openl.rules.ui.tablewizard.util;

import java.text.SimpleDateFormat;
import java.util.*;

import org.richfaces.json.JSONArray;
import org.richfaces.json.JSONException;
import org.richfaces.json.JSONObject;

public class JSONHolder {
    private JSONObject table;

    public JSONHolder(String json) throws JSONException {
        this.table = new JSONObject(json);
    }

    public String getHeaderStr() {
        try {
            String tableName = this.table.getJSONObject("header").getString("name");
            JSONArray inParam = new JSONArray(table.getJSONObject("header").get("inParam").toString());
            JSONObject returnObj = table.getJSONObject("header").getJSONObject("returnType");

            StringBuilder result = new StringBuilder();
            for (int i = 0; i < inParam.length(); i++) {
                JSONObject param = (JSONObject) inParam.get(i);

                result.append((i > 0) ? ", " : "")
                    .append(param.getString("type"))
                    .append((param.getBoolean("iterable")) ? "[]" : "")
                    .append(" ")
                    .append(param.getString("name"));
            }

            return returnObj.getString(
                "type") + ((returnObj.getBoolean("iterable")) ? "[]"
                                                              : "") + " " + tableName + "(" + result.toString() + ")";
        } catch (Exception e) {
            return "";
        }
    }

    public List<List<Map<String, Object>>> getDataRows(CellStyleManager styleManager) {
        List<List<Map<String, Object>>> dataRows = new ArrayList<>();

        if (!this.table.isNull("dataRows")) {
            try {
                JSONArray dataRow = new JSONArray(table.get("dataRows").toString());

                for (int i = 0; i < dataRow.length(); i++) {
                    JSONArray rowElements = new JSONArray(dataRow.get(i).toString());

                    List<Map<String, Object>> row = new ArrayList<>();
                    for (int j = 0; j < rowElements.length(); j++) {
                        Object value;

                        JSONObject dataCell = ((JSONObject) rowElements.get(j));
                        if (dataCell.getString("valueType").equals("DATE")) {
                            String dateString = dataCell.getString("value");
                            String dateFormat = "yyyy-MM-dd'T'HH:mm:ss";

                            SimpleDateFormat formatter = new SimpleDateFormat(dateFormat);
                            formatter.setLenient(false);
                            formatter.setTimeZone(TimeZone.getDefault());
                            try {
                                value = formatter.parse(dateString);
                            } catch (Exception e) {
                                value = dateString;
                            }
                        } else {
                            value = ((JSONObject) rowElements.get(j)).getString("value");
                        }

                        Map<String, Object> cell = new HashMap<>();
                        cell.put("value", value);
                        cell.put("style",
                            styleManager.getCellStyle(((JSONObject) rowElements.get(j)).getJSONObject("style")));

                        row.add(cell);
                    }

                    dataRows.add(row);
                }
            } catch (Exception e) {
                return new ArrayList<>();
            }
        }

        return dataRows;
    }

    public Map<String, Object> getProperties() {
        Map<String, Object> properties = new LinkedHashMap<>();

        try {
            if (!table.isNull("properties")) {
                JSONArray propertiesJSON = new JSONArray(table.get("properties").toString());
                for (int i = 0; i < propertiesJSON.length(); i++) {
                    JSONObject prop = (JSONObject) propertiesJSON.get(i);
                    String type = "";
                    String value = "";

                    try {
                        if (prop.has("type")) {
                            type = prop.getString("type");
                        }

                        if (prop.has("value")) {
                            value = prop.getString("value");
                        }
                    } catch (Exception e) {
                        continue;
                    }

                    properties.put(type, value);
                }
            }
        } catch (JSONException e) {
            return new HashMap<>();
        }

        return properties;
    }

    public int getFieldsCount() {
        try {
            JSONArray inParam = new JSONArray(table.getJSONObject("header").get("inParam").toString());
            return inParam.length() + 1;
        } catch (Exception e) {
            return 0;
        }
    }

    public JSONObject getHeaderStyle() {
        try {
            // FIXME header style saving. At the moment we have a problem with CSS2Properties
            JSONArray style = new JSONArray(table.getJSONObject("header").getString("style"));

            return style.getJSONObject(0);
        } catch (JSONException e) {
            return null;
        }
    }

    public JSONObject getPropertyStyle() {
        JSONArray inParam;
        try {
            inParam = new JSONArray(table.getString("properties"));

            if (inParam.length() > 0) {
                return inParam.getJSONObject(0).getJSONObject("style");
            } else {
                return null;
            }
        } catch (JSONException e) {
            return null;
        }
    }
}
