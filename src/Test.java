
public class Test {

	public static void main(String[] args) {
		Long time = System.currentTimeMillis();
		
		while(true){
			Long currentTime = (System.currentTimeMillis() - time);
			System.out.println(currentTime);
		}

	}

}
