package tales.services;




public class TalesException extends Exception {



	
	private static final long serialVersionUID = 1L;




	public TalesException(Throwable origin, Exception error){
		try {
        	Logger.error(origin, error);
        } catch (Exception j) {}
    }
    
    
    
    
    public TalesException(Throwable origin, Exception error, String args[]){
    	try {
        	Logger.error(origin, error, args);
		} catch (Exception j) {}
    }
    
}