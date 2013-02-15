package tales.services;




import java.util.ArrayList;
import java.util.Date;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.common.SolrInputDocument;

import tales.config.Config;
import tales.config.Globals;
import tales.templates.TemplateMetadataInterface;




public class Solr {




	private SolrServer server;
	private static ArrayList<String> cachedIndex = new ArrayList<String>();




	public Solr(TemplateMetadataInterface metadata) throws TalesException{

		String dbName = metadata.getDatabaseName();

		try{

			
			String indexName = Globals.DATABASE_NAMESPACE + dbName;
			String baseURL = "http://" + Config.getSolrHost(dbName) + ":" + Config.getSolrPort(dbName) + "/solr/";
			
			server = new CommonsHttpSolrServer(baseURL + indexName);


			// sees if the solr core/index exists
			if(!cachedIndex.contains(indexName)){

				Download downloader = new Download();
				if(!downloader.urlExists(baseURL + indexName + "/select?q=*:*&rows=0")){

					String url = baseURL + "admin/cores?action=CREATE&name=" + indexName + "&instanceDir=" + Globals.SOLR_INSTANCE_DIR + "&dataDir=" + Globals.SOLR_INDEXES_DIR + "/" + indexName;
					downloader.getURLContent(url);

				}
				
				cachedIndex.add(indexName);

			}
			

		}catch(Exception e){
			String[] args = {dbName};
			throw new TalesException(new Throwable(), e, args);
		}

	}




	public void updateAttributes(ArrayList<Attribute> attributes) throws TalesException {


		try{

			
			SolrInputDocument doc = new SolrInputDocument();
			
			
			// user id
			doc.addField("id", attributes.get(0).getDocumentId());
			
			
			// user attributes
			for(Attribute attribute : attributes){
				doc.addField(attribute.getName(), attribute.getData());
			}
			
			
			// added
			doc.addField("added", new Date());
			
			
			// update request
			UpdateRequest req = new UpdateRequest();
		    req.add(doc);
		    req.setCommitWithin(Globals.SOLR_COMMIT_WITHIN_INTERNAL);
		    req.process(server);


		}catch(Exception e){
			throw new TalesException(new Throwable(), e);
		}

	}
	
	
	
	
	public void delete() throws TalesException{
		
		try{
			
			server.deleteByQuery("*:*");
			server.commit();
			
		}catch(Exception e){
			throw new TalesException(new Throwable(), e);
		}
		
	}

}
