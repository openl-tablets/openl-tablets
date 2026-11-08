## Project Localization

This section introduces project localization and describes how to enable it in the OpenL Tablets project.

### Introducing Project Localization

The i18n localization uses the **msg(String code, Object... params)** function and the **locale** property of the runtime context.

The **msg(String code, Object... params)** function reads localization message bundles in the i18n format. All localization bundles are stored in the OpenL Tablets project, the **i18n** folder. The name of the localization bundle matches the following pattern:

`message%locale%.properties` where `%locale%` is a placeholder.

Examples ordered by descending priority are as follows:

-   **message_no_NO_NY.properties** – localization bundle for the Norwegian language, Norway country, Nynorsk form.
-   **message_de_LU.properties** – localization bundle for the German language, Luxembourg country.
-   **message_de.properties** – localization bundle for the German language.
-   **message.properties** – default localization bundle applied to all countries and languages. It has the lowest priority.

Localization bundles files are key-value plain text files where keys and values are separated with = character, for example, `greetings = Hello`.

For more information on supported locales, see [JDK 21 Supported Locales](https://www.oracle.com/java/technologies/javase/jdk21-suported-locales.html).

The **locale** property of IRulesRuntimeContext has the **java.util.Locale** type. When the **msg(String code, Object... params)** function is invoked, the current locale is retrieved from IRulesRuntimeContext and the list of message bundles is configured based on it.

### Enabling Localization in the OpenL Tablets Project

To enable location in the OpenL Tablets project, proceed as follows:

1.  In the OpenL Tablets project, create the **i18n** folder.

2.  In this folder, create a default **message.properties** file with the following contents:

    ```
    greetings = Hello, {0}.
    farewell = Goodbye, {0}.
    inquiry = How are you?

    ```

    When a default message bundle is created, its messages are translated into various languages. For example, for French, the **message_fr_FR.properties** properties file is created and its contains the following lines:

    ```
    greetings = Bonjour, {0}.
    farewell = Au revoir, {0}.
    inquiry = Comment allez-vous?
    ```

    Note that the values on the right are translated while the keys on the left size remain the same. It is important to maintain the keys without alterations as they serve as references when rules retrieve the translated text.

3.  Define the required locale in the runtime context property. By default, OpenL Rule Services automatically populates the 'locale' context property with the value from the Accept-Language HTTP header in the request (RFC3282). Alternatively, the 'locale' context property can be explicitly specified in the request body if needed.

4.  Define a localization message for this locale:

    ```
    '= msg("greetings", "John Smith") // Bonjour, John Smith
    '= msg("farewell", "John Smith") // Au revoir, John Smith
    '= msg("inquiry") // Comment allez-vous?

    ```

In this case, all localization messages are retrieved from **message_fr_FR.properties**. If the locale is set up for another language, for example, **uk_UA**, but the appropriate message bundle is not created, the properties are retrieved from the default file **message.properties**.

**Note:** The **message.properties** file must be encoded using the [UTF-8](https://en.wikipedia.org/wiki/UTF-8) character set.

### Using Messages of Dependency Projects

A project also reads the localization files of the projects it depends on. For each file name, the file of the project comes first, followed by the files of the same name in the dependency projects: a key that the file of the project does not have is taken from the dependency projects.

The more specific language still wins. For the **fr_FR** locale, a key is looked up as follows:

1.  **message_fr_FR.properties** of the project, then of the dependency projects.
2.  **message_fr.properties** of the project, then of the dependency projects.
3.  **message.properties** of the project, then of the dependency projects.

If no file has the key, the **msg** function returns the key itself.

### Checking Localization

When OpenL Studio compiles a project, it checks the localization files of the project before its modules and warns about the following issues:

-   a key written twice in a file, where the last text wins
-   a key written on a line without a separator, which has no text
-   a key that the default file does not have, so a language without its own text returns the key
-   placeholders of a text that differ from those of the text in the default language
-   a text that breaks the message format, such as an open brace, a placeholder that does not begin with a number, a full-width character in a placeholder, or a single apostrophe that hides placeholders
-   an apostrophe of the language in a text with placeholders, such as `l'étape {0}`, which is dropped when values are passed; apostrophes around a placeholder, as in `l'article "{0}" et l'offre`, also print the placeholder as written instead of its value. Quotes around a brace alone, such as `'{'`, print the brace and are not warned about. A text without placeholders is returned as written, its apostrophes kept
-   keys that a language does not translate, so the default texts are shown

As each module is compiled, OpenL Studio also checks the calls of the **msg** function whose key is written as text, and warns about a key that has no default text and about a call passing a number of values different from the number of placeholders of the message. A key built at runtime is not checked. The checks only warn: the rules still run. When the localization files change, OpenL Studio checks them again without compiling the project.
