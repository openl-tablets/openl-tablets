package org.openl.rules.ruleservice.core;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import org.openl.classloader.OpenLClassLoader;

class ProxyDefiningClassLoaderTest {

    @Test
    void proxyOfJdkClassIsDefinedHereAndLoadedByName() throws Exception {
        var loader = new ProxyDefiningClassLoader(getClass().getClassLoader());
        var name = "_java.util.HashMap$$Proxy";

        var proxy = loader.publicDefineClass(name, subclassOf("java/util/HashMap", name), null);

        assertSame(loader, proxy.getClassLoader());
        assertSame(HashMap.class, proxy.getSuperclass());
        assertSame(proxy, loader.loadClass(name));
        assertSame(proxy, Class.forName(name, true, loader));
    }

    @Test
    void proxyOfConfigurationLoaderClassIsDefinedBesideIt() throws Exception {
        try (var project = new OpenLClassLoader(getClass().getClassLoader())) {
            var config = project.parseClass("package spring; class SpringConfig {}");
            var loader = new ProxyDefiningClassLoader(config.getClassLoader());
            var name = "spring.SpringConfig$$Proxy";

            var proxy = loader.publicDefineClass(name, subclassOf("spring/SpringConfig", name), null);

            assertSame(config.getClassLoader(), proxy.getClassLoader());
            assertSame(proxy, loader.loadClass(name));
        }
    }

    @Test
    void proxyOfGroovyClassFromAnotherScriptIsDefinedBesideItAndLoadedByName() throws Exception {
        try (var project = new OpenLClassLoader(getClass().getClassLoader())) {
            var interceptor = project.parseClass("package demo; class Interceptor {}");
            var configLoader = project.parseClass("package spring; class SpringConfig {}").getClassLoader();
            var loader = new ProxyDefiningClassLoader(configLoader);
            var name = "demo.Interceptor$$Proxy";

            var proxy = loader.publicDefineClass(name, subclassOf("demo/Interceptor", name), null);

            assertSame(interceptor.getClassLoader(), proxy.getClassLoader());
            assertSame(proxy, Class.forName(name, true, loader));
        }
    }

    @Test
    void proxyOfDependencyClassIsDefinedBesideItThroughGroovyConfigurationLoader() throws Exception {
        try (var dependency = new OpenLClassLoader(getClass().getClassLoader());
                var project = new OpenLClassLoader(getClass().getClassLoader())) {
            dependency.addGeneratedClass("demo.lib.Hidden", subclassOf("java/lang/Object", "demo.lib.Hidden", 0));
            project.addClassLoader(dependency);
            var configLoader = project.parseClass("package spring; class SpringConfig {}").getClassLoader();
            var loader = new ProxyDefiningClassLoader(configLoader);
            var name = "demo.lib.Hidden$$Proxy";

            var proxy = loader.publicDefineClass(name, subclassOf("demo/lib/Hidden", name), null);

            assertSame(dependency, proxy.getClassLoader());
            assertSame(proxy, loader.loadClass(name));
        }
    }

    @Test
    void proxyNamedInAnotherPackageIsDefinedHere() throws Exception {
        try (var project = new OpenLClassLoader(getClass().getClassLoader())) {
            var config = project.parseClass("package spring; class SpringConfig {}");
            var loader = new ProxyDefiningClassLoader(config.getClassLoader());
            var name = "_spring.SpringConfig$$Proxy";

            var proxy = loader.publicDefineClass(name, subclassOf("spring/SpringConfig", name), null);

            assertSame(loader, proxy.getClassLoader());
            assertSame(config, proxy.getSuperclass());
        }
    }

    @Test
    void proxyAlreadyDefinedBesideTheClassForAnotherServiceIsReused() throws Exception {
        try (var project = new OpenLClassLoader(getClass().getClassLoader())) {
            var configLoader = project.parseClass("package spring; class SpringConfig {}").getClassLoader();
            var name = "spring.SpringConfig$$Proxy";
            var bytes = subclassOf("spring/SpringConfig", name);
            var proxy = new ProxyDefiningClassLoader(configLoader).publicDefineClass(name, bytes, null);

            assertSame(proxy, new ProxyDefiningClassLoader(configLoader).publicDefineClass(name, bytes, null));
        }
    }

    @Test
    void anotherClassWithTheProxyNameIsNotReused() throws Exception {
        try (var project = new OpenLClassLoader(getClass().getClassLoader())) {
            var configLoader = project.parseClass("package spring; class SpringConfig {}; class SpringConfig$$Proxy {}")
                    .getClassLoader();
            var loader = new ProxyDefiningClassLoader(configLoader);
            var name = "spring.SpringConfig$$Proxy";

            var proxy = loader.publicDefineClass(name, subclassOf("spring/SpringConfig", name), null);

            assertSame(loader, proxy.getClassLoader());
        }
    }

