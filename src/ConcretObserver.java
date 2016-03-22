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
import java.util.Collections;
import java.util.List;


public class ConcretObserver {
	private static List<Point> 			points;
	private static ServerSocket 		serverSocket;
	private static ServerSocket 		checkMasterSocket;
	private static int 					port = 6969;
	private static final int 			checkMasterPort = 6970;
	private static String 				masterIp;
	private static Frame 				frame;
	
	public ConcretObserver() throws IOException{
		serverSocket 		= new ServerSocket(port);
		points 				= Collections.synchronizedList(new ArrayList<Point>());
		masterIp			= null;
		checkMasterSocket 	= new ServerSocket(checkMasterPort);
		
		checkMasterSocket.setSoTimeout(3000);
		
		checkExistingMasters();
	}

	public static void main(String[] args) throws IOException {
		new ConcretObserver();
	}

	private static void startObserver() throws UnknownHostException, IOException{
		frame = new Frame();
		
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
				startObserver();
				register();
		    	
		    }  catch (SocketTimeoutException e){
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
							update(type,newPoints);
							
							socket.close();
						} catch (IOException | ClassNotFoundException e) {
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
	public static void update(int type,List<Point> newPoints) {
		
		switch(type){
			case 0: 
				synchronized (points) {
					points.addAll(newPoints);
				}
				
					break;
			case 1: for(Point point : newPoints)
				synchronized (point) {
					points.remove(point);
				}break;
		}
		
		frame.setPoints(points);
		frame.revalidate();
		frame.repaint();
	}

	/**
	 * USADA SOMENTE PELO OBSERVER
	 * Essa função é usada para inserir um novo observer 
	 */
	public static void register() throws UnknownHostException, IOException {
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
	public static void unregister() throws IOException {
		Socket s = new Socket(masterIp, port);

		ObserverMessage message = new ObserverMessage();
		message.setIp(s.getLocalAddress().getHostAddress());
		message.setType(1);
		
		ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
		out.writeObject(message);
		out.flush();
		out.close();

	}
}

