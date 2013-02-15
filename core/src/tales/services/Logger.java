package tales.services;




import java.util.Formatter;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.json.JSONObject;

import tales.system.TalesSystem;




public class Logger {




	public static final String LOG = "tales_log";
	public static final String ERROR = "tales_error";
	public static final String TEMPLATE_ERROR = "tales_template_error";
	public static final String DOWNLOAD_ERROR = "tales_download_error";




	private static void emit(StackTraceElement[] e, String logType, String data){


		try{

			
			int pid = TalesSystem.getPid();
			String publicDNS = TalesSystem.getPublicDNSName();
			String methodPath = e[0].getClassName() + "." + e[0].getMethodName();
			int lineNumber = e[0].getLineNumber();

			
			// websockets stream
			JSONObject obj = new JSONObject()
			.put("pid", pid)
			.put("process", TalesSystem.getProcess())
			.put("publicDNS", TalesSystem.getPublicDNSName())
			.put("methodPath", methodPath)
			.put("data", data);

			SocketStream.stream(obj);

			
			// saves logs
			if(logType.equals(LOG)){
				LogsDB.log(publicDNS, pid, LOG, methodPath, lineNumber, data);

			}else if(logType.equals(ERROR)){
				LogsDB.log(publicDNS, pid, ERROR, methodPath, lineNumber, data);
				
			}else if(logType.equals(DOWNLOAD_ERROR)){
				LogsDB.log(publicDNS, pid, DOWNLOAD_ERROR, methodPath, lineNumber, data);
			}


		}catch(Exception k){
			printError(new Throwable(), k, new String[]{});
		}

	}



	
	public static void log(Throwable origin, String data){
		System.out.format("%-50s %-2s %s %n", origin.getStackTrace()[0].getClassName(), "|", data);
		emit(origin.getStackTrace(), LOG, data + "");
	}



	
	public static void error(Throwable origin, Exception error) {
		String[] args = {};
		error(origin, error, args);
	}



	
	public static void error(Throwable origin, Exception error, String[] args) {
		emit(error.getStackTrace(), ERROR, printError(origin, error, args));
	}




	public static void error(Throwable origin, Exception error, int id) {
		String[] args = {Integer.toString(id)};
		error(origin, error, args);
	}




	public static void templateError(Throwable origin, Exception error, int documentId) {
		String[] args = {Integer.toString(documentId)};
		emit(error.getStackTrace(), TEMPLATE_ERROR, printError(origin, error, args));
	}
	
	
	
	
	public static void downloadError(Throwable origin, Exception error) {
		downloadError(origin, error, new String[]{});
	}
	
	
	
	
	public static void downloadError(Throwable origin, Exception error, String[] args) {
		emit(error.getStackTrace(), DOWNLOAD_ERROR, printError(origin, error, args));
	}
	
	
	
	
	public static String printError(Throwable origin, Exception error, String[] args){
		
		String data = "";
		data += "[ERROR START] >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>\n";
		data += new Formatter().format("%-15s %-2s %s %n", "class", "|", origin.getStackTrace()[0].toString()).toString();

		// args
		for(int i = 0; i < args.length; i++){
			data += new Formatter().format("%-15s %-2s %s %n", "args", "|", args[i]).toString();
		}

		data += "----------------------------------------------------------------------------------------------------------------------------------------------------\n";
		data += ExceptionUtils.getFullStackTrace(error);
		data += "[ERROR END] <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<";

		System.out.println(data);
		
		return data;
		
	}
}
