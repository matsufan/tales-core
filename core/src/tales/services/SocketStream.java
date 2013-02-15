package tales.services;




import java.net.URI;
import java.util.concurrent.Future;

import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocket.Connection;
import org.eclipse.jetty.websocket.WebSocketClient;
import org.eclipse.jetty.websocket.WebSocketClientFactory;
import org.json.JSONObject;

import tales.config.Config;




public class SocketStream {




	private static Connection connection;



	
	private static synchronized void init() throws Exception{


		if(connection == null){


			URI uri = new URI("ws://" + Config.getDashbaordURL() + ":" + Config.getDashbaordPort());

			WebSocketClientFactory webSocketClientFactory = new WebSocketClientFactory();
			webSocketClientFactory.start();

			WebSocketClient client = webSocketClientFactory.newWebSocketClient();

			Future<Connection> futureConnection = client.open(uri, new WebSocket.OnTextMessage(){

				public void onOpen(Connection connection){}
				public void onClose(int closeCode, String message){
					connection.close();
					connection = null;
				}
				public void onMessage(String data){}

			});

			connection = futureConnection.get();
		}

	}




	public static void stream(JSONObject json) throws Exception{

		init();
		connection.sendMessage(json.toString());

	}

}
