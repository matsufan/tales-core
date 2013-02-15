package tales.scrapers;




import tales.services.Connection;
import tales.templates.TemplateInterface;




public class ScraperConfig {
	
	
	
	
	private String scraperName;
	private TemplateInterface template;
	private Connection connection;
	
	
	
	
	public void setScraperName(String scraperName){
		this.scraperName = scraperName;	
	}
	
	
	
	
	public void setTemplate(TemplateInterface template){
		this.template = template;
	}
	
	
	
	
	public TemplateInterface getTemplate(){
		return template;
	}
	
	
	
	
	public void setConnection(Connection connection){
		this.connection = connection;
	}
	
	
	
	
	public Connection getConnection() {
		return connection;
	}
	
	
	
	
	public String getTaskName(){
		return scraperName + "_" + template.getMetadata().getDatabaseName();
	}
	
}
