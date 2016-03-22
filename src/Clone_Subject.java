import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Stack;

public class Clone_Subject implements Observable {
	private static List<Point> 			points;
	private static List<Point> 			newPoints;
	private static List<String> 		observers;
	private static int 					numPoints = 500;
	private static final int 			minimum = 150;
	private static final int 			maximum = 350;
	private static final int 			SUBJECT = 0;
	private static final int 			CLONE = 2;
	private static ServerSocket 		serverSocket;
	private static ServerSocket 		checkMasterSocket;
	private static Random 				random;
	private static final int 			port = 6969;
	private static final int 			checkMasterPort = 6970;
	private static int 					change;
	private static Clone_Subject		myInstance;
	private static Thread 				threadToCheckMaster;
	private static Thread 				threadMaster;
	private static Thread 				threadClone;
	private static String 				masterIp;
	private static String 				cloneIp;
	private static long 				timeLastMessageMaster; 
	private static int 					whoAmI; 
	private static final int 			UPDATE = 0;
	private static final int 			NEWMASTER = 1;
	
	public Clone_Subject() throws IOException{
		observers 			= Collections.synchronizedList(new ArrayList<String>());
		points 				= new ArrayList<Point>();
		newPoints 			= new ArrayList<Point>();
		random 				= new Random();
		masterIp			= null;
		cloneIp				= null;
		checkMasterSocket 	= new ServerSocket(checkMasterPort);
		
		checkMasterSocket.setSoTimeout(3000);
		
		checkExistingMasters();
				
		switch(whoAmI){
			case SUBJECT: 	serverSocket = new ServerSocket(port);
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
							threadMaster = new Thread(){
								@Override
								public void run(){
									startMaster();
								}
							};
							threadMaster.start();
							break;
							
			case CLONE: 	serverSocket = new ServerSocket(port);
							threadClone = new Thread(){
								@Override
								public void run(){
									startClone();
								}
							};
							threadClone.start();
							break;
		}
	}

	public static void main(String[] args) throws IOException {
		myInstance = new Clone_Subject();
	}

