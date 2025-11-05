## OpenL Tablets Business Expression Language
The OpenL Tablets language framework has been designed from the ground up to allow flexible combination of grammar and semantics. OpenL Tablets Business Expression (BEX) language proves this statement on practice by extending existing OpenL Tablets Java grammar and semantics presented in `org.openl.j` configuration by new grammar and semantic concepts that allow users to write "natural language" expressions.

The following topics are included in this chapter:

-   [Java Business Object Model as a Basis for OpenL Tablets Business Vocabulary](#java-business-object-model-as-a-basis-for-openl-tablets-business-vocabulary)
-   [New Keywords and Avoiding Possible Naming Conflicts](#new-keywords-and-avoiding-possible-naming-conflicts)
-   [Simplifying Expressions with Explanatory Variables](#simplifying-expressions-with-explanatory-variables)
-   [Simplifying Expressions by the Using Unique in Scope Concept](#simplifying-expressions-by-using-the-unique-in-scope-concept)
-   [OpenL Tablets Programming Language Framework](#openl-tablets-programming-language-framework)

### Java Business Object Model as a Basis for OpenL Tablets Business Vocabulary
OpenL Tablets minimizes the effort required to build a business vocabulary. Using BEX does not require any special mapping, and the existing Java BOM automatically becomes the basis for OpenL Tablets business vocabulary (OBV). For example, the following expressions are equivalent:

```
driver.age 
```
and
```
Age of the Driver 
```
Another example:
```
policy.effectiveDate 
```
and
```
Effective Date of the Policy 
```

### New Keywords and Avoiding Possible Naming Conflicts
In the previous chapter, a new **of the** keyword was introduced. There are other, self-explanatory, keywords in BEX language:

-   is less than
-   is more than
-   is less or equal
-   is no more than
-   is more or equal
-   is no less than

When adding new keywords to OpenL Tablets BEX language, there is a chance of a name clash with business vocabulary. The easiest way to avoid this clash is to use upper case notation when referring to the model attributes because BEX grammar is case sensitive, and all new keywords appear in the lower case. For example, there is an attribute called `isLessThanCoverageLimit.` When referring to it as `is less than coverage limit`, there is going to be a name clash with the keyword, but if `Is Less Than Coverage Limit` is written, no clash appears. Possible direction in extending keywords is to add numeric, measurement units, measure sensitive comparisons, such as `is longer than` or `is colder than,` or use any other similar approach.

### Simplifying Expressions with Explanatory Variables
Consider a rather simple expression in Java:
```
(vehicle.agreedValue - vehicle.marketValue) / vehicle.marketValue > limitDefinedByUser 
```

In BEX language, the same expression can be rewritten in a business-friendly way:
```
(Agreed Value of the vehicle - Market Value of the vehicle) / Market Value of the vehicle is more than Limit Defined By User
```

Unfortunately, the more complex is the expression, the less comprehensible the "natural language" expression becomes. OpenL Tablets BEX offers an elegant solution for this problem:
```
(A - M) / M > X, where
  A - Agreed Value of the vehicle,
  M - Market Value of the vehicle,
  X - Limit Defined By User
```

The syntax resembles the one used in scientific publications and is easy to understand for anybody. It is believed that the syntax provides the best mix of mathematical clarity and business readability.

### Simplifying Expressions by Using the Unique in Scope Concept
Humans differ from computers, in particular, by their ability to understand the scope of a language expression. For example, when discussing an insurance policy and the effective date is mentioned, there is no need to say the fully qualifying expression **the effective date of the policy** every time, because the context of the effective date is clearly understood. On the other hand, when discussing two policies, for example, the old and the new ones, one needs to say **the effective date of the new policy**, or **the effective date of the old policy**, to differentiate between two policies.

Similarly, when humans write so-called business documents, that is, files that serve as a reference point to a rule developer, they also often use an implied context in mind. Therefore, in documentation, they often use business terms, such as **effective date**, **driver**, and **account**, with the implied scope in mind. Scope resolution is left to a so-called rules engineer, who has to do it by manually analyzing BOM and setting appropriate paths from root objects.

OpenL Tablets BEX tries to close this semantic gap or at least make it a bit narrower by using attributes unique in scope. For example, if there is only one policy in the scope, user can write **effective date** instead of **effective date of the policy**. OpenL Tablets BEX automatically determines the uniqueness of the attribute and either produces a correct path, or emits an error message in case of an ambiguous statement. The level of the resolution can be modified programmatically and by default equals to **1**.

### OpenL Tablets Programming Language Framework
Business rules consist of rules, where each rule has a condition and action. A condition is a Boolean expression, the one that returns `true` or `false`. An action can be any sequence, usually simple, of programming statements.

Consider an expression `driver.age < 25`.

From semantic perspective, the expression defines the relationship between a value defined by the **driver.age** expression and literal **25**. This can be interpreted as **age of the driver is less than 25 years** or **select drivers who are younger than 25 years old**, or any other similar phrase.

From the programming language perspective, the semantic part is irrelevant due to the following reasons:

-   A statement must be valid in the language grammar.
-   A statement must be correct from the type-checking point of view.
-   If the language is compiled, the results of compiling, such as valid binary code, or bytecode, or code in another target language, can be considered as possible results of compiling and must be produced from the statement.
-   A runtime system, interpreter, or virtual machine must be able to execute, or interpret, this statement's compiled code and produce a resulting object.

The following topics are included in this section:

-   [OpenL Tablets Grammars](#openl-tablets-grammars)
-   [Context, Variables, and Types](#context-variables-and-types)
-   [OpenL Tablets Type System](#openl-tablets-type-system)
-   [OpenL Tablets as OpenL Tablets Type Extension](#openl-tablets-as-openl-tablets-type-extension)
-   [Operators](#operators)
-   [Binary Operators Semantic Map](#binary-operators-semantic-map)
-   [Unary Operators](#unary-operators)
-   [Cast Operators](#cast-operators)
-   [Strict Equality and Relation Operators](#strict-equality-and-relation-operators)
-   [List of org.openl.j Operators](#list-of-orgopenlj-operators)
-   [List of opg.openl.j Operator Properties](#list-of-opgopenlj-operator-properties)

#### OpenL Tablets Grammars
When the OpenL Tablets parser parses an OpenL Tablets expression, it produces a syntax tree. Each tree node has a node type, a literal value, a reference to the source code for displaying errors and debugging, and also may contain child nodes. This resembles what other parsers do, with one notable exception – the OpenL Tablets Grammar is not hard-coded, it can be configured, and a different one can be used. For all practical purposes, as of today, only the following grammars implemented in OpenL Tablets are distributed:

| Grammar           | Description                                                                               |
|-------------------|-------------------------------------------------------------------------------------------|
| **org.openl.j**   | Based on the classic Java 1.3 grammar. No templates and exception handling are supported. |
| **org.openl.bex** | org.openl.j grammar with business natural language extensions.                            |

By default, **org.openl.bex** is used in the OpenL Tablets business rules product.

An experimental **org.openl.n3 grammar** is available, and **org.openl.sql** grammar is targeted to be added in the future.

The syntax tree produced by the **org.openl.j** grammar for the expression mentioned previously in this chapter is as follows:

`        <`
`       / \`
`      .   25`
`     / \`
`driver  age`

Types of nodes are as follows:
-   **op.binary.lt** for '\<'
-   **literal.integer** for '25'
-   **chain** for '.'
-   **identifier** for 'driver'
-   **identifier** for 'age'

Node type names are significant. More information on the type names is available further in this chapter.

The grammar used in `org.openl.j` is similar not only to Java but to any other language in the C/C++/Java/C\# family. This makes OpenL Tablets easily to learn and apply by the huge pool of available Java/Cxx programmers and adds to its strength. Proliferation of new languages like Ruby and Groovy, multiple proprietary languages used in different business rules engines, CEP engines and so on, introduce new semantics to the programming community and make usage of new technologies much harder.

OpenL Tablets team makes their best to stay as close to the Java syntax as possible to make sure that the "entities would not be multiplied beyond necessity".

#### Context, Variables, and Types
After the syntax tree is created, syntax nodes must be bound to their semantic definitions. At this stage, OpenL Tablets uses specific binders for each node type. The modular structure of OpenL Tablets allows definition of custom binders for each node type. Once a syntax node is bound into the bound node, it is assigned a type, thus making the process type-safe.

Most of the time, the standard Java approach is used to assign type to the variable, so it must be defined in the context of the OpenL Tablets framework. Typical examples include the following components:

-   method parameter
-   local variable
-   member of surrounding class

For OpenL Tablets, it is usually the implementation of IOpenClass called **module**.

-   external types accessed as static, which are mostly Java classes imported into OpenL Tablets

Fields and methods used in binding context do not exist in Java; OpenL Tablets allows programmatically adding custom types, fields, and methods into binding context. For different examples of how it can be done, see the source code of the OpenLBuilder classes in different packages. For example, `org.openl.j` automatically imports all classes from the `java.util` in addition to the standard `java.lang` package. Since version 5.1.1, `java.math` is imported automatically.

#### OpenL Tablets Type System
Java is a type-safe language. However, its type-safety ends when Java has to deal with types that lie outside of the Java type system, such as database tables, http requests, or XML files.

There are two approaches to deal with those external types:

| Approach              | Specifics                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
|-----------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| using API             | API approach is inherently not type-safe, it treats attribute as literal strings; therefore, even spelling errors become visible only in runtime. <br/>Unless the standard API exists, user’s program becomes dependent on the particular API.                                                                                                                                                                                                                                 |
| using code generation | Code generation requires an extra building step and is dependent on particular generator, especially the part where names and name spaces <br/>are converted into Java names and packages. Often, generators introduce dependencies with runtime libraries that affect portability of the code. <br/>Generators usually require full conversion from external data into Java objects that may incur an unnecessary performance penalty <br/>when only a few attributes must be accessed. |

The **OpenL Tablets open type** system provides a simple way of adding new types into the OpenL Tablets language. It only requires defining a class object that implements the OpenClass interface and adding it to the OpenL Tablets type system. Implementations can vary, but access to object attributes and methods has the same syntax and provides the same type-checking in all OpenL Tablets code throughout the user application.

#### OpenL Tablets as OpenL Tablets Type Extension
OpenL Tablets is built on top of the OpenL Tablets type system, thus enabling natural integration into any Java or OpenL Tablets environment. Using the OpenL Tablets methodology, **decision tables** become **methods**, and **data tables** become **fields**. Similar conversion happens to all project artifacts. As a result, any project component can be easily modularly accessed through Java or OpenL Tablets code. An OpenL Tablets project itself becomes a **class** and easy Java access to it is provided through a generated JavaWrapper class.

#### Operators
**Operators** are methods with priorities defined by grammar. OpenL Tablets has two major types of operators, unary and binary. In addition, there are operator types used in special cases. A complete list of OpenL Tablets operators used in **org.openl.j** grammar is available at [List of org.openl.j Operators](#list-of-orgopenlj-operators).

OpenL Tablets has a modular structure, so OpenL Tablets has configurable, high-level separate components like **parser** and **binder**, and each node type can have its own NodeBinder. At the same time, the single NodeBinder can be assigned to a group of operators, as in the case of the **op.binary** prefix.

**op.binary.or** **\|\|** and **op.binary.and** **&&** have separate NodeBinders to provide short-circuiting for boolean operands. For all other binary operators, OpenL Tablets uses a simple algorithm based on the operator's node type name. For example, if the node type is **op.binary.add**, the algorithm looks for the **add()** method named in the following order:

-   **Tx add(T1 p1, T2 p2)** in the **org.openl.operators** namespace in **BindingContext**
-   **public Tx T1.add(T2 p2)** in **T1**
-   **static public Tx T1.add(T1 p1, T2 p2)** in **T1**
-   **static public Tx T2.add(T1 p1, T2 p2)** in **T2**

The found method is executed in runtime. So, to override binary operator **t1 OP t2**, where **t1**, **t2** are objects of classes **T1**, **T2**, perform the following steps:

1.  Check operators and find the operator's type name.
	The last part of the type name is the name of the method to be implemented.
1.  Use one of the following options available for implementing operators:
	-   Put it into the YourCustomOperators class as a static method and register the class as a library in the org.openl.operators namespace
		For more information on how to do that, see OpenLBuilder code.
	-   Implement **public Tx name(T2 p2)** as method in T1.
	-   Implement **Tx name(T1 p1,T2 p2) as** method in T1.
	-   Implement **static public Tx name(T1 p1,T2 p2)** as method in T2.
1.  If **T1** and **T2** are different, define both **OP(T1, T2)** and **OP(T2, T1**), unless **autocast()** operator can be relied on or binary operators semantic **map. Autocast** can help skipping implementation when there is already an operator implemented for the autocasted type.

For example, when having **OP(T1, double)**, there is no need to implement **OP(T1, int)** because **int** is autocasted to double. Some performance penalty can be incurred by doing this though. For more information on binary operators semantic map, see [Binary Operators Semantic Map](#binary-operators-semantic-map).

#### Binary Operators Semantic Map
There is a convenient feature called *operator semantic map*. It makes implementing some of the operators easier by describing symmetrical and inverse properties for some operators as described in [List of opg.openl.j Operator Properties](#list-of-opgopenlj-operator-properties).

#### Unary Operators
For unary operators, the same method resolution algorithm is being applied, with difference that there is only one parameter to deal with.

#### Cast Operators
**Cast operators** in general correspond to Java guidelines and come in two types, **cast** and **autocast**. **T2 autocast (T1 from, T2 to)** methods are used to overload implicit cast operators, as from int to long, so that actually no cast operators are required in code, T2 cast(T1 from, T2 to) methods are used with explicit cast operators.

**Note:** It is important to remember that while both **cast()** and **autocast()** methods require two parameters, only T1 from parameter is actually used. The second parameter is used to avoid ambiguity in Java method resolution.

#### Strict Equality and Relation Operators
**Strict operators** are the same as their original prototypes used for strict comparison of float point values. Float point numbers are used in JVM as value with an inaccuracy. The original relation and equality operators are used with inaccuracy of float point operations. An example is as follows:

```
1.0 == 1.0000000000000002 – returns true value,
1.0 ==== 1.0000000000000002 (1.0 + ulp(1.0)) – returns false value,
```

where` `1.0000000000000002` = 1.0 + ulp(1.0).`

#### List of org.openl.j Operators
The `org.openl.j` operators in order of priority are as follows:

| Operator                | org.openl.j operator     |
|-------------------------|--------------------------|
| Assignment              |                          |
| =                       | op.assign                |
| +=                      | op.assign.add            |
| -=                      | op.assign.subtract       |
| \*=                     | op.assign.multiply       |
| /=                      | op.assign.divide         |
| %=                      | op.assign.rem            |
| &=                      | op.assign.bitand         |
| \|=                     | op.assign.bitor          |
| \^=                     | op.assign.bitxor         |
| **Conditional Ternary** |                          |
| ? :                     | op.ternary.qmark         |
| **Implication**         |                          |
| -\>                     | op.binary.impl (\*)      |
| **Boolean OR**          |                          |
| \|\| or "or"            | op.binary.or             |
| **Boolean AND**         |                          |
| && or "and"             | op.binary.and            |
| **Bitwise OR**          |                          |
| \|                      | op.binary.bitor          |
| **Bitwise XOR**         |                          |
| \^                      | op.binary.bitxor         |
| **Bitwise AND**         |                          |
| &                       | op.binary.bitand         |
| **Equality**            |                          |
| ==                      | op.binary.eq             |
| !=                      | op.binary.ne             |
| ====                    | op.binary.strict_eq (\*) |
| !===                    | op.binary.strict_ne (\*) |
| **Relational**          |                          |
| \<                      | op.binary.lt             |
| \>                      | op.binary.gt             |
| \<=                     | op.binary.le             |
| \>=                     | op.binary.ge             |
| \<==                    | op.binary.strict_lt (\*) |
| \>==                    | op.binary.strict_gt (\*) |
| \<===                   | op.binary.strict_le (\*) |
| \>===                   | op.binary.strict_ge (\*) |
| **Bitwise Shift**       |                          |
| \<\<                    | op.binary.lshift         |
| \>\>                    | op.binary.rshift         |
| \>\>\>                  | op.binary.rshiftu        |
| **Additive**            |                          |
| +                       | op.binary.add            |
| -                       | op.binary.subtract       |
| **Multiplicative**      |                          |
| \*                      | op.binary.multiply       |
| /                       | op.binary.divide         |
| %                       | op.binary.rem            |
| **Power**               |                          |
| \*\*                    | op.binary.pow (\*)       |
| **Unary**               |                          |
| +                       | op.unary.positive        |
| -                       | op.unary.negative        |
| ++x                     | op.prefix.inc            |
| --x                     | op.prefix.dec            |
| x++                     | op.suffix.inc            |
| x--                     | op.suffix.dec            |
| !                       | op.unary.not             |
| \~                      | op.unary.bitnot          |
| (cast)                  | type.cast                |
| \|x\|                   | op.unary.abs (\*)        |

**Note:** (\*) Operators do not exist in Java standard and exist only in org.openl.j. They can be used and overloaded if necessary.

#### List of opg.openl.j Operator Properties

| Operator group  | Operators                                                                                                   |
|-----------------|-------------------------------------------------------------------------------------------------------------|
| **Symmetrical** | eq(T1,T2) <=> eq(T2, T1) add(T1,T2) <=> add(T2, T1)                                                         |
| **Inverse**     | `le(T1,T2) <=> gt(T2, T1)` `lt(T1,T2) <=> ge(T2, T1)` `ge(T1,T2) <=> lt(T2, T1)` `gt(T1,T2) <=> le(T2, T1)` |

