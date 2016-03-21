import java.io.BufferedOutputStream;
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
import java.util.List;


public class ConcretObserver implements Observer {
	private static List<Point> 			points;
	private static ServerSocket 		serverSocket;
	private static ServerSocket 		checkMasterSocket;
	private static int 					port = 6969;
	private static final int 			checkMasterPort = 6970;
	private static ConcretObserver		myInstance;
	private static String 				masterIp;
	private static Frame 				frame;
	
	public ConcretObserver() throws IOException{
		serverSocket 		= new ServerSocket(port);
		points 				= new ArrayList<Point>();
		masterIp			= null;
		checkMasterSocket 	= new ServerSocket(checkMasterPort);
		checkMasterSocket.setSoTimeout(3000);
				
		new Thread(){
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
		}.start();
	}

	public static void main(String[] args) throws IOException {
		myInstance = new ConcretObserver();
	}

	private static void startObserver() throws UnknownHostException, IOException{
		frame = new Frame(new ArrayList<Point>());
		
		checkExistingMasters();
		
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
	 * Essa função é executada por uma thread que fica esperando por novas conexões com o master 
	 * Essa conexão pode ser feita por um clone ou por um observer
	 * @throws IOException
	 */
	protected static void listen() throws IOException {
		while(true){
			Socket socket = serverSocket.accept();
			new Thread() {
				@Override
				public void run() {
					try {
						ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
						
						SubjectMessage message = (SubjectMessage) in.readObject();
						int type = message.getType();
						List<Point> newPoints = message.getPoints();
						myInstance.update(type,newPoints);
						
						socket.close();

						
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
		Socket s = new Socket(masterIp, port);
		
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
		Socket s = new Socket(masterIp, port);

		ObserverMessage message = new ObserverMessage();
		message.setIp(s.getLocalAddress().getHostAddress());
		message.setType(1);
		
		ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
		out.writeObject(message);
		out.flush();
		out.close();

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
		    }  catch (SocketTimeoutException e){
		    	System.out.println("No master has been found!");
			} catch (Exception e){
				e.printStackTrace();
			}
		    dSock.close();
		    
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

