package tales.s3;




import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;

import tales.config.Config;
import tales.config.Globals;
import tales.services.TalesException;
import tales.services.Logger;
import tales.system.AppMonitor;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.mysql.jdbc.Statement;




public class S3DBRestore{




	private static AmazonS3 s3 = null; // its not thread safe, 1 instance should be use for all the connections




	public static void restoreLastestDBs(String s3BucketName, ArrayList<String> dbNames) throws TalesException{

		try{

			for(int i = 0; i < dbNames.size(); i++){	

				Logger.log(new Throwable(), "processing " + (i + 1) + " of " + dbNames.size());

				String dbName = dbNames.get(i);
				restoreLastestDB(s3BucketName, dbName);

			}

		}catch(Exception e){
			throw new TalesException(new Throwable(), e);
		}

	}




	public static void restoreLastestDB(String s3BucketName, String dbName) throws TalesException{


		try{


			Logger.log(new Throwable(), "restoring latest version of \"" + dbName + "\"");


			// checks if the temp folder exists
			File tempFolder = new File(Globals.DB_RESTORE_TEMP_DIR);
			if(!tempFolder.exists()){
				tempFolder.mkdirs();
			}


			// s3 is not really statefull or multithread -- not sure whats happening in the background
			// but this blows up if you make multiple instances
			if(s3 == null){
				s3 = new AmazonS3Client(new BasicAWSCredentials(Config.getAWSAccessKeyId(), Config.getAWSSecretAccessKey()));
			}


			if(s3.doesBucketExist(s3BucketName)){


				ArrayList<RestoreObj> s3Objs = getS3FilenamesOrderByAdded(s3BucketName, dbName);


				if(s3Objs.size() > 0){

					
					RestoreObj restoreObj = s3Objs.get(0);
					String filePath = Globals.DB_RESTORE_TEMP_DIR + "/" + restoreObj.getName();


					// downloads the file
					S3Object s3Obj = s3.getObject(s3BucketName, restoreObj.getName());
					InputStream reader = new BufferedInputStream(s3Obj.getObjectContent());
					File file = new File(filePath); 


					// deletes the old file
					if(file.exists()){
						file.delete();
					}


					// downloads the file
					OutputStream writer = new BufferedOutputStream(new FileOutputStream(file));

					Logger.log(new Throwable(), "downloading file \"" + restoreObj.getName() + "\" added \"" + restoreObj.getDate() + "\"");

					int read = -1;

					while ((read = reader.read()) != -1) {
						writer.write(read);
					}

					writer.flush();
					writer.close();
					reader.close();


					// starts a local connection
					Class.forName("com.mysql.jdbc.Driver");
					Connection conn = DriverManager.getConnection("jdbc:mysql://"+
							"localhost:" + Config.getDBPort()+"/"+
							"mysql" +
							"?user="+ Config.getDBUsername() +
							"&password=" + Config.getDBPassword() +
							"&useUnicode=true&characterEncoding=UTF-8"
							);


					// checks if database exists, if exists then delete
					Statement statement = (Statement) conn.createStatement();
					ResultSet rs  = statement.executeQuery("SHOW DATABASES LIKE '" + dbName + "'");

					try{

						rs.next();
						rs.close();
						statement.close();

						// deletes the current db
						Logger.log(new Throwable(), "droppping mysql database \"" + dbName + "\"");

						statement = (Statement) conn.createStatement();
						statement.executeUpdate( "DROP DATABASE " + dbName);
						statement.close();


					}catch(Exception e){
						rs.close();
						statement.close();
					}


					// creates new database
					Logger.log(new Throwable(), "creating mysql database \"" + dbName + "\"");

					statement = (Statement) conn.createStatement();
					statement.executeUpdate("CREATE DATABASE " + dbName);
					statement.close();


					// restores the dump to mysql
					Logger.log(new Throwable(), "restoring file \"" + restoreObj.getName() + "\" to \"" + dbName + "\"");

					String command = "gunzip < " + filePath + " | mysql -u " + Config.getDBUsername() + " -p" + Config.getDBPassword() + " " + dbName + " --default-character-set=utf8";
					ProcessBuilder builder = new ProcessBuilder("/bin/sh", "-c", command);
					Process p = builder.start();
					p.waitFor();
					p.destroy();


					// deletes the s3 file and close db
					file.delete();
					conn.close();

					Logger.log(new Throwable(), "finished");


					// mysql is slow, lets wait for him to finish
					Thread.sleep(5000);


				}else{
					Logger.log(new Throwable(), "file not found");
				}

				
			}else{
				Logger.log(new Throwable(), "bucket doesnt exists");
			}


		}catch(Exception e){
			throw new TalesException(new Throwable(), e);
		}

	}




	// returns the list of s3Filenames (also call keys) order by added date
	private static ArrayList<RestoreObj> getS3FilenamesOrderByAdded(String s3BucketName, String originalFilename){


		// gets all the s3 objects
		ObjectListing current = s3.listObjects(s3BucketName);
		List<S3ObjectSummary> objSummaries = current.getObjectSummaries();

		while (current.isTruncated()) {
			current = s3.listNextBatchOfObjects(current);
			objSummaries.addAll(current.getObjectSummaries());
		}


		ArrayList<RestoreObj> s3Objs = new ArrayList<RestoreObj>();

		for(S3ObjectSummary objSummary: objSummaries){

			String s3Filename     = objSummary.getKey();
			String[] pieces       = objSummary.getKey().split("-");
			Date added            = new Date(Long.parseLong(pieces[0]));
			//String ip             = pieces[1];
			String filename       = pieces[2];

			if(filename.contains(originalFilename)){

				RestoreObj restoreObj = new RestoreObj();
				restoreObj.setName(s3Filename);
				restoreObj.setDate(added);
				s3Objs.add(restoreObj);

			}

		}


		// desc
		Collections.reverse(s3Objs);

		return s3Objs;

	}




	public static void main(String[] args) throws TalesException {

		try{


			Options options = new Options();
			options.addOption("bucket", true, "s3 bucket name");
			options.addOption("db_names", true, "database names, seperated by comma");

			CommandLineParser parser = new PosixParser();
			CommandLine cmd = parser.parse(options, args);

			String s3Bucket = cmd.getOptionValue("bucket");
			String[] dbNames = cmd.getOptionValue("db_names").split(",");


			// monitors the app performance
			AppMonitor.init();


			// restore lastest dbs
			S3DBRestore.restoreLastestDBs(s3Bucket, new ArrayList<String>(Arrays.asList(dbNames)));


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
