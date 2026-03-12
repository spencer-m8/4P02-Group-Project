import Engine.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public class main {
    public static void main(String[] args) throws IOException {
        List<Path> zipPaths = new ArrayList<>();

        File testZipsDirectory = new File (Objects.requireNonNull(main.class.getClassLoader().getResource("testZips")).getPath());
        File [] zipFiles = testZipsDirectory.listFiles(((dir, name) -> name.endsWith(".zip")));

        if (zipFiles != null) {
            for (File zip : zipFiles) {
                zipPaths.add(Path.of(zip.getPath()));
            }
        }

        int kSize = 5;
        int t = 7;
        Engine engine = new Engine(kSize, t);
        Submission[] s = engine.run(zipPaths);
        List<Results> results = FingerprintAnalyser.compareAll(s);
        System.out.println(s.length);
        for (int i = 0; i < s.length; i++) {
            System.out.println("AnonID: " + s[i].getID());
            System.out.println("Language: " + s[i].getLanguage());
            System.out.println("Code:");
            for(int[] elements : s[i].getFingerprint()) {
                System.out.print(Arrays.toString(elements));
            }
            for (Results paste : results) {
                System.out.println();
                System.out.println(paste.returnZip1FingerprintCount());
                System.out.println(paste.returnZip2FingerprintCount());
                System.out.println(paste.returnMatchingFingerprints());
                System.out.println(paste.returnJaccardSimilarity());
                System.out.println(paste.returnContainsZip1());
                System.out.println(paste.returnContainsZip2());

            }
            System.out.println("\n\n");
        }
    }
}