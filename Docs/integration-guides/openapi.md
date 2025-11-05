## OpenAPI Schema Customization

The OpenL Tablets Rule Services web service produces the OpenAPI schema for each deployed service automatically from the
OpenL projects based on the OpenAPI and JAX-RS annotations.
To change the output of openapi.json, in the working directory or root of the classpath, create the `openapi-configuration.json` file with the following contents:

```json
{
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

For the description of the `openapi-configuration.json` file, see "[Swagger 2.X Integration and Configuration](https://github.com/swagger-api/swagger-core/wiki/Swagger-2.X---Integration-and-Configuration#configuration)".
