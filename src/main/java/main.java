import Engine.*;

import java.io.IOException;
import java.util.*;

public class main {
    public static void main(String[] args) throws IOException {
        List<String> zipPaths = new ArrayList<>();
        String zipPath = Objects.requireNonNull(main.class.getClassLoader().getResource("4P02testZip.zip")).getPath();
        zipPaths.add(zipPath);
        int kSize = 5;
        int wSize = 4;
        Engine engine = new Engine(kSize, wSize);
        Submission[] s = engine.run(zipPaths);
        System.out.println(s.length);
        System.out.println("AnonID: " + s[0].getID());
        System.out.println("Language: " + s[0].getLanguage());
        System.out.println("Code:");
        System.out.println(s[0].getCode());
        System.out.println(s[0].getHashes());
        System.out.println(s[0].getFingerprint());
    }
}