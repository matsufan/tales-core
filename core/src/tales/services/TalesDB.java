package tales.services;




import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Set;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import tales.config.Config;
import tales.config.Globals;
import tales.templates.TemplateMetadataInterface;
import tales.utils.DBUtils;

import com.mysql.jdbc.Statement;




public class TalesDB {




	private String dbName;
	private Connection conn;
	private JedisPool jedisPool;




	// index
	private final static HashMap<String, Integer> index = new HashMap<String, Integer>();

	// connections
	private final static HashMap<String, ArrayList<Connection>> conns = new HashMap<String, ArrayList<Connection>>();

	// cache tables
	private final static HashMap<String, ArrayList<String>> cachedTables = new HashMap<String, ArrayList<String>>();

	// jedis pool
	private final static HashMap<String, JedisPool> jedisPools = new HashMap<String, JedisPool>();




	public TalesDB(tales.services.Connection talesConn, TemplateMetadataInterface metadata) throws TalesException{


		this.dbName = metadata.getDatabaseName();


		try{


			// database and memcache connections
			if(!conns.containsKey(dbName)){


				// init connection holders
				conns.put(dbName, new ArrayList<Connection>());
				cachedTables.put(dbName, new ArrayList<String>());


				// checks if the database exists, if not create it
				DBUtils.checkDatabase(dbName);


				// builds a redis pool
				final JedisPoolConfig config = new JedisPoolConfig();
				config.setMaxActive(talesConn.getConnectionsNumber());
				config.setTestWhileIdle(true);

				jedisPool = new JedisPool(config, Config.getRedisHost(dbName), Config.getRedisPort(dbName), Globals.REDIS_TIMEOUT);
				jedisPools.put(dbName, jedisPool);


				// creates the conns
				Logger.log(new Throwable(), "openning " + talesConn.getConnectionsNumber() + " connections to host \"" + Config.getDataDBHost(dbName) + "\" database \"" + dbName + "\"");

				for(int i = 0; i < talesConn.getConnectionsNumber(); i++){

					Class.forName("com.mysql.jdbc.Driver");
					conn = DriverManager.getConnection("jdbc:mysql://"+
							Config.getDataDBHost(dbName)+":"+Config.getDBPort(dbName)+"/"+
							Globals.DATABASE_NAMESPACE + dbName +
							"?user="+Config.getDBUsername(metadata.getDatabaseName()) +
							"&password="+Config.getDBPassword(metadata.getDatabaseName()) +
							"&useUnicode=true&characterEncoding=UTF-8" +
							"&autoReconnect=true&failOverReadOnly=false&maxReconnects=10"
							);
					conns.get(dbName).add(conn);

				}


				// checks if the doc table exists
				if(!documentsTableExists()){
					createDocumentsTable();
				}


				// adds the first document if none
				if(getDocumentsCount() == 0){
					for(final String document : metadata.getFirstDocuments()){
						if(!documentExists(document)){
							addDocument(document);
						}
					}
				}


				// checks if the ignored document table exists
				if(!ignoredDocumentsTableExists()){
					createIgnoredDocumentsTable();
				}


				// load documents in memory
				this.loadDocumentsIntoMem();


			}else{


				// index
				if(!index.containsKey(dbName)){
					index.put(dbName, 0);
				}

				index.put(dbName, index.get(dbName) + 1);

				if(index.get(dbName) >= conns.get(dbName).size()){
					index.put(dbName, 0);
				}


				// db conn
				conn = conns.get(dbName).get(index.get(dbName));


				// jedis
				jedisPool = jedisPools.get(dbName);

			}


		}catch(final Exception e){
			final String[] args = {dbName};
			throw new TalesException(new Throwable(), e, args);
		}

	}




	public String getDBName(){
		return this.dbName;
	}




