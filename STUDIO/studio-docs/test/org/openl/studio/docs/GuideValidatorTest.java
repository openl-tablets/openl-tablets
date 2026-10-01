package org.openl.studio.docs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GuideValidatorTest {

    @TempDir
    Path root;

    @Test
    void acceptsGuidesThatLinkWithinThemselves() throws IOException {
        write("index.md", """
                ---
                title: User Guides
                ---
                # Guides

                See [the editor](studio/editor.md#editing-a-table), [the guide](studio/), [here](#guides) and
                [the site](https://example.org/guides). A bare https://example.org/page is a link too.

                ![Editor](studio/images/editor.png)

                > [!Note]
                > A note.

                ```java
                [not a link](missing.md)
                ```
                """);
        write("studio/index.md", "# Studio\n\n<img src=\"images/sized.png\" width=\"400\" alt=\"Sized\"/><br/>\n");
        write("studio/editor.md", "# Editor\n\n## Editing a Table\n");
        write("studio/images/editor.png", "");
        write("studio/images/sized.png", "");
        write("downloads/sample.xlsx", "");

        assertEquals(List.of(), problems());
    }

    @Test
    void reportsLinksToMissingPagesAndHeadings() throws IOException {
        write("index.md", """
                # Guides

                [Missing](missing.md), [Folder](studio/) and [Heading](page.md#nowhere).

                [Here](#nowhere)
                """);
        write("page.md", "# Page\n");
        write("images.png", "");

        assertEquals(List.of(
                "images.png: No page shows the image.",
                "index.md:3: The link missing.md does not exist.",
                "index.md:3: The link studio/ does not exist.",
                "index.md:3: The link page.md#nowhere names no heading of page.md.",
                "index.md:5: The link #nowhere names no heading of index.md."), messages());
    }

    @Test
    void comparesNamesCaseSensitively() throws IOException {
        write("index.md", "[Page](Page.md)\n\n![Shot](Images/shot.png)\n");
        write("page.md", "# Page\n");
        write("images/shot.png", "");

        assertEquals(List.of(
                "images/shot.png: No page shows the image.",
                "index.md:1: The link Page.md differs in case from page.md.",
                "index.md:3: The image Images/shot.png differs in case from images/shot.png."), messages());
    }

    @Test
    void reportsLinksLeavingTheGuides() throws IOException {
        write("guide/index.md", "[Up](../../DEPLOYMENT.md), [Root](/user-guides/index.md) and [Sibling](../page.md).\n");
        write("page.md", "# Page\n");

        assertEquals(List.of(
                "guide/index.md:1: The link ../../DEPLOYMENT.md leaves the guides. Link to the documentation site instead.",
                "guide/index.md:1: The link /user-guides/index.md leaves the guides. Link to the documentation site instead."),
                messages());
    }

    @Test
    void numbersRepeatedHeadings() throws IOException {
        write("index.md", """
                # Guides

                ## Example

                ## Example

                [First](#example), [second](#example-1) and [third](#example-2).
                """);

        assertEquals(List.of("index.md:7: The link #example-2 names no heading of index.md."), messages());
    }

    @Test
    void reportsMissingImages() throws IOException {
        write("index.md", "![Shot](images/shot.png)\n\n<img src=\"images/sized.png\" alt=\"Sized\"/>\n");

        assertEquals(List.of(
                "index.md:1: The image images/shot.png does not exist.",
                "index.md:3: The image images/sized.png does not exist."), messages());
    }

    @Test
    void allowsOnlyListedExternalLinks() throws IOException {
        write("index.md", """
                [Allowed](https://example.org/a), [other](https://other.org/), https://bare.org/x, <mailto:me@x.org>.

                A JDBC URL is no link: jdbc:mysql://localhost:3306/db.

                <p><iframe src="https://videos.org/embed/1"></iframe></p>
                """);

        assertEquals(List.of(
                "index.md:1: The external link https://other.org/ is not allowed."
                        + " Add its prefix to allowed-links.txt when it is right.",
                "index.md:1: The external link https://bare.org/x is not allowed."
                        + " Add its prefix to allowed-links.txt when it is right.",
                "index.md:1: The external link mailto:me@x.org is not allowed."
                        + " Add its prefix to allowed-links.txt when it is right.",
                "index.md:5: The external link https://videos.org/embed/1 is not allowed."
                        + " Add its prefix to allowed-links.txt when it is right."), messages());
    }

    @Test
    void reportsUnsupportedSyntax() throws IOException {
        write("index.md", """
                # Guides

                > [!Warning]
                > Careful.

                <div>
                <table><tr><td>cell</td></tr></table>
                </div>

                ```sh
                ls
                ```
                """);

        assertEquals(List.of(
                "index.md:3: The alert [!Warning] is not supported. Use [!Note].",
                "index.md:6: The HTML tag <div> is not supported.",
                "index.md:7: The HTML tag <table> is not supported.",
                "index.md:7: The HTML tag <tr> is not supported.",
                "index.md:7: The HTML tag <td> is not supported.",
                "index.md:10: The code language `sh` is not supported."), messages());
    }

    @Test
    void reportsMalformedTables() throws IOException {
        write("index.md", """
                # Guides

                ```csv
                Name,Value
                one,"open
                ```

                ```openl
                Rules void hello(String a, String b)
                ---
                <,x
                ---
                ```

                ```csv
                ```
                """);

        assertEquals(List.of(
                "index.md:5: A quoted value is not closed.",
                "index.md:11: A `<` in the first column has no cell on its left to join.",
                "index.md:12: A second `---` line. One line ends the column headers.",
                "index.md:16: The table has no rows."), messages());
    }

    @Test
    void reservesTheNameOfTheTableOfContents() throws IOException {
        write("index.md", "# Guides\n");
        write("toc.json", "{}");

        assertEquals(List.of("toc.json: The name is reserved for the table of contents OpenL Studio builds."),
                messages());
    }

    @Test
    void resolvesRelativePaths() {
        assertEquals("a/c.md", GuideValidator.resolve("a/b.md", "c.md"));
        assertEquals("c.md", GuideValidator.resolve("a/b.md", "../c.md"));
        assertEquals("a/d", GuideValidator.resolve("a/b.md", "./d/"));
        assertEquals("", GuideValidator.resolve("a/b.md", ".."));
        assertNull(GuideValidator.resolve("a/b.md", "../../c.md"));
    }

    private void write(String file, String text) throws IOException {
        var path = root.resolve(file);
        Files.createDirectories(path.getParent());
        Files.writeString(path, text);
    }

    private List<Problem> problems() throws IOException {
        return new GuideValidator(root, List.of("https://example.org/")).problems();
    }

    private List<String> messages() throws IOException {
        return problems().stream().map(Problem::toString).toList();
    }
}
