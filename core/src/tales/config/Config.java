package tales.config;




import java.io.File;
import java.util.ArrayList;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import org.apache.commons.io.FileUtils;

import tales.dirlistener.DirListenerObj;
import tales.services.TalesException;
import tales.system.TalesSystem;
import tales.workers.FailoverAttempt;




public class Config{




	private static JSONObject json;
	private static boolean inited = false;




	public static String getDashbaordURL() throws TalesException{
		load();
		return json.getJSONObject("templates")
				.getJSONObject("static")
				.getString("dashboardURL");
	}




	public static int getDashbaordPort() throws TalesException{
		load();
		return json.getJSONObject("templates")
				.getJSONObject("static")
				.getInt("dashboardPort");
	}




	public static String getLogDBHost() throws TalesException{
		load();
		return json.getJSONObject("templates")
				.getJSONObject("static")
				.getString("logDB");
	}




	public static String getJarPath() throws TalesException{
		load();
		return json.getJSONObject("templates")
				.getJSONObject("static")
				.getString("jarPath");
	}




	public static String getDBUsername() throws TalesException{
		return getDBUsername("");
	}




	public static String getDBUsername(String dbName) throws TalesException{
		load();
		return getTemplateKeyValue(dbName, "dbUsername");
	}




	public static String getDBPassword() throws TalesException{
		return getDBPassword("");
	}




	public static String getDBPassword(String dbName) throws TalesException{
		load();
		return getTemplateKeyValue(dbName, "dbPassword");
	}




	public static String getDataDBHost(String dbName) throws TalesException{
		load();
		return getTemplateKeyValue(dbName, "dataDB");
	}




	public static String getTasksDBHost(String dbName) throws TalesException{
		load();
		return getTemplateKeyValue(dbName, "tasksDB");
	}




	public static int getDBPort() throws TalesException{
		load();
		return getDBPort("");
	}




	public static int getDBPort(String dbName) throws TalesException{
		load();
		return Integer.parseInt(getTemplateKeyValue(dbName, "dbPort"));
	}




	public static String getRedisHost(String dbName) throws TalesException{
		load();
		return getTemplateKeyValue(dbName, "redisHost");
	}




	public static int getRedisPort(String dbName) throws TalesException{
		load();
		return Integer.parseInt(getTemplateKeyValue(dbName, "redisPort"));
	}




	public static String getSolrHost(String dbName) throws TalesException{
		load();
		return getTemplateKeyValue(dbName, "solrHost");
	}




	public static int getSolrPort(String dbName) throws TalesException{
		load();
		return Integer.parseInt(getTemplateKeyValue(dbName, "solrPort"));
	}



	
	public static int getSolrPort() throws TalesException{
		load();
		return Integer.parseInt(getTemplateKeyValue(null, "solrPort"));
	}
	
	
	
	
	public static String getMongoHost(String dbName) throws TalesException{
		load();
		return getTemplateKeyValue(dbName, "mongoHost");
	}




	public static int getMongoPort(String dbName) throws TalesException{
		load();
		return Integer.parseInt(getTemplateKeyValue(dbName, "mongoPort"));
	}
	
	
	
	
	private static String getTemplateKeyValue(String dbName, String key) throws TalesException{

		if(!key.equals("") && templateKeyExists(dbName, key)){
			return json.getJSONObject("templates")
					.getJSONObject(dbName)
					.getString(key);

		}else{
			return json.getJSONObject("templates")
					.getJSONObject("default")
					.getString(key);

		}
	}




	public static ArrayList<FailoverAttempt> getFailover(String dbName) throws TalesException{
		load();

		if(!templateKeyExists(dbName, "failover")){
			dbName = "default";
		}

		JSONArray failovers = json.getJSONObject("templates").getJSONObject(dbName).getJSONArray("failover");
		ArrayList<FailoverAttempt> objs = new ArrayList<FailoverAttempt>();

		for(int i = 0; i < failovers.size(); i++){

			FailoverAttempt failover = new FailoverAttempt();
			failover.setMaxFails(failovers.getJSONObject(i).getInt("fails"));
			failover.setDuring(failovers.getJSONObject(i).getInt("during"));
			failover.setSleep(failovers.getJSONObject(i).getInt("sleep"));
			
			objs.add(failover);

		}

		return objs;
	}




	private static boolean templateKeyExists(String dbName, String key) throws TalesException{

		if(json.getJSONObject("templates").containsKey(dbName)){
			return json.getJSONObject("templates")
					.getJSONObject(dbName)
					.has(key);
		}

		return false;

	}