	public synchronized final int addDocument(final String name) throws TalesException{


		try{


			// memcache	
			final Jedis redis = jedisPool.getResource();
			redis.set(dbName + name, Integer.toString(-1));


			// db query
			final PreparedStatement statement = conn.prepareStatement("INSERT INTO documents (name) values (?)", Statement.RETURN_GENERATED_KEYS);
			statement.setString(1, name);
			statement.executeUpdate();


			final ResultSet rs = statement.getGeneratedKeys();
			rs.next();


			final int id = rs.getInt(1);


			rs.close();
			statement.close();


			// memcache
			redis.set(dbName + name, Integer.toString(id));
			redis.set(dbName + "lastDocumentIndex", Integer.toString(id));
			jedisPool.returnResource(redis);


			return id;


		}catch(final Exception e){
			final String[] args = {name};
			throw new TalesException(new Throwable(), e, args);
		}

	}




	public synchronized final boolean documentExists(final String name) throws TalesException{


		try {

			final Jedis redis = jedisPool.getResource();
			final Boolean result = redis.exists(dbName + name);
			jedisPool.returnResource(redis);

			return result;

		}catch(final Exception e){
			final String[] args = {name};
			throw new TalesException(new Throwable(), e, args);
		}

	}




	public synchronized final boolean documentIdExists(final int documentId) throws TalesException{


		try {


			boolean exists = false;


			final PreparedStatement statement = conn.prepareStatement("SELECT count(*) FROM documents WHERE id=? LIMIT 1");
			statement.setInt(1, documentId);


			final ResultSet rs = statement.executeQuery();
			rs.next();


			if(rs.getInt(1) > 0){
				exists = true;
			}


			rs.close();
			statement.close();


			return exists;


		}catch(final Exception e){
			final String[] args = {documentId + ""};
			throw new TalesException(new Throwable(), e, args);
		}

	}




	public final int getDocumentId(final String name) throws TalesException{


		try{

			final Jedis redis = jedisPool.getResource();
			final int result = Integer.parseInt(redis.get(dbName + name));
			jedisPool.returnResource(redis);

			return result;

		}catch(final Exception e){
			final String[] args = {name};
			throw new TalesException(new Throwable(), e, args);
		}
	}




	public final Document getAndUpdateLastCrawledDocument() throws TalesException{


		try {
			return getAndUpdateLastCrawledDocuments(1).get(0);


		}catch(final Exception e){
			throw new TalesException(new Throwable(), e);
		}

	}




	public final ArrayList<Document> getAndUpdateLastCrawledDocuments(final int number) throws TalesException{


		try {


			// db query
			final ArrayList<Document> list      = new ArrayList<Document>();
			final PreparedStatement statement   = conn.prepareStatement("SELECT *,lastUpdate FROM documents WHERE active = 1 ORDER BY lastUpdate ASC LIMIT 0,?");
			statement.setInt(1, number);


			final ResultSet rs                  = statement.executeQuery();

			while(rs.next()){

				final Document document = new Document();
				document.setId(rs.getInt("id"));
				document.setName(rs.getString("name"));
				document.setAdded(rs.getTimestamp("added"));
				document.setLastUpdate(rs.getTimestamp("lastUpdate"));
				document.setActive(rs.getBoolean("active"));

				list.add(document);

				// update
				updateDocumentLastUpdate(document.getId());


			}


			rs.close();
			statement.close();


			return list;


		}catch(final Exception e){
			final String[] args = {number + ""};
			throw new TalesException(new Throwable(), e, args);
		}

	}




	public final ArrayList<Document> getMostRecentCrawledDocuments(final int number) throws TalesException{


		try {


			final ArrayList<Document> list      = new ArrayList<Document>();
			final PreparedStatement statement   = conn.prepareStatement("SELECT *,lastUpdate FROM documents WHERE active = 1 ORDER BY lastUpdate DESC LIMIT 0,?");
			statement.setInt(1, number);


			final ResultSet rs                  = statement.executeQuery();

			while(rs.next()){

				final Document document = new Document();
				document.setId(rs.getInt("id"));
				document.setName(rs.getString("name"));
				document.setAdded(rs.getTimestamp("added"));
				document.setLastUpdate(rs.getTimestamp("lastUpdate"));
				document.setActive(rs.getBoolean("active"));

				list.add(document);

			}


			rs.close();
			statement.close();


			return list;


		}catch(final Exception e){
			final String[] args = {number + ""};
			throw new TalesException(new Throwable(), e, args);
		}

	}




