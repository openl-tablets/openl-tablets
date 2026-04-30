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

### Multiple Configuration Files

OpenL Rule Services scans the classpath for every file matching the `openapi-configuration*.json` pattern and merges them on top of the auto-generated schema. This makes it possible to split overrides across modules or layer environment-specific tweaks on top of a shared base.

Files are applied in a deterministic order:

- `openapi-configuration.json` is always applied first.
- Suffixed variants such as `openapi-configuration-1.json` or `openapi-configuration-alpha.json` follow in natural filename order, so `openapi-configuration-alpha.json` is applied before `openapi-configuration-zeta.json`.
- Each subsequent file overrides matching fields from the previous ones; non-overlapping fields are merged.

> [Note:] Resources are resolved against the deployed service classpath, which means an extension JAR can ship its own `openapi-configuration-*.json` and it will be picked up automatically alongside the base file.

### Spring Property Placeholders

Each `openapi-configuration*.json` file may contain Spring-style property placeholders that are resolved against the application environment before JSON parsing:

- `${property.name}` — replaced with the value of `property.name`. Deployment fails if the property is not defined.
- `${property.name:default-value}` — replaced with the value of `property.name`, or with `default-value` if the property is not defined.

Example:

```json
{
  "openAPI": {
    "info": {
      "title": "${service.title:OpenL Rule Services}",
      "version": "${service.version}"
    }
  }
}
```

Properties come from the standard OpenL Rule Services configuration sources (`application.properties`, JVM `-D` system properties, environment variables, and so on).
