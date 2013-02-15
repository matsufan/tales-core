package tales.utils;




import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.ArrayList;

import tales.config.Config;
import tales.config.Globals;
import tales.services.Logger;
import tales.services.TalesException;

import com.mysql.jdbc.Statement;




public class DBUtils {




	private static Connection conn;
	private static ArrayList<String> cachedDatabases = new ArrayList<String>();




	public static void checkDatabase(String dbName) throws TalesException{

		try {


			if(!cachedDatabases.contains(dbName)){


				// starts a local connection
				Class.forName("com.mysql.jdbc.Driver");
				conn = DriverManager.getConnection("jdbc:mysql://"+
						Config.getDataDBHost(dbName)+":"+Config.getDBPort(dbName)+"/"+
						"mysql" +
						"?user="+Config.getDBUsername() +
						"&password="+Config.getDBPassword() +
						"&useUnicode=true&characterEncoding=UTF-8"
						);

				if(!databaseExists(dbName)){
					createDatabase(dbName);
				}

				cachedDatabases.add(dbName);

			}


		}catch(final Exception e){
			String[] args = {Config.getDataDBHost(dbName), Config.getDBPort(dbName) + ""};
			throw new TalesException(new Throwable(), e, args);
		}

	}




	private static boolean databaseExists(String dbName) throws TalesException{


		boolean exists          = false;
		Statement statement     = null;
		ResultSet rs            = null;


		try {


			statement           = (Statement) conn.createStatement();
			rs                  = statement.executeQuery("SHOW DATABASES LIKE '" + Globals.DATABASE_NAMESPACE + dbName + "'");
			if (rs.next()) exists = true;


		}catch(final Exception e){
			final String[] args = {dbName};
			throw new TalesException(new Throwable(), e, args);
		}


		try{rs.close();}catch(final Exception e){}
		try{statement.close();}catch(final Exception e){}


		return exists;

	}




	private static void createDatabase(String dbName) throws TalesException{


		try {


			final String sql = "CREATE DATABASE " + Globals.DATABASE_NAMESPACE + dbName;
			final Statement statement = (Statement) conn.createStatement();
			statement.executeUpdate(sql);
			statement.close();


		}catch(final Exception e){
			final String[] args = {dbName};
			throw new TalesException(new Throwable(), e, args);
		}

	}





	public static void waitUntilMysqlIsReady(){

		while(true){

			try {

				Class.forName("com.mysql.jdbc.Driver");
				conn = DriverManager.getConnection("jdbc:mysql://"+
						"localhost:"+Config.getDBPort()+"/"+
						"mysql" +
						"?user="+Config.getDBUsername() +
						"&password="+Config.getDBPassword() +
						"&useUnicode=true&characterEncoding=UTF-8"
						);

				conn.close();
				break;

			}catch(final Exception e){	
				try {
					Logger.log(new Throwable(), "waiting for mysql to be up...");
					Thread.sleep(1000);
				} catch (Exception e1) {}
			}

		}

	}




	public static ArrayList<String> getTalesDBs() throws TalesException{

		try{

			ArrayList<String> dbNames = new ArrayList<String>();

			// connects to mysql
			Class.forName("com.mysql.jdbc.Driver");
			Connection conn = DriverManager.getConnection("jdbc:mysql://"+
					"localhost:"+Config.getDBPort()+"/"+
					"mysql" +
					"?user="+Config.getDBUsername() +
					"&password="+Config.getDBPassword() +
					"&useUnicode=true&characterEncoding=UTF-8"
					);

			ResultSet rs = conn.getMetaData().getCatalogs();


			// lists the dbs
			while (rs.next()) {

				String dbName = rs.getString("TABLE_CAT");

				if(dbName.contains(Globals.DATABASE_NAMESPACE)){
					dbNames.add(dbName);
				}

			}

			conn.close();

			return dbNames;


		}catch(final Exception e){
			throw new TalesException(new Throwable(), e);
		}

	}

}
