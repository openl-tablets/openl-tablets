---
title: OpenL Tablets 5.21.3 Migration Notes
---

## Rule Services — SOAP Fault Message Structure

The SOAP fault message structure has been modified. The `<detail>` element now includes `<type>` and `<stackTrace>`
elements that provide enhanced fault descriptions.

Review any clients that parse the SOAP fault `<detail>` element and update them to handle the new structure.
