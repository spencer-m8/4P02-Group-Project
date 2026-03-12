package Engine;


import java.util.List;

public class Submission {

    private final String ID;
    private final String language;

    private List<int[]> fingerprint;
    private int fingerPrintSize;


    public Submission(String ID, String language, List<int[]> fingerprint) {
        this.ID = ID;
        this.language = language;
        this.fingerprint = fingerprint;
    }


    public String getID() { return ID; }
    public String getLanguage() { return language; }

    public void addFingerprint(List<int[]> fingerprint){this.fingerprint = fingerprint;this.fingerPrintSize = fingerprint.size();}
    public List<int[]> getFingerprint(){return  fingerprint;}
    public int getFingerPrintSize(){return fingerPrintSize;}
}