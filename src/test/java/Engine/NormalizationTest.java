package Engine;

import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.jupiter.api.Assertions.*;

@RunWith(JUnit4.class)
class NormalizationTest {

    @Test
    public void testStripName_Null() {}
    @Test
    public void testStripName_Empty() {}

    @Test
    public void testStripName_CommentSecondLine() {
        String code = "public class Test{\n int x=1;//this is a commment \n}";
        assertEquals(code, normalization.stripName(code));
    }

    @Test
    public void testStripName_CommentFirstLine() {
        String code = "// This is a comment\npublic class Test{\n int x=1;\n}";
        String stripped = "public class Test{\n int x=1;\n}\n";
        assertEquals(stripped, Normalization.stripName(code));
    }

    @Test
    public void testStripName_CommentsMultiLine() {
        String code = "/* This is a\n comment */\npublic class Test{\n int x=1;\n}";
        String stripped = "public class Test{\n int x=1;\n}";
        assertEquals(stripped, Normalization.stripName(code));
    }

    @Test
    public void testStripName_PoundCommentFirstLine() {
        String input = "# This is a shell comment\npublic class Test {\n int x = 5;\n}";
        String expected = "public class Test {\n int x = 5;\n}\n";
        assertEquals(expected, Normalization.stripName(input));
    }

    @Test
    public void testStripName_OnlyCommentLines() {
        String input = "// First line comment\n// Second line comment\n# Third line comment";
        String expected = "\n\n";
        assertEquals(expected, Normalization.stripName(input));
    }

    @Test
    public void testStripName_FirstLineCommentWithSpaces() {
        String input = "   // Comment with spaces\npublic class Test {\n    int x = 5;\n}";
        String expected = "public class Test {\n    int x = 5;\n}\n";
        assertEquals(expected, Normalization.stripName(input));
    }

    @Test
    public void testNormalize_NullInput() {
        assertEquals("", Normalization.normalize(null));
    }

    @Test
    public void testNormalize_EmptyInput() {
        assertEquals("", Normalization.normalize(""));
    }

    @Test
    public void testNormalize_RemoveMultiLineComments() {
        String input = "public class Test {\n    /* This is a\n       multi-line comment */\n    int x = 5;\n}";
        String expected = "public class Test {\nint x = 5;\n}\n";
        assertEquals(expected, Normalization.normalize(input));
    }

    @Test
    public void testNormalize_RemoveSingleLineComments() {
        String input = "public class Test {\n    int x = 5; // This is a comment\n    int y = 10;\n}";
        String expected = "public class Test {\nint x = 5;\nint y = 10;\n}\n";
        assertEquals(expected, Normalization.normalize(input));
    }

    @Test
    public void testNormalize_RemoveTabs() {
        String input = "public\tclass\tTest\t{\n\tint\tx\t=\t5;\n}";
        String expected = "public class Test {\nint x = 5;\n}\n";
        assertEquals(expected, Normalization.normalize(input));
    }

    @Test
    public void testNormalize_RemoveEmptyLines() {
        String input = "public class Test {\n\n    int x = 5;\n\n\n    int y = 10;\n\n}";
        String expected = "public class Test {\nint x = 5;\nint y = 10;\n}\n";
        assertEquals(expected, Normalization.normalize(input));
    }

    @Test
    public void testNormalize_TrimLines() {
        String input = "public class Test {\n    int x = 5;    \n    int y = 10;    \n}";
        String expected = "public class Test {\nint x = 5;\nint y = 10;\n}\n";
        assertEquals(expected, Normalization.normalize(input));
    }

    @Test
    public void testNormalize_ComplexCodeWithAllFeatures() {
        String input = """
                public class Test {
                    // Class comment
                    /* Multi-line
                       comment */
                    int x = 5;  // inline comment
                
                    /* Another comment */
                    int y = 10;
                   \s
                    // Final comment
                }""";

        String expected = """
                public class Test {
                int x = 5;
                int y = 10;
                }
                """;

        assertEquals(expected, Normalization.normalize(input));
    }

    @Test
    public void testNormalize_CodeWithStringLiteralsContainingComments() {
        String input = """
                String s1 = "/* not a comment */";
                String s2 = "// not a comment";
                int x = 5; // real comment""";

        String expected = """
                String s1 = "/* not a comment */";
                String s2 = "// not a comment";
                int x = 5;
                """;

        // Note: The regex might remove comments inside strings, which could be a limitation
        assertEquals(expected, Normalization.normalize(input));
    }

    @Test
    public void testNormalize_NestedComments() {
        String input = """
                /* Outer comment /* inner comment */ end */
                public class Test {
                    int x = 5;
                }""";

        String result = Normalization.normalize(input);
        assertNotNull(result);
        assertTrue(result.contains("public class Test {"));
    }

    @Test
    public void testNormalize_WhitespaceOnlyLines() {
        String input = "public class Test {\n    \n    int x = 5;\n\t\n    int y = 10;\n    \n}";
        String expected = "public class Test {\nint x = 5;\nint y = 10;\n}\n";
        assertEquals(expected, Normalization.normalize(input));
    }

    @Test
    public void testStripAndNormalize_Combined() {
        String input = """
                // File header comment
                /* Multi-line
                   header */
                public class Test {
                    // Class comment
                    int x = 5;  // inline comment
                   \s
                    /* Another comment */
                    int y = 10;
                }""";

        String stripped = Normalization.stripName(input);
        String normalized = Normalization.normalize(stripped);

        String expected = """
                public class Test {
                int x = 5;
                int y = 10;
                }
                """;

        assertEquals(expected, normalized);
    }
}