import java.io.IOException;
import java.net.UnknownHostException;
import java.util.List;

public interface Observer {
	public void update(int type,List<Point> points);
	public void register() throws UnknownHostException, IOException;
	public void unregister() throws IOException;
}
