package org.openl.rules.ruleservice.storelogdata.elasticsearch;

import java.io.IOException;

import org.openl.rules.ruleservice.storelogdata.StoreLogData;
import org.openl.rules.ruleservice.storelogdata.StoreLogDataConverter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JSONResponse implements StoreLogDataConverter<Object> {

    @Override
    public Object convert(StoreLogData storeLogData) {
        if (storeLogData.getResponseMessage() != null && storeLogData.getResponseMessage().getPayload() != null) {
            String payloadAsString = storeLogData.getResponseMessage().getPayload().toString();
            JsonNode validJSON = tryMakeJson(payloadAsString);
            if (validJSON != null) {
                return validJSON;
            } else {
                return new ElasticsearchStoreObject(payloadAsString);
            }
        }
        return null;
    }

    public JsonNode tryMakeJson(String jsonInString) {
        try {
            final ObjectMapper mapper = new ObjectMapper();
            return mapper.readTree(jsonInString);
        } catch (IOException e) {
            return null;
        }
    }
}
