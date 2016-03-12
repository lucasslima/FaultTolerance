import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.util.List;

import javax.swing.JFrame;

public class Frame extends JFrame{
	private static final long 	serialVersionUID = 1L;
	private List<Point> 		points; 
	private final int  			dimensionsFrame = 1000;
	private final int  			dimensionsDot   = 5;
	
	public Frame(List<Point> points){
		this.points = points;
		
		this.setPreferredSize(new Dimension(dimensionsFrame, dimensionsFrame));
        this.pack();
        this.setVisible(true);
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
	}
	
	public void setPoints(List<Point> points) {
		this.points = points;
	}
	
	 @Override
	    public void paint(Graphics g) {
	        super.paint(g);

	        for(Point point : points){
	        	// define the position
		        int locX = point.getX();
		        int locY = point.getY();
		        
		        switch(point.getCor()){
			        case 0: g.setColor(Color.BLACK);
			        	break;
			        case 1:g.setColor(Color.YELLOW);
			        	break;
			        case 2: g.setColor(Color.RED);
			        	break;
			        case 3: g.setColor(Color.BLUE);
			        	break;
			        case 4: g.setColor(Color.ORANGE);
			        	break;
		        }
	
		        // draw a line (there is no drawPoint..)
		        g.drawLine(locX, locY, locX, locY); 
		        g.fillRect(locX, locY,
		        		dimensionsDot, dimensionsDot);
	        }
	    }
}
