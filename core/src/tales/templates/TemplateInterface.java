package tales.templates;




import tales.services.Connection;
import tales.services.Task;




public interface TemplateInterface {

	
	public void init(Connection connection, Task task);
	public Task getTask();
	public TemplateMetadataInterface getMetadata();
	public void run();
	public boolean isTemplateActive();
	public boolean isTaskValid();
	public boolean hasFailed();
	
	
}
