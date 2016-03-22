import java.io.Serializable;
import java.util.List;

public class SubjectMessage implements Serializable{
	private static final long serialVersionUID = 1L;
	private List<Point> points;
	private int 		type;
	private String 		ip;

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public void setPoints(List<Point> points) {
		this.points = points;
	}

	public List<Point> getPoints() {
		return points;
	}
}
