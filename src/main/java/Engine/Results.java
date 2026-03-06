package Engine;

public class Results {
    public final String Zip1;
    public final String Zip2;
    public final int Zip1FingerprintCount;
    public final int Zip2FingerprintCount;
    public final int matchingFingerprints;
    public final double jaccardSimilarity;
    public final double containsZip1; // what % of Zip1 appears in Zip2
    public final double containsZip2; // what % of Zip2 appears in Zip1


    public Results(String Zip1, String Zip2, int Zip1Count, int Zip2Count, int matches) {
        this.Zip1 = Zip1;
        this.Zip2 = Zip2;
        this.Zip1FingerprintCount = Zip1Count;
        this.Zip2FingerprintCount = Zip2Count;
        this.matchingFingerprints = matches;

        // Jaccard Similarity: |A ∩ B| / |A ∪ B|
        int union = Zip1Count + Zip2Count - matches;
        this.jaccardSimilarity = (double) matches / union;

        //Contains:
        this.containsZip1 = (double) matches / Zip1Count;
        this.containsZip2 = (double) matches / Zip2Count;
    }

    public int returnZip1FingerprintCount () {return Zip1FingerprintCount;}
    public int returnZip2FingerprintCount () {return Zip2FingerprintCount;}
    public int returnMatchingFingerprints () {return matchingFingerprints;}
    public double returnJaccardSimilarity () {return jaccardSimilarity;}
    public double returnContainsZip1() {return containsZip1;}
    public double returnContainsZip2 () {return containsZip2;}
}
