import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class Subject implements Observable {
	private static List<Point> 	points;
	private static List<Point> 	newPoints;
	private static List<String> observers;
	private static int 			numPoints = 500;
	private static ServerSocket serverSocket;
	private static Random 		random;
	private static final int 	port = 6969;
	private static final int 	minimum = 150;
	private static final int 	maximum = 350;
	private static int 			change;
	private static Subject		subject;
	private static Thread		threadServer; 
	private static Thread 		threadClone;
	private static String 		masterIp;
	private static boolean 		amIMaster; 
	
	public Subject() throws IOException{
		serverSocket 		= new ServerSocket(6970);
		observers 			= new ArrayList<String>();
		points 				= new ArrayList<Point>();
		newPoints 			= new ArrayList<Point>();
		random 				= new Random();
		amIMaster			= true; 
		
		if(amIMaster){
			for (int i = 0; i < numPoints; i++) {
				Point point = new Point();
				int x = random.nextInt(1000);
				int y = random.nextInt(1000);
				int cor = random.nextInt(5);
				point.setX(x);
				point.setY(y);
				point.setCor(cor);
				points.add(point);
			}
			startMaster();
		} else 
			startClone();
	}

	public static void main(String[] args) throws IOException {
		subject = new Subject();
	}

	public static void startClone(){
		new Thread(){
			@Override
			public void run(){
				try {
					listenFromMaster();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}.start();
	}
	
	public void startMaster(){
		//inicia o servidor para ficar ouvindo os clientes em uma thread 
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
		
		//inicia uma nova thread para aleatoriamente adicionar ou remover pontos e enviar os novos pontos para os observers
		new Thread() {
			@Override
			public void run() {
				try {
					while(true){
						int seg = random.nextInt(350);
						Thread.sleep(seg);
						changePoints();
						notifyObservers();
					}

				} catch (InterruptedException e) {					
					e.printStackTrace();
				}
			}
		}.start();
	}
	
	protected static void changePoints() {
		change = random.nextInt(2);
		switch (change) {
		case 0:
			for (int i = 0; i < getNumPointToChange(); i++) {
				Point point = new Point();
				int x = random.nextInt(1000);
				int y = random.nextInt(1000);
				int cor = random.nextInt(5);
				point.setX(x);
				point.setY(y);
				point.setCor(cor);
				newPoints.add(point);

			}
			points.addAll(newPoints);
			numPoints = points.size();
			break;

		case 1:
			Point [] currentPoints = (Point[]) points.toArray(new Point[numPoints]);
			for (int i = 0; i < getNumpointtoremove(); i++) {
				int aux;
				aux = random.nextInt(numPoints);
				newPoints.add(currentPoints[aux]);
			}
			points.removeAll(newPoints);
			numPoints = points.size();
			break;
		}
	}

	public static void listenFromMaster() throws IOException{
		while(true){ 
			Socket s = serverSocket.accept();
			new Thread() {
				@Override
				public void run() {
					try {
						ObjectInputStream in = new ObjectInputStream(s.getInputStream());
						Object object = in.readObject();
						
						if(object instanceof CloneMessage){
							CloneMessage message = (CloneMessage) object;
							
							switch(message.getType()){
								case 0: observers.add(message.getObserver());
										break;
								case 1: points.clear();
										points = message.getPoints();
										sendMessageToMaster();
										break;
							}
						}
						
						
					} catch (IOException e) {
						e.printStackTrace();
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					}
				}
			}.start();
		}
	}
	
	protected static void listen() throws IOException {
		while(true){
			Socket client = serverSocket.accept();
			new Thread() {
				@Override
				public void run() {
					try {
						ObjectInputStream in = new ObjectInputStream(client.getInputStream());
						
						ObserverMessage message = (ObserverMessage) in.readObject();;
						switch (message.getType()) {
						case 0:	subject.registerObserver(message.getIp());
								break;
						case 1:	subject.unregisterObserver(message.getIp());
								break;

						}
						
					} catch (IOException e) {
						e.printStackTrace();
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					}
				}
			}.start();
		}
	}
	
	private static void getMessageFromSubject(){
		
	}
	
	private static void getMessageFromClone(){
		
	}
	
	private static void sendMessageToMaster() throws UnknownHostException, IOException{
		Socket s = new Socket(masterIp, port);
		
		CloneMessage message = new CloneMessage();
		message.setType(CloneMessage.ACK);
		
		ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
		out.writeObject(message);
		out.flush();
		out.close();
		
		s.close();	
	}
	
	@Override
	public void registerObserver(String ip) throws UnknownHostException, IOException {
		observers.add(ip);
		notifyObserverJustRegistered(ip);
	}

	@Override
	public void unregisterObserver(String ip) {
		int index = observers.indexOf(ip);
		observers.remove(index);
	}

	@Override
	public void notifyObservers() {
		for (String ip : observers) {
			try {
				Socket s = new Socket(ip, port);
				ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
				
				SubjectMessage message = new SubjectMessage();
				message.setPoints(newPoints);
				message.setType(change);
				
				out.writeObject(message);
				out.flush();
				out.close();
				s.close();
				
			} catch (IOException e) {
				observers.remove(ip);
			}
		}
		newPoints.clear();
	}
	
	private void notifyObserverJustRegistered(String ip) throws UnknownHostException, IOException{
		Socket s = new Socket(ip,port);
		
		ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
		SubjectMessage message = new SubjectMessage();
		message.setType(0);
		message.setPoints(points);
		
		out.writeObject(message);
		out.flush();
		out.close();
		s.close();
	}

	public static int getNumPointToChange() {
		int n = maximum - minimum + 1;
		int i = random.nextInt() % n;
		int randomNum = minimum + i;
		return Math.abs(randomNum);
	}

	public static int getNumpointtoremove() {
		int randomNum = random.nextInt(numPoints);
		return Math.abs(randomNum);
	}

}
