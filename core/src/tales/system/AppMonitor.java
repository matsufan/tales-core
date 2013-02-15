package tales.system;




import java.util.Date;

import tales.services.TalesException;
import tales.services.Logger;
import tales.utils.Average;




public class AppMonitor{




	private static Monitor monitor;




	public static void init(){

		if(monitor == null){
			monitor = new AppMonitor().new Monitor();
			monitor.run();
		}

	}




	public static void stop(){
		monitor.stop();
	}



	
	public static long getExecutionSeconds(){
		return monitor.getExecutionSeconds();
	}

	

	
	private class Monitor implements Runnable{




		private long start;
		private Average cpuAverage;
		private Average memAverage;
		private boolean active = true;




		public Monitor(){

			start = System.currentTimeMillis();
			cpuAverage = new Average(30);
			memAverage = new Average(30);

		}




		public void run(){


			try{

				// total time
				long end = System.currentTimeMillis();
				int totalTime = (int)((end-start) / 1000);


				// averages
				cpuAverage.add(TalesSystem.getServerCPUUsage());
				memAverage.add((float)TalesSystem.getMemoryUsage());


				// print
				Logger.log(new Throwable(), new Date() + " -freeMem: " + TalesSystem.getFreeMemory() + " -memUsed: " + TalesSystem.getMemoryUsage() +  " -serverCPUAvg: " +  cpuAverage.getAverage() + " -totalTime(secs): " + totalTime + " -env: " + TalesSystem.getTemplatesGitBranchName());


				// loop
				if(active){

					Thread.sleep(1000);
					Thread t = new Thread(this);
					t.start();

				}


			}catch(Exception e){
				new TalesException(new Throwable(), e);
			}

		}




		public void stop(){
			Logger.log(new Throwable(), "-executionTime(secs): " + getExecutionSeconds());
			active = false;
		}

		
		
		
		public long getExecutionSeconds(){
			return ((new Date().getTime() - start) / 1000);
		}

	}

}