	public final void updateDocumentLastUpdate(final int documentId) throws TalesException{


		try {


			final PreparedStatement statement = conn.prepareStatement("UPDATE documents SET lastUpdate=? WHERE id=?");
			statement.setTimestamp(1, new Timestamp(new Date().getTime()));
			statement.setInt(2, documentId);
			statement.executeUpdate();
			statement.close();


		}catch(final Exception e){
			final String[] args = {documentId + ""};
			throw new TalesException(new Throwable(), e, args);
		}	

	}




	public final void disableDocument(final int documentId) throws TalesException{


		try {


			final PreparedStatement statement = conn.prepareStatement("UPDATE documents SET active=? WHERE id=?");
			statement.setBoolean(1, false);
			statement.setInt(2, documentId);
			statement.executeUpdate();
			statement.close();


		}catch(final Exception e){
			final String[] args = {documentId + ""};
			throw new TalesException(new Throwable(), e, args);
		}

	}




	public final Document getDocument(final int documentId) throws TalesException{


		try{


			final PreparedStatement statement = conn.prepareStatement("SELECT *,id FROM documents WHERE id = ? LIMIT 1");
			statement.setInt(1, documentId);


			final ResultSet rs                = statement.executeQuery();
			rs.next();


			final Document document = new Document();
			document.setId(rs.getInt("id"));
			document.setName(rs.getString("name"));
			document.setAdded(rs.getTimestamp("added"));
			document.setLastUpdate(rs.getTimestamp("lastUpdate"));
			document.setActive(rs.getBoolean("active"));


			rs.close();
			statement.close();


			return document;


		}catch(final Exception e){
			final String[] args = {documentId + ""};
			throw new TalesException(new Throwable(), e, args);
		}

	}




	public final synchronized int addAttribute(final Attribute attribute) throws TalesException{


		try {


			// checks if the db row xists
			if(!attributeTableExists(attribute.getName())){
				createStringAttributeTable(attribute.getName());
			}


			String tbName                 = Globals.ATTRIBUTE_TABLE_NAMESPACE + attribute.getName();
			tbName                        = tbName.replace(".", "_");


			final PreparedStatement statement = conn.prepareStatement("INSERT INTO " + tbName + " (documentId, data) values (?, ?)", Statement.RETURN_GENERATED_KEYS);
			statement.setInt(1, attribute.getDocumentId());
			statement.setString(2, attribute.getData());
			statement.executeUpdate(); 


			final ResultSet rs = statement.getGeneratedKeys();
			rs.next();


			final int id = rs.getInt(1);


			rs.close();
			statement.close();


			return id;


		}catch(final Exception e){
			final String[] args = {attribute.getId() + "", attribute.getDocumentId() + "", attribute.getData(), attribute.getName()};
			throw new TalesException(new Throwable(), e, args);
		}

	}




	public final synchronized void updateAttribute(final Attribute attribute) throws TalesException{


		try {


			// checks if the db row xists
			if(!attributeTableExists(attribute.getName())){
				createStringAttributeTable(attribute.getName());

			}


			// update
			final boolean attributeExists = attributeExist(attribute.getName(), attribute.getDocumentId());

			if(attributeExists){

				final String lastestData = getAttributeLastestStateData(attribute.getName(), attribute.getDocumentId());

				if (
						((lastestData == null && attribute.getData() != null) || (lastestData != null && attribute.getData() == null))
						|| (lastestData != null && attribute.getData() != null && !lastestData.equals(attribute.getData()))
						){

					addAttribute(attribute);

				}

			}else if(!attributeExists){
				addAttribute(attribute);
			}



		}catch(final Exception e){
			final String[] args = {attribute.getId() + "", attribute.getDocumentId() + "", attribute.getData(), attribute.getName()};
			throw new TalesException(new Throwable(), e, args);
		}

	}




