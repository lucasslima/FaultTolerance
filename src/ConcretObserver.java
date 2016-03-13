//import java.io.IOException;
//import java.io.ObjectInputStream;
//import java.io.ObjectOutputStream;
//import java.net.ServerSocket;
//import java.net.Socket;
//import java.util.ArrayList;
//import java.util.List;
//
//
//public class ConcretObserver implements Observer{
//	private static ServerSocket 	server;
//	private static final int 		portSubject = 6969;
//	private static final int 		portObserver = 6970;
//	private static List<Point> 		points;
//	private static String 			ipServer = "localhost";
//	private static Frame 			frame;
//	private static ConcretObserver	concretObserver;
//	
//	public ConcretObserver() throws IOException{
//		server 		= new ServerSocket(portObserver);
//		points 		= new ArrayList<Point>();
//		frame 		= new Frame(new ArrayList<Point>());
//		
//		new Thread() {
//			@Override
//			public void run() {
//				try {
//					listen();
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
//			}
//		}.start();
//		register();
//	}
//
//	public static void main(String[] args) throws IOException {
//		concretObserver = new ConcretObserver();
//	}
//
//	protected static void listen() throws IOException {
//		while(true){
//			Socket socket = server.accept();
//			
//			new Thread() {
//				@Override
//				public void run() {
//					try {
//						ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
//						
//						SubjectMessage message = (SubjectMessage) in.readObject();
//						int type = message.getType();
//						List<Point> newPoints = message.getPoints();
//						
//						if(message.getType() == 2){
//							System.out.println("Getting new ip");
//							ipServer = message.getIp();
//						}
//						else
//							concretObserver.update(type,newPoints);
//						
//						socket.close();
//						
//					} catch (IOException | ClassNotFoundException e) {
//						e.printStackTrace();
//					}
//	
//				}
//			}.start();
//		}
//	}
//
//	@Override
//	public void update(int type,List<Point> newPoints) {
//		
//		switch(type){
//			case 0: points.addAll(newPoints);
//					break;
//			case 1: for(Point point : newPoints)
//						points.remove(point.getIndex());
//					break;
//		}
//		
//		frame.setPoints(points);
//		frame.revalidate();
//		frame.repaint();
//	}
//	
//	@SuppressWarnings("resource")
//	public static void register() throws IOException {
//		Socket s = new Socket(ipServer, portSubject);
//		
//		ObserverMessage message = new ObserverMessage();
//		message.setIp(s.getLocalAddress().getHostAddress());
//		message.setType(0);
//		
//		ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
//		out.writeObject(message);
//		out.flush();
//		out.close();
//	}
//
//	@SuppressWarnings("resource")
//	public static void unRegister() throws IOException {
//		Socket s = new Socket(ipServer, portSubject);
//
//		ObserverMessage message = new ObserverMessage();
//		message.setIp(s.getLocalAddress().getHostAddress());
//		message.setType(1);
//		
//		ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
//		out.writeObject(message);
//		out.flush();
//		out.close();
//
//	}
//}

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;


public class ConcretObserver implements Observable, Observer {
	private static List<Point> 			points;
	private static List<Point> 			newPoints;
	private static List<String> 		observers;
	private static Map<String,Integer> 	clones;
	private static int 					numPoints = 500;
	private static final int 			minimum = 150;
	private static final int 			maximum = 350;
	private static ServerSocket 		serverSocket;
	private static Random 				random;
	private static final int 			portMaster = 6969;
	private static final int 			portObserver = 6970;
	private static final int 			portClone = 6971;
	private static int 					change;
	private static ConcretObserver		myInstance;
	private static Thread 				threadToCheckMaster;
	private static Thread 				threadMaster;
	private static Thread 				threadObserver;
	private static Thread 				threadClone;
	private static String 				masterIp;
	private static String 				cloneIp;
	private static boolean 				amIMaster; 
	private static long 				timeLastMessage; 
	private static boolean				firstCommunication;
	private static Frame 				frame;
	
