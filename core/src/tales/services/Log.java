package tales.services;

import java.util.Date;




public class Log {
	
	
	
	
	private int id;
	private String publicDNS;
	private int pid;
	private String logType;
	private String methodPath;
	private int lineNumber;
	private String data;
	private Date added;
	
	
	
	
	public void setId(int id){
		this.id = id;
	}
	
	public int getId(){
		return id;
	}
	
	
	
	
	public void setPublicDNS(String publicDNS){
		this.publicDNS = publicDNS;
	}
	
	public String getPublicDNS(){
		return publicDNS;
	}
	
	
	
	
	public void setPid(int pid){
		this.pid = pid;
	}
	
	public int getPid(){
		return pid;
	}
	
	
	
	
	public void setLogType(String logType){
		this.logType = logType;
	}
	
	public String getLogType(){
		return logType;
	}
	
	
	
	
	public void setMethodPath(String methodPath){
		this.methodPath = methodPath;
	}
	
	public String getMethodPath(){
		return methodPath;
	}
	
	
	
	
	public void setLineNumber(int lineNumber){
		this.lineNumber = lineNumber;
	}
	
	public int getLineNumber(){
		return lineNumber;
	}
	
	
	
	
	public void setData(String data){
		this.data = data;
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
