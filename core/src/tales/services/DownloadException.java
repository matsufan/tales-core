package tales.services;




public class DownloadException extends Exception {



	
	private static final long serialVersionUID = 1L;



	
	public DownloadException(Throwable origin, Exception error){
		try {
        	Logger.downloadError(origin, error);
        } catch (Exception j) {}
    }
    
    
    
    
    public DownloadException(Throwable origin, Exception error, String args[]){
    	try {
        	Logger.downloadError(origin, error, args);
		} catch (Exception j) {}
    }
    
}