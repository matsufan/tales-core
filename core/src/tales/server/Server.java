package tales.server;




import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.websocket.WebSocketHandler;

import tales.config.Config;
import tales.config.Globals;
import tales.services.Logger;
import tales.services.TalesException;
import tales.system.TalesSystem;
import tales.utils.DBUtils;
import tales.utils.GitSync;




public class Server{




	public static void main(String[] args) throws TalesException {

		try{


			// wait for mysql to be ready
			DBUtils.waitUntilMysqlIsReady();
			
			
			// pulls the latest changes from git
			String pullOutput = GitSync.pull();
			
			
			// compiles
			for(String path : Config.getOnStartCompile()){

				ProcessBuilder builder = new ProcessBuilder("/bin/sh", "-c", "cd " + path + " && ant");
				Process p = builder.start();
				p.waitFor();
				p.destroy();

			}


			// checks if there is any new java class, if so reboot
			if(pullOutput.contains(".java")){
				
				
				// reboots this class
				String processCall = "java -cp " + Config.getJarPath() + " " + TalesSystem.getProcess();
				ProcessBuilder builder = new ProcessBuilder("/bin/sh", "-c", processCall);
				builder.start();
				

			}else{


				// start the server
				org.eclipse.jetty.server.Server server = new org.eclipse.jetty.server.Server(Config.getDashbaordPort());


				// resource handler
				ResourceHandler resourceHandler = new ResourceHandler();
				resourceHandler.setResourceBase(Globals.WEB_DIR);
				resourceHandler.setWelcomeFiles(new String[]{Globals.WEB_DASHBOARD});


				// handlers
				HandlerCollection handlers = new HandlerCollection();
				handlers.setHandlers(new Handler[]{resourceHandler, new APIHandler()});


				// sockets
				WebSocketHandler webSocketsHandler = new SocketServlet();
				webSocketsHandler.setHandler(handlers);


				// start server
				server.setHandler(webSocketsHandler);
				server.start();


				// starts the onstart services
				for(String command : Config.getOnStartList()){

					ProcessBuilder builder = new ProcessBuilder("/bin/sh", "-c", command + " >/dev/null 2>&1");
					builder.start();

				}

				Logger.log(new Throwable(), "server up and running...");

			}


		}catch(Exception e){
			throw new TalesException(new Throwable(), e);
		}

	}

}