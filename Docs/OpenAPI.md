## OpenAPI schema customization

OpenL Rule Services WS produces OpenAPI schema for each deployed service automatically from the
OpenL projects based on the OpenAPI and JAX-RS annotations.
To change output of the openapi.json it needs to create `openapi-configuration.json` file, located
in the working directory or in the root of the classpath:

```json
{
  "prettyPrint": false,
  "sortOutput": true,
  "cacheTTL": -1,
  "openAPI": {
    "info": {
      "version": "2.4.7",
      "description": "Secured"
    },
    "security": [
      {
        "Basic": []
      }
    ],
    "components": {
      "securitySchemes": {
        "Basic": {
          "type": "http",
          "scheme": "basic"
        }
      }
    }
  }
}
```

The description of the `openapi-configuration.json` file can be found at the "[Swagger 2.X Integration and Configuration](https://github.com/swagger-api/swagger-core/wiki/Swagger-2.X---Integration-and-Configuration#configuration)"
page.
