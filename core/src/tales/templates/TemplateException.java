package tales.templates;




import tales.services.Logger;




public class TemplateException extends Exception{

	

	
	private static final long serialVersionUID = 1L;

	
	
	
	public TemplateException(Throwable origin, Exception error, int documentId){

		try {
			Logger.templateError(origin, error, documentId);
		} catch (Exception j) {
			j.printStackTrace();
		}

	}

}
