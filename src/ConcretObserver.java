import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;


public class ConcretObserver implements Observer{
	private static ServerSocket 	server;
	private static final int 		portSubject = 6969;
	private static final int 		portObserver = 6970;
	private static List<Point> 		points;
	private static String 			ipServer = "localhost";
	private static Frame 			frame;
	private static ConcretObserver	concretObserver;
	
	public ConcretObserver() throws IOException{
		server 		= new ServerSocket(portObserver);
		points 		= new ArrayList<Point>();
		frame 		= new Frame(new ArrayList<Point>());
		
		new Thread() {
			@Override
			public void run() {
				try {
					listen();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}.start();
		register();
	}

	public static void main(String[] args) throws IOException {
		concretObserver = new ConcretObserver();
	}

	protected static void listen() throws IOException {
		while(true){
			Socket socket = server.accept();
			
			new Thread() {
				@Override
				public void run() {
					try {
						ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
						
						SubjectMessage message = (SubjectMessage) in.readObject();
						int type = message.getType();
						List<Point> newPoints = message.getPoints();
						
						if(message.getType() == 2){
							System.out.println("Getting new ip");
							ipServer = message.getIp();
						}
						else
							concretObserver.update(type,newPoints);
						
						socket.close();
						
					} catch (IOException | ClassNotFoundException e) {
						e.printStackTrace();
					}
	
				}
			}.start();
		}
	}

	@Override
	public void update(int type,List<Point> newPoints) {
		
		switch(type){
			case 0: points.addAll(newPoints);
					break;
			case 1: for(Point point : newPoints)
						points.remove(point.getIndex());
					break;
		}
		
		frame.setPoints(points);
		frame.revalidate();
		frame.repaint();
	}
	
	@SuppressWarnings("resource")
	public static void register() throws IOException {
		Socket s = new Socket(ipServer, portSubject);
		
		ObserverMessage message = new ObserverMessage();
		message.setIp(s.getLocalAddress().getHostAddress());
		message.setType(0);
		
		ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
		out.writeObject(message);
		out.flush();
		out.close();
	}

	@SuppressWarnings("resource")
	public static void unRegister() throws IOException {
		Socket s = new Socket(ipServer, portSubject);

		ObserverMessage message = new ObserverMessage();
		message.setIp(s.getLocalAddress().getHostAddress());
		message.setType(1);
		
		ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
		out.writeObject(message);
		out.flush();
		out.close();

	}
}
