package Engine;

import java.util.ArrayList;
import java.util.List;


public class FingerprintAnalyser {

    //comparing two submission
    public static Results compare (Submission zip1, Submission zip2) {
        //gets number of fingerprints for each submission
        int Zip1count = zip1.getFingerprint().size();
        int Zip2count = zip2.getFingerprint().size();

        int matches = 0; //count for matching hashes
        //Compare fingerprint hashes between the two submissions
        for (int[] element : zip1.getFingerprint()) {
            for(int i = 0; i < zip2.getFingerprint().size(); i++) {
                
                if (element[0] == zip2.getFingerprint().get(i)[0]) {
                    matches++; // when two hash values match increase count
                }
            }
        }
        //Stores comparison results
        return new Results(zip1.getID(), zip2.getID(), Zip1count, Zip2count, matches);
    }

    // to compare one submission to multiple submissions
    public static List<Results> compareOneToMany (Submission mainZip, Submission[] comparisonZips) {
        List<Results> comparedResults = new ArrayList<>();

        //runs comparison for each submission in our list
        for (Submission comparisonZip : comparisonZips) {
            Results result = compare(mainZip, comparisonZip);
            comparedResults.add(result);
        }
        return comparedResults;
    }

    //compares every submission against every other one
    public static List<Results> compareAll (Submission[] Zips) {
        List<Results> comparedResults = new ArrayList<>();

        //Comapare every submission pairs
        for (int i = 0; i < Zips.length; i++) {
            for (int j = 0; j < Zips.length; j++) {
                //stops submission with being compared to itself
                if (i != j) {
                    Results result = compare(Zips[i],Zips[j]);
                    comparedResults.add(result);
                }
            }
        }
        return comparedResults;
    }
}
