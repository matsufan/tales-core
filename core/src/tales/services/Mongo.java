package tales.services;




import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import tales.config.Config;
import tales.config.Globals;
import tales.templates.TemplateMetadataInterface;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.MongoClient;




public class Mongo {




	private DBCollection collection;
	private static HashMap<String, DBCollection> clients = new HashMap<String, DBCollection>();




	public Mongo(TemplateMetadataInterface metadata) throws TalesException{
		
		String dbName = metadata.getDatabaseName();

		try{
			
			if(!clients.containsKey(dbName)){
				
				MongoClient client = new MongoClient(Config.getMongoHost(dbName), Config.getMongoPort(dbName));
				DB db = client.getDB(Globals.MONGO_DATABASE_NAME);
				collection = db.getCollection(dbName);
				
				clients.put(dbName, collection);
			
			}else{
				collection = clients.get(dbName);
			}


		}catch(Exception e){
			String[] args = {dbName};
			throw new TalesException(new Throwable(), e, args);
		}

	}




	public void updateAttributes(ArrayList<Attribute> attributes) throws TalesException {

		try{


			// document id
			BasicDBObject doc = new BasicDBObject("id", attributes.get(0).getDocumentId());

			// deletes the current doc
			DBCursor results = collection.find(doc);
			while(results.hasNext()) {
				collection.remove(results.next());
			}

			// user attributes
			for(Attribute attribute : attributes){
				doc.append(attribute.getName(), attribute.getData());
			}

			// added
			doc.append("added", new Date());

			// insert
			collection.insert(doc);


		}catch(Exception e){
			new TalesException(new Throwable(), e);
		}

	}
	
	
	
	
	public void delete(){
	
		try{
			collection.drop();
		}catch(Exception e){
			new TalesException(new Throwable(), e);
		}
		
	}

}
