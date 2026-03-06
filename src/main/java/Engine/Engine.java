package Engine;

import java.io.IOException;
import java.util.List;



import static Engine.ReadZip.*;
import static Engine.GenerateFingerprints.*;



public class Engine {

    private final int kSize;
    private final int wSize;


    public Engine(int kSize, int wSize) {
        this.kSize = kSize;
        this.wSize = wSize;
    }

    public Submission[] run(List<String> zipPaths) throws IOException {
        zipPaths.removeIf(this::check);
        Submission[] submissions = new Submission[zipPaths.size()];
        for(int i = 0; i < zipPaths.size(); i++) {
            submissions[i] = readZip(zipPaths.get(i));
            submissions[i].addHashes(hashes(submissions[i].getCode(), kSize));
            //submissions[i].removeCode();
            submissions[i].addFingerprint(fingerprint(submissions[i].getHashes(), wSize));
        }
        return submissions;
    }



    //will update this once we integrate cloud storage, such that it will check to see if a zip has already been analyzed
    public boolean check(String zipPath) {
        return false;
    }
}
