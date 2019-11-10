import co.signal.loadgen.SyntheticLoadGenerator;
import java.util.Random;
public class LoadGenerator implements SyntheticLoadGenerator {

  public LoadGenerator(String ignored) {}

  @Override
  public String nextMessage() {
  	Random r = new Random();
  	return "{\"measurement\": \"pollution\", \"id\": "+r.ints(0,(10000+1)).findFirst().getAsInt()+", \"long\": "+ r.ints(0,(100+1)).findFirst().getAsInt() +", \"lat\": "+ r.ints(0,(100+1)).findFirst().getAsInt() +", \"humidity\": 50, \"temperature\" : "+ (r.nextFloat() * (30 - 1)) +", \"pm2\": "+ (1 + r.nextFloat() * (100 - 1)) +"}";
  }
}