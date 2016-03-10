import java.io.Serializable;
import java.util.List;

public class CloneMessage implements Serializable{
	private static final long 	serialVersionUID = 1L;
	public static final int 	ACK = 0;
	public static final int 	REQUEST = 1; 
	private int 				type; 
	private List<Point> 		points;
	private String 				observer; 
	
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
