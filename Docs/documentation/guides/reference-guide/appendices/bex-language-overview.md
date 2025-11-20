# BEX Language Overview

## Introduction to BEX

BEX language provides a flexible combination of grammar and semantics by extending the existing Java grammar and semantics presented in the `org.openl.j` configuration using new grammar and semantic concepts. It enables users to write expressions similar to natural human language.

BEX does not require any special mapping; the existing Java business object model automatically becomes the basis for open business vocabulary used by BEX. For example, the **policy.effectiveDate** Java expression is equivalent to the **Effective Date of the Policy** BEX expression.

If the Java model correctly reflects business vocabulary, no further action is required. Otherwise, custom type-safe mapping or renaming can be applied.

## Keywords

The following table represents BEX keyword equivalents to Java expressions:

| Java expression | BEX equivalents                      |
|-----------------|--------------------------------------|
| `==`            | `equals to` `is same as`                |
| `!=`            | `does not equal to` `is different from` |
| `a.b`           | `b of the a`                         |
| `<`             | `is less than`                       |
| `>`             | `is more than`                       |
| `<=`            | `is less or equal` `is in`           |
| `!>`            | `is no more than`                    |
| `>=`            | `is more or equal`                   |
| `!<`            | `is no less than`                    |

Because of these keywords, name clashes with business vocabulary can occur. The easiest way to avoid clashes is to use upper case notation when referring to model attributes because BEX grammar is case sensitive and all keywords are in lower case.

For example, assume there is an attribute called `isLessThanCoverageLimit`. If it is referred to as **is less than coverage limit**, a name clash with keywords **is less than** occurs. The workaround is to refer to the attribute as **Is Less Than Coverage Limit**.

## Simplifying Expressions

Unfortunately, the more complex an expression is, the less comprehensible the natural language expression becomes in BEX. For this purpose, BEX provides the following methods for simplifying expressions:

-   [Notation of Explanatory Variables](#notation-of-explanatory-variables)
-   [Uniqueness of Scope](#uniqueness-of-scope)

### Notation of Explanatory Variables

BEX supports a notation where an expression is written using simple variables followed by the attributes they represent. For example, assume that the following expression is used in Java:

`(Agreed Value of the vehicle - Market Value of the vehicle) / Market Value of the vehicle is more than Limit Defined By User`

The expression is hard to read. However, it becomes much simpler if written according to the notion of explanatory variables as follows:

```
(A - M) / M > X, where
  A - Agreed Value of the vehicle,
  M - Market Value of the vehicle,
  X - Limit Defined By User
```

This syntax resembles the one used in scientific publications and is much easier to read for complex expressions. It provides a good mix of mathematical clarity and business readability.

### Uniqueness of Scope

BEX provides another way for simplifying expressions using the concept of unique scope. For example, if there is only one policy in the scope of expression, a user can write **effective date** instead of **effective date of the policy**. BEX automatically determines uniqueness of the attribute and either produces a correct path or emits an error message in case of ambiguous statement. The level of the resolution can be modified programmatically and by default equals 1.
