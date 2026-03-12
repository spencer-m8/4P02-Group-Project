package Engine;

import java.nio.file.*;
import java.util.*;
import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class FingerPrintGenerator {

    public static Submission[] generateFingerprints(List<Path> ZipPaths, int k, int w) throws IOException {
        Submission[] submissions = new Submission[ZipPaths.size()];
        for (int i = 0; i < submissions.length; i++) {
            submissions[i] = generateFingerprints(ZipPaths.get(i),k,w);
        }
        return submissions;
    }
    public static Submission generateFingerprints(Path ZipPath, int k, int w) throws IOException {
        String ID = ZipPath.getFileName().toString().split("\\.",2)[0];

        String code = "";
        String language = "";

        // Read from zip file
        try (ZipInputStream z = new ZipInputStream(new FileInputStream(ZipPath.toFile()))) {
            ZipEntry entry;
            while ((entry = z.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    String fileName = entry.getName();

                    if (fileName.endsWith(".java")) {
                        language = "java";
                        code = new String(z.readAllBytes());
                        break;
                    } else if (fileName.endsWith(".c")) {
                        language = "c";
                        code = new String(z.readAllBytes());
                        break;
                    } else if (fileName.endsWith(".cpp")) {
                        language = "cpp";
                        code = new String(z.readAllBytes());
                        break;
                    }
                }
                z.closeEntry();
            }
        }
        List<int[]> fingerprint = fingerprint(hashes(normalize(code), k), w);

        return new Submission(ID, language, fingerprint);
    }



    public static String normalize(String code) {

        if (code == null || code.isEmpty()) {
            return "";
        }

        code = code.replaceAll("(?s)/\\*.*?\\*/", "");
        code = code.replaceAll("//.*", "");
        code = code.replace("\t", " ");

        String[] lines = code.split("\n");
        String result = "";

        for (int i = 0; i < lines.length; i++) {

            String line = lines[i].trim();

            if (!line.isEmpty()) {
                result += line + "\n";
            }
        }
        return result;
    }

    public static List<Integer> hashes(String code, int K) {

        List<Integer> hashes = new ArrayList<>();

        if (code == null || code.isEmpty()) {
            return hashes;
        }

        //tokenize
        String[] tokens = code.split("\\s+");

        for (int i = 0; i <= tokens.length - K; i++) {

            StringBuilder gram = new StringBuilder();

            for (int j = 0; j < K; j++) {
                gram.append(tokens[i + j]);
                gram.append(" ");
            }

            int hash = gram.toString().hashCode();

            hashes.add(hash);
        }
        return hashes;
    }

    public static List<int[]> fingerprint(List<Integer> hashes, int wSize) {
        List<int[]> FingerPrint = new ArrayList<>();

        if(hashes.size() < wSize) {
            for (int i = 0; i < hashes.size(); i++) {
                FingerPrint.add(new int[]{hashes.get(i),i});
            }
            return FingerPrint;
        }

        int minIndex = 0;

        for(int i = 0; i < hashes.size() - wSize; i++) {
            long minHash = Long.MAX_VALUE;
            int currentMinIndex = i;

            for (int j = i; j < i + wSize && j < hashes.size(); j++) {
                if (hashes.get(j) <= minHash) {
                    minHash = hashes.get(j);
                    currentMinIndex = j;
                }
            }

            if (currentMinIndex > minIndex) {
                FingerPrint.add(new int[]{hashes.get(currentMinIndex),currentMinIndex});
                minIndex = currentMinIndex;
            }
        }
        return FingerPrint;
    }
}
