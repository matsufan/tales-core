package tales.server;




import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.servlet.http.HttpServletRequest;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocketHandler;

import tales.services.TalesException;




public class SocketServlet extends WebSocketHandler {




	private Set<SocketController> users = new CopyOnWriteArraySet<SocketController>();
	private static JSONArray messages = new JSONArray();
	private boolean timerRunning = false;




	public WebSocket doWebSocketConnect(HttpServletRequest request, String protocol) {
		return new SocketController();
	}




	class SocketController implements WebSocket.OnTextMessage {

		private Connection connection;

		@Override
		public void onClose(int closeCode, String message) {
			users.remove(this);
		}

		@Override
		public void onMessage(String data) {

			messages.add((JSONObject) JSONSerializer.toJSON(data));

			if(!timerRunning){
				timerRunning = true;
				Timer timer = new Timer();
				timer.run();
			}

		}

		@Override
		public void onOpen(Connection connection) {

			this.connection = connection;
			users.add(this);

		}

	}




	private class Timer implements Runnable{

		@Override
		public void run() {


			try{

				if(messages.size() > 0){

					for(SocketController user : users){

						try{

							if(user.connection.isOpen()){
								user.connection.sendMessage(messages.toString());
							}

						}catch (IOException e){

							user.connection.close();
							users.remove(user);
							e.printStackTrace();

						}

					}

				}


				messages = new JSONArray();

				Thread.sleep(100);
				Thread t = new Thread(this);
				t.start();


			}catch(Exception e){
				new TalesException(new Throwable(), e);
			}

		}

	}

}