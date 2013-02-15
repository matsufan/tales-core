package tales.workers;




import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import tales.config.Config;
import tales.scrapers.ScraperConfig;
import tales.services.TalesException;
import tales.services.Logger;
import tales.services.Task;
import tales.services.TasksDB;
import tales.templates.TemplateInterface;
import tales.utils.Average;




public class TaskWorker{




	private ScraperConfig config;
	private FailoverInterface failover;
	private Worker worker;




	public TaskWorker(ScraperConfig config) throws TalesException{
		this.config = config;
		this.failover = new DefaultFailover(Config.getFailover(config.getTemplate().getMetadata().getDatabaseName()));
	}




	public TaskWorker(ScraperConfig config, FailoverInterface failover){
		this.config = config;
		this.failover = failover;
	}




	public void init() throws TalesException{

		if(worker == null){

			worker = new Worker(config, failover);
			Thread t = new Thread(worker);
			t.start();

		}

	}




	public boolean isWorkerActive(){
		return worker.isWorkerActive();
	}




	public void stop(){
		worker.stop();
	}




	public ArrayList<Task> getTasksRunning(){
		return worker.getTasksRunning();
	}




	public boolean hasFailover(){
		return failover.hasFailover();
	}




	private class Worker implements Runnable{




		private ArrayList<TemplateInterface> threads;
		private TasksDB taskDB;
		private ScraperConfig config;
		private Average processAverage;
		private int processed;
		private int processedOld;
		private int loops;
		private int averageLoop;
		private boolean stop;
		private FailoverInterface failover;




		public Worker(ScraperConfig config, FailoverInterface failover) throws TalesException{

			this.config                = config;
			this.failover              = failover;
			stop                       = false;
			threads                    = new ArrayList<TemplateInterface>();
			processAverage             = new Average(20);
			taskDB                     = new TasksDB(config);

		}




		public void run() {

			try {


				if(!stop && !failover.isFallingOver()){


					int tasksPending = taskDB.count();


					if(tasksPending > 0){


						// checks the threads
						ArrayList<TemplateInterface> tempThreads = new ArrayList<TemplateInterface>();
						for(TemplateInterface thread : threads){

							if(thread.isTemplateActive()){
								tempThreads.add(thread);

							}else if(thread.hasFailed()){
								failover.fail();

							}

						}
						threads = tempThreads;


						// calcs the thread number
						int maxThreads = config.getConnection().getConnectionsNumber() - threads.size();
						if(maxThreads < 0){maxThreads = 0;}


						if(maxThreads > 0){


							for(Task task : taskDB.getList(maxThreads)){

								// template
								TemplateInterface template = (TemplateInterface) config.getTemplate().getClass().newInstance();
								template.init(config.getConnection(), task);

								if(template.isTaskValid()){

									threads.add(template);

									Thread t = new Thread((Runnable)template);
									t.start();

									processed++;

								}

								// deletes the task from the queue
								taskDB.deleteTaskWithDocumentId(task.getDocumentId());

							}

						}


						//-----------------------------------------
						// LOOP -----------------------------------
						//-----------------------------------------

						// process per second
						if(loops == 20){ // num = secs

							processAverage.add((processed - processedOld));
							processedOld = processed;
							loops = 0;
							averageLoop++;


							Logger.log(new Throwable(), "-taskName: " + config.getTaskName() 
									+ " -processPerSecond: " + processAverage.getAverage() 
									+ " -processed: " + processed 
									+ " -tasksPending: " + tasksPending 
									+ " -maxThreads: " + config.getConnection().getConnectionsNumber());

						}

						loops++;


						// average loop reset
						if(averageLoop == 2000){
							averageLoop = 0;
							processAverage = new Average(20);
						}


						// loop
						Thread.sleep(50);
						Thread t = new Thread(this);
						t.start();


					}else{

						if(threads.size() == 0){
							stop = true;

						}else{
							Thread.sleep(50);
							Thread t = new Thread(this);
							t.start();
						}

					}

				}


			} catch (Exception e) {
				new TalesException(new Throwable(), e);
			}
		}




		// returns of the machine is active
		public boolean isWorkerActive(){
			return !stop;
		}




		// stops
		public void stop(){

			stop = true;
			boolean finished = false;

			while(!finished){

				Logger.log(new Throwable(), "waiting for the tasks to finish...");

				finished = true;
				List<TemplateInterface> threadList = threads;

				for(Iterator<TemplateInterface> it = threadList.iterator(); it.hasNext();){

					TemplateInterface template = it.next();

					if(template.isTemplateActive()){
						finished = false;
						break;
					}

				}

				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}




		// get all active tasks
		public ArrayList<Task> getTasksRunning(){

			ArrayList<Task> tasks = new ArrayList<Task>();

			for(TemplateInterface template : threads){

				if(template.isTemplateActive()){
					tasks.add(template.getTask());
				}

			}

			return tasks;

		}

	}

}