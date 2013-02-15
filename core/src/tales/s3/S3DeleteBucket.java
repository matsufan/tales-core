package tales.s3;




import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;

import tales.config.Config;
import tales.services.TalesException;
import tales.services.Logger;
import tales.system.AppMonitor;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;




public class S3DeleteBucket{




	public static void deleteBucket(String s3BucketName) throws TalesException{

		try{


			AmazonS3 s3 = new AmazonS3Client(new BasicAWSCredentials(Config.getAWSAccessKeyId(), Config.getAWSSecretAccessKey()));


			if(s3.doesBucketExist(s3BucketName)) {


				// gets all the s3 objects
				Logger.log(new Throwable(), "getting the list of all the objects saved in the bucket");

				ObjectListing current = s3.listObjects(s3BucketName);
				List<S3ObjectSummary> objSummaries = current.getObjectSummaries();

				while (current.isTruncated()) {
					current = s3.listNextBatchOfObjects(current);
					objSummaries.addAll(current.getObjectSummaries());
				}


				// deletes the files
				int i = 0;
				int accum = 0;
				List<DeleteObjectsRequest.KeyVersion> keys = new ArrayList<DeleteObjectsRequest.KeyVersion>();

				for(S3ObjectSummary objSummary: objSummaries){

					keys.add(new DeleteObjectsRequest.KeyVersion(objSummary.getKey()));
					i++;
					accum++;

					if(i > 500){
						i = 0;
						deleteObjects(s3, s3BucketName, objSummaries, keys, accum);
						keys.clear();
					}

				}


				if(keys.size() > 0){
					deleteObjects(s3, s3BucketName, objSummaries, keys, accum);
				}


				// deletes the bucket
				s3.deleteBucket(s3BucketName);


			}else{
				Logger.log(new Throwable(), "bucket not found");
			}


		}catch(Exception e){
			throw new TalesException(new Throwable(), e);
		}

	}




	private static void deleteObjects(AmazonS3 s3, String s3BucketName, List<S3ObjectSummary> objSummaries, List<DeleteObjectsRequest.KeyVersion> keys, int accum){

		Logger.log(new Throwable(), "deleting " + (objSummaries.size() - accum) + " objects...");
		DeleteObjectsRequest deleteResquests = new DeleteObjectsRequest(s3BucketName);
		deleteResquests.setKeys(keys);
		s3.deleteObjects(deleteResquests);

	}




	public static void main(String[] args) throws TalesException {

		try{

			
			Options options = new Options();		
			options.addOption("bucket", true, "s3 bucket name");

			CommandLineParser parser = new PosixParser();
			CommandLine cmd = parser.parse(options, args);

			String s3Bucket = cmd.getOptionValue("bucket");


			// monitors the app performance
			AppMonitor.init();


			// deletes the bucket content
			S3DeleteBucket.deleteBucket(s3Bucket);


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