	public final synchronized boolean attributeExist(final String attributeName, final int documentId) throws TalesException{


		if(!attributeTableExists(attributeName)){
			return false;
		}


		try {


			boolean exists                = false;


			String tbName                 = Globals.ATTRIBUTE_TABLE_NAMESPACE + attributeName;
			tbName                        = tbName.replace(".", "_");


			final PreparedStatement statement = conn.prepareStatement("SELECT count(*) FROM " + tbName + " WHERE documentId = ? LIMIT 1");
			statement.setInt(1, documentId);


			final ResultSet rs = statement.executeQuery();
			rs.next();


			if(rs.getInt(1) > 0){
				exists  = true;
			}


			rs.close();
			statement.close();


			return exists;


		}catch(final Exception e){
			final String[] args = {attributeName, documentId + ""};
			throw new TalesException(new Throwable(), e, args);
		}

	}




	public final String getAttributeLastestStateData(final String attributeName, final int documentId) throws TalesException{


		try {


			String tbName                      = Globals.ATTRIBUTE_TABLE_NAMESPACE + attributeName;
			tbName                             = tbName.replace(".", "_");

			final PreparedStatement statement  = conn.prepareStatement("SELECT data FROM " + tbName + " WHERE documentId = ? ORDER BY id DESC LIMIT 1");
			statement.setInt(1, documentId);


			final ResultSet rs                 = statement.executeQuery();
			rs.next();


			final String data = rs.getString("data");


			rs.close();
			statement.close();


			return data;


		}catch(final Exception e){
			final String[] args = {attributeName, documentId + ""};
			throw new TalesException(new Throwable(), e, args);
		}

	}




	public final Attribute getAttributeLastestState(final String attributeName, final int documentId) throws TalesException{


		try {


			String tbName                      = Globals.ATTRIBUTE_TABLE_NAMESPACE + attributeName;
			tbName                             = tbName.replace(".", "_");


			final PreparedStatement statement  = conn.prepareStatement("SELECT * FROM " + tbName + " WHERE documentId = ? ORDER BY id DESC LIMIT 1");
			statement.setInt(1, documentId);


			final ResultSet rs                 = statement.executeQuery();
			rs.next();


			final Attribute attribute     = new Attribute(documentId, attributeName);
			attribute.setId(rs.getInt("id"));
			attribute.setData(rs.getString("data"));
			attribute.setAdded(rs.getTimestamp("added"));


			rs.close();
			statement.close();


			return attribute;


		}catch(final Exception e){
			final String[] args = {attributeName, documentId + ""};
			throw new TalesException(new Throwable(), e, args);
		}

	}




	public final Attribute getAttributeFirstState(final String attributeName, final int documentId) throws TalesException{


		try {


			String tbName                      = Globals.ATTRIBUTE_TABLE_NAMESPACE + attributeName;
			tbName                             = tbName.replace(".", "_");


			final PreparedStatement statement  = conn.prepareStatement("SELECT * FROM " + tbName + " WHERE documentId = ? ORDER BY id ASC LIMIT 1");
			statement.setInt(1, documentId);


			final ResultSet rs                 = statement.executeQuery();
			rs.next();


			final Attribute attribute     = new Attribute(documentId, attributeName);
			attribute.setId(rs.getInt("id"));
			attribute.setData(rs.getString("data"));
			attribute.setAdded(rs.getTimestamp("added"));


			rs.close();
			statement.close();


			return attribute;


		}catch(final Exception e){
			final String[] args = {attributeName, documentId + ""};
			throw new TalesException(new Throwable(), e, args);
		}

	}




