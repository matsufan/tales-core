package tales.system;




import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.List;

import org.apache.commons.io.IOUtils;

import tales.config.Config;
import tales.services.TalesException;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;




public class TalesSystem {




	private static String serverIP;
	private static OperatingSystemMXBean osbean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
	private static RuntimeMXBean runbean = (RuntimeMXBean) ManagementFactory.getRuntimeMXBean();
	private static int nCPUs = osbean.getAvailableProcessors();
	private static String branchName;
	private static Instance instance;
	private static long prevUpTime = runbean.getUptime();
	private static float lastResult = 0;
	private static int pid = 0;
	private static long prevProcessCpuTime = ((com.sun.management.OperatingSystemMXBean) osbean).getProcessCpuTime();
	private static String processName;




	public static float getServerCPUUsage() {

		long upTime = runbean.getUptime();
		long processCpuTime = ((com.sun.management.OperatingSystemMXBean) osbean).getProcessCpuTime();

		if (prevUpTime > 0L && upTime > prevUpTime && processCpuTime > prevProcessCpuTime && upTime > 0L && processCpuTime > 0L) {

			long elapsedCpu = processCpuTime - prevProcessCpuTime;
			long elapsedTime = upTime - prevUpTime;

			float cpuUsage = Math.min(99F, elapsedCpu / (elapsedTime * 10000F * nCPUs));
			lastResult = cpuUsage;

		}

		prevUpTime = upTime;
		prevProcessCpuTime = processCpuTime;
		return lastResult;

	}




	public static float getFreeMemory() {
		return (Runtime.getRuntime().freeMemory()) / 1048576;
	}




	public static double getMemoryUsage() {
		return (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1048576;
	}




	public static int getPid() {

		if (pid == 0) {
			String name = runbean.getName();
			String[] parts = name.split("@");
			pid = Integer.parseInt(parts[0]);
		}

		return pid;

	}




	public static String getPublicDNSName() throws TalesException{

		try{

			Exception error = null;

			try{
				if(TalesSystem.getAWSInstanceMetadata() != null){
					return TalesSystem.getAWSInstanceMetadata().getPublicDnsName();
				}	
			}catch(Exception e){
				error = e;
			}


			if (serverIP == null) {

				serverIP = InetAddress.getLocalHost().getHostAddress();

				Enumeration<NetworkInterface> n = NetworkInterface.getNetworkInterfaces();
				while(n.hasMoreElements()){

					NetworkInterface e = n.nextElement();
					Enumeration<InetAddress> a = e.getInetAddresses();

					while(a.hasMoreElements()){

						InetAddress addr = a.nextElement();
						String publicIP = addr.getHostAddress();

						if(publicIP.split("\\.").length == 4 && !publicIP.startsWith("10") && !publicIP.startsWith("127")){
							serverIP = publicIP;
							return serverIP;
						}

					}

				}

			}
			
			if(error != null){
				new TalesException(new Throwable(), error);
			}

			return serverIP;

		}catch( Exception e){
			throw new TalesException(new Throwable(), e);
		}

	}




	public static String getTemplatesGitBranchName() throws TalesException{


		try{

			if(branchName == null){

				Process process = null;

				try{

					// linux
					ProcessBuilder builder = new ProcessBuilder("/usr/lib/git-core/git", "--git-dir", System.getProperty("user.home") + "/tales-templates/.git", "branch");
					process = builder.start();

					String output = IOUtils.toString(process.getInputStream());
					process.destroy();

					int ini = output.indexOf("*");
					int end = output.indexOf("\n", ini);
					branchName = output.substring(ini + 2, end).trim(); // 2 cuz of * + space (example: * master);

				}catch(Exception e){
					branchName = "development";
				}

			}

			return branchName;


		}catch( Exception e){
			throw new TalesException(new Throwable(), e);
		}

	}




	public static Instance getAWSInstanceMetadata() throws Exception{

		if(instance != null){
			return instance;
		}

		AmazonEC2 ec2 = new AmazonEC2Client(new BasicAWSCredentials(Config.getAWSAccessKeyId(), Config.getAWSSecretAccessKey()));

		DescribeInstancesResult describeInstancesRequest = ec2.describeInstances();
		List<Reservation> reservations = describeInstancesRequest.getReservations();

		for(Reservation reservation : reservations) {

			for(Instance instance : reservation.getInstances()){

				if(instance.getPrivateIpAddress() != null && instance.getPrivateIpAddress().equals(InetAddress.getLocalHost().getHostAddress())){
					TalesSystem.instance = instance;
					return instance;
				}

			}

		}

		return null;

	}




	public static String getProcess() {

		if(processName == null){

			try{

				ProcessBuilder builder = new ProcessBuilder("/bin/sh", "-c", "/bin/ps aux | grep " + TalesSystem.getPid());
				Process process = builder.start();
				process.waitFor();

				processName = IOUtils.toString(process.getInputStream(), "utf-8");

				String find = "jar";
				int ini = processName.indexOf(find) + find.length() + 1;
				int end = processName.indexOf("root", ini);
				processName = processName.substring(ini, end - 1);

				process.destroy();

			}catch( Exception e){
				new TalesException(new Throwable(), e);
			}

		}

		return processName;
	}

}
