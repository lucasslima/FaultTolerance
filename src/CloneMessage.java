import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class CloneMessage implements Serializable{
	private static final long 	serialVersionUID = 1L;
	public static final int 	SAVED = 0;
	public static final int 	CONFIRMATION = 1; 
	public static final int 	ADD = 2;
	public static final int 	SAVE = 3; 
	public static final int 	REGISTER = 4; 
	public static final int 	NEW = 5; 
	private int 				type; 
	private List<Point> 		points;
	private String 				observer; 
	private List<String> 		observers; 
	private String 				cloneIp; 
	private int 				clonePort; 
	
	public List<String> getObservers() {
		return observers;
	}

	public void setObservers(List<String> observers) {
		this.observers = new ArrayList<String>(observers);
	}
	
	public int getClonePort() {
		return clonePort;
	}
	
	public void setClonePort(int clonePort) {
		this.clonePort = clonePort;
	}
	
	public String getCloneIp() {
		return cloneIp;
	}
	public void setCloneIp(String cloneIp) {
		this.cloneIp = cloneIp;
	}
	
	public String getObserver() {
		return observer;
	}
	public void setObserver(String clientIP) {
		this.observer = clientIP;
	}
	
	public int getType() {
		return type;
	}
	public void setType(int type) {
		this.type = type;
	}
	
	
	public List<Point> getPoints() {
		return points;
	}
	public void setPoints(List<Point> points) {
		this.points = points;
	} 
}