	public final ArrayList<Attribute> getAttributeStates(final String attributeName, final int documentId, final int limit) throws TalesException{


		try {


			String tbName                      = Globals.ATTRIBUTE_TABLE_NAMESPACE + attributeName;
			tbName                             = tbName.replace(".", "_");


			final PreparedStatement statement  = conn.prepareStatement("SELECT data, added FROM " + tbName + " WHERE documentId = ? ORDER BY id DESC LIMIT ?");
			statement.setInt(1, documentId);
			statement.setInt(2, limit);


			final ResultSet rs                 = statement.executeQuery();

			ArrayList<Attribute> attributes    = new ArrayList<Attribute>();
			while(rs.next()){

				final Attribute attribute      = new Attribute(documentId, attributeName);
				attribute.setData(rs.getString("data"));
				attribute.setAdded(rs.getTimestamp("added"));
				attributes.add(attribute);

			}


			rs.close();
			statement.close();


			return attributes;


		}catch(final Exception e){
			final String[] args = {attributeName, documentId + "", limit + ""};
			throw new TalesException(new Throwable(), e, args);
		}

	}




	private final synchronized boolean attributeTableExists(final String attributeName) throws TalesException{


		boolean exists = false;


		try {


			String tbName           = Globals.ATTRIBUTE_TABLE_NAMESPACE + attributeName;
			tbName                  = tbName.replace(".", "_");


			if(!cachedTables.get(dbName).contains(tbName)){


				final ResultSet tables = conn.getMetaData().getTables(null, null, tbName, null);
				if(tables.next()){
					exists = true;
					cachedTables.get(dbName).add(tbName);
				}


			}else{
				exists = true;
			}


		}catch(final Exception e){
			final String[] args = {attributeName};
			throw new TalesException(new Throwable(), e, args);
		}


		return exists;

	}




	private synchronized final void createStringAttributeTable(final String attributeName) throws TalesException{


		try {


			String tbName       = Globals.ATTRIBUTE_TABLE_NAMESPACE + attributeName;
			tbName              = tbName.replace(".", "_");

			final Statement statement = (Statement) conn.createStatement();
			statement.executeUpdate("CREATE TABLE " + tbName + " (id INT NOT NULL AUTO_INCREMENT, documentId INT NOT NULL, data VARCHAR( 20000 ) CHARACTER SET utf8 COLLATE utf8_unicode_ci NULL, added TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY (id), KEY documentId (documentId)) ENGINE = MYISAM DEFAULT CHARSET=utf8");
			statement.executeUpdate("OPTIMIZE TABLE " + tbName);
			statement.close();


		}catch(final Exception e){
			final String[] args = {attributeName};
			throw new TalesException(new Throwable(), e, args);
		}
	}




	public final int getDocumentsCount() throws TalesException{


		try {


			final PreparedStatement statement = conn.prepareStatement("SELECT COUNT(*) FROM documents");


			final ResultSet rs            = statement.executeQuery();
			rs.next();


			final int count               = rs.getInt(1);


			rs.close();
			statement.close();            


			return count;


		}catch(final Exception e){
			throw new TalesException(new Throwable(), e);
		}

	}




	private final synchronized void loadDocumentsIntoMem() throws TalesException{


		try{


			int lastId         = 1;
			final Jedis redis  = jedisPool.getResource();


			// wait for redis to be up and running
			while(true){
				try{
					redis.exists("test");
					break;
				}catch(Exception e){
					Logger.log(new Throwable(), "waiting for redis to be up...");
					Thread.sleep(1000);
				}
			}


			// looks for all the documents
			while(true){


				// lastDocumentIndex
				if(redis.exists(dbName + "lastDocumentIndex")){
					lastId = Integer.parseInt(redis.get(dbName + "lastDocumentIndex"));
				}


				Logger.log(new Throwable(), "checking last documentId cached " + lastId);


				// db query
				final PreparedStatement statement = conn.prepareStatement("SELECT id, name FROM documents WHERE id > ? LIMIT 10000"); // no >= needs to be >
				statement.setInt(1, lastId);
				final ResultSet rs                = statement.executeQuery();


				// results
				int id = 0;

				while(rs.next()){

					id               = rs.getInt("id");
					String document  = rs.getString("name");

					try{

						// stores the info in mem
						if(!redis.exists(dbName + document)){
							redis.set(dbName + document, Integer.toString(id));
						}

						redis.set(dbName + "lastDocumentIndex", Integer.toString(id));


					}catch(final Exception e){
						final String[] args = {Integer.toString(id)};
						new TalesException(new Throwable(), e, args);
					}
				}


				rs.close();
				statement.close();


				// if no results. completed!
				if(id == 0){
					break;
				}

			}


			jedisPool.returnResource(redis);


		}catch(final Exception e){
			throw new TalesException(new Throwable(), e);
		}

	}




