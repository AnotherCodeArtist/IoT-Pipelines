import co.signal.loadgen.SyntheticLoadGenerator;
import java.util.Random;

public class SlopeGenerator implements SyntheticLoadGenerator {

    int iteration=0;
    float startValue = 22;
    float stepSize = 0.5f;
    public SlopeGenerator(String ignored) { }

    @Override
    public String nextMessage() {
        Random r = new Random();
        iteration++;
        return "{\"measurement\": \"pollution\", \"id\": " + 128 + ", \"long\": " + 10 + ", \"lat\": " + 10 + ", \"humidity\": 50, \"temperature\" : " + (startValue+(iteration*stepSize)) + ", \"pm2\": " + (startValue-(iteration*stepSize)) + "}";
    }
}