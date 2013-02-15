package tales.system;




import java.util.Date;

import net.sf.json.JSONObject;

import org.apache.commons.io.IOUtils;

import tales.services.Logger;
import tales.services.TalesException;




public class ServerMonitor{




	private static Monitor monitor;




	public static void init(){
		
		if(monitor == null){
			monitor = new ServerMonitor().new Monitor();
			monitor.run();
		}
		
	}




	private class Monitor implements Runnable{




		private long start;




		public Monitor(){
			start = System.currentTimeMillis();
		}




		public void run(){


			try{
				
				
				// uptime / secs
				long uptime = ((new Date().getTime() - start) / 1000);

				
				// mem
				ProcessBuilder builder = new ProcessBuilder("/bin/sh", "-c", "/usr/bin/free -m");
				Process process = builder.start();
				process.waitFor();

				String memory = IOUtils.toString(process.getInputStream(), "utf-8");
				memory = memory.substring(0, memory.indexOf("Swap:"));

				process.destroy();

				
				// cpu
				builder = new ProcessBuilder("/bin/sh", "-c", "/usr/bin/top -b -d1 -n1|grep -i \"Cpu(s)\"");
				process = builder.start();
				process.waitFor();

				String cpu = IOUtils.toString(process.getInputStream(), "utf-8");
				cpu = cpu.substring(cpu.indexOf(":") + 1, cpu.length());

				process.destroy();
				
				
				// hard drive
				builder = new ProcessBuilder("/bin/sh", "-c", "/bin/df -h");
				process = builder.start();
				process.waitFor();

				String disk = IOUtils.toString(process.getInputStream(), "utf-8");
				disk = disk.substring(0, disk.indexOf("none"));

				process.destroy();
				

				// print
				JSONObject json = new JSONObject();
				json.put("uptime", uptime);
				json.put("memory", memory);
				json.put("cpu", cpu);
				json.put("disk", disk);
				Logger.log(new Throwable(), json.toString());

				
				// loop
				Thread.sleep(1000);
				Thread t = new Thread(this);
				t.start();


			}catch(Exception e){
				new TalesException(new Throwable(), e);
			}

		}

	}
	
	
	
	
	public static void main(String[] args) {
		ServerMonitor.init();
	}

}
