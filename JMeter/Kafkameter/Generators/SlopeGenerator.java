import co.signal.loadgen.SyntheticLoadGenerator;
import java.util.Random;
import java.lang.Math;

public class SlopeGenerator implements SyntheticLoadGenerator {

    int iteration=0;
    float startValue = 0;
    float stepSize = 0.5f;
    final int latI;
    final int longI;
    final int id;
    public SlopeGenerator(java.lang.Integer threadNumber) { 
    	id = threadNumber-1;
        latI = (id/100)+1;
        longI = (id%100)+1;
    }
    @Override
    public String nextMessage() {
        Random r = new Random();
        iteration++;
       
        return "{\"measurement\": \"pollution\", \"sensorId\": \"" + id + "\", \"lon\": " + longI + ", \"lat\": " + latI + ", \"humidity\": 50, \"temperature\" : " + (startValue+(iteration*stepSize)) + ", \"pm2\": " + (startValue+(iteration*stepSize)) + "}";
    }
    @Override
    public String nextTag()
    {
    	return "{\"sensor\":"+ id +"}";
    }
}