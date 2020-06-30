package org.openl.rules.openapi.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import io.swagger.v3.oas.models.media.ObjectSchema;
import org.apache.commons.jxpath.JXPathContext;
import org.openl.rules.model.scaffolding.DatatypeModel;
import org.openl.rules.model.scaffolding.ProjectModel;
import org.openl.rules.model.scaffolding.SpreadsheetResultModel;
import org.openl.rules.model.scaffolding.TypeModel;
import org.openl.rules.openapi.OpenAPIModelConverter;
import org.openl.util.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;

import static org.openl.rules.openapi.impl.OpenLOpenAPIUtils.getSchemas;
import static org.openl.rules.openapi.impl.OpenLOpenAPIUtils.getSimpleName;

public class OpenAPIExperiments implements OpenAPIModelConverter {

    private final Logger logger = LoggerFactory.getLogger(OpenAPIExperiments.class);

    @Override
    public ProjectModel extractProjectModel(String pathTo) {
        ParseOptions options = OpenLOpenAPIUtils.getParseOptions();
        OpenAPI openAPI = new OpenAPIV3Parser().read(pathTo, null, options);
        JXPathContext jxPathContext = JXPathContext.newContext(openAPI);

        Map<String, Integer> allUsedRefs = OpenLOpenAPIUtils.getAllUsedRefs(openAPI, OpenLOpenAPIUtils.Path.ALL);
        Map<String, Integer> allUsedRefsInRequests = OpenLOpenAPIUtils.getAllUsedRefs(openAPI,
            OpenLOpenAPIUtils.Path.REQUESTS);

        // all the requests which were used only once needed to be extracted
        Set<String> refsToExpand = allUsedRefsInRequests.entrySet()
            .stream()
            .filter(x -> x.getValue().equals(1))
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());

        Map<String, PathItem> paths = openAPI.getPaths();
        Map<String, Set<String>> allSchemaRefResponses = new HashMap<>();
        if (paths != null) {
            for (Map.Entry<String, PathItem> p : paths.entrySet()) {
                PathItem path = p.getValue();
                Map<PathItem.HttpMethod, Operation> operationsMap = path.readOperationsMap();
                if (CollectionUtils.isNotEmpty(operationsMap)) {
                    Operation satisfyingOperation = OpenLOpenAPIUtils.getOperation(path);
                    if (satisfyingOperation != null) {
                        ApiResponses responses = satisfyingOperation.getResponses();
                        if (responses != null) {
                            ApiResponse response = OpenLOpenAPIUtils.getResponse(openAPI, responses);
                            if (response != null) {
                                if (CollectionUtils.isNotEmpty(response.getContent())) {
                                    MediaType mediaType = OpenLOpenAPIUtils.getMediaType(response.getContent());
                                    if (mediaType != null) {
                                        Schema mediaTypeSchema = mediaType.getSchema();
                                        if (mediaTypeSchema != null) {
                                            Set<String> refs = OpenLOpenAPIUtils.visitResponseSchema(openAPI,
                                                mediaTypeSchema);
                                            allSchemaRefResponses.put(p.getKey(), refs);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // paths with responses with primitive returns are possible spreadsheetResults too
        Set<String> primitiveReturnsPaths = allSchemaRefResponses.entrySet()
            .stream()
            .filter(entry -> entry.getValue().isEmpty())
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());

        // check path returns are containing in requests? no -> potential spr!!
        Map<String, Set<String>> pathWithPotentialSprResult = allSchemaRefResponses.entrySet()
            .stream()
            .filter(entry -> !entry.getValue().isEmpty() && entry.getValue()
                .stream()
                .noneMatch(allUsedRefsInRequests::containsKey))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        Set<String> sprResultRefs = pathWithPotentialSprResult.values()
            .stream()
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());

        Set<String> allTheRefsWhichAreDatatypes = allUsedRefs.keySet()
            .stream()
            .filter(x -> !sprResultRefs.contains(x) && !refsToExpand.contains(x))
            .collect(Collectors.toSet());

        List<DatatypeModel> dts = generateDatatypeTables(openAPI, allTheRefsWhichAreDatatypes);
        List<SpreadsheetResultModel> spreadsheetResultModels = generateSpreadSheetResults(openAPI,
            jxPathContext,
            pathWithPotentialSprResult,
            primitiveReturnsPaths,
            refsToExpand);

        return null;
    }

    private List<SpreadsheetResultModel> generateSpreadSheetResults(OpenAPI openAPI,
            JXPathContext jxPathContext,
            Map<String, Set<String>> pathWithPotentialSprResult,
            Set<String> pathsWithPrimitiveReturns,
            Set<String> refsToExpand) {

        List<SpreadsheetResultModel> spr = new ArrayList<>();
        Paths paths = openAPI.getPaths();
        if (paths != null) {
            for (Map.Entry<String, Set<String>> p : pathWithPotentialSprResult.entrySet()) {
                PathItem pathItem = paths.get(p.getKey());
                if (pathItem != null) {
                    SpreadsheetResultModel spreadsheetResultModel = new SpreadsheetResultModel();
                    // spreadsheetResultModel.setType();
                    // spreadsheetResultModel.setSignature();
                    // spreadsheetResultModel.setSteps();
                }
            }
        }
        return null;
    }

    private List<DatatypeModel> generateDatatypeTables(OpenAPI openAPI, Set<String> allTheRefsWhichAreDatatypes) {
        List<DatatypeModel> result = new ArrayList<>();
        for (String datatypeRef : allTheRefsWhichAreDatatypes) {
            // todo it is possible to have there a response,request etc -> use resolve
            Schema schema = getSchemas(openAPI).get(getSimpleName(datatypeRef));
            if (schema != null) {
                // get parent name

            }
        }
        return result;
    }

    private void extractSpreadSheetResults(Paths paths) {

    }

    private TypeModel extractInfo(Schema inputSchema) {
        String result;
        boolean isArray = false;
        if (inputSchema instanceof ArraySchema) {
            isArray = true;
            result = extractArrayType((ArraySchema) inputSchema);
        } else {
            if (inputSchema.getType() != null) {
                result = inputSchema.getType();
                String format = inputSchema.getFormat();
                if (format != null) {
                    switch (format) {
                        case "int32":
                            result = "integer";
                            break;
                        case "int64":
                            result = "long";
                            break;
                        case "float":
                            result = "float";
                            break;
                        case "double":
                            result = "double";
                            break;
                        default:
                            break;
                    }
                }
            } else {
                result = inputSchema.get$ref();
            }

        }

        return new TypeModel(result, isArray);
    }

    private String extractArrayType(ArraySchema inputSchema) {
        String result;
        Schema<?> items = inputSchema.getItems();
        if (items.getType() != null) {
            result = items.getType();
        } else {
            result = items.get$ref();
        }
        return result;
    }

}
