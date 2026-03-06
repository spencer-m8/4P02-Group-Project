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
        int mainZipCount = mainZip.getFingerprint().size();

        List<Results> comparedResults = new ArrayList<>();
        for (int i = 0; i < comparisonZips.length; i++) {
            int matches = 0;
            int comparisonZipsCount = comparisonZips[i].getFingerprint().size();
            for (int[] element : mainZip.getFingerprint()) {
                for(int j = 0; j < comparisonZips[i].getFingerprint().size(); j++) {
                    if (element[0] == comparisonZips[i].getFingerprint().get(i)[0]) {
                        matches++;
                    }
                }
            }
            Results result = new Results(mainZip.getID(), comparisonZips[i].getID(), mainZipCount, comparisonZipsCount, matches);
            comparedResults.add(result);
        }

        return comparedResults;
    }

    public static List<Results> compareAll (Submission[] Zips) {
        List<Results> comparedResults = new ArrayList<>();

        for (int i = 0; i < Zips.length; i++) {
            Submission main = Zips[i];
            for (int j = 0; j < Zips.length; j++) {
                if (i != j) {
                    int matches = 0;
                    int comparisonZipsCount = Zips[j].getFingerprint().size();
                    for (int[] element : main.getFingerprint()) {
                        for(int k = 0; k < comparisonZipsCount; k++) {
                            if (element[0] == Zips[j].getFingerprint().get(j)[0]) {
                                matches++;
                            }
                        }
                    }
                    Results result = new Results(main.getID(), Zips[i].getID(), main.getFingerprint().size(), comparisonZipsCount, matches);
                    comparedResults.add(result);
                }
            }

        }
        return comparedResults;
    }
}
