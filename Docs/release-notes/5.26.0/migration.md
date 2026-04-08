---
title: OpenL Tablets 5.26.0 Migration Notes
---

### Quick Role-Based Pointers

* **If you are a Rules Author** → pay special attention to section **3**
* **If you are a Developer** → pay special attention to sections **2, 3**
* **If you are an Administrator / Platform Owner** → pay special attention to sections **1, 2**

---

### 1. WebStudio SAML Configuration

* For 5.25 to 5.26 migration, add the "Entity ID" parameter to `webstudio.properties` before migrating. The entity ID corresponds to the Client SAML name.

  ```properties
  security.saml.entity-id=webstudio
  ```

* The default value in 5.25 was `http://localhost:8080/webstudio/saml/metadata`; in 5.26 it is `webstudio`.
* SAML metadata download link: `http://localhost:8080/webstudio/saml2/service-provider-metadata/webstudio`.
* Private certificate is now generated per WebStudio instance and stored in the properties file.
* Public key for IdP must be updated from WebStudio SAML metadata XML.
* Identity Provider configuration required:
    * Single sign-on URL: `http://localhost:8080/webstudio/login/saml2/sso/webstudio`
    * Audience URI (Entity ID): `webstudio`

---

### 2. Runtime Environment & Database

* Java 8 is no longer supported; **Java 11** is required.
* MSSQL driver upgrade requires `encrypt=false;trustServerCertificate=true` in the connection string if the server does not support encryption.
* H2 database upgrade requires migration per [H2 migration guide](https://www.h2database.com/html/migration-to-v2.html).

---

### 3. Classloader & Groovy Script Changes

Starting from 5.26.0, Groovy Scripts access all Java types from the project and dependency projects generated from Datatype tables. The classloader compilation algorithm has changed to use the same classloader per project (previously each module had its own classloader).

This may break backward compatibility if multiple projects use Datatypes with identical names without module relationships.

**Solution**: Define unique package names. Refer to the Reference Guide, Dev Properties section.
