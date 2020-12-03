package com.BH.monitor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.mail.Message.RecipientType;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.net.telnet.TelnetClient;

import com.sun.mail.util.logging.MailHandler;

import wt.manager.RemoteServerManager;
import wt.manager.ServerLauncher;
import wt.manager.ServerMonitors;
import wt.manager.ServiceStat;
import wt.method.RemoteMethodServer;
import wt.util.WTException;
import wt.util.WTProperties;

public class BHDailyMonitoringStarter {

	private static final String BR = "<br />";

	private static final String B_START = "<b>";

	private static final String B_END = "</b>";


	static Properties dmProperties = new Properties();
	static Properties wtproperties  = new Properties(); 

	public static InputStream input = null;

	public static String wtHostName = "";

	/** E-Mail**/
	public static String mailServerHost = "";

	public static String mailServerPort = "";

	public static String mailFrom = "";
	public static String mailFromMaintenance ="";

	public static HashSet<String> recipients = new HashSet<String> ();
	public static HashSet<String> recipientsMaintenance = new HashSet<String> ();
	public static Boolean isEmailEnabled;


	/** Queue **/
	public static String skipQ = "";

	public static Integer queueEntrylastMinutes ;


	/** WVS **/
	public static Integer wvsMaxExeTime;

	public static Integer wvslastMinutes;


	/** WVS Maintenance **/
	public static Integer autoCleanJobsOlderThan;

	public static Boolean isautoCleanJobsEnabled = false;


	/** Disk Space **/
	public static String wtMinFreeSpaceLimitInGB="";

	public static String wtMinFreeSpaceInPercentage="";

	public static Long fvMinFreeSpaceLimitInGB = Long.valueOf(10);

	public static Long fvMinFreeSpaceInPercentage =Long.valueOf(10);

	public static String logsDirSizeLimit ="";

	/** Maintenance **/
	public static Integer autoCleanReportOlderThan;

	public static Boolean isautoCleanReportsEnabled = false;

	public static String reportDir = "";

	/** Enable /Disable **/
	public static Boolean isCheckServerStatusPgEnabled = false;

	public static Boolean isCompleteQueueCounEnabled = false;

	public static Boolean isQueueEntryDetailsEnabled = false;

	public static Boolean isQueueRunningStatusEnabled = false;

	public static Boolean isCADWorkerStatusEnabled = false;

	public static Boolean isFileServersEnabled = false;

	public static Boolean isCheckMasterDiskSpaceEnabled = false;

	public static Boolean ischeckLogsDirectorySizeEnabled = false;

	public static Boolean isVaultStatusEnabled = false;

	public static Boolean isJobStatusEnabled = false;


	/** Debug **/
	public static Boolean monitoringVerbose = false;

	public static Boolean isDynamicSenderIdEnabled = false;

	public static Boolean isMaintenance = false;

