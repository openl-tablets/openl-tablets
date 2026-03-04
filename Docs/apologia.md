# OpenL Tablets Apologia

> *I don't think anyone really captures their requirements as Decision tables, but many requirements are captured as
Excel tables.*
>
> **The BRMS Blog**

## OpenL Tablets Engine Differentiators

**OpenL Tablets** is a full-blown rule engine based on an optimized sequential algorithm which is considered to be the
best choice for business rules. Sophisticated pattern-matching algorithms like Rete III are not included in OpenL; they
are inappropriate for business and are for general purpose rules, e.g. biology simulations (so we believe).

As the simplest classification, one might be tempted to say OpenL Tablets Rules Engine is a "table processor". To some
extent it is a table processor. It takes documents (Excel, Word, or clones), extracts tables and then makes them
available to access from your application. What features set OpenL apart from the competition?

1. OpenL Tablets removes the gap between business documents (rules and policies) and software implementation
2. OpenL Tablets checks all data in project documents for syntax and type errors, providing convenient and detailed
   error reporting. Its ability to point to the problem directly within Word and Excel documents is unique among other
   similar products
3. OpenL Tablets provides calculation explanation capabilities, making it possible to actually drill down any
   calculation result while showing all the source arguments within original documents
4. OpenL provides cross-indexing and search capabilities within all project documents
5. OpenL Tablets makes this process transparent to developers. For example, in Java wrappers, Decision Tables become
   available as methods and Data Tables become accessible as data arrays through the familiar getter/setter Beans
   mechanism. All this is done automatically without any manual effort. This feature makes integration with any
   application a snap.

This article is an attempt to explain in detail why the OpenL approach is better for real-world business applications.
Here is a brief summary of this explanation:

- 90-95% of business logic is table-based or can be presented as a table
- Structured information is easier to understand and maintain
- Most of it is already maintained in Excel files
- Most of business logic is either Decision, Lookup, or Spreadsheet Tables

If you believe that the above statements are correct, then should you choose the tool that takes the best care of the
90-95% of your business logic or the one that handles better the remaining 5-10%? And are you sure that other products
actually do a good job with the remaining 5-10%? If you want to know the answer, continue reading.

## OpenL Features

OpenL Tablets is built on top of the OpenL framework for developing programming languages. It brings to OpenL Tablets
the following features:

### 1. Type Safety

We consider this one of the necessary requirements for a modern software tool. Increased complexity of software systems
implies that a user should get as much help as possible at the development stage. Compile-time type checking does its
part.

### 2. Extended Type System

Most programming languages work well within their semantic paradigm. For example, Java and Java tools provide very nice
support while you are within the Object/Class world. Once you step outside and start to deal with external entities like
database tables or web forms, you find yourself in a completely different world — you have to work through APIs that
mostly understand only strings and numbers. OpenL allows you to extend the Java type system with custom type systems
that can be accessible at compile time and therefore become an integral part of your application. Examples of such
systems could be the already mentioned database schemas, XML files (schemas), OWL/RDF types, or (as in OpenL Tablets)
Excel tables.

### 3. Syntax/Semantic Flexibility