	public final ArrayList<String> getAttributesNames() throws TalesException{


		try{


			final PreparedStatement statement = conn.prepareStatement("SHOW TABLES");
			final ResultSet rs                = statement.executeQuery();
			final ArrayList<String> tables    = new ArrayList<String>();


			while(rs.next()){

				final String attr = rs.getString(1);

				if(attr.contains(Globals.ATTRIBUTE_TABLE_NAMESPACE)){
					tables.add(attr.replace(Globals.ATTRIBUTE_TABLE_NAMESPACE, ""));
				}

			}


			rs.close();
			statement.close();


			return tables;


		}catch(final Exception e){
			throw new TalesException(new Throwable(), e);
		}

	}




	private final synchronized void createDocumentsTable() throws TalesException{


		try {


			final String sql = "CREATE TABLE documents (id int(11) NOT NULL AUTO_INCREMENT,"
					+ "name varchar(1000) NOT NULL,"
					+ "added timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,"
					+ "lastUpdate timestamp NOT NULL DEFAULT '1999-12-31 17:00:00',"
					+ "active int(2) NOT NULL DEFAULT '1',"
					+ "PRIMARY KEY (id),"
					+ "KEY lastUpdate (lastUpdate),"
					+ "KEY active (active)"
					+ ") ENGINE=InnoDB DEFAULT CHARSET=utf8 AUTO_INCREMENT=1;";


			final Statement statement = (Statement) conn.createStatement();
			statement.executeUpdate(sql);
			statement.close();


		}catch(final Exception e){
			throw new TalesException(new Throwable(), e);
		}

	}




	private final synchronized boolean documentsTableExists() throws TalesException{


		boolean exists          = false;


		try {


			final ResultSet tables = conn.getMetaData().getTables(null, null, "documents", null);
			if(tables.next()){
				exists = true;
			}


		}catch(final Exception e){
			new TalesException(new Throwable(), e);
		}


		return exists;

	}




	private final synchronized void createIgnoredDocumentsTable() throws TalesException{


		try {


			final String sql = "CREATE TABLE ignoredDocuments ("
					+ "id INT NOT NULL AUTO_INCREMENT, "
					+ "name VARCHAR(1000) NOT NULL, "
					+ "added timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP, "
					+ "PRIMARY KEY (id)" 
					+ ") ENGINE = MYISAM DEFAULT CHARSET=utf8 AUTO_INCREMENT=1;";


			final Statement statement = (Statement) conn.createStatement();
			statement.executeUpdate(sql);
			statement.close();


		}catch(final Exception e){
			throw new TalesException(new Throwable(), e);
		}

	}




	private final synchronized boolean ignoredDocumentsTableExists() throws TalesException{


		boolean exists          = false;


		try {


			final ResultSet tables = conn.getMetaData().getTables(null, null, "ignoredDocuments", null);
			if(tables.next()){
				exists = true;
			}


		}catch(final Exception e){
			new TalesException(new Throwable(), e);
		}


		return exists;

	}




	public synchronized final void addIgnoredDocument(final String name) throws TalesException{


		try{


			final PreparedStatement statement = conn.prepareStatement("INSERT INTO ignoredDocuments (name) values (?)");
			statement.setString(1, name);
			statement.executeUpdate();
			statement.close();


		}catch(final Exception e){
			final String[] args = {name};
			throw new TalesException(new Throwable(), e, args);
		}

	}




