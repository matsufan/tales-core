package tales.services;




import java.util.Date;




public class Attribute {

	
	
	
	private int id;
	private int documentId;
	private String name;
	private String data;
	private Date added = new Date();
	
	
	
	
	public Attribute(int documentId, String name){
		this.documentId = documentId;
		this.name = name;
	}
	
	
	
	
	public void setId(int id){
		this.id = id;
	}
	
	public int getId(){
		return id;
	}
	
	
	
	
	public void setDocumentId(int documentId){
		this.documentId = documentId;
	}
	
	public int getDocumentId(){
		return documentId;
	}
	
	
	
	
	public String getName(){
		return name;
	}
	
	
	
	
	public void setData(String data){
		this.data = data;
	}
	
	public void setData(int data){
		this.data = Integer.toString(data);
	}
	
	public String getData(){
		return data;
	}
	
	
	
	
	public void setAdded(Date added){
		this.added = added;
	}
	
	public Date getAdded(){
		return added;
	}

}