package Engine;

import java.nio.file.*;
import java.util.*;
import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class FingerPrintGenerator {
    
    // creates fingerprints for multiple zip submissions
    public static Submission[] generateFingerprints(List<Path> ZipPaths, int k, int w) throws IOException {
        Submission[] submissions = new Submission[ZipPaths.size()];
        for (int i = 0; i < submissions.length; i++) {
            submissions[i] = generateFingerprints(ZipPaths.get(i),k,w);
        }
        return submissions;
    }

    // creates fingerprints for a single zip submission
    public static Submission generateFingerprints(Path ZipPath, int k, int w) throws IOException {
        //file name (without file extention)
        String ID = ZipPath.getFileName().toString().split("\\.",2)[0];

        String code = "";
        String language = "";

        // Read from zip file
        try (ZipInputStream z = new ZipInputStream(new FileInputStream(ZipPath.toFile()))) {
            ZipEntry entry;
            while ((entry = z.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    String fileName = entry.getName();
                    //setting language type
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
        //Normalize, hash k-grams and produce fingerprints with winnowing
        List<int[]> fingerprint = fingerprint(hashes(normalize(code), k), w);

        return new Submission(ID, language, fingerprint);
    }

    //removes extra whitespace
    public static String normalize(String code) {

        if (code == null || code.isEmpty()) {
            return "";
        }

        //code = code.replaceAll("(?s)/\\*.*?\\*/", "");
        //code = code.replaceAll("//.*", "");
        
        //removing tab characters
        code = code.replace("\t", " ");

        String[] lines = code.split("\n");
        String result = "";
        
        //removing empty lines
        for (int i = 0; i < lines.length; i++) {

            String line = lines[i].trim();

            if (!line.isEmpty()) {
                result += line + "\n";
            }
        }
        return result;
    }

    // Generate hashes for all k-gram
    public static List<Integer> hashes(String code, int K) {

        List<Integer> hashes = new ArrayList<>();

        if (code == null || code.isEmpty()) {
            return hashes;
        }

        //tokenize code by splitting whitespace
        String[] tokens = code.split("\\s+");
        
        //create k-grams and hashes them
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

    //Winnowing
    public static List<int[]> fingerprint(List<Integer> hashes, int wSize) {
        List<int[]> FingerPrint = new ArrayList<>();

        //if there are less hashes then the windows size, we simply keep all 
        if(hashes.size() < wSize) {
            for (int i = 0; i < hashes.size(); i++) {
                FingerPrint.add(new int[]{hashes.get(i),i});
            }
            return FingerPrint;
        }

        int minIndex = 0;
        
        // examine through groups of w hashes at one time
        for(int i = 0; i < hashes.size() - wSize; i++) {
            long minHash = Long.MAX_VALUE;
            int currentMinIndex = i;

            // find the smallest hash from the current window
            for (int j = i; j < i + wSize && j < hashes.size(); j++) {
                if (hashes.get(j) <= minHash) {
                    minHash = hashes.get(j);
                    currentMinIndex = j;
                }
            }

            //Adds the min hash as a fingerprint, if its not added already
            if (currentMinIndex > minIndex) {
                FingerPrint.add(new int[]{hashes.get(currentMinIndex),currentMinIndex});
                minIndex = currentMinIndex;
            }
        }
        return FingerPrint;
    }
}
