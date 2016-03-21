import java.io.Serializable;
public class ObserverMessage implements Serializable{
	
	private static final long serialVersionUID = 1L;
	private int type;
	private String ip;
	
	
	public int getType() {
		return type;
	}
	
	public void setType(int type) {
		this.type = type;
	}
	
	public String getIp() {
		return ip;
	}
	
	public void setIp(String ip) {
		this.ip = ip;
	}

}
