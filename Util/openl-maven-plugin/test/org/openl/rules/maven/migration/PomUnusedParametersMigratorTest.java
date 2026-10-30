package org.openl.rules.maven.migration;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Covers {@link PomUnusedParametersMigrator}: the parameters of the {@code openl-maven-plugin} configurations that
 * have no effect are removed, the rest of {@code pom.xml} stays as it was.
 *
 * @author Yury Molchan
 */
class PomUnusedParametersMigratorTest {

    private static final Set<String> PLUGIN_PARAMETERS = Set.of("sourceDirectory", "dependenciesThreshold",
            "skipTests");

    @TempDir
    Path folder;

    @Test
    void removesUnusedParametersAndKeepsTheFormatting() throws IOException {
        var pom = write("""
                <project>
                    <build>
                        <plugins>
                            <plugin>
                                <groupId>org.openl.rules</groupId>
                                <artifactId>openl-maven-plugin</artifactId>
                                <configuration>
                                    <!-- the rules are in the root -->
                                    <sourceDirectory>./</sourceDirectory>
                                    <deploymentName>Custom name</deploymentName>
                                    <generateUnitTests>
                                        true
                                    </generateUnitTests>
                                    <dependenciesThreshold>5</dependenciesThreshold>   <deploymentPackage/>
                                </configuration>
                            </plugin>
                        </plugins>
                    </build>
                </project>
                """);

        assertEquals(List.of(pom), migrate(pom));

        assertEquals("""
                <project>
                    <build>
                        <plugins>
                            <plugin>
                                <groupId>org.openl.rules</groupId>
                                <artifactId>openl-maven-plugin</artifactId>
                                <configuration>
                                    <!-- the rules are in the root -->
                                    <sourceDirectory>./</sourceDirectory>
                                    <dependenciesThreshold>5</dependenciesThreshold>
                                </configuration>
                            </plugin>
                        </plugins>
                    </build>
                </project>
                """, Files.readString(pom));
    }

    @Test
    void removesTheConfigurationLeftEmpty() throws IOException {
        var pom = write("""
                <project>
                    <build>
                        <pluginManagement>
                            <plugins>
                                <plugin>
                                    <artifactId>openl-maven-plugin</artifactId>
                                    <groupId>org.openl.rules</groupId>
                                    <extensions>true</extensions>
                                    <configuration>
                                        <deploymentPackage>true</deploymentPackage>
                                        <deploymentName>Custom name</deploymentName>
                                    </configuration>
                                </plugin>
                            </plugins>
                        </pluginManagement>
                    </build>
                </project>
                """);

        assertEquals(List.of(pom), migrate(pom));

        assertEquals("""
                <project>
                    <build>
                        <pluginManagement>
                            <plugins>
                                <plugin>
                                    <artifactId>openl-maven-plugin</artifactId>
                                    <groupId>org.openl.rules</groupId>
                                    <extensions>true</extensions>
                                </plugin>
                            </plugins>
                        </pluginManagement>
                    </build>
                </project>
                """, Files.readString(pom));
    }

    @Test
    void removesSeveralParametersOfOneLine() throws IOException {
        var pom = write("""
                <project><build><plugins><plugin>
                    <groupId>org.openl.rules</groupId><artifactId>openl-maven-plugin</artifactId>
                    <configuration>
                        <deploymentName>x</deploymentName>  <deploymentPackage/>
                        <deploymentPackage/> <deploymentName>x</deploymentName><skipTests>true</skipTests>
                        <skipTests>true</skipTests> <deploymentPackage/>  <deploymentName>x</deploymentName>
                    </configuration>
                </plugin></plugins></build></project>
                """.replace("\n", "\r\n"));

        migrate(pom);

        assertEquals("""
                <project><build><plugins><plugin>
                    <groupId>org.openl.rules</groupId><artifactId>openl-maven-plugin</artifactId>
                    <configuration>
                        <skipTests>true</skipTests>
                        <skipTests>true</skipTests>
                    </configuration>
                </plugin></plugins></build></project>
                """.replace("\n", "\r\n"), Files.readString(pom));
    }

    @Test
    void keepsTheConfigurationThatOverridesTheInheritedOne() throws IOException {
        var pom = write("""
                <project><build><plugins><plugin>
                    <groupId>org.openl.rules</groupId><artifactId>openl-maven-plugin</artifactId>
                    <configuration combine.self="override">
                        <deploymentName>Custom name</deploymentName>
                    </configuration>
                </plugin></plugins></build></project>
                """);

        migrate(pom);

        assertEquals("""
                <project><build><plugins><plugin>
                    <groupId>org.openl.rules</groupId><artifactId>openl-maven-plugin</artifactId>
                    <configuration combine.self="override">
                    </configuration>
                </plugin></plugins></build></project>
                """, Files.readString(pom));
    }