	public static int getMaxTasks() throws TalesException{
		load();
		return json.getJSONObject("templates")
				.getJSONObject("static")
				.getInt("maxTasks");
	}




	public static int getMinTasks() throws TalesException{
		load();
		return json.getJSONObject("templates")
				.getJSONObject("static")
				.getInt("minTasks");
	}




	public static String getAWSAccessKeyId() throws TalesException{
		load();
		return json.getJSONObject("cloud")
				.getJSONObject("aws")
				.getString("accessKeyId");
	}




	public static String getAWSSecretAccessKey() throws TalesException{
		load();
		return json.getJSONObject("cloud")
				.getJSONObject("aws")
				.getString("secretAccessKey");
	}




	public static String getAWSAMI() throws TalesException{
		load();
		return json.getJSONObject("cloud")
				.getJSONObject("aws")
				.getString("ami");
	}




	public static String getAWSSecurityGroup() throws TalesException{
		load();
		return json.getJSONObject("cloud")
				.getJSONObject("aws")
				.getString("securityGroup");
	}




	public static String getAWSInstanceType() throws TalesException{
		load();
		return json.getJSONObject("cloud")
				.getJSONObject("aws")
				.getString("instanceType");
	}




	public static String getAWSEndpoint() throws TalesException{
		load();
		return json.getJSONObject("cloud")
				.getJSONObject("aws")
				.getString("endpoint");
	}
	
	
	
	
	public static String getRackspaceUsername() throws TalesException{
		load();
		return json.getJSONObject("cloud")
				.getJSONObject("rackspace")
				.getString("username");
	}




	public static String getRackspaceKey() throws TalesException{
		load();
		return json.getJSONObject("cloud")
				.getJSONObject("rackspace")
				.getString("key");
	}




	public static int getRackspaceImageId() throws TalesException{
		load();
		return json.getJSONObject("cloud")
				.getJSONObject("rackspace")
				.getInt("imageId");
	}




	public static int getRackspaceFlavor() throws TalesException{
		load();
		return json.getJSONObject("cloud")
				.getJSONObject("rackspace")
				.getInt("flavor");
	}




	public static int getRackspaceAccount() throws TalesException{
		load();
		return json.getJSONObject("cloud")
				.getJSONObject("rackspace")
				.getInt("account");
	}

	
	
		
	public static ArrayList<String> getSyncList() throws TalesException{
		load();

		ArrayList<String> list = new ArrayList<String>();
		for(int i = 0; i < json.getJSONArray("gitSync").size(); i++){
			list.add(json.getJSONArray("gitSync").getString(i));
		}

		return list;
	}




	public static ArrayList<String> getOnStartCompile() throws TalesException{
		load();

		ArrayList<String> list = new ArrayList<String>();
		for(int i = 0; i < json.getJSONArray("onStartCompile").size(); i++){
			list.add(json.getJSONArray("onStartCompile").getString(i));
		}

		return list;
	}




	public static ArrayList<DirListenerObj> getDirListenerList() throws TalesException{
		load();

		ArrayList<DirListenerObj> list = new ArrayList<DirListenerObj>();
		for(int i = 0; i < json.getJSONArray("dirListener").size(); i++){

			JSONObject jsonObj = json.getJSONArray("dirListener").getJSONObject(i);

			DirListenerObj obj = new DirListenerObj();
			obj.setDir(jsonObj.getString("dir"));
			obj.setExec(jsonObj.getString("exec"));
			obj.setIgnoreRegex(jsonObj.getString("ignoreRegex"));

			list.add(obj);
			
		}

		return list;
	}




	public static ArrayList<String> getOnStartList() throws TalesException{
		load();

		ArrayList<String> list = new ArrayList<String>();
		for(int i = 0; i < json.getJSONArray("onStart").size(); i++){
			list.add(json.getJSONArray("onStart").getString(i));
		}

		return list;
	}




	private static void load() throws TalesException{

		if(!inited){

			inited = true;
			Loader loader = new Config().new Loader();
			loader.run();

		}

	}




	private class Loader implements Runnable{

		public void run() {

			try{

				File file = new File(Globals.ENVIRONMENTS_CONFIG_DIR + "/" + TalesSystem.getTemplatesGitBranchName() + ".json");
				String data = FileUtils.readFileToString(file);
				Config.json = (JSONObject) JSONSerializer.toJSON(data);

				Thread.sleep(Globals.RELOAD_CONFIG_INTERNAL);
				Thread t = new Thread(this);
				t.start();

			}catch(Exception e){
				try{
					new TalesException(new Throwable(), e);
				}catch(Exception e1){
					e.printStackTrace();
				}
			}

		}

	}

}