OpenL makes it possible to have customized syntax for your programming needs. At the current stage we came to the
conclusion that the optimal syntax would be a somewhat limited Java syntax (it makes it immediately familiar to anybody
with C++ or C# background) with the following extensions: operator overloading, extended type syntax, and business user
extensions.

Having Java syntax/semantics at the core of the language greatly reduces the learning curve for developers. Having the
ability to overload operators simplifies code for formulas. An extended type system provides compile-time type checking
and allows incorporating external APIs and data structures directly within familiar Java syntax. This is important
because business logic is independent of implementing APIs.

## OpenL Tablets Advantage

OpenL Tablets treats tables in Excel and Word files as source code. It means that Excel and Word documents become source
code files, the same as `.java` files for a Java program. This approach may be unusual, but it has its own unique
advantages. In particular, it allows closing the gap between the business world and the IT world. Let's consider a
typical scenario where a Business Analyst (BA) creates design documents, including business model, business rules, and
business processes. In most cases it results in a set of Word and Excel documents. Then, IT specialists, architects, and
data modelers translate business documents according to their understanding into software artifacts. In general, there
is no formal link between the original documents and the resulting model. Therefore, the only way to keep software
consistent with the model is through a tedious, error-prone manual process. While the OpenL Tablets approach does not
solve this problem completely, it allows significantly reducing the costs of this translation in the following areas:

- Business Rules — in most information-intensive businesses (insurance, finance, banking, etc.) 95% of business rules
  are either already presented in Excel (or Word) tables or can be easily presented as such
- Lookup Tables
- Spreadsheet Tables — for Excel-like calculators
- Data Tables for reference data and test cases
- Datatype Tables — to define problem domain inside rules files
- Finite State Machine (FSM) Event Transition Tables for business process/lifecycle modeling

Our experience shows that these areas cover a significant part of development efforts, and these are the areas where
OpenL Tablets indeed outshines the competition. Let's imagine a scenario where most of your business knowledge is
presented in the form of the tables mentioned above. For our competitors, Excel tables are just an auxiliary tool to
import data; their support for the tables is minimal, usually through an "import" function in a menu. Needless to say,
that this treatment of the medium where most of your business knowledge is contained is neither sufficient nor
acceptable. OpenL Tablets provides both business users and IT specialists with a rich and comprehensive set of tools to
make maintenance of your business knowledge presented in Excel and Word documents a simple task. Business users have a
choice of working either in the familiar Excel application or through the Web interface. IT specialists can also use the
Maven Plugin that will provide them with nice error navigation display during compile or runtime, Java integration tools
that automatically generate Java wrapper classes, unit tests, debug facilities, version control, etc.

## OpenL Tablets vs. Traditional BRMS

It has been common knowledge for a while that a BRMS must have:

- Rule Engine
- Rule Language (universal and domain specific)
- IDE to capture rules and organize them into rule sets
- Rule Storage Format — i.e. files where rules are stored

Each of these "must haves" has its own implications. The industry cheerleaders will tell you only about the positive
ones, so we will concentrate only on the negative ones.

## Rete Algorithm

The article from [http://en.wikipedia.org/wiki/Rete_algorithm](http://en.wikipedia.org/wiki/Rete_algorithm) states that
the Rete Algorithm provides an efficient implementation of a pattern-matching algorithm for production (expert) systems.
It provides a significant (orders of magnitude) performance advantage over the naive implementation.

This statement is definitely true. Also, the truth is that the Rete algorithm provides a significant performance/memory
overhead over implementations that take advantage of having rules in structured form like decision or lookup tables.

In addition, the dynamic nature of the Rete algorithm (strictly speaking, it's not the algorithm itself, it is the
semantics of the production systems it is built to implement) makes it difficult to debug programs due to the fact that
many problems become visible only at run-time. You have to worry not only about business logic but also about some
obscure "rule salience" factors.

It does not mean that production systems are bad; there are areas where they can be used with great success, for example
to expand OWL/RDF axioms, etc. Unfortunately they are less suitable and less efficient when we deal with "modern
business processes". A modern business process is:

a) Streamlined to the point where no "deduction" is necessary; one can come up with a few exceptions to this statement,
but they are definitely not mainstream

b) Most decisions are table-based, because rules must cover all the "input space". Nobody creates a single rule for a
23-year-old driver; instead there is a rather large decision table that includes all driver age ranges, usually in
combination with other factors, also in range mode to cover all the possible combinations of possible input data.

c) The process itself is rather sequential; it consists of well-defined stages that follow each other in strict order.
For example, insurance application processing order is:

1. **data validation →**
2. **calculation of discount/surcharge factors for individual drivers/cars →**
3. **calculation of integral discount/surcharge factors such as multi-car discount →**
4. **calculation of the entire policy or rejection based on previous calculations**

It is possible to use a production system for this kind of problem, but is it necessary? For example, instead of
explicit sequencing, people often use so-called rule salience or priority to do the job. Needless to say that this
approach looks as transparent and fresh as line numbering in the first BASIC.

In addition to performance overhead, you will need a very qualified "knowledge engineer" a.k.a. "business rules expert"
who will be able to take care of all the problems with performance, debugging, and sequencing. But should you have these
problems in the first place?

## Rule Language

