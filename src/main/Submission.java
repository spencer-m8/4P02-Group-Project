public class Submission {

    private final String anonId;
    private final String language;
    private final String code;

    public Submission(String anonId, String language, String code) {
        this.anonId = anonId;
        this.language = language;
        this.code = code;
    }

    public String getAnonId() { return anonId; }
    public String getLanguage() { return language; }
    public String getCode() { return code; }
}