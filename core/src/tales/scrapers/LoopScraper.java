package tales.scrapers;




import java.util.ArrayList;
import java.util.Date;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;

import tales.config.Config;
import tales.services.Connection;
import tales.services.TalesDB;
import tales.services.TalesException;
import tales.services.Logger;
import tales.services.Task;
import tales.services.TasksDB;
import tales.services.Document;
import tales.system.AppMonitor;
import tales.templates.TemplateInterface;
import tales.workers.TaskWorker;




public class LoopScraper {




	private static TalesDB talesDB;
	private static TasksDB tasksDB;
	private static Date loopReferenceTime;
	private static TaskWorker taskWorker;




	public static void init(ScraperConfig scraperConfig) throws TalesException{


		try{

			// inits the services
			talesDB = new TalesDB(scraperConfig.getConnection(), scraperConfig.getTemplate().getMetadata());
			tasksDB = new TasksDB(scraperConfig);


			// starts the task machine with the template
			taskWorker = new TaskWorker(scraperConfig);
			taskWorker.init();


			loopReferenceTime = talesDB.getMostRecentCrawledDocuments(1).get(0).getLastUpdate();


			while(!taskWorker.hasFailover()){

				// adds tasks
				if((tasksDB.count() + taskWorker.getTasksRunning().size()) < Config.getMinTasks()){

					ArrayList<Task> tasks = getTasks();

					if(tasks.size() > 0){

						Logger.log(new Throwable(), "adding tasks to \"" + scraperConfig.getTaskName() + "\"");

						tasksDB.add(tasks);

						if(!taskWorker.isWorkerActive() && !taskWorker.hasFailover()){
							taskWorker = new TaskWorker(scraperConfig);
							taskWorker.init();
						}
					}

				}


				// if no tasks means we are finished
				if((tasksDB.count() + taskWorker.getTasksRunning().size()) == 0){
					break;
				}


				try{Thread.sleep(1000);
				}catch(Exception e){new TalesException(new Throwable(), e);}

			}

		}catch(Exception e){
			throw new TalesException(new Throwable(), e);
		}

	}




	private static ArrayList<Task> getTasks() throws TalesException{

		ArrayList<Task> tasks = new ArrayList<Task>();

		for(Document document : talesDB.getAndUpdateLastCrawledDocuments(Config.getMaxTasks())){

			// checks if the most recently crawled user is older than this new user, 
			// this means that the "most recent user" is now old and we have looped			
			if(loopReferenceTime.getTime() >= document.getLastUpdate().getTime()){

				Task task = new Task();
				task.setDocumentId(document.getId());
				task.setDocumentName(document.getName());

				tasks.add(task);

			}

		}

		return tasks;

	}




	public static void main(String[] args) throws TalesException{

		try{

			Options options = new Options();
			options.addOption("template", true, "template class path");
			options.addOption("threads", true, "number of templates");
			CommandLineParser parser = new PosixParser();
			CommandLine cmd = parser.parse(options, args);

			String templatePath = cmd.getOptionValue("template");
			int threads = Integer.parseInt(cmd.getOptionValue("threads"));


			// when app is killed
			Runtime.getRuntime().addShutdownHook(new Thread() {

				public void run() {

					if(taskWorker != null){
						taskWorker.stop();
					}

					Logger.log(new Throwable(), "---> bye...");

				}
			});


			// monitors the app performance
			AppMonitor.init();


			// reflection / new template
			TemplateInterface template = (TemplateInterface) Class.forName(templatePath).newInstance();

			
			// connection
			Connection connection = new Connection();
			connection.setConnectionsNumber(threads);


			// scraper config
			ScraperConfig scraperConfig = new ScraperConfig();
			scraperConfig.setScraperName("LoopScraper");
			scraperConfig.setTemplate(template);
			scraperConfig.setConnection(connection);


			// scraper
			LoopScraper.init(scraperConfig);


			// stop
			AppMonitor.stop();
			System.exit(0);


		}catch(Exception e){
			AppMonitor.stop();
			System.exit(0);
			throw new TalesException(new Throwable(), e);
		}

	}

}