	private static Vector services = null;
	private static ServerMonitors sm = null;
	static{
		try {
			input = BHDailyMonitoringStarter.class.getClassLoader().getResourceAsStream("com" + File.separator + "BH"+ File.separator+ "monitor"+ File.separator + "monitor_01.BH");
			dmProperties.load(input);
			wtproperties = WTProperties.getLocalProperties();

			mailServerHost = wtproperties.getProperty("wt.mail.mailhost","localhost");
//			mailServerPort = wtproperties.getProperty("wt.mail.port","");

			if(mailServerHost.equalsIgnoreCase("localhost"))
			{
				mailServerHost = dmProperties.getProperty("wt.monitoring.mailhost","localhost");
			}

//			mailServerPort = wtproperties.getProperty("wt.mail.port","");
			if(mailServerPort.equalsIgnoreCase(""))
			{
				mailServerPort= dmProperties.getProperty("wt.monitoring.mailport","25");
			}

			wtHostName = wtproperties.getProperty("java.rmi.server.hostname");

			isEmailEnabled = Boolean.valueOf(dmProperties.getProperty("wt.monitor.mail.Enabled","false"));


			//for WC 10.2
			if(wtHostName == null)
				wtHostName = wtproperties.getProperty("wt.rmi.server.hostname");
			mailFrom =  dmProperties.getProperty("wt.monitor.MailFrom","DailyMonitoring");

			if(dmProperties.getProperty("wt.monitor.mail.recipients",null) != null)
			{
				if(dmProperties.getProperty("wt.monitor.mail.recipients").contains(","))
					recipients.addAll(Arrays.asList((dmProperties.getProperty("wt.monitor.mail.recipients").split(","))));
				else
				{
					recipients.add(dmProperties.getProperty("wt.monitor.mail.recipients"));					
				}
			}
			else
				isEmailEnabled=false;


			if(!mailFrom.contains("@"))
				mailFrom =  dmProperties.getProperty("wt.monitor.MailFrom","DailyMonitoring") +"@"+ wtHostName;

			reportDir = dmProperties.getProperty("itc.monitor.report.location","C:\\temp");
			skipQ = dmProperties.getProperty("wt.monitor.queue.skip","");
			wvslastMinutes = Integer.parseInt(dmProperties.getProperty("wt.monitor.wvsqueues.lastMinutes","59"));
			queueEntrylastMinutes  = Integer.parseInt(dmProperties.getProperty("wt.monitor.queueEntry.lastMinutes","59"));
			wvsMaxExeTime  = Integer.parseInt(dmProperties.getProperty("wt.monitor.wvsqueues.wvsMaxExeTime","15"));
			autoCleanJobsOlderThan= Integer.parseInt(dmProperties.getProperty("wt.monitor.wvs.AutoCleanJobsOlderThan","0"));
			autoCleanReportOlderThan = Integer.parseInt(dmProperties.getProperty("wt.monitor.AutoCleanReportOlderThan","0"));

			wtMinFreeSpaceLimitInGB = dmProperties.getProperty("wt.home.minFreeSpaceLimitInGB","5");
			wtMinFreeSpaceInPercentage = dmProperties.getProperty("wt.home.minFreeSpaceLimitInPercentage","5");
			fvMinFreeSpaceLimitInGB = Long.valueOf(dmProperties.getProperty("wt.fv.minFreeSpaceLimitInGB","5"));
			fvMinFreeSpaceInPercentage = Long.valueOf(dmProperties.getProperty("wt.fv.minFreeSpaceLimitInPercentage","5"));
			logsDirSizeLimit = dmProperties.getProperty("wt.logs.dir.SizelimitInGB","1");


			isCheckServerStatusPgEnabled = Boolean.valueOf(dmProperties.getProperty("wt.monitor.isCheckServerStatusPg.Enabled","false"));
			isCompleteQueueCounEnabled = Boolean.valueOf(dmProperties.getProperty("wt.monitor.wvsqueues.completeQueueCount.Enabled","false"));

			isautoCleanJobsEnabled = !(autoCleanJobsOlderThan < 1);
			isautoCleanReportsEnabled = !(autoCleanReportOlderThan < 1);

			isQueueEntryDetailsEnabled = Boolean.valueOf(dmProperties.getProperty("wt.monitor.isQueueEntryDetailsEnabled","false"));
			isQueueRunningStatusEnabled = Boolean.valueOf(dmProperties.getProperty("wt.monitor.isQueueRunningStatusEnabled","false"));
			isCADWorkerStatusEnabled = Boolean.valueOf(dmProperties.getProperty("wt.monitor.isCADWorkerStatusEnabled","false"));
			isFileServersEnabled = Boolean.valueOf(dmProperties.getProperty("wt.monitor.isFileServersEnabled","false"));
			isCheckMasterDiskSpaceEnabled = Boolean.valueOf(dmProperties.getProperty("wt.monitor.isCheckMasterDiskSpaceEnabled","false"));
			ischeckLogsDirectorySizeEnabled = Boolean.valueOf(dmProperties.getProperty("wt.monitor.ischeckLogsDirectorySizeEnabled","false"));
			isVaultStatusEnabled = Boolean.valueOf(dmProperties.getProperty("wt.monitor.isVaultStatusEnabled","false"));
			isJobStatusEnabled = Boolean.valueOf(dmProperties.getProperty("wt.monitor.isJobStatusEnabled","false"));
			monitoringVerbose = Boolean.valueOf(dmProperties.getProperty("wt.monitor.Verbose","false"));
			isDynamicSenderIdEnabled  = Boolean.valueOf(dmProperties.getProperty("wt.monitor.isDynamicSenderIdEnabled","false"));
			if(isDynamicSenderIdEnabled)
			{
				mailFrom =  "DM_"+Calendar.getInstance().get(Calendar.MILLISECOND)+"@"+ wtHostName;
			}
			isMaintenance = false;

		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}finally {
			try {
				if (input != null)
					input.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}			
	}


	public static void main(String[] args) throws RemoteException, InvocationTargetException 
	{

		//	final ExecutorService service = Executors.newSingleThreadExecutor();

		try
		{
			//		final Future<Object> f = service.submit(complicatedCheck);

			//		f.get(120, TimeUnit.SECONDS);

			checkServiceStartup();
			String userName = dmProperties.getProperty("wt.username");
			String password = dmProperties.getProperty("wt.password");

			RemoteMethodServer rms = RemoteMethodServer.getDefault();
			rms.setUserName(userName); 
			rms.setPassword(password);

			Class<?>[] paramArrayOfClass = {String.class, String.class,HashMap.class};
			HashMap<String,Object> freshMap=	loadProperties();
			Object[] paramArrayOfObject = {userName, password,freshMap};

			if(args.length == 2)
			{
				loadMaintenanceProperty();
				if(args[1].equals("BeforeMaintenance"))
				{
					System.out.println("recipientsMaintenance -- "+recipientsMaintenance);
					SendMail("Windchill will stop in next 10 Mintues for Maintenance",
							B_START+"This is an automated mail triggered by ITC Maintenance Script"+B_END+
					 BR +"Windchill will stop in next 10 Mintues for Maintenance. "
							+ "If you want to skip this maintenace, then call ITC PLM support team or Kill Maintenance task running in service user account."+
					 BR+"by"+
					 BR+"ITC PLM Support Team."
					 ,
							recipientsMaintenance,mailServerHost,mailServerPort,mailFromMaintenance);
				}
				else if(args[1].equals("AfterMaintenance"))
				{
					System.out.println("recipientsMaintenance -- "+recipientsMaintenance);
					freshMap.put("isMaintenance", true);
					freshMap.put("mailFromMaintenance", mailFromMaintenance);
					freshMap.put("recipientsMaintenance" ,recipientsMaintenance);
					paramArrayOfObject[2] =freshMap;
					rms.invoke("monitorWC",BHDailyMonitor.class.getCanonicalName(), null,paramArrayOfClass, paramArrayOfObject);
				}
			}
			else
			{
				rms.invoke("monitorWC",BHDailyMonitor.class.getCanonicalName(), null,paramArrayOfClass, paramArrayOfObject);
			}
		}
		catch (final Exception e)
		{

			e.printStackTrace();

			String message = "";
			String subject = "";
			Integer dbservices = 2;
			Integer ldapservices = 2;
			Integer webservices = 2;
			java.lang.StackTraceElement[] stackTrace = e.getStackTrace();
			StringBuilder completeError = new StringBuilder();
			if(stackTrace!=null)
				for (int i = 0; i < stackTrace.length ; i++) {

					java.lang.StackTraceElement stackTraceElement = stackTrace[i];
					completeError.append(stackTraceElement.toString()+BR);			
				}



			File dbFile = new File(args[0]+ File.separator + "db" + File.separator + "db.properties");  
			File ieFile = new File(args[0]+ File.separator + "codebase" + File.separator + "WEB-INF" + File.separator + "ieStructProperties.txt");  
			Properties dbProperties = new Properties();
			Properties ieProperties = new Properties();

			try {
				dbProperties.load(new FileInputStream(dbFile));
				ieProperties.load(new FileInputStream(ieFile));
				String dbhost = dbProperties.getProperty("wt.pom.jdbc.host");
				Integer dbPort = Integer.parseInt(dbProperties.getProperty("wt.pom.jdbc.port"));
				Integer smPort = Integer.parseInt(wtproperties.getProperty("wt.manager.port"));
				Integer webServerPort = Integer.parseInt(wtproperties.getProperty("wt.webserver.port"));
				//String ldap = wtproperties.getProperty("wt.federation.ie.ldapServer");

				//System.out.println("\n ldap "+ ldap );
				String ldaphost = ieProperties.getProperty("ie.ldap.serverHostName");//iePropertiesldap.substring(ldap.lastIndexOf("//")+2,ldap.lastIndexOf(":"));
				Integer ldapport = Integer.parseInt(ieProperties.getProperty("ie.ldap.serverPort"));
				TelnetClient conn = new TelnetClient();


				try
				{
					conn.connect(dbhost,dbPort);
					conn.disconnect();
					message = "Database("+dbhost+") services is responding in port ("+dbPort+")." + BR;
					dbservices = 0;
				}
				catch (Exception e1)
				{
					message = B_START + "Database("+dbhost+") services is not responding in port ("+dbPort+")." + B_END + BR;
					dbservices = 1;
				}

				try
				{
					conn.connect(ldaphost,ldapport);
					conn.disconnect();
					message += "LDAP("+ldaphost+") services is responding in port ("+ldapport+")" + BR;
					ldapservices = 0;
				}
				catch (Exception e2)
				{
					message += B_START + "LDAP("+ldaphost+") services is not responding in port ("+ldapport+")" + B_END + BR;
					ldapservices = 1;
				}


				try
				{
					conn.connect(wtHostName,webServerPort);
					conn.disconnect();
					message += "Web Server ("+wtHostName+") services  is responding in port ("+webServerPort+")" + BR;
					webservices = 0;
				}
				catch (Exception e2)
				{
					message += B_START + "Web Server ("+wtHostName+") services is not responding in port ("+webServerPort+")" + B_END + BR;
					webservices = 1;
				}


				try
				{
					conn.connect(wtHostName,smPort);
					conn.disconnect();
					message += "Server Manager ("+wtHostName+")  is responding in port ("+smPort+")" + BR;
					sm = (ServerMonitors)RemoteServerManager.getDefault().getServer("ServerMonitor");				
					message += BR+BR+ getCompleteStartupMsg(sm.getServicesStarted().size());
				}
				catch (Exception e3)
				{
					message += B_START + "Server Manager ("+wtHostName+")  is not responding in port ("+smPort+")" + B_END + BR;
				}

			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			} catch (IOException e1) {
				e1.printStackTrace();
			}


			if(dbservices == 1)
				subject = "DB Services";

			if(ldapservices == 1)
			{
				subject = subject.equalsIgnoreCase("")?"":",";
				subject += "LDAP Services";
			}


			if(webservices == 1)
			{
				subject = subject.equalsIgnoreCase("")?"":",";
				subject += "Web Server";
			}


			subject += subject.equalsIgnoreCase("")?"":" not working, ";


			subject += "EOne QA: Unable to Access Windchill ";
			System.out.println("Subject :-\n"+ subject);
			System.out.println("message :-\n"+ message);

			System.out.println("\n completeError \t:"+completeError);

			if(isEmailEnabled)
			{
				String messageContent = message +BR+ B_START+"Unable to Access Windchill with URL:"+ BR + wtHostName
						+ BR  +"complet eError \t:"+completeError;
				SendMail(subject, messageContent, recipients, mailServerHost, mailServerPort, mailFrom);
				System.err.println("Exception from Windchill Monitoring \n");
			}
			else
			{
				e.printStackTrace();
			}


		}
		finally
		{
			if(Boolean.valueOf(dmProperties.getProperty("wt.monitor.isDevlopmentTesting","false")))
			{
				SendMail("Monitoring Working","Testing",recipients,mailServerHost,mailServerPort,mailFrom);
			}
			
			//	service.shutdown();

		}
	}

	public static void loadMaintenanceProperty() throws IOException {
		InputStream inputMaintenance = BHDailyMonitoringStarter.class.getClassLoader().getResourceAsStream("com" + File.separator + "BH"+ File.separator+ "monitor"+ File.separator + "Maintenance_01.BH");
		Properties maintenanceProperties = new Properties(); 
		maintenanceProperties.load(inputMaintenance);
		mailFromMaintenance =  maintenanceProperties.getProperty("wt.monitor.MailFromMaintenance","Maintenance") +"@"+ wtHostName;
		
		if(maintenanceProperties.getProperty("wt.monitor.mail.MaintenanceRecipients",null) != null)
		{
			if(maintenanceProperties.getProperty("wt.monitor.mail.MaintenanceRecipients").contains(","))
				recipientsMaintenance.addAll(Arrays.asList((maintenanceProperties.getProperty("wt.monitor.mail.MaintenanceRecipients").split(","))));
			else
			{
				recipientsMaintenance.add(maintenanceProperties.getProperty("wt.monitor.mail.MaintenanceRecipients"));					
			}
		}
		inputMaintenance.close();
	}

	public static HashMap<String,Object> loadProperties()
	{
		HashMap<String,Object> freshProperties = new HashMap<String,Object> ();
		freshProperties.put("reportDir" , reportDir);
		freshProperties.put("skipQ" , skipQ);
		freshProperties.put("wvslastMinutes" , wvslastMinutes);
		freshProperties.put("queueEntrylastMinutes"  , queueEntrylastMinutes);
		freshProperties.put("wvsMaxExeTime"  , wvsMaxExeTime);
		freshProperties.put("autoCleanJobsOlderThan", autoCleanJobsOlderThan);
		freshProperties.put("autoCleanReportOlderThan", autoCleanReportOlderThan);
		freshProperties.put("isCompleteQueueCounEnabled" , isCompleteQueueCounEnabled);
		freshProperties.put("mailServerHost" , mailServerHost);
		freshProperties.put("mailServerPort" , mailServerPort);
		freshProperties.put("mailFrom" ,mailFrom);
		freshProperties.put("mailFromMaintenance", mailFromMaintenance);
		freshProperties.put("wtHostName" , wtHostName);
		freshProperties.put("wtMinFreeSpaceLimitInGB" , wtMinFreeSpaceLimitInGB);
		freshProperties.put("wtMinFreeSpaceInPercentage",wtMinFreeSpaceInPercentage);
		freshProperties.put("fvMinFreeSpaceLimitInGB",fvMinFreeSpaceLimitInGB);
		freshProperties.put("fvMinFreeSpaceInPercentage",fvMinFreeSpaceInPercentage);
		freshProperties.put("logsDirSizeLimit",logsDirSizeLimit);
		freshProperties.put("dmProperties",dmProperties);
		freshProperties.put("recipients" ,recipients);
		freshProperties.put("recipientsMaintenance" ,recipientsMaintenance);
		freshProperties.put("isCheckServerStatusPgEnabled",isCheckServerStatusPgEnabled);
		freshProperties.put("isautoCleanJobsEnabled",isautoCleanJobsEnabled);
		freshProperties.put("isautoCleanReportsEnabled",isautoCleanReportsEnabled);
		freshProperties.put("isEmailEnabled" ,isEmailEnabled);
		freshProperties.put("isQueueEntryDetailsEnabled" ,isQueueEntryDetailsEnabled);
		freshProperties.put("isQueueRunningStatusEnabled" ,isQueueRunningStatusEnabled);
		freshProperties.put("isCADWorkerStatusEnabled" ,isCADWorkerStatusEnabled);
		freshProperties.put("isFileServersEnabled" ,isFileServersEnabled);
		freshProperties.put("isCheckMasterDiskSpaceEnabled" ,isCheckMasterDiskSpaceEnabled);
		freshProperties.put("ischeckLogsDirectorySizeEnabled" ,ischeckLogsDirectorySizeEnabled);
		freshProperties.put("isVaultStatusEnabled" ,isVaultStatusEnabled);
		freshProperties.put("isJobStatusEnabled" ,isJobStatusEnabled);
		freshProperties.put("monitoringVerbose" ,monitoringVerbose);
		freshProperties.put("isDynamicSenderIdEnabled" ,isDynamicSenderIdEnabled);
		freshProperties.put("isDynamicSenderIdEnabled" ,isDynamicSenderIdEnabled);
		freshProperties.put("isMaintenance", isMaintenance);
		return freshProperties;
	}




	public static void SendMail(String subject,String messageContent,HashSet<String> recipients,String mailHost,String mailPort,String mailFrom)
	{    
		try
		{
			Properties properties = System.getProperties();
			if(!mailHost.equalsIgnoreCase(""))
				properties.setProperty("mail.smtp.host", mailHost);
			else
			{
				System.out.println("Mail Host is not defined");
				return;
			}
			if(!mailPort.equalsIgnoreCase(""))
				properties.setProperty("mail.smtp.port", mailPort);

			Session session = Session.getDefaultInstance(properties);
			MimeMessage message = new MimeMessage(session);
			message.setFrom(new InternetAddress(mailFrom));

			InternetAddress[] addressTo = new InternetAddress[recipients.size()];
			int i=0;
			for (Iterator<String> it=(Iterator<String>) recipients.iterator();it.hasNext();i++)
			{
				String id=it.next();
				if(isValidEmailAddress(id))
					addressTo[i] = new InternetAddress(id);
				else
					System.out.println("\n Email Id is invalid");
			}
			String mailUser=dmProperties.getProperty("wt.monitoring.mailUser","");
			String mailpassword=dmProperties.getProperty("wt.monitoring.mailpassword","");
			if(mailUser.equalsIgnoreCase(""))
				properties.setProperty("mail.user",mailUser);
			if(mailpassword.equalsIgnoreCase(""))
				properties.setProperty("mail.password", mailpassword);
			message.setRecipients(RecipientType.TO, addressTo); 
			String subject1="EONE QA:"+subject;
			message.setSubject(subject1);
			//message.setSubject("EONE QA:");
			message.setContent(messageContent, "text/html; charset=utf-8");
			Transport.send(message);
		}catch (Exception e) {
			e.printStackTrace();
		}

	}

	private static boolean isValidEmailAddress(String email) {
		boolean result = true;
		try {
			InternetAddress emailAddr = new InternetAddress(email);
			emailAddr.validate();
		} catch (AddressException ex) {
			result = false;
		}
		return result;
	}


	public static void startServerManager()
			throws Exception
	{
		try
		{
			System.out.println("Start the server manager ....");
			ServerLauncher.start();
		}
		catch (Exception localException)
		{
			System.out.println("Could not start server manager, aborting");
			localException.printStackTrace();
			throw localException;
		}
		System.out.println("Make sure it's running ...");
		long l1 = System.currentTimeMillis();
		long l2 = l1 + WTProperties.getLocalProperties().getProperty("wt.load.installer.serverManagerTimeout", 30) * 1000;
		while (l1 < l2)
		{
			try
			{
				Thread.sleep(5000L);
				RemoteServerManager.getDefault().ping();
				System.out.println("ServerManager is alive!!");
			}
			catch (RemoteException localRemoteException) {}
			l1 = System.currentTimeMillis();
		}
		RemoteServerManager.getDefault().ping();
		checkServiceStartup();
	}


	private static boolean checkServiceStartup()
			throws Exception
	{
		int i = 0;
		int j = 0;
		sm = (ServerMonitors)RemoteServerManager.getDefault().getServer("ServerMonitor");
		services = sm.getServicesStarted();
		j = services.size();
		for (int k = 0; k < j; k++) {
			String str = (String)services.elementAt(k);
			ServiceStat localServiceStat = sm.getServiceStats(str);
			i += localServiceStat.getInstances();
			//System.out.println("Windchill service (" + str + ") starting (" + localServiceStat.getInstances() + ") instance(s)");
		}
		boolean bool = startupComplete(j);
		long l1; long l2; long l3;
		if (!bool)
		{
			l1 = System.nanoTime() / 1000000L;
			l2 = i * WTProperties.getLocalProperties().getProperty("wt.ntservice.startWait", 60) * 1000L;
			for (l3 = 0L; (!bool) && (l3 < l2);)
			{
				try
				{
					//System.out.println(" L2 "+l2	+"\nl3 "+l3	+ "\nThread sleep Math.min(l2 - l3, 10000L) :"+Math.min(l2 - l3, 10000L));
					Thread.sleep(Math.min(l2 - l3, 10000L));
				}
				catch (Exception localException) {}
				l3 = System.nanoTime() / 1000000L - l1;
				bool = startupComplete(j);
			}
		}
		if (!bool) {
			throw new Exception("Unable to complete startup of all Windchill Services");
		}
		else
		{
			return true;
		}
	}

	private static boolean isServerUp()
			throws WTException
	{
		boolean bool = false;
		URL localURL = null;
		try
		{
			localURL = new URL(WTProperties.getLocalProperties().getProperty("wt.httpgw.url.anonymous") + "/wt.httpgw.HTTPServer/ping");
			if (!localURL.getProtocol().toLowerCase().startsWith("https")) {
				throw new WTException("Webserver is not running.\n");
			}
			HttpURLConnection localHttpURLConnection = (HttpURLConnection)localURL.openConnection();
			localHttpURLConnection.connect();
			if (localHttpURLConnection.getResponseCode() != 200) {
				bool = false;
			} else {
				//System.out.println("\n ----3");			
				bool = true;
			}
		}
		catch (IOException localIOException) {
			//System.out.println("-----catch---");
			throw new WTException(localIOException, "Unable to connect to your web server.\nAttempts to read the URL " + localURL + " failed.\n");
		}
		//System.out.println("\n isServerUp "+bool);
		return bool;
	}

	private static String getCompleteStartupMsg(int paramInt)
			throws RemoteException, WTException
	{
		String msg=BR;
		for (int i = 0; i < paramInt; i++) {
			String str = (String)services.elementAt(i);

			ServiceStat localServiceStat = sm.getServiceStats(str);
			if (!localServiceStat.getStartup()) {
				msg += str+ "is Not Ready "+BR;
			}
			else
			{
				msg += str+ "is Ready "+BR;
			}
		}
		return msg;
	}

	private static boolean startupComplete(int paramInt)
			throws RemoteException, WTException
	{
		boolean bool = true;
		for (int i = 0; i < paramInt; i++) {
			String str = (String)services.elementAt(i);

			ServiceStat localServiceStat = sm.getServiceStats(str);
			if (!localServiceStat.getStartup()) {
				bool = false;
			}
			//System.out.println("startupComplete  "+str+ "  "+localServiceStat.getStartup());
		}
		if (bool) {
			bool = isServerUp();
		}
		return bool;
	}
	static Callable<Object> complicatedCheck = new Callable<Object>()
			{

		@Override
		public Object call() throws Exception
		{
			String userName = dmProperties.getProperty("wt.username");
			String password = dmProperties.getProperty("wt.password");
			RemoteMethodServer rms = RemoteMethodServer.getDefault();
			rms.setUserName(userName); 
			rms.setPassword(password);
			Class<?>[] paramArrayOfClass ={};
			Object[] paramArrayOfObject = {}; 
			rms.invoke("isWC",BHDailyMonitorWcStartedCheck.class.getCanonicalName(), null, paramArrayOfClass, paramArrayOfObject);
			return 1;
		}

			};


}
