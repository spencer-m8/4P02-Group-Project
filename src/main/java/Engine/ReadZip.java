package Engine;

import java.io.*;
import java.util.*;
import java.util.zip.*;

public class ReadZip {

    public static Submission readZip(String zipPath) throws IOException {

        ZipInputStream z = new ZipInputStream(new FileInputStream(zipPath));
        ZipEntry entry;

        StringBuilder allCode = new StringBuilder();
        String language = "";

        while ((entry = z.getNextEntry()) != null) {

            if (entry.isDirectory()) {
                continue;
            }

            String fileName = entry.getName();
            if (fileName.endsWith(".java")){
                language = "java";
            } else if (fileName.endsWith(".c")) {
                language = "c";
            }else if (fileName.endsWith(".cpp"))
                language = "cpp";

            if (fileName.endsWith(".java") || fileName.endsWith(".c") || fileName.endsWith(".cpp")) {

                BufferedReader reader = new BufferedReader(new InputStreamReader(z));
                StringBuilder fileCode = new StringBuilder();

                String line;
                while ((line = reader.readLine()) != null) {
                    fileCode.append(line).append("\n");
                }

                String noName = Normalization.stripName(fileCode.toString());
                String normalized = Normalization.normalize(noName);

                allCode.append("\n");
                allCode.append(normalized);
                allCode.append("\n");
            }

            z.closeEntry();
        }

        z.close();

        String anonId = UUID.randomUUID().toString();
        return new Submission(anonId, language, allCode.toString());
    }
}