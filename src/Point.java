import java.io.Serializable;

public class Point implements Serializable, Comparable<Point>{
	private static final long serialVersionUID = 1L;
	private int index; 
	private int id;
	private int cor;
	private int x;
	private int y;
	
	public int getIndex() {
		return index;
	}
	
	public void setIndex(int index) {
		this.index = index;
	}
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}

	
	public int getCor() {
		return cor;
	}
	public void setCor(int cor) {
		this.cor = cor;
	}
	public int getX() {
		return x;
	}
	public void setX(int x) {
		this.x = x;
	}
	public int getY() {
		return y;
	}
	public void setY(int y) {
		this.y = y;
	}
	
	@Override
	public int compareTo(Point o) {

		Point p = (Point)o;
	    if(this.x == p.getX() && y == p.getY())
	    	return 0; 
	    else
	    	return -1;	    		
	 }

	
	
}