	/**
	 * USADA SOMENTE PELO CLONE
	 * Essa função é responsável por iniciar o cloen 
	 * Ela cria uma thread para esperar por novas conexões 
	 * E outra thread para verificar se o master está ativo
	 */
	public static void startClone(){
		sendMessageToMaster(CloneMessage.REGISTER);
		
		new Thread(){
			@Override
			public void run(){
				listenFromMaster();
			}
		}.start();
		
		threadToCheckMaster = new Thread(){
			@Override
			public void run(){
				while((System.currentTimeMillis() - timeLastMessageMaster) < 950);
				System.out.println("I am the master now");
				try {
					startNewMaster();
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
					udpListen();
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
						int seg = random.nextInt(600);
						Thread.sleep(seg);
						changePoints();
						sendMessageToClone(CloneMessage.SAVE,null);
					}

				} catch (InterruptedException e) {					
					e.printStackTrace();
				}
			}
		}.start();

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
	public static void listenFromMaster(){
		while(true){ 
			try {
				Socket s = serverSocket.accept();
				new Thread() {
					@Override
					public void run() {
						try {
							ObjectInputStream in = new ObjectInputStream(s.getInputStream());
							Object object = in.readObject();
							
							getMessageFromMaster(object);
							
							s.close();						
						} catch (IOException e) {
							e.printStackTrace();
						} catch (ClassNotFoundException e) {
							e.printStackTrace();
						}
					}
				}.start();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
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
						
					} catch (IOException e) {
						e.printStackTrace();
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					}
				}
			}.start();
			
		}
	}
	@SuppressWarnings("resource")
	protected static void udpListen() throws IOException{
			DatagramSocket dsocket = new DatagramSocket(checkMasterPort);
			new Thread(){
				@SuppressWarnings("unused")
				public void run(){
					while (true){
						try
						{
							byte[] recvBuf = new byte[5000];
							DatagramPacket receivepacket = new DatagramPacket(recvBuf,
						                                                 recvBuf.length);
							dsocket.receive(receivepacket);
							System.out.println("Package recieved");
							int byteCount = receivepacket.getLength();
							ByteArrayInputStream receivebyteStream = new ByteArrayInputStream(recvBuf);
						      ObjectInputStream is = new
						           ObjectInputStream(new BufferedInputStream(receivebyteStream));
						      CheckMasterMessage o = (CheckMasterMessage) is.readObject();
						      is.close();
						      if (o.getType() == CheckMasterMessage.CHECK){
						    	  Socket s = new Socket(o.getIP(), checkMasterPort);
									
									CheckMasterMessage message = new CheckMasterMessage();
									message.setIP(InetAddress.getLocalHost().getHostAddress());
									message.setType(CheckMasterMessage.SUBJECT);
									
									ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
									out.writeObject(message);
									out.flush();
									out.close();
									
									s.close();
						      }
						      
						    }
						    catch (IOException e)
						    {
						      System.err.println("Exception:  " + e);
						      e.printStackTrace();
						    }
							catch (ClassNotFoundException e){
								System.err.println("Exception:  " + e);
								e.printStackTrace();
							}
					}
				}
			}.start();
			//dsocket.close();
	}
	
	/**
	 * Essa função é chamada quando o master é desligado
	 * O clone agora assumirá o controle
	 * @throws InterruptedException 
	 */
	private static void startNewMaster() throws InterruptedException{
		threadClone.interrupt();
		threadMaster = new Thread(){
			@Override
			public void run(){
				whoAmI = SUBJECT;
				startMaster();
			}
		};
		threadMaster.start();
		if(observers.size() != 0)
			myInstance.notifyObservers(NEWMASTER);
		
		
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
		CloneMessage message = (CloneMessage) object;
		
		timeLastMessageMaster = System.currentTimeMillis();
		
		switch(message.getType()){	
			case CloneMessage.NEW:		points.addAll(message.getPoints());
										synchronized (observers) {
											observers.addAll(message.getObservers() ) ;
										}
										threadToCheckMaster.start();
										break;
										
			case CloneMessage.ADD: 		
										synchronized (observers) {
											observers.add(message.getObserver());
										}
										break;
										
			case CloneMessage.SAVE:		
										points.clear();
										points.addAll(message.getPoints());
										sendMessageToMaster(CloneMessage.SAVED);
										break;
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
			case CloneMessage.REGISTER: if(cloneIp == null){
											cloneIp = message.getCloneIp();
											sendMessageToClone(CloneMessage.NEW,cloneIp);
										}
										break;
										
			case CloneMessage.SAVED: 	if(observers.size() != 0)
											myInstance.notifyObservers(UPDATE);
									 	break;
		}
	}
	
	/**
	 * USADA SOMENTE PELO CLONE
	 * Essa função envia uma mensagem para o clone com uma cópia do conjunto de pontos
	 * Após ser enviado o master aguarda uma confirmação do clone para então enviar 
	 * os pontos para todos os observers
	 */
	private static void sendMessageToMaster(int type) {
		Socket s;
		try {
			s = new Socket(masterIp, port);
			CloneMessage message = new CloneMessage();
			
			switch(type){
				case CloneMessage.REGISTER: message.setType(CloneMessage.REGISTER);
											message.setPoints(points);
											message.setObservers(observers);
											message.setCloneIp(s.getLocalAddress().getHostAddress());
											break;
										
				case CloneMessage.SAVED:	message.setType(CloneMessage.SAVED);
											break;
			}
			
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
			if (cloneIp == null)
				throw new IOException();
			s = new Socket(cloneIp, port);
			
			CloneMessage message = new CloneMessage();

			switch(type){	
				case CloneMessage.NEW: 		message.setType(CloneMessage.NEW);
											message.setPoints(points);
											message.setObservers(observers);
											break;
				
				case CloneMessage.ADD: 		message.setType(CloneMessage.ADD);
											message.setObserver(ip);
											break;
											
				case CloneMessage.SAVE: 	message.setType(CloneMessage.SAVE);
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
			if(observers.size() != 0)
				myInstance.notifyObservers(UPDATE);
		}
	}
	
	@SuppressWarnings("unused")
	private static void checkExistingMasters(){
		Socket s;
		try {
			DatagramSocket dSock = new DatagramSocket(checkMasterPort,InetAddress.getByName("200.239.139.255") );
			dSock.setBroadcast(true);
			
			CheckMasterMessage message = new CheckMasterMessage();
			message.setIP(InetAddress.getLocalHost().getHostAddress());
			message.setType(CheckMasterMessage.CHECK);
			
			InetAddress group = InetAddress.getByName("200.239.139.255");
			
			ByteArrayOutputStream byteStream = new ByteArrayOutputStream(5000);
			ObjectOutputStream os = new ObjectOutputStream(new BufferedOutputStream(byteStream));
			os.flush();
			os.writeObject(message);
			os.flush();
			byte[] sendBuffer = byteStream.toByteArray();
			
			DatagramPacket packet = new DatagramPacket(sendBuffer, sendBuffer.length,dSock.getLocalAddress(),checkMasterPort);
			
			dSock.send(packet);
		    os.close();
		    try{
		    	s = checkMasterSocket.accept();
		    	ObjectInputStream in = new ObjectInputStream(s.getInputStream());
				CheckMasterMessage response = (CheckMasterMessage) in.readObject();
				masterIp = response.getIP();
				whoAmI = CLONE;
		    	
		    }  catch (SocketTimeoutException e){
		    	whoAmI = SUBJECT;
			} catch (Exception e){
				e.printStackTrace();
			}
		    dSock.close();
		    
		} catch (IOException e) {
			e.printStackTrace();
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
		synchronized (observers) {
			observers.add(ip);
		}
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
		synchronized (observers) {
			observers.remove(index);
		}
	}
	
	/**
	 * USADA SOMENTE PELO MASTER
	 * Essa função envia uma mensagem com o conjunto de pontos que acabou de ser atualizado 
	 * para todos os observers cadastrados 
	 */
	@Override
	public void notifyObservers(int type) {
		synchronized (observers) {
			for (String ip : observers) {
				try {
					Socket s = new Socket(ip, port);
					ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
					
					SubjectMessage message = new SubjectMessage();
					
					switch(type){
						case UPDATE:	message.setPoints(newPoints);
										message.setType(change);
										break;
						case NEWMASTER: message.setIp(serverSocket.getInetAddress().getHostAddress());
										message.setType(NEWMASTER);
										break;
					}
					
					out.writeObject(message);
					out.flush();
					out.close();
					s.close();
					
				} catch (IOException e) {
					System.out.println(observers.size());
					observers.remove(ip);
					System.out.println(observers.size());
				}
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
	
}
