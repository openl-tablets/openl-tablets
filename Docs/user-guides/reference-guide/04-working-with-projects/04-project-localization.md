## Project Localization

This section introduces project localization and describes how to enable it in the OpenL Tablets project.

### Introducing Project Localization

To enable the i18n localization, the **msg(String code, Object... params)** function and new **locale** property are introduced.

The **msg(String code, Object... params)** function reads localization message bundles in the i18n format. All localization bundles are stored in the OpenL Tablets project, the **i18n** folder. The name of the localization bundle matches the following pattern:

`message%locale%.properties` where `%locale%` is a placeholder.

Examples ordered by descending priority are as follows:

-   **message_no_NO_NY.properties** – localization bundle for the Norwegian language, Norway country, Nynorsk form.
-   **message_de_LU.properties** – localization bundle for the German language, Luxembourg country.
-   **message_de.properties** – localization bundle for the German language.
-   **message.properties** – default localization bundle applied to all countries and languages. It has the lowest priority.

Localization bundles files are key-value plain text files where keys and values are separated with = character, for example, `greetings = Hello`.

For more information on supported locales, see [JDK 11 Supported Locales](https://www.oracle.com/java/technologies/javase/jdk11-suported-locales.html).

To support the i18n localization, a new **locale** property of the **java.lang.Locale** type is added to IRulesRuntimeContext to support i18n localization. When the **msg(String code, Object... params)** function is invoked, the current locale is retrieved from IRulesRuntimeContext and the list of message bundles is configured based on it.

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

**Note: **The **message.properties** file must be encoded using the [UTF-8](https://en.wikipedia.org/wiki/UTF-8) character set. Use the following tool for quick encoding: [https://native2ascii.net/](https://native2ascii.net/).
