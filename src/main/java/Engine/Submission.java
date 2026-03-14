package Engine;


import java.util.List;

public class Submission { 

    private final String ID; //Unique submission identifier 
    private final String language; // identifies of coding language between (java, c and cpp)

    private List<int[]> fingerprint; // list of fingerprint hashes, chosen by winnowing algorithm
    private int fingerPrintSize; // number of fingerprints

    public Submission(String ID, String language, List<int[]> fingerprint) {
        this.ID = ID;
        this.language = language;
        this.fingerprint = fingerprint;
    }

    //Returns Submission ID
    public String getID() { return ID; }
    
    //Returns the language
    public String getLanguage() { return language; }
    
    //adds new fingerprint to list and increase size
    public void addFingerprint(List<int[]> fingerprint){
        this.fingerprint = fingerprint;
        this.fingerPrintSize = fingerprint.size();
    }
    //returns list of fingerprints
    public List<int[]> getFingerprint(){return  fingerprint;}
    //returns the number of fingerprints
    public int getFingerPrintSize(){return fingerPrintSize;}
}
