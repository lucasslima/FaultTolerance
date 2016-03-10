import java.io.IOException;
import java.net.UnknownHostException;
import java.util.List;

public interface Observable {
	public void registerObserver(String ip) throws UnknownHostException, IOException;
    public void unregisterObserver(String ip);
    public void notifyObservers(); 
}
