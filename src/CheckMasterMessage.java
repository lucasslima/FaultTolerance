import java.io.Serializable;


public class CheckMasterMessage implements Serializable{
	private static final long 	serialVersionUID = 1L;
	public static final int 	SUBJECT = 0;
	public static final int 	CHECK = 1;
	private int 				type;  
	private String 				IP;
	
	public int getType() {
		return type;
	}
	
	public void setType(int type) {
		this.type = type;
	}
	
	public String getIP() {
		return IP;
	}
	
	public void setIP(String iP) {
		IP = iP;
	}
	
	public static long getSerialversionuid() {
		return serialVersionUID;
	}
}
