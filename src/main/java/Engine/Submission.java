package Engine;

import java.util.List;

public class Submission {

    private String ID;
    private String language;
    private String code;
    private List<Integer> hashes;
    private List<Integer> fingerprint;


    public Submission(String ID, String language, String code) {
        this.ID = ID;
        this.language = language;
        this.code = code;
    }


    public void addID (String ID) {this.ID = ID;}
    public String getID() { return ID; }
    public void addLanguage(String language) {this.language = language;}
    public String getLanguage() { return language; }
    public void removeCode() {this.code = null;}
    public String getCode() { return code; }
    public void addHashes(List<Integer> hashes){this.hashes = hashes;}
    public List<Integer> getHashes(){return  hashes;}
    public void addFingerprint(List<Integer> fingerprint){this.fingerprint = fingerprint;}
    public List<Integer> getFingerprint(){return  fingerprint;}


}