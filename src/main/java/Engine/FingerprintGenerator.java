package Engine;

import java.util.*;
public class FingerprintGenerator {

    public static List<Integer> hashes(String code, int K) {

        List<Integer> fingerprints = new ArrayList<>();

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

    public static List<Integer> fingerprint(List<Integer> hashes, int wSize) {
        List<Integer> FingerPrint = new ArrayList<>();

        if(hashes.size() < wSize) {
            return hashes;
        }

        int minIndex = 0;

        for(int i = 0; i < hashes.size() - wSize; i++) {
            long minHash = Long.MAX_VALUE;
            int currentMinIndex = i;

            for (int j = i; j < i + wSize && j < hashes.size(); j++) {
                if (hashes.get(j) <minHash) {
                    minHash = hashes.get(j);
                    currentMinIndex = j;
                }
            }

            if (currentMinIndex > minIndex || i == 0) {
                FingerPrint.add(hashes.get(currentMinIndex));
                minIndex = currentMinIndex;
            }
        }

        return FingerPrint;
    }
}
