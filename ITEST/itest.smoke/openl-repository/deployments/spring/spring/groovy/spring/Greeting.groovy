package spring

/**
 * A plain Groovy bean, compiled together with SpringConfig.
 */
class Greeting {

    String greet(String name) {
        "Hello, " + name
    }
}
