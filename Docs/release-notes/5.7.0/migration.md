---
title: OpenL Tablets 5.7.0 Migration Notes
---

## Package Relocations

Several packages and projects have been relocated or renamed. Review your import statements and update references
accordingly. Six package or project naming changes are documented in this release.

## DynamicObject Initialization

Update `DynamicObject` initialization code to reflect API changes in this release.

## Web Service Clients

Web service clients must be updated for compatibility with CXF 2.2.8, as the service configuration structure has
changed.
