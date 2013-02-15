package tales.dirlistener;




import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.vfs2.FileChangeEvent;
import org.apache.commons.vfs2.FileListener;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.VFS;
import org.apache.commons.vfs2.impl.DefaultFileMonitor;

import tales.config.Config;
import tales.config.Globals;
import tales.services.TalesException;
import tales.services.Logger;




public class DirListener {




	private static boolean inited = false;
	private static HashMap<String, Exec> execs = new HashMap<String, Exec>();




	public static void init() throws TalesException {

		try{

			if(!inited){

				inited = true;

				for(DirListenerObj dirObj : Config.getDirListenerList()){

					String dir = dirObj.getDir().replace("~", System.getProperty("user.home"));

					FileSystemManager fsManager = VFS.getManager();
					FileObject listendir = fsManager.resolveFile(dir);
					DefaultFileMonitor fm = new DefaultFileMonitor(new OnChange(dirObj));
					fm.setRecursive(true);
					fm.addFile(listendir);
					fm.start();
					
				}


				// prevents the app of exiting
				while(true){
					Thread.sleep(1000);
				}

			}

		}catch(Exception e){
			throw new TalesException(new Throwable(), e);
		}

	}




	private static class OnChange implements FileListener {

		private DirListenerObj dirObj;

		public OnChange(DirListenerObj dirObj){
			this.dirObj = dirObj;
		}

		public void fileChanged(FileChangeEvent e) throws Exception {
			onChange(e);
		}

		public void fileCreated(FileChangeEvent e) throws Exception {
			onChange(e);
		}

		public void fileDeleted(FileChangeEvent e) throws Exception {
			onChange(e);
		}

		public void onChange(FileChangeEvent e) throws Exception{

			try{

				Pattern pattern = Pattern.compile(dirObj.getIgnoreRegex());
				Matcher matcher = pattern.matcher(e.getFile().toString());

				if(!matcher.find()){

					Logger.log(new Throwable(), "change found in directory \"" + e.getFile().toString() + "\"");

					// clears the current exec
					if(execs.containsKey(dirObj.getDir())){
						execs.get(dirObj.getDir()).stop();
						execs.get(dirObj.getDir()).cancel();
						execs.remove(dirObj.getDir());
					}

					Timer timer = new Timer();
					Exec exec = new Exec(dirObj);
					timer.schedule(exec, Globals.DIR_SYNC_REFESH_INTERVAL);

					execs.put(dirObj.getDir(), exec);

				}

			}catch(Exception exp){
				throw new TalesException(new Throwable(), exp);
			}

		}

	}




	public static class Exec extends TimerTask{

		private DirListenerObj dirObj;
		private Process process;

		public Exec(DirListenerObj dirObj){
			this.dirObj = dirObj;
		}

		public void run() {

			try{

				Logger.log(new Throwable(), "executing \"" + dirObj.getExec() + "\" in directory \"" + dirObj.getDir());

				ProcessBuilder builder = new ProcessBuilder("/bin/sh", "-c", dirObj.getExec());
				process = builder.start();
				process.waitFor();
				process.destroy();

				Logger.log(new Throwable(), "directory \"" + dirObj.getDir() + "\" finished");


			}catch(Exception e){
				new TalesException(new Throwable(), e);
			}

		}

		public void stop(){
			if(process != null){
				process.destroy();
			}
		}

	}




	public static void main(String[] args) throws TalesException {

		try{
			DirListener.init();
		}catch(Exception e){
			throw new TalesException(new Throwable(), e);
		}
		
	}

}