    @Test
    void keepsTheConfigurationWithComments() throws IOException {
        var pom = write("""
                <project><build><plugins><plugin>
                    <groupId>org.openl.rules</groupId><artifactId>openl-maven-plugin</artifactId>
                    <configuration>
                        <!-- to be removed in the next release -->
                        <deploymentName>Custom name</deploymentName>
                    </configuration>
                </plugin></plugins></build></project>
                """);

        migrate(pom);

        assertEquals("""
                <project><build><plugins><plugin>
                    <groupId>org.openl.rules</groupId><artifactId>openl-maven-plugin</artifactId>
                    <configuration>
                        <!-- to be removed in the next release -->
                    </configuration>
                </plugin></plugins></build></project>
                """, Files.readString(pom));
    }

    @Test
    void cleansTheExecutionsOfProfilesWithWindowsLineBreaks() throws IOException {
        var pom = write("""
                <project>
                    <profiles>
                        <profile>
                            <build>
                                <plugins>
                                    <plugin>
                                        <groupId>org.openl.rules</groupId>
                                        <artifactId>openl-maven-plugin</artifactId>
                                        <executions>
                                            <execution>
                                                <goals><goal>test</goal></goals>
                                                <configuration>
                                                    <skipTests>true</skipTests>
                                                    <singleModuleMode>true</singleModuleMode>
                                                </configuration>
                                            </execution>
                                        </executions>
                                    </plugin>
                                </plugins>
                            </build>
                        </profile>
                    </profiles>
                </project>
                """.replace("\n", "\r\n"));

        migrate(pom);

        assertEquals("""
                <project>
                    <profiles>
                        <profile>
                            <build>
                                <plugins>
                                    <plugin>
                                        <groupId>org.openl.rules</groupId>
                                        <artifactId>openl-maven-plugin</artifactId>
                                        <executions>
                                            <execution>
                                                <goals><goal>test</goal></goals>
                                                <configuration>
                                                    <skipTests>true</skipTests>
                                                </configuration>
                                            </execution>
                                        </executions>
                                    </plugin>
                                </plugins>
                            </build>
                        </profile>
                    </profiles>
                </project>
                """.replace("\n", "\r\n"), Files.readString(pom));
    }

    @Test
    void leavesOtherPluginsAndCommentedOutParametersAlone() throws IOException {
        var text = """
                <?xml version="1.0" encoding="UTF-8"?>
                <!-- <plugin><artifactId>openl-maven-plugin</artifactId><configuration><x/></configuration></plugin> -->
                <project>
                    <build>
                        <plugins>
                            <plugin>
                                <artifactId>maven-surefire-plugin</artifactId>
                                <configuration>
                                    <deploymentName>Тарифы</deploymentName>
                                </configuration>
                            </plugin>
                            <plugin>
                                <groupId>org.openl.rules</groupId>
                                <artifactId>openl-maven-plugin</artifactId>
                                <configuration>
                                    <!-- <deploymentName>Тарифы</deploymentName> -->
                                    <sourceDirectory><![CDATA[<rules>]]></sourceDirectory>
                                </configuration>
                            </plugin>
                        </plugins>
                    </build>
                </project>
                """;
        var pom = write(text);

        assertEquals(List.of(), migrate(pom));

        assertArrayEquals(text.getBytes(UTF_8), Files.readAllBytes(pom));
    }

    @Test
    void keepsTheBytesOfTheTextAroundTheRemovedParameters() throws IOException {
        var pom = folder.resolve("pom.xml");
        Files.writeString(pom, """
                <project><!-- Größe -->
                <build><plugins><plugin><groupId>org.openl.rules</groupId><artifactId>openl-maven-plugin</artifactId>
                <configuration><sourceDirectory>Größe</sourceDirectory><deploymentName>x</deploymentName></configuration>
                </plugin></plugins></build></project>
                """, ISO_8859_1);

        migrate(pom);

        assertEquals("""
                <project><!-- Größe -->
                <build><plugins><plugin><groupId>org.openl.rules</groupId><artifactId>openl-maven-plugin</artifactId>
                <configuration><sourceDirectory>Größe</sourceDirectory></configuration>
                </plugin></plugins></build></project>
                """, Files.readString(pom, ISO_8859_1));
    }

    @Test
    void skipsAProjectWithoutPom() throws IOException {
        assertEquals(List.of(), migrate(folder.resolve("pom.xml")));
    }

    private Path write(String text) throws IOException {
        return Files.writeString(folder.resolve("pom.xml"), text);
    }

    private List<Path> migrate(Path pom) throws IOException {
        var context = new MigrationContext(null, null, pom, PLUGIN_PARAMETERS);
        return new PomUnusedParametersMigrator().migrate(folder, context);
    }
}