	public synchronized final boolean ignoredDocumentExists(final String name) throws TalesException{


		try{


			final PreparedStatement statement  = conn.prepareStatement("SELECT count(*) FROM ignoredDocuments WHERE name=? LIMIT 1");
			statement.setString(1, name);


			final ResultSet rs                 = statement.executeQuery();
			rs.next();


			boolean exists                     = false;
			if(rs.getInt(1) > 0){
				exists                         = true;
			}


			rs.close();
			statement.close();


			return exists;


		}catch(final Exception e){
			final String[] args = {name};
			throw new TalesException(new Throwable(), e, args);
		}

	}




	public final void deleteAll() throws TalesException {


		try {


			// db
			Logger.log(new Throwable(), "dropping database");

			final Statement statement = (Statement) conn.createStatement();
			statement.executeUpdate("drop database " + Globals.DATABASE_NAMESPACE + dbName);
			statement.close();


			// redis
			final Jedis redis = jedisPool.getResource();
			final Set<String> keys = redis.keys(dbName + "*");

			Logger.log(new Throwable(), "deleting " + keys.size() + " redis keys");

			for(final String key : keys){
				redis.del(key);
			}

			jedisPool.returnResource(redis);


		}catch(final Exception e){
			throw new TalesException(new Throwable(), e);
		}

	}



	public final static synchronized void closeDBConnections(final String dbName) throws TalesException{


		try {


			// closes the connections
			if(conns.containsKey(dbName)){

				int i = conns.get(dbName).size();

				for(final Connection conn : conns.get(dbName)){
					conn.close();
				}

				conns.remove(dbName);

				Logger.log(new Throwable(), "connections closed " + i);

			}


		}catch(final Exception e){
			throw new TalesException(new Throwable(), e);
		}

	}




	public final ArrayList<Document> getAllDocumentsWithAttributeOrderedByDocumentLastUpdateDesc(final String attributeName) throws TalesException{


		try {


			String tbName                      = Globals.ATTRIBUTE_TABLE_NAMESPACE + attributeName;
			tbName                             = tbName.replace(".", "_");


			final PreparedStatement statement  = conn.prepareStatement("SELECT DISTINCT " + tbName + ".documentId, documents.* FROM " + tbName + ", documents WHERE " + tbName + ".documentId = documents.id ORDER BY lastUpdate DESC;");

			final ResultSet rs                 = statement.executeQuery();

			final ArrayList<Document> documents   = new ArrayList<Document>();
			while(rs.next()){

				final Document document = new Document();
				document.setId(rs.getInt("id"));
				document.setName(rs.getString("name"));
				document.setAdded(rs.getTimestamp("added"));
				document.setLastUpdate(rs.getTimestamp("lastUpdate"));
				document.setActive(rs.getBoolean("active"));

				documents.add(document);

			}


			rs.close();
			statement.close();


			return documents;


		}catch(final Exception e){
			final String[] args = {attributeName};
			throw new TalesException(new Throwable(), e, args);
		}

	}




	public final ArrayList<Document> getAndUpdateLastCrawledDocumentsWithAttribute(final String attributeName, final int number) throws TalesException{


		try {


			String tbName                      = Globals.ATTRIBUTE_TABLE_NAMESPACE + attributeName;
			tbName                             = tbName.replace(".", "_");


			final PreparedStatement statement  = conn.prepareStatement("SELECT DISTINCT " + tbName + ".documentId, documents.* FROM " + tbName + ", documents WHERE " + tbName + ".documentId = documents.id ORDER BY documents.lastUpdate DESC LIMIT ?;");
			statement.setInt(1, number);

			final ResultSet rs                 = statement.executeQuery();

			final ArrayList<Document> documents   = new ArrayList<Document>();
			while(rs.next()){

				final Document document = new Document();
				document.setId(rs.getInt("id"));
				document.setName(rs.getString("name"));
				document.setAdded(rs.getTimestamp("added"));
				document.setLastUpdate(rs.getTimestamp("lastUpdate"));
				document.setActive(rs.getBoolean("active"));

				documents.add(document);

				// update
				updateDocumentLastUpdate(document.getId());

			}


			rs.close();
			statement.close();


			return documents;


		}catch(final Exception e){
			final String[] args = {attributeName};
			throw new TalesException(new Throwable(), e, args);
		}

	}

}