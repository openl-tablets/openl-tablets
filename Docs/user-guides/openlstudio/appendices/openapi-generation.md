## Appendix B: OpenAPI Project Generation Algorithm

OpenAPI document describes all API endpoints, their parameters, request bodies, responses, and so on. Based on this document, OpenL Tablets generates the corresponding spreadsheets and data type models. The result of generation is two modules, Algorithms and Modules, with Excel files inside. These two models are always generated even if there are no rules or modules in the project.

The following topics are included:

-   [Generation Features](#generation-features)
-   [Table Generation Details](#table-generation-details)
-   [OpenAPI to OpenL Type Transformation](#openapi-to-openl-type-transformation)
-   [Generated Annotation Template Class](#generated-annotation-template-class)

### Generation Features

OpenL Tablets generates exactly one corresponding data table, or spreadsheet table, or spreadsheet result for each path described in the Open API file.

The following topics are described in this section:

-   [Parameters](#parameters)
-   [Responses](#responses)
-   [Request Body Model Decomposition](#request-body-model-decomposition)
-   [Inheritance and Polymorphism Using OneOf, AllOf, and AnyOf](#inheritance-and-polymorphism-using-oneof-allof-and-anyof)

#### Parameters

The OpenAPI specification provides multiple places where properties for the endpoint can be located. These properties can be defined as path parameters, that is, a list of parameters applicable to all operations described under this path, or described for one of the operation parameters. OpenAPI project generation algorithm merges all these parameters and uses them as input parameters for a corresponding generated spreadsheet table.

#### Responses

The OpenAPI specification allows describing multiple operations for one path, such as GET, PATCH, or DELETE. An example is as follows.

![](../../../assets/images/webstudio/03d478a49b65d8b7d31e0660f8b7e60e.png)

*Example of the path with multiple operations*

The OpenAPI format supports multiple HTTP codes, such as 200, 400, and 500, and different response media types, such as application/JSON, application/XML, and text/plain, and they all can be described for one path.

Just as for operations, only one HTTP code and media type can be used for spreadsheet generation. The algorithm selects API responses based on the HTTP codes definition as follows:

1.  If 200 is found, use it.
2.  If DEFAULT is found, use it.
3.  If no 200 or DEFAULT code status is found, the first found http code is used for table generation.

The priority of media types is as follows:

1.  Application/JSON
2.  Text/Plain
3.  If there is no such media type defined, the first found media type is used for generation process.

**Important note**: While for generation only one response code or media type can be processed, for filtration and spreadsheet results determination, all codes and media types are considered.

#### Request Body Model Decomposition

If the request body is used only once per all OpenAPI schema and it has more than one field, it is decomposed to its fields. If this schema is a child, parent, or a field of another schema, it is not expanded.

An example of the OpenAPI schema with decomposed request body is as follows.

![](../../../assets/images/webstudio/fe937d41fc2dc055ed8f2e612a6dc441.png)

*Request body schema to be decomposed*

An example of request body decomposition result is as follows.

![](../../../assets/images/webstudio/25b316f9cc588aa9d40451f21034e625.png)

*Generated spreadsheet with a decomposed schema in parameters*

In this example, the AnotherDatatype schema is decomposed to its fields.

#### Inheritance and Polymorphism Using OneOf, AllOf, and AnyOf

The following keywords are responsible for the inheritance and polymorphism in the OpenAPI schema:

| Keyword      | Description                                                                                                                |
|--------------|----------------------------------------------------------------------------------------------------------------------------|
| AllOf        | Used for model extending. The principle is the same as for nesting classes in Java.                                        |
| OneOf, AnyOf | Reserved for future use. If a schema with these features is used, the corresponding schema is replaced by the Object type. |

An example of a schema with included nesting is as follows:

![](../../../assets/images/webstudio/5de027a9121e1ec4228041c0919e75a7.png)

*Example of nesting in the OpenAPI schema*

An example of result is as follows.

![](../../../assets/images/webstudio/4802e3c75993d61a11dcfd8261e82488.png)

*Example of generated datatypes*

### Table Generation Details

The OpenAPI project generation algorithm produces tables based on the OpenAPI path details. The following table types are available:

-   spreadsheet table
-   data table
-   datatype table

The following topics are included in this section:

-   [Decision Making](#decision-making)
-   [Data Table](#data-table)
-   [Spreadsheet Results](#spreadsheet-results)
-   [Spreadsheet Tables and Datatypes](#spreadsheet-tables-and-datatypes)
-   [Step Default Values](#step-default-values)

#### Decision Making

An OpenL Tablets project can be generated from any OpenAPI file. It is also possible to upload the OpenAPI schema generated by the OpenL Tablets Rule Services and thus upload the project with the following features:

-   project with enabled RuntimeContext
    
    If any path in the OpenAPI document has an input parameter with a link to DefaultRulesRuntimeContext as \#/components/schemas/DefaultRulesRuntimeContext, the generated project will contain the corresponding setting and RuntimeContext will be enabled.
    
    ![](../../../assets/images/webstudio/3e4b2b936c9baa8d9fcf97fc72534861.png)
    
    *The enabled option for providing runtime context*
    
    The DefaultRulesRuntimeContext input parameter is extracted from input parameters for generated spreadsheets. If any path contains RuntimeContext as a parameter, the generated project will have enabled runtime context. If there is any path without runtime context, the generated spreadsheet will be marked as non-OpenL Tablets rule, not included in the included methods regexp, and available only in the generated service AnnotationTemplate class.
    
    An example of a schema with partially provided runtime context is as follows.
    
    ![](../../../assets/images/webstudio/9fa467ceff42e098546f38009bcac342.png)
    
    *Example of partially provided runtime context*
    
    Based on this schema, the examplePathWithRC path with the **POST** operation is included in the exposed methods, but pathWithoutRC/{a} path is not included.
    
-   project with allowed variations
    
    If the OpenAPI schema contains all schemas named Variation, NoVariation, VariationsPack, ArgumentReplacementVariation, ComplexVariation, DeepCloningVariation, JXPathVariation, and VariationResult, it is considered that the OpenAPI file is generated from the project with variations support enabled.
    
    ![](../../../assets/images/webstudio/dec09f25687684ebc31fd9b5a9f30294.png)
    
    *The enabled option for providing variations*
    
    All paths which contain variations will be ignored and a generated project will also have enabled variations.

#### Data Table

The path is recognized as a data table model if the following conditions are met:

-   The path starts with the **“/get”** prefix.
-   The path returns an array of potential OpenL Tablets data types or simple types, such as String or Integer.
-   No input parameters or operations are marked as “**GET**”.
-   One input parameter DefaultRulesRuntimeContext and operation are marked as “**POST**”.

#### Spreadsheet Results

The path is converted to the spreadsheet result if the schema used in response is used only in responses through all the OpenAPI schema, and there is no reference from potential datatypes to this schema.

In addition, if there is a schema in the OpenAPI document that has a reference to a potential spreadsheet result type and this schema does not participate in datatypes, the schema is marked as a spreadsheet result.

An example of the schema with the spreadsheet result that is not returned by any path is as follows.

![](../../../assets/images/webstudio/eaf154621182110513a5515426b6b6ec.png)

*An example of a schema with two spreadsheet results*

The result of generation is as follows.

![](../../../assets/images/webstudio/7e6f0cb2f4c0420039b4a37db2835a8f.png)

*Lost spreadsheet generation result*

The LostSpreadsheet is generated because it has a reference to the mySpr spreadsheet result, which is converted as a spreadsheet result call. Nevertheless, this LostSpreadsheet is not included in the Included Methods section not to break full validation of the project.

![](../../../assets/images/webstudio/f583e1d8dbfe9a8cfa1c5cb50f9172e5.png)

*Module settings*

#### Spreadsheet Tables and Datatypes

If a path response returns a primitive schema type or a schema participating in data tables or requests, the corresponding path is marked and generated as a spreadsheet, and the returning schema is generated as an OpenL Tablets data type.

#### Step Default Values

Spreadsheet tables and spreadsheet result table steps are presented by fields of the schema returned in the Response section of the OpenAPI path.

-   If a step is a primitive type, the default value for a corresponding type is set as a value of the step.
    
    Default values are retrieved from the OpenAPI schema. If a default value is not present, the following values are set:
    
    | Type    | Default value in a cell |
    |---------|-------------------------|
    | Integer | = 0                     |
    | Long    | = 0L                    |
    | Double  | = 0.0                   |
    | Float   | = 0.0f                  |
    | Boolean | = false                 |
    | String  | = “” (empty string)     |
    | Date    | = new Date()            |
    | Object  | = new Object()          |
    
-   If a step is an OpenL Tablets datatype, a new instance creation is called.
-   If a step is an OpenL spreadsheet call, it is called with default input parameters.

### OpenAPI to OpenL Type Transformation

The following table describes correlation between types described in the OpenAPI schema and types that will be generated by OpenL Tablets.

| OpenAPI type (format)   | OpenL generated type |
|-------------------------|----------------------|
| Integer (int32)         | Integer              |
| Integer (int64)         | Long                 |
| Integer(no format)      | Integer              |
| String                  | String               |
| String (date/date-time) | Date                 |
| Number(float)           | Float                |
| Number (double)         | Double               |
| Number(no format)       | Double               |
| Boolean                 | Boolean              |

**Note:** Parameters of the GET operation for the path are converted to primitive types, such as int, float, double, long, and Boolean. An example of such schema is described in the GET operation.

### Generated Annotation Template Class

Names for generated datatypes, spreadsheet tables, and data tables are retrieved from the OpenAPI schema. There is no limitation for names in the OpenAPI specification. Names for spreadsheets and data types are generated from the path of the OpenAPI schema which can include path variables, such as path/{a}/{b}/{c} or /api/v1/example. Not all characters in the path are allowed in spreadsheet names because names of the OpenL Tablets rules, data tables, and data types must follow Java naming conventions.

The OpenL OpenAPI generation functionality can transform invalid table names into correct ones but in this case, the original paths are lost, and reconciliation is done with errors. To avoid this situation, in addition to the generated modules, OpenL Tablets generates an additional template class written using a Groovy script, which makes it easy to update.

Original paths are stored in the generated service interface and OpenL Rule Services will provide the endpoint with the same URL as in the original OpenAPI structure.

The annotation template class will be applied by OpenL Tablets Rule Services due to automatically generated property in the rules-deploy.xml available at **Repository \> Project \> Rules Deploy Configuration.**

![](../../../assets/images/webstudio/a258bf4a15e81d52cd8eb48eb632d197.png)

*Example of project properties with annotation template class*

![](../../../assets/images/webstudio/cf7f9061c2f8fb1ec057bea90dbc8612.png)

*Example of the generated Groovy file location*

By default, the script is saved to `classes/org/openl/generated/services/Service.groovy` and the script name reflects the same location.

An example of the OpenAPI JSON file with the annotation template class generated script is as follows.

![](../../../assets/images/webstudio/d98afbe9c7bd71b3b766f8a9d51b380c.png)

*Path which requires script generation*

In this example, the file contains the path name “/api/save” and the path itself contains illegal characters for a spreadsheet table name.

An example of the generated spreadsheet table is as follows.

![](../../../assets/images/webstudio/a25936ba6b4914da78c70f916a1d8e3c.png)

*Example of the formatted path name*

The api/save path is transformed to the apisave spreadsheet table name.

An example of the generated script is as follows.

![](../../../assets/images/webstudio/f6b08e6ab62213a565e35a2072aa9d8a.png)

*Example of the generated Groovy script*

Original path is present in the generated service interface, and this service is presented by OpenL Tablets Rule Services. Endpoint will be available via the same path as for the original OpenAPI structure.

