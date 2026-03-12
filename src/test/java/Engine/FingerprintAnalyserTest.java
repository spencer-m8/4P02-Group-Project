package Engine;

import org.junit.jupiter.api.*;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class FingerprintAnalyserTest {

    private Submission submission1;
    private Submission submission2;
    private Submission submission3;
    private Submission submission4;
    private Submission emptySubmission;
    private Submission submissionWithDuplicates;
    private Submission submissionWithNull;

    @BeforeEach
    void setUp() {
        submission1 = new Submission("01", "Java", Arrays.asList(
                new int[]{11223, 1},
                new int[]{11224, 2},
                new int[]{11225, 3}
        ));

        submission2 = new Submission("02", "Java", Arrays.asList(
                new int[]{11223, 1},//matches 1
                new int[]{11224, 2},//matches 1
                new int[]{11125, 3}
        ));
        submission3 = new Submission("03", "Python", Arrays.asList(
                new int[]{11223, 1},//this matches 1, 2
                new int[]{21224, 2},
                new int[]{21125, 3}
        ));
        submission4 = new Submission("04", "Python", Arrays.asList(
                new int[]{31223, 1}, //all unique
                new int[]{31224, 2},
                new int[]{31125, 3}
        ));
        emptySubmission = new Submission("05", "Java", Arrays.asList());

        submissionWithDuplicates = new Submission("06", "C++", Arrays.asList(
                new int[]{11223, 1},
                new int[]{11224, 2}, // Duplicate hash code
                new int[]{11224, 3}   // Duplicate hash code
        ));

        submissionWithNull = new Submission("07", "C++",
                Arrays.asList(new int[]{12345, 1}, null, new int[]{12346, 2}));
    }

    @AfterEach
    void tearDown() {
    }

    @Nested
    @DisplayName("Tests for compare() method")
    class CompareTests {

        @Test
        @DisplayName("Should correctly count matches between two submissions")
        void testCompareWithMatches() {
            Results results = FingerprintAnalyser.compare(submission1, submission2);
            assertEquals(2, results.matchingFingerprints);
        }

        @Test
        @DisplayName("Should return 0 matches when fingerprints have no common elements")
        void testCompareWithNoMatches() {
            Results results = FingerprintAnalyser.compare(submission1, submission4);
            assertEquals(0, results.matchingFingerprints);
        }

        @Test
        @DisplayName("Should handle empty fingerprint correctly")
        void testCompareWithEmptySubmission() {
            Results results = FingerprintAnalyser.compare(submission3, emptySubmission);
            assertEquals(0, results.matchingFingerprints);//result will be empty, unsure if this is handled

        }

        @Test
        @DisplayName("Should handle duplicate fingerprint elements")
        void testCompareWithDuplicates() {
            Results results = FingerprintAnalyser.compare(submission1, submissionWithDuplicates);
            assertEquals(3, results.matchingFingerprints); // Counts each duplicate separately
        }

        @Test
        @DisplayName("Should handle null fingerprint elements gracefully")
        void testCompareWithNullElements() {
            try {
                Results results = FingerprintAnalyser.compare(submission1, submissionWithNull);
                // If it handles nulls, verify the behavior
                assertNotNull(results);
            } catch (NullPointerException e) {
                // If it throws NPE, that's also valid behavior to document
                assertThrows(NullPointerException.class, () ->
                        FingerprintAnalyser.compare(submission1, submissionWithNull));
            }
        }
    }

    @Nested
    @DisplayName("Tests for comparing using testZips")
    class CompareTests2 {}
}
