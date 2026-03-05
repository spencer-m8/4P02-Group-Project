package Engine;

public class Normalization {

    public static String stripName(String code) {

        if (code == null || code.isEmpty()) {
            return "";
        }

        String[] lines = code.split("\n");
        
        if (lines[0].trim().startsWith("//") ||
                lines[0].trim().startsWith("/*") ||
                lines[0].trim().startsWith("#")) {

            String result = "";

            for (int i = 1; i < lines.length; i++) {
                result += lines[i] + "\n";
            }

            return result;
        }
        return code;
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
}