All BRMS use some rule language to express the rule logic. There is no standard for this language even though there are
attempts in this direction like RuleML (it is not so much a language, rather a rule-interchange format). So goes your
rules portability. In OpenL, your business logic presented as tables in Excel is platform-neutral. Plus you have the
following advantages over other approaches: it is easy to distribute your rules; anybody who has Excel or OpenOffice (
and who doesn't?) can read them; you can easily export them to any other system including PDF files or a database. And,
finally, if you later switch from OpenL to a competitor for some unknown reason, they all provide some ways to import
these tables into their systems.

So let us summarize: when you write rules as text, the logic is dependent on the rule language; when you use tables, the
logic is language-neutral.

There are some attempts to use "natural English language" to define rule logic; some of them are really impressive like
Haley's Authority. Others chose the path of DSL (Domain Specific Languages) that basically are the set of parameterized
language constructs that also look like natural language to the end user. But the real question is: what is better, to
have your rules in this form (a bit *pre-fabricated example*, but anyway it demonstrates the problem):

```
Rule  CF1
When
    Driver has gender "male" AND Driver has age at least 16 AND at most 20
    AND Car mileage is at least 0 miles AND at most 10000 miles AND Car use is "pleasure"
Then
    Set Factor with name "classification" for Coverage named "Bodily Injury" to 3.90
    AND Set Factor with name "classification" for Coverage named "Property Damage" to 3.90
    AND Set Factor with name "classification" for Coverage named "Medical" to 2.10
    AND Set Factor with name "classification" for Coverage named "Collision" to 4.45
    AND Set Factor with name "classification" for Coverage named "Comprehensive" to 2.95

Rule  CF2
When
    Driver has gender "male" AND Driver has age at least 21 AND at most 24
    AND Car mileage is at least 0 miles AND at most 10000 miles AND Car use is "pleasure"
Then
    Set Factor with name "classification" for Coverage named "Bodily Injury" to 2.00
    AND Set Factor with name "classification" for Coverage named "Property Damage" to 2.00
    AND Set Factor with name "classification" for Coverage named "Medical" to 1.50
    AND Set Factor with name "classification" for Coverage named "Collision" to 2.55
    AND Set Factor with name "classification" for Coverage named "Comprehensive" to 2.20

Rule  CF3
When
    Driver has gender "female" AND Driver has age at least 16 AND at most 20
    AND Car mileage is at least 0 miles AND at most 10000 miles AND Car use is "pleasure"
Then
    Set Factor with name "classification" for Coverage named "Bodily Injury" to 2.80
    AND Set Factor with name "classification" for Coverage named "Property Damage" to 2.80
    AND Set Factor with name "classification" for Coverage named "Medical" to 1.90
    AND Set Factor with name "classification" for Coverage named "Collision" to 2.90
    AND Set Factor with name "classification" for Coverage named "Comprehensive" to 1.60

.....................................
 50 or more rules of similar nature
```

Or like this:

| Gender | Age   | Marital Status | Mileage | Use      | Bodily Injury | Property Damage | Medical | Collision | Comprehensive |
|--------|-------|----------------|---------|----------|---------------|-----------------|---------|-----------|---------------|
| Male   | 16-20 | Single         | 0-10K   | Pleasure | 3.90          | 3.90            | 2.10    | 4.45      | 2.95          |
| Male   | 21-24 | Single         | 0-10K   | Pleasure | 2.00          | 2.00            | 1.50    | 2.55      | 2.20          |
| Female | 16-20 | Single         | 0-10K   | Pleasure | 2.80          | 2.80            | 1.90    | 2.90      | 2.20          |
| Female | 21-24 | Single         | 0-10K   | Pleasure | 1.65          | 1.65            | 1.50    | 2.00      | 1.40          |

*... 50 or more rows of similar nature*

For us the answer is obvious, and we rest our case.

## The Road Ahead

The following is the list of tasks the OpenL team will concentrate its efforts on in the near future. They will provide
users with even more advanced capabilities and an enhanced experience:

- Improve Web Admin Interface — **on the way (5.9.0 introduces new UI)**
- Use Tables Meta-Information to provide features like Effective/Expiration Dates — **done (extremely powerful
  versioning is available starting with 5.5.0)**
- Add Advanced Lookup Tables (multi-dimensional) — **done (since 5.3.0)**
- Add Database Connectivity module
- Enhance Rules Data Type Library
- Enhance Domain Model definition — **done (more convenient than custom Java code, since 5.7.2)**
- Add code-generation capability to OpenL — **partial (Datatypes are generated as Java bytecode since 5.7.3)**
- Add generics and convenient Smalltalk-like Collection and Iterator operations
- Add RDF/OWL Type Library, inference engine, move configuration to OWL format

## Conclusion

At the end we recommend you to take a close look at your application. If you see that a lot of business logic is already
presented as tables, or can be easily presented as one, you owe it to yourself to give OpenL Tablets a try.

## Origins

```
Main Entry:  tablet
Pronunciation: 'ta-bl&t
Function: noun
Etymology: Middle English tablett, from Anglo-French tablet, diminutive of table
1 a : a flat slab or plaque suited for or bearing an inscription
  b : a thin slab or one of a set of portable sheets used for writing
2 a : a compressed or molded block of a solid material
  b : a small mass of medicated material "an aspirin tablet"

We believe Webster guys are going to update the entry soon:

3  Tablet - executable decision or lookup table of well-defined structure. (comp. slang). See applet, servlet, pagelet
```

And if they are not, there is always Wikipedia.

## Disclaimer

In all OpenL Tablets documentation you can freely substitute *table* for *tablet* and vice versa without losing any bit
of useful information; we just think that *tablet* is cool and *table* is square (or rectangular).