    @Test
    void proxyWithOtherInterfacesIsNotReused() throws Exception {
        try (var project = new OpenLClassLoader(getClass().getClassLoader())) {
            var configLoader = project.parseClass("package spring; class SpringConfig {}").getClassLoader();
            var name = "spring.SpringConfig$$Proxy";
            new ProxyDefiningClassLoader(configLoader)
                    .publicDefineClass(name, subclassOf("spring/SpringConfig", name), null);
            var loader = new ProxyDefiningClassLoader(configLoader);
            var bytes = subclassOf("spring/SpringConfig", name, Opcodes.ACC_PUBLIC, "java/io/Serializable");

            var proxy = loader.publicDefineClass(name, bytes, null);

            assertSame(loader, proxy.getClassLoader());
        }
    }

    @Test
    void proxyGeneratedOtherwiseWithTheSameTypesIsNotReused() throws Exception {
        try (var project = new OpenLClassLoader(getClass().getClassLoader())) {
            var configLoader = project.parseClass("package spring; class SpringConfig {}").getClassLoader();
            var name = "spring.SpringConfig$$Proxy";
            new ProxyDefiningClassLoader(configLoader)
                    .publicDefineClass(name, subclassOf("spring/SpringConfig", name), null);
            var loader = new ProxyDefiningClassLoader(configLoader);
            var writer = new ClassWriter(0);
            writer.visit(Opcodes.V21,
                    Opcodes.ACC_PUBLIC,
                    "spring/SpringConfig$$Proxy",
                    null,
                    "spring/SpringConfig",
                    null);
            writer.visitField(Opcodes.ACC_PRIVATE, "CGLIB$CALLBACK_0", "Ljava/lang/Object;", null, null).visitEnd();
            writer.visitEnd();

            var proxy = loader.publicDefineClass(name, writer.toByteArray(), null);

            assertSame(loader, proxy.getClassLoader());
        }
    }

    @Test
    void proxyTheClassLoaderOfItsClassCannotLinkIsDefinedHere() throws Exception {
        try (var isolated = new OpenLClassLoader(ClassLoader.getPlatformClassLoader());
                var project = new OpenLClassLoader(getClass().getClassLoader())) {
            isolated.addGeneratedClass("demo.lib.Isolated", subclassOf("java/lang/Object", "demo.lib.Isolated"));
            project.addClassLoader(isolated);
            var loader = new ProxyDefiningClassLoader(project);
            var name = "demo.lib.Isolated$$Proxy";
            var bytes = subclassOf("demo/lib/Isolated",
                    name,
                    Opcodes.ACC_PUBLIC,
                    "org/springframework/core/SmartClassLoader");

            var proxy = loader.publicDefineClass(name, bytes, null);

            assertSame(loader, proxy.getClassLoader());
        }
    }

    @Test
    void proxyOfNonPublicJdkClassCannotBeDefined() {
        var loader = new ProxyDefiningClassLoader(getClass().getClassLoader());
        var name = "_java.util.ImmutableCollections$AbstractImmutableList$$Proxy";
        var bytes = subclassOf("java/util/ImmutableCollections$AbstractImmutableList", name);

        assertThrows(IllegalAccessError.class, () -> loader.publicDefineClass(name, bytes, null));
    }

    @Test
    void proxyOfUnknownClassFailsToLink() {
        var loader = new ProxyDefiningClassLoader(getClass().getClassLoader());
        var name = "demo.Unknown$$Proxy";
        var bytes = subclassOf("demo/Unknown", name);

        assertThrows(NoClassDefFoundError.class, () -> loader.publicDefineClass(name, bytes, null));
    }

    @Test
    void resourcesComeFromParent() {
        var loader = new ProxyDefiningClassLoader(getClass().getClassLoader());

        assertNotNull(loader.getResourceAsStream("org/openl/rules/ruleservice/core/ProxyDefiningClassLoader.class"));
    }

    private static byte[] subclassOf(String superclass, String name) {
        return subclassOf(superclass, name, Opcodes.ACC_PUBLIC);
    }

    private static byte[] subclassOf(String superclass, String name, int access, String... interfaces) {
        var writer = new ClassWriter(0);
        writer.visit(Opcodes.V21, access, name.replace('.', '/'), null, superclass, interfaces);
        writer.visitEnd();
        return writer.toByteArray();
    }
}