	public ConcretObserver() throws IOException{
		serverSocket 		= new ServerSocket(portObserver);
		observers 			= new ArrayList<String>();
		points 				= new ArrayList<Point>();
		newPoints 			= new ArrayList<Point>();
		clones				= new TreeMap<String,Integer>();
		random 				= new Random();
		amIMaster			= false; 
		masterIp			= "localhost";
		firstCommunication  = true; 
				
		threadObserver = new Thread(){
			@Override
			public void run(){
				try {
					startObserver();
				} catch (UnknownHostException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		};
		threadObserver.start();
	}

	public static void main(String[] args) throws IOException {
		myInstance = new ConcretObserver();
	}

	/**
	 * USADA SOMENTE PELO CLONE
	 * Essa função é responsável por iniciar o cloen 
	 * Ela cria uma thread para esperar por novas conexões 
	 * E outra thread para verificar se o master está ativo
	 */
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
		
		threadToCheckMaster = new Thread(){
			@Override
			public void run(){
				while((System.currentTimeMillis() - timeLastMessage) < 700);
				System.out.println("I am the master now");
				try {
					setNewMaster();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		};
	}
	
	/**
	 * USADA SOMENTE PELO MASTER
	 * Essa função é responsável por iniciar o master
	 * Ela cria uma nova thread para esperar por novas conexões 
	 * E cria uma outra thread para mudar o conjunto de pontos aleatóriamente
	 */
	private static void startMaster(){
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
//						sendMessageToClone(CloneMessage.SAVE,null);
						myInstance.notifyObservers(0);
					}

				} catch (InterruptedException e) {					
					e.printStackTrace();
				}
			}
		}.start();
	}
	
	
	private static void startObserver() throws UnknownHostException, IOException{
		frame = new Frame(new ArrayList<Point>());
		
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
		myInstance.register();
	}
	
	/**
	 * USADA SOMENTE PELO MASTER
	 * Essa função modifica o conjunto de pontos atuais 
	 * Ela pode adicionar novos pontos ou remover os já existentes
	 */
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
			for (int i = 0; i < getNumPointToRemove(); i++) {
				int aux;
				aux = random.nextInt(numPoints);
				newPoints.add(currentPoints[aux]);
			}
			points.removeAll(newPoints);
			numPoints = points.size();
			break;
		}
	}

	/**
	 * USADA SOMENTE PELO CLONE
	 * Essa função é executada por uma thread que fica esperando por novas conexões com o clone 
	 * Essa conexão só pode ser feita pelo master
	 * @throws IOException
	 */
	public static void listenFromMaster() throws IOException{
		while(!amIMaster){ 
			Socket s = serverSocket.accept();
			new Thread() {
				@Override
				public void run() {
					try {
						ObjectInputStream in = new ObjectInputStream(s.getInputStream());
						Object object = in.readObject();
						
						getMessageFromMaster(object);
						
						
					} catch (IOException e) {
						e.printStackTrace();
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					}
				}
			}.start();
		}
	}
	
	/**
	 * USADA SOMENTE PELO MASTER
	 * Essa função é executada por uma thread que fica esperando por novas conexões com o master 
	 * Essa conexão pode ser feita por um clone ou por um observer
	 * @throws IOException
	 */
	protected static void listen() throws IOException {
		while(true){
			Socket client = serverSocket.accept();
			new Thread() {
				@Override
				public void run() {
					try {
						ObjectInputStream in = new ObjectInputStream(client.getInputStream());
						Object object = in.readObject();
												
						if(object instanceof ObserverMessage)
							getMessageFromObserver(object);
						else if(object instanceof CloneMessage)
							getMessageFromClone(object);
						else if(object instanceof SubjectMessage)
							getMessageFromMaster(object);
						
					} catch (IOException e) {
						e.printStackTrace();
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					}
				}
			}.start();
		}
	}
	
	/**
	 * Essa função é chamada quando o master é desligado
	 * O clone agora assumirá o controle
	 * @throws InterruptedException 
	 */
	private static void setNewMaster() throws InterruptedException{
		threadClone.interrupt();
		amIMaster = true;
		cloneIp = observers.get(0);
		//TODO sent message to observer to make him a clone
		clones.put(cloneIp, portObserver);
		threadMaster = new Thread(){
			@Override
			public void run(){
				startMaster();
			}
		};
		threadMaster.start();
		myInstance.notifyObservers(1);
		
		
	}
	
	/**
	 * USADA SOMENTE PELO CLONE
	 * Essa função recebe uma mensagem do master
	 * Essa mensagem pode ser para adicionar um novo observer
	 * Ou para fazer uma cópia do comjunto de pontos do master
	 * @param object é a mensagem que foi recebida
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	private static void getMessageFromMaster(Object object) throws UnknownHostException, IOException{
		if(object instanceof CloneMessage){
			CloneMessage message = (CloneMessage) object;
			
			switch(message.getType()){										
				case CloneMessage.ADD: 		observers.add(message.getObserver());
											break;
											
				case CloneMessage.SAVE:		timeLastMessage = System.currentTimeMillis();
											if(firstCommunication){
												firstCommunication = false; 
												threadToCheckMaster.start();
											}
											points.clear();
											points = message.getPoints();
											sendMessageToMaster();
											break;
			}
		} else {
			SubjectMessage message = (SubjectMessage) object;
			
			int type = message.getType();
			List<Point> newPoints = message.getPoints();
			
			if(message.getType() == 2){
				System.out.println("Getting new ip");
				masterIp = message.getIp();
			}
			else
				myInstance.update(type,newPoints);
		}
	}
	
	/**
	 * USADA SOMENTE PELO MASTER
	 * Essa função recebe uma mensagem de um observer
	 * Essa mensagem pode ser para registrar um novo observer
	 * Ou remover um observer já registrado
	 * @param object é a mensagem que foi recebida
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	private static void getMessageFromObserver(Object object) throws UnknownHostException, IOException{
		ObserverMessage message = (ObserverMessage) object;
		switch (message.getType()) {
			case 0:	myInstance.registerObserver(message.getIp());
					break;
			case 1:	myInstance.unregisterObserver(message.getIp());
					break;
		}		
	}
	
	/**
	 * USADA SOMENTE PELO MASTER
	 * Essa função recebe uma mensagem do clone 
	 * Essa mensagem é a confirmação que os dados foram salvos com sucesso
	 * Após a confirmação, o master notifica todos os observers com os novos pontos
	 * @param object é a mensagem que foi recebida
	 */
	private static void getMessageFromClone(Object object){
		CloneMessage message = (CloneMessage) object;
		
		switch(message.getType()){
			case CloneMessage.REGISTER: clones.put(message.getCloneIp(), message.getClonePort());
										break;
										
			case CloneMessage.SAVED: 	myInstance.notifyObservers(0);
									 	break;
		}
	}
	
	/**
	 * USADA SOMENTE PELO CLONE
	 * Essa função envia uma mensagem para o clone com uma cópia do conjunto de pontos
	 * Após ser enviado o master aguarda uma confirmação do clone para então enviar 
	 * os pontos para todos os observers
	 */
	private static void sendMessageToMaster() {
		Socket s;
		try {
			s = new Socket(masterIp, portMaster);
			CloneMessage message = new CloneMessage();
			message.setType(CloneMessage.SAVED);
			
			ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
			out.writeObject(message);
			out.flush();
			out.close();
			
			s.close();	
		} catch (UnknownHostException e) {
			
		} catch (IOException e) {
			
		}
	}

	/**
	 * USADA SOMENTE PELO MASTER
	 * Essa função envia uma mensagem para o clone com uma cópia do conjunto de pontos
	 * Após ser enviado o master aguarda uma confirmação do clone para então enviar 
	 * os pontos para todos os observers
	 */
	private static void sendMessageToClone(int type, String ip){
		Socket s;
		try {
			s = new Socket(cloneIp, clones.get(cloneIp));
			
			CloneMessage message = new CloneMessage();

			switch(type){
			case CloneMessage.ADD: message.setType(CloneMessage.ADD);
					message.setObserver(ip);
					break;
			case CloneMessage.SAVE: message.setType(CloneMessage.SAVE);
					message.setPoints(points);
					break;
			}
			
			ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
			out.writeObject(message);
			out.flush();
			out.close();
			
			s.close();	
		} catch (IOException e) {
			System.out.println("No connection");
			updateClones();
			myInstance.notifyObservers(0);
		}
	}
	
	private static void updateClones(){
		clones.remove(cloneIp);
		if(clones.size() >= 1){
			leaveFor1:
			for(String key : clones.keySet()){
				cloneIp = key; 
				break leaveFor1;
			}
		} else {
			int index = 0; 
			while(cloneIp.equals(observers.get(index)))
				cloneIp = observers.get(++index);
			//TODO send message to clone
		}
	}
	
	/**
	 * USADA SOMENTE PELO MASTER
	 * Essa função insere um novo observer na lista 
	 * @param ip do observer que deseja ser inserido 
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	@Override
	public void registerObserver(String ip) throws UnknownHostException, IOException {
		observers.add(ip);
		notifyObserverJustRegistered(ip);
		sendMessageToClone(CloneMessage.ADD,ip);
	}

	/**
	 * USADA SOMENTE PELO MASTER
	 * Essa função remove um observer da lista 
	 * @param ip do observer que deseja ser removido
	 */
	@Override
	public void unregisterObserver(String ip) {
		int index = observers.indexOf(ip);
		observers.remove(index);
	}
	
	/**
	 * USADA SOMENTE PELO MASTER
	 * Essa função envia uma mensagem com o conjunto de pontos que acabou de ser atualizado 
	 * para todos os observers cadastrados 
	 */
	@Override
	public void notifyObservers(int type) {
		for (String ip : observers) {
			try {
				Socket s = new Socket(ip, portObserver);
				ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
				
				SubjectMessage message = new SubjectMessage();
				
				switch(type){
					case 0:	message.setPoints(newPoints);
							message.setType(change);
							break;
					case 1: message.setIp(serverSocket.getInetAddress().getHostAddress());
							message.setType(2);
							break;
				}
				
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
	
	
	/**
	 * USADA SOMENTE PELO MASTER
	 * Essa função envia o conjunto de pontos atuais para o Observer que acabou de se registrar
	 * @param ip => recebe como parâmetro o ip do Observer
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	private void notifyObserverJustRegistered(String ip) throws UnknownHostException, IOException{
		Socket s = new Socket(ip,portObserver);
		
		ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
		SubjectMessage message = new SubjectMessage();
		message.setType(0);
		message.setPoints(points);
		
		out.writeObject(message);
		out.flush();
		out.close();
		s.close();
	}

	
	/**
	 * USADA SOMENTE PELO MASTER
	 * Essa função sorteia aleatóriamente um número de pontos a serem inseridos no cojunto de pontos já existente
	 * @return a quantidade de pontos a serem inseridos
	 */
	public static int getNumPointToChange() {
		int n = maximum - minimum + 1;
		int i = random.nextInt() % n;
		int randomNum = minimum + i;
		return Math.abs(randomNum);
	}

	
	/**
	 * USADA SOMENTE PELO MASTER
	 * Essa função sorteia aleatóriamente um número de pontos para serem removidos do conjunto já existente
	 * @return a quantidade de pontos a serem removidos
	 */
	public static int getNumPointToRemove() {
		int randomNum = random.nextInt(numPoints);
		return Math.abs(randomNum);
	}
	
	/**
	 * USADA SOMENTE PELO OBSERVER
	 * Essa função atualiza o frame com o novo conjunto de pontos
	 */
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

	/**
	 * USADA SOMENTE PELO OBSERVER
	 * Essa função é usada para inserir um novo observer 
	 */
	@Override
	public void register() throws UnknownHostException, IOException {
		Socket s = new Socket(masterIp, portMaster);
		
		ObserverMessage message = new ObserverMessage();
		message.setIp(s.getLocalAddress().getHostAddress());
		message.setType(0);
		
		ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
		out.writeObject(message);
		out.flush();
		out.close();
		
		s.close();
	}

	/**
	 * USADA SOMENTE PELO OBSERVER
	 * Essa função é usada para remover um observer 
	 */
	@SuppressWarnings("resource")
	@Override
	public void unregister() throws IOException {
		Socket s = new Socket(masterIp, portMaster);

		ObserverMessage message = new ObserverMessage();
		message.setIp(s.getLocalAddress().getHostAddress());
		message.setType(1);
		
		ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
		out.writeObject(message);
		out.flush();
		out.close();

	}
}

