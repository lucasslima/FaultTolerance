import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.sound.midi.Receiver;


public class Subject implements Observable {
	private static List<Point> 	points;
	private static List<Point> 	newPoints;
	private static List<String> observers;
	private static int 			numPoints = 200;
	private static ServerSocket serverSocket;
	private static Random 		random;
	private static final int 	port = 6969;
	private static final int 	minimum = 50;
	private static final int 	maximum = 150;
	private static int 			change;
	private static Subject		subject;
	private static boolean 		amIMaster; 
	private static String 		myCloneIP;
	private static String 		myMasterIP;
	private static long 		timedOut; 
	private static long 		receivedTimeMessage=0;
	
	public Subject() throws IOException{
		serverSocket = new ServerSocket(6970);
		observers = new ArrayList<String>();
		points = new ArrayList<Point>();
		newPoints = new ArrayList<Point>();
		random = new Random();
		amIMaster = true;
		myCloneIP = "200.239.138.205";
		myMasterIP = "200.239.139.32";
		timedOut = 0; 
		
		for (int i = 0; i < numPoints; i++) {
			Point point = new Point();
			setSeed();
			int x = random.nextInt(1000);
			int y = random.nextInt(1000);
			int cor = random.nextInt(5);
			point.setX(x);
			point.setY(y);
			point.setCor(cor);
			points.add(point);
		}
		
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
						while(amIMaster){
							setSeed();
							int seg = random.nextInt(500);
							Thread.sleep(seg);
							changePoints();
							sendMessageToClone();
						}
					}

				} catch (InterruptedException e) {					
					e.printStackTrace();
				}
			}
		}.start();
		
		if(!amIMaster){
			new Thread() {
				@Override
				public void run() {
					while(System.currentTimeMillis() - receivedTimeMessage < 700){
						
					}
					amIMaster = true;
				}
			}.start();
		}
		
	}

	public static void main(String[] args) throws IOException {
		subject = new Subject();
	}

	protected static void changePoints() {
		setSeed();
		change = random.nextInt(2);
		switch (change) {
		case 0:
			for (int i = 0; i < getNumPointToChange(); i++) {
				Point point = new Point();
				setSeed();
				int x = random.nextInt(1000);
				int y = random.nextInt(1000);
				int cor = random.nextInt(5);
				point.setX(x);
				point.setY(y);
				point.setCor(cor);
				points.add(point);
				newPoints.add(point);

			}
			numPoints = points.size();
			break;

		case 1:
			for (int i = 0; i < getNumpointtoremove(); i++) {
				int aux;
				setSeed();
				aux = random.nextInt(numPoints);
				newPoints.add(points.get(aux));
			}
			points.removeAll(newPoints);
			numPoints = points.size();
			break;
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
						Object object = in.readObject();
						
						if(amIMaster){
							if(object instanceof ObserverMessage)
								subject.receiveDataFromObservers(object);
							else 
								subject.receiveDataFromClone(object);
						}
						else 
							subject.receiveDataFromSubject(object);
						
					} catch (IOException e) {
						e.printStackTrace();
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					}
				}
			}.start();
		}
	}
	
	private void sendMessageToClone() {
		try{
			Socket s = new Socket(myCloneIP, port);
			
			ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
			
			CloneMessage message = new CloneMessage(); 
			message.setPoints(points);
			message.setType(CloneMessage.REQUEST);
			
			out.writeObject(message);
			out.flush();
			out.close();
			
			s.close();
		} catch(Exception e){
			
		}
	}

	private void sendMessageToClone(String ip) {
		try{
			Socket s = new Socket(myCloneIP, port);
			
			ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
			
			CloneMessage message = new CloneMessage(); 
			message.setObserver(ip);
			
			out.writeObject(message);
			out.flush();
			out.close();
			
			s.close();
		} catch(Exception e){
			
		}
	}
	
	private void sendMessageToSubject(){
		try{
			Socket s = new Socket(myMasterIP, port);
			
			ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
			
			CloneMessage message = new CloneMessage(); 
			message.setType(CloneMessage.ACK);
			
			out.writeObject(message);
			out.flush();
			out.close();
			
			s.close();
		} catch(Exception e){
			
		}
	}
	
	private void receiveDataFromSubject(Object object){
		CloneMessage message = (CloneMessage) object;
		switch(message.getType()){
			case CloneMessage.REQUEST: 	points = message.getPoints();
				 						sendMessageToSubject();
										break;
			
			default: observers.add(message.getObserver());
			 		 break;
		}
		receivedTimeMessage = System.currentTimeMillis();
		
	}
	
	private void receiveDataFromClone(Object object) throws IOException, ClassNotFoundException{
		CloneMessage message = (CloneMessage) object;
		switch(message.getType()){
			case CloneMessage.ACK: 	notifyObservers();
				 					newPoints.clear();
				 					break;
		}
	}
	
	private void receiveDataFromObservers(Object object) throws IOException, ClassNotFoundException{
		ObserverMessage message = (ObserverMessage) object;
		switch (message.getType()) {
		case 0:	subject.registerObserver(message.getIp());
				break;
		case 1:	subject.unregisterObserver(message.getIp());
				break;

		}
	}

	@Override
	public void registerObserver(String ip) throws UnknownHostException, IOException {
		observers.add(ip);
		notifyObserverJustRegistered(ip);
		sendMessageToClone(ip);
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
		setSeed();
		int n = maximum - minimum + 1;
		int i = random.nextInt() % n;
		int randomNum = minimum + i;
		return Math.abs(randomNum);
	}

	public static int getNumpointtoremove() {
		setSeed();
		int randomNum = random.nextInt(numPoints);
		return Math.abs(randomNum);
	}

	public static void setSeed(){
		long seed = System.currentTimeMillis();
		random.setSeed(seed);
	}
}
