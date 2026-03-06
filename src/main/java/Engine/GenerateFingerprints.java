package Engine;

import java.util.*;
public class GenerateFingerprints {

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
