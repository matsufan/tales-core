package tales.services;




import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

import tales.config.Config;
import tales.config.Globals;
import tales.scrapers.ScraperConfig;
import tales.utils.DBUtils;

import com.mysql.jdbc.Statement;




public class TasksDB {




	private Connection conn;
	private String taskName;
	private ArrayList<String> tables = new ArrayList<String>();
	private String dbName = "tasks";




	public TasksDB(ScraperConfig config) throws TalesException{

		try{

			this.taskName = config.getTaskName();


			if(conn == null){
				
				
				// checks if the database exists, if not create it
				DBUtils.checkDatabase(dbName);
				

				// connects
				Class.forName("com.mysql.jdbc.Driver");
				conn = DriverManager.getConnection("jdbc:mysql://"+
						Config.getTasksDBHost(config.getTemplate().getMetadata().getDatabaseName())+":"+Config.getDBPort(config.getTemplate().getMetadata().getDatabaseName())+"/"+
						Globals.DATABASE_NAMESPACE + dbName +
						"?user=" + Config.getDBUsername(config.getTemplate().getMetadata().getDatabaseName()) +
						"&password=" + Config.getDBPassword(config.getTemplate().getMetadata().getDatabaseName()) +
						"&useUnicode=true&characterEncoding=UTF-8"
						);
			}


			if(!this.tableExists()){
				this.createTable();
			}

		}catch(Exception e){
			throw new TalesException(new Throwable(), e);
		}

	}



	public void add(Task task) throws TalesException{
		ArrayList<Task> tasks = new ArrayList<Task>();
		tasks.add(task);
		add(tasks);
	}
	
	
	
	
	public void add(ArrayList<Task> tasks) throws TalesException{

		try{


			// ignore because we want to guarantee that the other tasks will be added
			PreparedStatement statement = conn.prepareStatement("INSERT IGNORE INTO " + taskName + " (id, name) values (?,?)");


			// stores the data into a batch
			for(Task task : tasks){
				statement.setInt(1, task.getDocumentId());
				statement.setString(2, task.getDocumentName());
				statement.addBatch();
			}

			statement.executeBatch();
			statement.clearBatch();


			// close
			statement.close();


		}catch(Exception e){
			String[] args = {"tableName:" + taskName};
			throw new TalesException(new Throwable(), e, args);
		}
	}




	public ArrayList<Task> getList(int amount) throws TalesException{

		try{


			// conn
			ArrayList<Task> list         = new ArrayList<Task>();
			PreparedStatement statement  = conn.prepareStatement("SELECT * FROM " + taskName + " LIMIT 0,?");
			statement.setInt(1, amount);

			statement.executeQuery();
			ResultSet rs                 = statement.executeQuery();

			while(rs.next()){

				Task task = new Task();
				task.setDocumentId(rs.getInt("id"));
				task.setDocumentName(rs.getString("name"));

				list.add(task);
			}


			// close
			rs.close();
			statement.close();


			return list;

		}catch(Exception e){
			String[] args = {amount + ""};
			throw new TalesException(new Throwable(), e, args);
		}
	}




	public void deleteTaskWithDocumentId(int documentId) throws TalesException{

		try{

			PreparedStatement statement = conn.prepareStatement("DELETE FROM " + taskName + " WHERE id=?");
			statement.setInt(1, documentId);
			statement.executeUpdate();
			statement.close();

		}catch(Exception e){
			String[] args = {documentId + ""};
			throw new TalesException(new Throwable(), e, args);
		}

	}




	public int count() throws TalesException{

		try{


			// conn
			PreparedStatement statement = conn.prepareStatement("SELECT COUNT(*) FROM " + taskName);
			statement.executeQuery();

			ResultSet rs            = statement.executeQuery();
			rs.next();

			// results
			int count                     = rs.getInt(1);

			// clone
			rs.close();
			statement.close();            


			return count;


		}catch(Exception e){
			throw new TalesException(new Throwable(), e);
		}
	}




	private synchronized void createTable() throws TalesException{

		try {

			Statement statement = (Statement) conn.createStatement();
			statement.executeUpdate("CREATE TABLE " +  taskName + " (id INT NOT NULL, name VARCHAR( 500 ) CHARACTER SET utf8 COLLATE utf8_unicode_ci NOT NULL) ENGINE = MYISAM CHARSET=utf8");

			// clone
			statement.close();


		}catch(Exception e){
			String[] args = {taskName};
			throw new TalesException(new Throwable(), e, args);
		}
	}




	private synchronized boolean tableExists() throws TalesException{

		boolean exists          = false;
		Statement statement     = null;
		ResultSet rs            = null;

		try {


			if(!tables.contains(taskName)){

				statement = (Statement) conn.createStatement();
				rs = statement.executeQuery("SHOW TABLES LIKE '" + taskName + "'");
				if (rs.next()) exists = true;

				tables.add(taskName);


			}else{
				exists = true;
			}



		}catch(Exception e){
			String[] args = {taskName};
			throw new TalesException(new Throwable(), e, args);
		}

		// clone
		try{rs.close();}catch(Exception e){}
		try{statement.close();}catch(Exception e){}

		return exists;
	}




	public String getTaskName() {
		return taskName;
	}




	public void closeConnection() throws TalesException{

		try {

			if(conn != null){
				conn.close();
				conn = null;
			}

		}catch(Exception e){
			throw new TalesException(new Throwable(), e);
		}
	}




	public static void deleteTaskTablesFromDomain(String domain) throws TalesException {

		try {


			// connects
			Class.forName("com.mysql.jdbc.Driver");
			Connection conn = DriverManager.getConnection("jdbc:mysql://"+
					Config.getTasksDBHost(domain)+":"+Config.getDBPort(domain)+"/"+
					"tales_tasks" +
					"?user=" + Config.getDBUsername() +
					"&password=" + Config.getDBPassword() +
					"&useUnicode=true&characterEncoding=UTF-8"
					);


			// gets all the tables that contains the domain name
			Statement statement = (Statement) conn.createStatement();
			ResultSet rs = statement.executeQuery("SHOW TABLES LIKE '%" + domain + "'");

			while(rs.next()){


				Logger.log(new Throwable(), "dropping task table \"" + rs.getString(1) + "\"");
				Statement statement2 = (Statement) conn.createStatement();
				statement2.executeUpdate("DROP TABLE " + rs.getString(1));
				statement2.close();

			}

			rs.close();
			statement.close();
			conn.close();


		}catch(Exception e){
			throw new TalesException(new Throwable(), e);
		}

	}

}
