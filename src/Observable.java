import java.io.IOException;
import java.net.UnknownHostException;

public interface Observable {
	public void registerObserver(String ip) throws UnknownHostException, IOException;
    public void unregisterObserver(String ip);
    public void notifyObservers(int type); 
}
