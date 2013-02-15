package tales.workers;




public class FailoverAttempt {
	
	
	
	
	private int maxFails;
	private int during;
	private int sleep;
	
	
	
	
	public void setMaxFails(int maxFails){
		this.maxFails = maxFails;
	}
	
	
	public int getMaxFails(){
		return maxFails;
	}
	
	
	
	
	public void setDuring(int during){
		this.during = during;
	}
	
	
	public int getDuring(){
		return during;
	}
	
	
	
	
	public void setSleep(int sleep){
		this.sleep = sleep;
	}
	
	public int getSleep(){
		return sleep;
	}
	
}
