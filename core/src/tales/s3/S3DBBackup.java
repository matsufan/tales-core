package tales.s3;




import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;

import tales.config.Config;
import tales.config.Globals;
import tales.services.Logger;
import tales.services.TalesException;
import tales.system.AppMonitor;
import tales.system.TalesSystem;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.PutObjectRequest;




public class S3DBBackup {




	private static AmazonS3 s3 = null;




	public static void backupAllExcept(String s3BucketName, ArrayList<String> excludeDBNames) throws TalesException{


		try{

			// checks if the temp folder exists
			File tempFolder = new File(Globals.DB_BACKUP_TEMP_DIR);
			if(!tempFolder.exists()){
				tempFolder.mkdirs();
			}


			// connects to mysql
			Class.forName("com.mysql.jdbc.Driver");
			Connection conn = DriverManager.getConnection("jdbc:mysql://"+
					"localhost:"+Config.getDBPort()+"/"+
					"mysql" +
					"?user="+Config.getDBUsername() +
					"&password="+Config.getDBPassword() +
					"&useUnicode=true&characterEncoding=UTF-8"
					);

			ResultSet rs = conn.getMetaData().getCatalogs();


			// backups the dbs
			while (rs.next()) {

				String dbName = rs.getString("TABLE_CAT");

				if(!excludeDBNames.contains(dbName)){

					String command = "mysqldump --default-character-set=utf8"
							+ " -u " + Config.getDBUsername()
							+ " -p" + Config.getDBPassword()
							+ " " + dbName + " | gzip > " + Globals.DB_BACKUP_TEMP_DIR + "/" + dbName + ".sql.gz";

					Logger.log(new Throwable(), "backing up \"" + dbName + "\"");

					ProcessBuilder builder = new ProcessBuilder("/bin/sh", "-c", command);
					Process p = builder.start();
					p.waitFor();

				}

			}

			conn.close();


			// uploads the files
			File[] files = new File(Globals.DB_BACKUP_TEMP_DIR).listFiles();
			int i = 1;
			long id = new Date().getTime();


			for(File file : files){


				String s3Name = id  + "-" + TalesSystem.getPublicDNSName() + "-" + file.getName();
				Logger.log(new Throwable(), "uploading " + i +" of "+ files.length + " to bucket \"" + s3BucketName + "\" file \"" + file.getAbsolutePath() + "\" filesize(mb) \"" + (double)(file.length() / (double)(1024L * 1024L)) + "\"");


				// s3 is not really statefull or multithread -- not sure whats happening in the background
				// but this blows up if you make multiple instances
				if(s3 == null){
					s3 = new AmazonS3Client(new BasicAWSCredentials(Config.getAWSAccessKeyId(), Config.getAWSSecretAccessKey()));
				}


				// creates the s3 bucket if it doesnt exists
				if(!s3.doesBucketExist(s3BucketName)){
					Logger.log(new Throwable(), "creating bucket \"" + s3BucketName + "\"");
					s3.createBucket(s3BucketName);
				}


				// stores the file into s3
				s3.putObject(new PutObjectRequest(s3BucketName, s3Name, file));


				// deletes the physical file
				file.delete();

				i++;

			}

			Logger.log(new Throwable(), "finished");


		}catch(Exception e){
			throw new TalesException(new Throwable(), e);
		}

	}




	public static void main(String[] args) throws TalesException {

		try{

			
			Options options = new Options();
			options.addOption("bucket", true, "s3 bucket name");
			options.addOption("exclude_db_names", true, "database names, seperated by comma");

			CommandLineParser parser = new PosixParser();
			CommandLine cmd = parser.parse(options, args);

			String s3Bucket = cmd.getOptionValue("bucket");
			ArrayList<String> excludeDBs = new ArrayList<String>();

			if(cmd.hasOption("exclude_db_names")){
				excludeDBs = new ArrayList<String>(Arrays.asList(cmd.getOptionValue("exclude_db_names").split(",")));
			}

			
			// monitors the app performance
			AppMonitor.init();


			// backups the dbs
			S3DBBackup.backupAllExcept(s3Bucket, excludeDBs);


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