package tales.services;




import java.util.Date;




public class Document {

	
	
	
	private int id;
	private String name;
	private Date added;
	private Date lastUpdate;
	private boolean active;
	
	
	
	
	public void setId(int id){
		this.id = id;
	}
	
	public int getId(){
		return id;
	}
	
	
	
	
	public void setName(String name){
		this.name = name;
	}
	
	public String getName(){
		return name;
	}
	
	
	
	
	public void setAdded(Date added){
		this.added = added;
	}
	
	public Date getAdded(){
		return added;
	}
	
	
	
	
	public void setLastUpdate(Date lastUpdate){
		this.lastUpdate = lastUpdate;
	}
	
	public Date getLastUpdate(){
		return lastUpdate;
	}
	
	
	
	
	public void setActive(boolean active){
		this.active = active;
	}
	
	public boolean getActive(){
		return active;
	}
	
}