package tales.dirlistener;




public class DirListenerObj {
	
	
	
	
	private String dir;
	private String exec;
	private String ignoreRegex;
	
	
	
	
	public void setDir(String dir){
		this.dir = dir;
	}
	
	
	public String getDir(){
		return dir;
	}

	
	public void setExec(String exec){
		this.exec = exec;
	}
	
	
	public String getExec(){
		return exec;
	}
	
	
	public void setIgnoreRegex(String ignoreRegex){
		this.ignoreRegex = ignoreRegex;
	}
	
	
	public String getIgnoreRegex(){
		return ignoreRegex;
	}

}
