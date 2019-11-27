import co.signal.loadgen.SyntheticLoadGenerator;
import java.util.Random;

public class LoadGeneratorMQTT implements SyntheticLoadGenerator {

  public LoadGeneratorMQTT (String ignored) {}

  @Override
  public String nextMessage() {
  	Random r = new Random();
  	return "{\"measurement\": \"pollution\", \"id\": "+ r.ints(0,(12760+1)).findFirst().getAsInt()+ ", \"long\": " + (r.nextFloat() * (47.067 - 47.064)) +", \"lat\": "+ (r.nextFloat() * (15.47 - 15.43)) +", \"humidity\": 50, \"temperature\" : "+ (r.nextFloat() * (30 - 1)) +", \"pm2\": "+ (1 + r.nextFloat() * (40 - 1)) +"}";
  }
}