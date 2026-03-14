package Engine;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static Engine.FingerPrintGenerator.*;

public class Engine {

    private final int k;   //k size in k-gram, k tokens ro form k-gram to be hashed
    private final int w;   //w is used for window size in the winnowing process to select fingerprints

    public Engine(int k, int t) {
        this.k = k;
        
        if (k > t) {   // Winnowing requires t to greater than or equal to k
            throw new IllegalArgumentException("k cannot be greater than t");
        }
        this.w = t - k + 1;
    }

    //runs the FingerPrintGenerator for all zip submissions
    public Submission[] run(List<Path> zipPaths) throws IOException {
        return FingerPrintGenerator.generateFingerprints(zipPaths, k, w);
    }


    //will update this once we integrate cloud storage, such that it will check to see if a zip has already been analyzed
    public boolean check(String zipPath) {
        return false;
    }
}
