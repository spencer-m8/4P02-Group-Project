package Engine;

import java.util.ArrayList;
import java.util.List;


public class FingerprintAnalyser {

    public static Results compare (Submission zip1, Submission zip2) {
        int Zip1count = zip1.getFingerprint().size();
        int Zip2count = zip2.getFingerprint().size();

        int matches = 0;
        for (int[] element : zip1.getFingerprint()) {
            for(int i = 0; i < zip2.getFingerprint().size(); i++) {
                if (element[0] == zip2.getFingerprint().get(i)[0]) {
                    matches++;
                }
            }
        }
        return new Results(zip1.getID(), zip2.getID(), Zip1count, Zip2count, matches);
    }

    public static List<Results> compareOneToMany (Submission mainZip, Submission[] comparisonZips) {
        List<Results> comparedResults = new ArrayList<>();

        for (Submission comparisonZip : comparisonZips) {
            Results result = compare(mainZip, comparisonZip);
            comparedResults.add(result);
        }
        return comparedResults;
    }

    public static List<Results> compareAll (Submission[] Zips) {
        List<Results> comparedResults = new ArrayList<>();

        for (int i = 0; i < Zips.length; i++) {
            for (int j = 0; j < Zips.length; j++) {
                if (i != j) {
                    Results result = compare(Zips[i],Zips[j]);
                    comparedResults.add(result);
                }
            }
        }
        return comparedResults;
    }
}
