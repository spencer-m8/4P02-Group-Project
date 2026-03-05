import java.util.*;
public class FingerPrinter {

    private static final int K = 4;

    public static Set<Integer> fingerprint(String code) {

        Set<Integer> fingerprints = new HashSet<>();

        if (code == null || code.isEmpty()) {
            return fingerprints;
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

            fingerprints.add(hash);
        }

        return fingerprints;
    }
}