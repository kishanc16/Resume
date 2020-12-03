package com.BH.monitor;
 import com.ptc.core.appsec.CSRFProtector;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.net.HttpURLConnection;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;

import javax.mail.internet.AddressException;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;
import javax.xml.bind.DatatypeConverter;

import org.apache.commons.io.FileUtils;
import org.apache.commons.net.telnet.TelnetClient;

import wt.dataservice.DataServiceFactory;
import wt.dataservice.Datastore;
import wt.dataservice.Oracle;
import wt.dataservice.SQLServer;
import wt.fc.Persistable;
import wt.fc.QueryResult;
import wt.intersvrcom.SiteMonitorMBean;
import wt.jmx.core.MBeanUtilities;
import wt.jmx.core.SelfAwareMBean;
import wt.method.RemoteAccess;
import wt.pom.DBProperties;
import wt.queue.QueueEntry;
import wt.queue.WtQueue;
import wt.queue.mvc.datautil.MVCQueuePage;
import wt.tools.sql.SQLCommandTool;
import wt.util.WTContext;
import wt.util.WTException;
import wt.util.WTProperties;

import com.ptc.wvs.common.cadagent.CadProxy;
import com.ptc.wvs.common.util.WVSProperties;
import com.ptc.wvs.server.cadagent.Inifile;
import com.ptc.wvs.server.publish.PublishQueueHelper;
import com.ptc.wvs.server.publish.WVSProcessingJob;
import com.ptc.wvs.server.util.Util;




public class BHDailyMonitor  implements RemoteAccess{	

	static Datastore localDatastore = DataServiceFactory.getDefault().getDatastore();

	private static final ObjectName  vaultSitesMBeanName = newObjectName( "com.ptc:wt.subsystem=Monitors,wt.monitorType=VaultSites" );

	static Properties dmProperties = BHDailyMonitoringStarter.dmProperties;
	static Properties wtproperties  = BHDailyMonitoringStarter.wtproperties; 

	private static final String START_TABLE = "<table border=1 cellspacing=0 cellpadding=0 style='border-collapse:collapse;border:none'>";

	private static final String END_TABLE = "</table>";

	private static final String START_TR = "<tr>";

	private static final String END_TR = "</tr>";

	private static final String END_TD = "</td>";

	private static final String START_TD_S = "<td>";

	private static final String NA = "NA";

	private static final String B_START = "<b>";

	private static final String B_END = "</b>";

	private static final String START_TD_COMMON = "<td valign=center style='width:90.15pt;border:solid windowtext 1.0pt;padding:0cm 5.4pt 0cm 5.4pt";

	private static final String START_TD_H = START_TD_COMMON + ";background:#5B9BD5' align=center>"+B_START+"<span style='font-size:12.0pt;color:white'>";

	private static final String END_HIGH = "</span>"+ B_END;

	private static final String END_TD_H = END_HIGH + END_TD;

	private static final String START_TD_ODD = START_TD_COMMON + ";background:#DEEAF6'>";

	private static final String START_TD_EVEN = START_TD_COMMON + "'>";

	private static final String START_HIGH = B_START+"<span style='color:red'>";

	private static final String TABLE_HEADLINE_START = "<h3>" ; //"<b><font size =3 color='black'>";

	private static final String START_DIV = "<div class=\"pane\">";

	private static final String TABLE_HEADLINE_END =   "</h3>" + START_DIV;//"</font></b>";

	private static final String END_DIV = "</div>";

	private static final String BR = "<br />";

	private static final String D_BR = BR+BR;

	private static final String MAIL = "mail";

	private static final String REPORT = "report";

	private static final SimpleDateFormat todispayTime = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

	private static final DateFormat folderFormat = new SimpleDateFormat("dd_MMMM_yyyy");

	private static final String GB = "GB";

	private static String wtHostName = BHDailyMonitoringStarter.wtHostName;


	/** E-Mail **/
	private static String mailServerHost = BHDailyMonitoringStarter.mailServerHost;

	private static String mailServerPort = BHDailyMonitoringStarter.mailServerPort;

	private static String mailFrom = BHDailyMonitoringStarter.mailFrom;
	private static String mailFromMaintenance = BHDailyMonitoringStarter.mailFromMaintenance;

	private static HashSet<String> recipients = BHDailyMonitoringStarter.recipients;


	private static HashSet<String> recipientsMaintenance = BHDailyMonitoringStarter.recipientsMaintenance;


	private static Boolean isEmailEnabled = BHDailyMonitoringStarter.isEmailEnabled;

	/** Queue **/
	private static String skipQ = "";

	private static Integer queueEntrylastMinutes = BHDailyMonitoringStarter.queueEntrylastMinutes;


	/** WVS **/	
	private static Integer wvslastMinutes = BHDailyMonitoringStarter.wvslastMinutes;

	private static Integer wvsMaxExeTime = BHDailyMonitoringStarter.wvsMaxExeTime;

	/** WVS Maintenance **/	
	private static Integer autoCleanJobsOlderThan = BHDailyMonitoringStarter.autoCleanJobsOlderThan;

	private static Boolean isautoCleanJobsEnabled = BHDailyMonitoringStarter.isautoCleanJobsEnabled;


	/** Disk Space **/
	private static String wtMinFreeSpaceLimitInGB = BHDailyMonitoringStarter.wtMinFreeSpaceLimitInGB;

	private static String wtMinFreeSpaceInPercentage = BHDailyMonitoringStarter.wtMinFreeSpaceInPercentage;

	private static Long fvMinFreeSpaceLimitInGB = BHDailyMonitoringStarter.fvMinFreeSpaceLimitInGB;

	private static Long fvMinFreeSpaceInPercentage = BHDailyMonitoringStarter.fvMinFreeSpaceInPercentage;


	/** Maintenance **/
	private static Boolean isautoCleanReportsEnabled = BHDailyMonitoringStarter.isautoCleanReportsEnabled;

	private static Integer autoCleanReportOlderThan = BHDailyMonitoringStarter.autoCleanReportOlderThan;

	private static String reportDir = BHDailyMonitoringStarter.reportDir;

	private static String logsDirSizeLimit = BHDailyMonitoringStarter.logsDirSizeLimit;




	/** Enable /Disable **/
	private static Boolean isCheckServerStatusPgEnabled = BHDailyMonitoringStarter.isCheckServerStatusPgEnabled;

	private static Boolean isCompleteQueueCounEnabled = BHDailyMonitoringStarter.isCompleteQueueCounEnabled;

	private static Boolean isQueueEntryDetailsEnabled = BHDailyMonitoringStarter.isQueueEntryDetailsEnabled;

	private static Boolean isQueueRunningStatusEnabled = BHDailyMonitoringStarter.isQueueRunningStatusEnabled;

	private static Boolean isCADWorkerStatusEnabled = BHDailyMonitoringStarter.isCADWorkerStatusEnabled;

	private static Boolean isFileServersEnabled = BHDailyMonitoringStarter.isFileServersEnabled;

	private static Boolean isCheckMasterDiskSpaceEnabled = BHDailyMonitoringStarter.isCheckMasterDiskSpaceEnabled;

	private static Boolean ischeckLogsDirectorySizeEnabled = BHDailyMonitoringStarter.ischeckLogsDirectorySizeEnabled;

	private static Boolean isVaultStatusEnabled = BHDailyMonitoringStarter.isVaultStatusEnabled;

	private static Boolean isJobStatusEnabled = BHDailyMonitoringStarter.isJobStatusEnabled;
	private static Boolean isMaintenance= BHDailyMonitoringStarter.isMaintenance;


	/** Debug **/
	private static Boolean monitoringVerbose = BHDailyMonitoringStarter.monitoringVerbose;



	/*	public static void main(String[] args) 
	{
		System.out.println("-----------------Monitoring Started----------------11111");

		try
		{

			String userName = dmProperties.getProperty("wt.username");
			String password = dmProperties.getProperty("wt.password");

			RemoteMethodServer rms = RemoteMethodServer.getDefault();
			rms.setUserName(userName); 
			rms.setPassword(password);

			Class<?>[] paramArrayOfClass = {String.class, String.class};
			Object[] paramArrayOfObject = {userName, password};

			rms.invoke("monitorWC",ITCDailyMonitor_MV001.class.getCanonicalName(), null,paramArrayOfClass, paramArrayOfObject);
		}
		catch (final Exception e)
		{

			java.lang.StackTraceElement[] stackTrace = e.getStackTrace();
			StringBuilder completeError = new StringBuilder();
			if(stackTrace!=null)
				for (int i = 0; i < stackTrace.length ; i++) {
					java.lang.StackTraceElement stackTraceElement = stackTrace[i];
					completeError.append(stackTraceElement.toString()+BR);			
				}

			if(isEmailEnabled & e instanceof TimeoutException)
			{
				String messageContent = B_START+" Unable to Access Windchill "+B_END+ D_BR+"Error Message From Daily Monitoring :"+BR+ e.getMessage();
				ITCDailyMonitoringStarter_MV005.SendMail("Unable to Access Windchill", messageContent, recipients, mailServerHost, mailServerPort, mailFrom);
				System.err.println("Calculation took to long"+e);
				System.err.println("Windchill is Down");
			}
			else if(isEmailEnabled)
			{
				String messageContent = B_START+" Exception from Windchill Monitoring"+B_END+ D_BR+"Error Message From Daily Monitoring :"+BR+ e.getMessage();
				ITCDailyMonitoringStarter_MV005.SendMail("Exception from Windchill Monitoring", messageContent, recipients, mailServerHost, mailServerPort, mailFrom);
				System.err.println("Exception from Windchill Monitoring");
			}
			else
			{
				e.printStackTrace();
			}
		}
		finally
		{
			System.out.println("-----------------Monitoring Final----------------");
			System.exit(0);
		}
	}*/

	public static void monitorWC(String user,String password,HashMap<String,Object> newProperties) throws Exception
	{
		System.out.println("-----------------Monitoring monitorWC----------------");

		Connection conn = getSQLConnection();

		wtproperties = WTProperties.getLocalProperties();

		refreshProperties(newProperties);

		StringBuilder page=new StringBuilder();

		page.append("<!DOCTYPE html>"
				+ "<html>"
				+ "<head>"
				+ "<script src=\"../MonitoringScript.js\"></script>"
				+ "<style>.accordion {margin:1em 0}.accordion h3 {background:#559b6a;color:#fff;cursor:pointer;margin:0 0 1px 0;padding:4px 10px}.accordion h3.current {background:#4289aa;cursor:default}.accordion div.pane {padding:5px 10px}</style>" 
				+ "</head><body>");


		page.append("<div class=\"accordion\">");
		page.append(getMaintenanceMsg());

		String todayFolder = reportDir + File.separator + folderFormat.format(new Date());
		if(monitoringVerbose)
		{
			System.out.println("\n Before Today Check Folder");
			System.out.println("\n Today Folder is :"+todayFolder);
		}

		checkFolder(todayFolder);

		if(monitoringVerbose)
			System.out.println("\n After Today Check Folder");

		File reportFile = new File(todayFolder,"Report_"+(new Date()).getTime()+".html");

		if(monitoringVerbose)
			System.out.println("\n reportFile is :"+reportFile);

		StringBuilder subject=new StringBuilder();

		StringBuilder message=new StringBuilder();

		

		if(isQueueEntryDetailsEnabled)
		{
			if(monitoringVerbose)
				System.out.println("isQueueEntryDetailsEnabled is True");

			HashMap<String, String> getQueueEntryDetails = getQueueEntryDetails(conn);
			page.append(getQueueEntryDetails.get(REPORT));

			if(isEmailEnabled)
				appendMessagetoMail(getQueueEntryDetails,"Queue Entry",subject,message);
		}

		if(isQueueRunningStatusEnabled)
		{
			if(monitoringVerbose)
				System.out.println("isQueueRunningStatusEnabled is True");

			HashMap<String, String> getQueueRunningStatus = getQueueRunningStatus();
			page.append(getQueueRunningStatus.get(REPORT));

			if(isEmailEnabled)
				appendMessagetoMail(getQueueRunningStatus,"Queue Running Status",subject,message);
		}

		if(isVaultStatusEnabled)
		{
			if(monitoringVerbose)
				System.out.println("isVaultStatusEnabled is True");

			HashMap<?, ?>[] vaultStatus = getVaultStatus(conn);
			page.append(vaultStatus[1].get(REPORT));
			page.append(vaultStatus[0].get(REPORT));

			if(isEmailEnabled)
			{
				appendMessagetoMail(vaultStatus[1],"Vault Disk Space",subject,message);
				appendMessagetoMail(vaultStatus[0],"Vault Mount Status",subject,message);				
			}

		}
		if(isFileServersEnabled)
		{
			if(monitoringVerbose)
				System.out.println("isFileServersEnabled is True");

			HashMap<String, String> getFileServers = getFileServers();
			page.append(getFileServers.get(REPORT));

			if(isEmailEnabled)
				appendMessagetoMail(getFileServers,"File Server",subject,message);
		}

		if(isCheckMasterDiskSpaceEnabled)
		{
			if(monitoringVerbose)
				System.out.println("isCheckMasterDiskSpaceEnabled is True");

			HashMap<String, String> getMasterDiskSpace = getMasterDiskSpace();
			page.append(getMasterDiskSpace.get(REPORT));

			if(isEmailEnabled)
				appendMessagetoMail(getMasterDiskSpace,"Master Disk Space",subject,message);
			
				
			HashMap<String, String> getCDriveDiskSpace = getCDriveDiskSpace();
			page.append(getCDriveDiskSpace.get(REPORT));

			if(isEmailEnabled)
				appendMessagetoMail(getCDriveDiskSpace,"C Drive Disk Space",subject,message);
			
			HashMap<String, String> getEDriveDiskSpace = getEDriveDiskSpace();
			page.append(getEDriveDiskSpace.get(REPORT));

			if(isEmailEnabled)
				appendMessagetoMail(getEDriveDiskSpace,"E Drive Disk Space",subject,message);
		}

		if(ischeckLogsDirectorySizeEnabled)
		{
			if(monitoringVerbose)
				System.out.println("ischeckLogsDirectorySizeEnabled is True");

			HashMap<String, String> getMasterLogsDirectorySize = getMasterLogsDirectorySize();
			page.append(getMasterLogsDirectorySize.get(REPORT));

			if(isEmailEnabled)
				appendMessagetoMail(getMasterLogsDirectorySize,"Logs Directory Size",subject,message);	
		}

		if(isCADWorkerStatusEnabled)
		{
			if(monitoringVerbose)
				System.out.println("isCADWorkerStatusEnabled is True");


			HashMap<?, ?>[] CADWorkerStatus = getCADWorkerStatus();
			page.append(CADWorkerStatus[0].get(REPORT));
		//page.append(CADWorkerStatus[1].get(REPORT));
		page.append(CADWorkerStatus[1].get(REPORT));
			if(isEmailEnabled)
			{
				appendMessagetoMail(CADWorkerStatus[0],"CAD Worker Status",subject,message);
				appendMessagetoMail(CADWorkerStatus[1],"Tibco URL Status",subject,message);
			}




	
	}

		if(isJobStatusEnabled)
		{
			if(monitoringVerbose)
				System.out.println("isJobStatusEnabled is True");

			HashMap<?, ?>[] jobs = getJobStatus();
			if(isCompleteQueueCounEnabled)
				page.append(jobs[0].get(REPORT));
			page.append(jobs[1].get(REPORT));
			page.append(jobs[2].get(REPORT));

			if(isEmailEnabled)
			{
				appendMessagetoMail(jobs[1],"Executing Jobs",subject,message);
				appendMessagetoMail(jobs[2],"Failed Jobs",subject,message);
			}

		}


		if(isEmailEnabled)
		{
			if(monitoringVerbose)
				System.out.println("isEmailEnabled is True");
			if(isMaintenance)
			{
				BHDailyMonitoringStarter.SendMail("Windchill started after Maintenance",
						B_START+"This is an automated mail triggered by ITC Maintenance Script"+B_END+
						"Windchill started after Maintenance"+
						BR+
						BR+"by"+
						BR+"ITC PLM Support Team.",
						recipientsMaintenance,mailServerHost,mailServerPort,
						mailFromMaintenance);
			}	

			if(!message.toString().equalsIgnoreCase(""))
			{
				//String sub="EONE QA :";
				String subEnd = subject.toString().contains(",")? " are":" is";
				
				subEnd +=" having issue.";
				subject.append(subEnd);
				String finalMsg = subject.toString() +D_BR 
						+B_START+" Complete Report avalabile in server at "+ reportFile.getAbsolutePath() +
						B_END +D_BR+ message.toString();
				try{
					BHDailyMonitoringStarter.SendMail(subject.toString(), finalMsg, recipients, mailServerHost, mailServerPort, mailFrom);
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}			
			} 

		}
		

		if(isCheckServerStatusPgEnabled)
		{
			if(monitoringVerbose)
				System.out.println("isCheckServerStatusPgEnabled is True");

			getServerStatusPage(page,todayFolder, user, password);
			//getServerStatusPage(page,todayFolder, user, password);
			//getTibcoStatusPage(page,todayFolder, user, password);
			page.append(END_DIV);
			page.append("</body></html>");		
		}

		PrintWriter pw =new PrintWriter(reportFile);
		pw.println(page);
		pw.close();
		checkForSupportedFiles();
		doMaintenance(conn);
		conn.close();		
		System.out.println("---------- Monitoring Completed ---------- ");	
	}

	private static void refreshProperties(HashMap<String, Object> newProperties) {
		monitoringVerbose = (Boolean) newProperties.get("monitoringVerbose");

		if(monitoringVerbose)
			System.out.println("Refresh Properties Started Again");
		reportDir=(String) newProperties.get("reportDir");
		skipQ = (String) newProperties.get("skipQ" );
		wvslastMinutes = (Integer) newProperties.get("wvslastMinutes" );
		queueEntrylastMinutes = (Integer) newProperties.get("queueEntrylastMinutes" );
		wvsMaxExeTime = (Integer) newProperties.get("wvsMaxExeTime" );
		autoCleanJobsOlderThan = (Integer) newProperties.get("autoCleanJobsOlderThan");
		autoCleanReportOlderThan= (Integer) newProperties.get("autoCleanReportOlderThan");
		isCompleteQueueCounEnabled = (Boolean) newProperties.get("isCompleteQueueCounEnabled");
		mailServerHost = (String) newProperties.get("mailServerHost" );
		mailServerPort = (String) newProperties.get("mailServerPort" );
		mailFrom = (String) newProperties.get("mailFrom");
		wtHostName = (String) newProperties.get("wtHostName" );
		wtMinFreeSpaceLimitInGB = (String) newProperties.get("wtMinFreeSpaceLimitInGB");
		wtMinFreeSpaceInPercentage = (String) newProperties.get("wtMinFreeSpaceInPercentage");
		fvMinFreeSpaceLimitInGB =	(Long) newProperties.get("fvMinFreeSpaceLimitInGB");
		fvMinFreeSpaceInPercentage = (Long) newProperties.get("fvMinFreeSpaceInPercentage");
		logsDirSizeLimit = (String) newProperties.get("logsDirSizeLimit");
		dmProperties = (Properties)newProperties.get("dmProperties");
		recipients = (HashSet<String>) newProperties.get("recipients");

		isCheckServerStatusPgEnabled = (Boolean) newProperties.get("isCheckServerStatusPgEnabled");
		isautoCleanJobsEnabled =  (Boolean) newProperties.get("isautoCleanJobsEnabled");
		isautoCleanReportsEnabled =  (Boolean) newProperties.get("isautoCleanReportsEnabled");	

		isEmailEnabled =  (Boolean) newProperties.get("isEmailEnabled");
		isQueueEntryDetailsEnabled =  (Boolean) newProperties.get("isQueueEntryDetailsEnabled");
		isQueueRunningStatusEnabled =  (Boolean) newProperties.get("isQueueRunningStatusEnabled");
		isCADWorkerStatusEnabled =  (Boolean) newProperties.get("isCADWorkerStatusEnabled");
		isFileServersEnabled =  (Boolean) newProperties.get("isFileServersEnabled");
		isCheckMasterDiskSpaceEnabled =  (Boolean) newProperties.get("isCheckMasterDiskSpaceEnabled");
		ischeckLogsDirectorySizeEnabled =  (Boolean) newProperties.get("ischeckLogsDirectorySizeEnabled");
		isVaultStatusEnabled =  (Boolean) newProperties.get("isVaultStatusEnabled");
		isJobStatusEnabled =  (Boolean) newProperties.get("isJobStatusEnabled");	
		
		isMaintenance = (Boolean) newProperties.get("isMaintenance");	
		recipientsMaintenance = (HashSet<String>) newProperties.get("recipientsMaintenance");
		mailFromMaintenance=(String) newProperties.get("mailFromMaintenance");
	}

	private static void checkForSupportedFiles() throws IOException {

		File destFileJs = new File(reportDir + File.separator + "MonitoringScript.js");
		if(!destFileJs.exists())
		{
			File srcFileJs= new File(wtproperties.getProperty("wt.home")+ File.separator+ "codebase"  + File.separator + "com" + File.separator + "BH" + File.separator + "monitor" + File.separator + "MonitoringScript.js");
			FileUtils.copyFile(srcFileJs, destFileJs);
		}
	}

	private static void checkFolder(String location) throws IOException {
		File theDir = new File(location);
		if (!theDir.exists())
		{
			System.out.println("\n Creating Todays Folder");
			theDir.mkdir(); 
		}
	}

	public static StringBuilder getMaintenanceMsg()
	{
		StringBuilder msg=new StringBuilder();
		msg.append(TABLE_HEADLINE_START +"Report Summary" +TABLE_HEADLINE_END);

		if(isEmailEnabled)
			msg.append(" Auto Mail Notification is enabled to "+recipients);
		else
			msg.append(" Auto Mail Notification is disabled");

		/*if(isautoCleanJobsEnabled)
			msg.append(D_BR+" Auto clean jobs older than "+autoCleanJobsOlderThan+" days is enabled");
		else
			msg.append(D_BR+" Auto clean jobs is disabled");*/

		if(isautoCleanReportsEnabled)
			msg.append(D_BR+" Auto clean Reports older than "+autoCleanReportOlderThan+" days is enabled");
		else
			msg.append(D_BR+" Auto clean Reports is disabled");		

		/*if(isCompleteQueueCounEnabled)
			msg.append(D_BR+" Display complete publish jobs count is enabled");
		else
			msg.append(D_BR+" Display complete publish jobs count is disabled");*/

		if(isCheckServerStatusPgEnabled)
			msg.append(D_BR+" Display Server Status Page is enabled");
		else
			msg.append(D_BR+" Display Server Status Page is disabled");

		msg.append(END_DIV);
		return msg;
	}

	private static void doMaintenance(Connection conn)
	{
		try{

			if(isautoCleanJobsEnabled)
				autoCleanOlderJobs(conn);

			if(isautoCleanReportsEnabled)
				autoCleanOlderReports();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}


	public static HashMap<String,String> getMasterDiskSpace() {

		File wtDrive= new File(wtproperties.getProperty("wt.home") + File.separator);
		long freeSpaceInDrive=wtDrive.getFreeSpace();
		StringBuilder table=new StringBuilder();
		float freeSpaceInPercentage = getPercentage(freeSpaceInDrive, wtDrive.getTotalSpace());
		float minFreeSpaceLimitInGB= Long.valueOf(wtMinFreeSpaceLimitInGB);
		float minFreeSpaceInPercentage = Long.valueOf(wtMinFreeSpaceInPercentage);
		boolean isActionReq = true;
		if(freeSpaceInDrive<= minFreeSpaceLimitInGB ||freeSpaceInPercentage <= minFreeSpaceInPercentage){
			isActionReq =true;
		}
		else
		{
			isActionReq =false;
		}
		table.append(TABLE_HEADLINE_START +"Master Windchill Disk Space" +TABLE_HEADLINE_END);
		table.append("Minimum space limit specified "+minFreeSpaceLimitInGB+" GB");
		table.append(BR+"Minimum space percentage "+minFreeSpaceInPercentage+"%");
		table.append(START_TABLE);
		String[] columns ={" Drive "," Total Space "," Free Space "," Free Space Percentage "};
		table.append(getHeader(columns));
		int rowCount=1;
		String td[] = getTD(rowCount, isActionReq);
		table.append(START_TR);
		table.append(td[0] +" Windchill Drive " + td[1]);
		table.append(td[0] + convertBytetoGB(wtDrive.getTotalSpace()) + GB+ td[1]);
		table.append(td[0] + convertBytetoGB(freeSpaceInDrive) + GB+ td[1]);
		table.append(td[0] + freeSpaceInPercentage + "%"+ td[1]);
		table.append(END_TR);
		table.append(END_TABLE);
		table.append(END_DIV);
		return getReturnMap(table, isActionReq);
	}
	
	
	
	public static HashMap<String,String> getCDriveDiskSpace() {

		File CDrive= new File("C:\\");
		long freeSpaceInDrive=CDrive.getFreeSpace();
		StringBuilder table=new StringBuilder();
		float freeSpaceInPercentage = getPercentage(freeSpaceInDrive, CDrive.getTotalSpace());
		float minFreeSpaceLimitInGB= Long.valueOf(wtMinFreeSpaceLimitInGB);
		float minFreeSpaceInPercentage = Long.valueOf(wtMinFreeSpaceInPercentage);
		boolean isActionReq = true;
		if(freeSpaceInDrive<= minFreeSpaceLimitInGB ||freeSpaceInPercentage <= minFreeSpaceInPercentage){
			isActionReq =true;
		}
		else
		{
			isActionReq =false;
		}
		table.append(TABLE_HEADLINE_START +"C Drive Disk Space" +TABLE_HEADLINE_END);
		table.append("Minimum space limit specified "+minFreeSpaceLimitInGB+" GB");
		table.append(BR+"Minimum space percentage "+minFreeSpaceInPercentage+"%");
		table.append(START_TABLE);
		String[] columns ={" Drive "," Total Space "," Free Space "," Free Space Percentage "};
		table.append(getHeader(columns));
		int rowCount=1;
		String td[] = getTD(rowCount, isActionReq);
		table.append(START_TR);
		table.append(td[0] +" C Drive " + td[1]);
		table.append(td[0] + convertBytetoGB(CDrive.getTotalSpace()) + GB+ td[1]);
		table.append(td[0] + convertBytetoGB(freeSpaceInDrive) + GB+ td[1]);
		table.append(td[0] + freeSpaceInPercentage + "%"+ td[1]);
		table.append(END_TR);
		table.append(END_TABLE);
		table.append(END_DIV);
		return getReturnMap(table, isActionReq);
	}
	
	public static HashMap<String,String> getEDriveDiskSpace() {

		File EDrive= new File("E:\\");
		long freeSpaceInDrive=EDrive.getFreeSpace();
		StringBuilder table=new StringBuilder();
		float freeSpaceInPercentage = getPercentage(freeSpaceInDrive, EDrive.getTotalSpace());
		float minFreeSpaceLimitInGB= Long.valueOf(wtMinFreeSpaceLimitInGB);
		float minFreeSpaceInPercentage = Long.valueOf(wtMinFreeSpaceInPercentage);
		boolean isActionReq = true;
		if(freeSpaceInDrive<= minFreeSpaceLimitInGB ||freeSpaceInPercentage <= minFreeSpaceInPercentage){
			isActionReq =true;
		}
		else
		{
			isActionReq =false;
		}
		table.append(TABLE_HEADLINE_START +"E Drive Disk Space" +TABLE_HEADLINE_END);
		table.append("Minimum space limit specified "+minFreeSpaceLimitInGB+" GB");
		table.append(BR+"Minimum space percentage "+minFreeSpaceInPercentage+"%");
		table.append(START_TABLE);
		String[] columns ={" Drive "," Total Space "," Free Space "," Free Space Percentage "};
		table.append(getHeader(columns));
		int rowCount=1;
		String td[] = getTD(rowCount, isActionReq);
		table.append(START_TR);
		table.append(td[0] +" E Drive " + td[1]);
		table.append(td[0] + convertBytetoGB(EDrive.getTotalSpace()) + GB+ td[1]);
		table.append(td[0] + convertBytetoGB(freeSpaceInDrive) + GB+ td[1]);
		table.append(td[0] + freeSpaceInPercentage + "%"+ td[1]);
		table.append(END_TR);
		table.append(END_TABLE);
		table.append(END_DIV);
		return getReturnMap(table, isActionReq);
	}

	public static HashMap<String, String> getMasterLogsDirectorySize() {

		StringBuilder table=new StringBuilder();
		long logSize = convertBytetoGB(FileUtils.sizeOfDirectory(new File(wtproperties.getProperty("wt.logs.dir")+File.separator)));
		long locallogsDirSizeLimit= Long.valueOf(logsDirSizeLimit);
		boolean isActionReq = true;
		if(logSize > locallogsDirSizeLimit)
		{
			isActionReq =true;
		}
		else
		{
			isActionReq =false;
		}
		table.append(TABLE_HEADLINE_START +"Windchill Log Directory Size" +TABLE_HEADLINE_END);
		table.append("Size limit specified in properties "+locallogsDirSizeLimit+" GB");

		table.append(START_TABLE);

		String[] columns ={" Directory "," Current Size "," Maximum Limit "};
		table.append(getHeader(columns));

		int rowCount=1;
		String td[] = getTD(rowCount, isActionReq);
		table.append(START_TR);
		table.append(td[0] +" Master Logs Directory " + td[1]);
		table.append(td[0] + logSize + GB+ td[1]);
		table.append(td[0] + locallogsDirSizeLimit + GB+ td[1]);
		table.append(END_TR);
		table.append(END_TABLE);
		table.append(END_DIV);
		return getReturnMap(table, isActionReq);
	}

	public static File getServerStatusPage(StringBuilder page,String todayFolder, String userName, String password) throws IOException, WTException {
		userName = (userName.equalsIgnoreCase(""))?dmProperties.getProperty("wt.username"):userName;
		password = (password.equalsIgnoreCase(""))?dmProperties.getProperty("wt.password"):password;

		URL url = new URL(wtproperties.getProperty("wt.server.codebase","https://localhost/Windchill")+"/wtcore/jsp/jmx/serverStatus.jsp");
		URLConnection urlConnection = url.openConnection();
		String authString = userName + ":" + password;
		String basicAuth = "Basic " + DatatypeConverter.printBase64Binary(authString.getBytes());
		urlConnection.setRequestProperty ("Authorization", basicAuth);
		String serverStatusPage ="No Message";
    
		byte[] html;
		try {
			InputStream in = urlConnection.getInputStream();
			ByteArrayOutputStream bout = new ByteArrayOutputStream(); 
			byte[] buffer = new byte[512];
			int read = 0;
			while ((read = in.read(buffer)) != -1) {
				bout.write(buffer, 0, read);
			}
			html = bout.toByteArray();
			String jsForOpenServerStatusPage="<style>.wizbodybg {	background: #FFFFFF; 	font-family: Arial, Helvetica, sans-serif;}\n .wizHdr {text-align: right; 	margin-top: 8px; 	margin-bottom: 8px; 	background-color: #03447e;	table-layout:fixed;	background: #4F85AA; 	padding: 4px; 	color: #FFFFFF; font-weight: bold; 	font-size: 1.1em; 	vertical-align: middle;}\n"+
					".wizTitleTD {	padding: 4px; 	text-overflow:ellipsis;	overflow:hidden;	white-space:nowrap;}\n"+
					".frameTitle {	border-bottom: #babfb0 1px solid;	background-color: #bbbbbb;	width: 100%;	padding: 2px 0px; left: expression(parentNode.parentNode.scrollLeft); z-index: 35;	position: relative;}\n"+
					".pplabel {	text-align: left;	vertical-align: top; font-weight: bold;}\n +"
					+ ".frameContent {	padding-right: 2px; padding-top: 0px;}\n"+
					".tablecellsepbg {	background: #d6d6d6;}\n"+
					".frameTable {	width: 100%;}\n"+
					".tablecolumnheaderbg {border-style:solid;border-width: 1px;white-space: nowrap;z-index: 20;position: relative;font-weight: normal;font-family: Arial, Helvetica, sans-serif;text-align :left;padding-left: 5px; }\n"+
					" .tablecolumnheaderbg {background: #B5D2E3;border-color: #ffffff #76766e #76766e #f2f2ee; }\n"+
					".tablebody .e {background: #eeeeee;}\n"+
					".tablebody .o {background: #ffffff;}\n"+
					".c {color: #000000;font-size: .8em;border-right: #e5e5d9 1px solid;border-bottom: #e5e5d9 1px solid;font-weight: normal;padding-left: 2px;	padding-right: 2px;	text-align:left;vertical-align: top;}\n"+
					"a:link, a {	color:#03447E;	font-weight: bold;}\n"+"a:hover {color:#B66036;}</style>";

			serverStatusPage = new String(html, org.apache.commons.lang.CharEncoding.UTF_8);
			serverStatusPage = jsForOpenServerStatusPage + serverStatusPage;
			page.append(TABLE_HEADLINE_START +"Windchill Server Status Page(Static)" +TABLE_HEADLINE_END);
			String SupportedPath = todayFolder + File.separator + "SupportedFiles";

			checkFolder(SupportedPath);
			File Supportedfolder = new File (SupportedPath);
			File ServerStatus=new File(Supportedfolder,"ServerStatus_"+(new Date()).getTime()+".html");

			page.append("<iframe frameborder=\"0\" src=\"SupportedFiles/"+ServerStatus.getName()+"\" scrolling=\"yes\" width=\"100%\" height=\"1200\" name=\"serverbox\" id=\"serverbox\">");
			page.append("</iframe>");
			page.append(END_DIV);

			PrintWriter pwS =new PrintWriter(ServerStatus);
			pwS.println(serverStatusPage);
			pwS.close();
			return ServerStatus;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		return null;		
	}
	
	
		


	// Method to Stop Cad Monitors
	public static String resetWorkers(String result, CadProxy proxy) throws InterruptedException {
		
		String trim = result.replaceAll("td", "\\+");
		
		String[] list = trim.split("\\+");
		
		String workerResetStatus = "";
		
		int len = list.length;
		
		//System.out.println("Length of final string : " + len);

		boolean flag = false;
		
		for (int i = 1; i < len; i += 2) {
			
			list[i] = list[i].replace(">", "").replace("</", "");
			
			if (flag) {
				
				list[i] = list[i].replace(">", "").replace("</", "");
				
				if (list[i].equals("Fails to Start")) {	//Removed off condition while checking the worker status
					
					String host = list[i-2].substring(0,list[i-2].lastIndexOf("-"));
					String shapeType = list[i-2].substring(list[i-2].lastIndexOf("-")+1, list[i-2].length()-2);
					int instNumber = Integer.parseInt(list[i-2].substring(list[i-2].length()-1, list[i-2].length()));
					
					proxy.stopMonitor(shapeType, host);
					//proxy.startWorker(shapeType, host, instNumber, "");
					
					 // change : adding worker reset status
					workerResetStatus = workerResetStatus + "<p style = \"color:green;\"><b> Worker [ " + list[i-2] + " ] has been Reset! </b><p><br>";
					
					//Thread.sleep(60000); // implementing a necessary time lag between resetting workers from 15 --> 60 seconds (60 seconds)
					
				}
			}
			flag = flag ? false : true;
		}
		
		return workerResetStatus;
	}
	
	
	@SuppressWarnings("deprecation")
	public static HashMap<?,?>[] getCADWorkerStatus() throws IOException, WTException{

		String result = "";
		String verify = "";
		
		StringBuilder table = new StringBuilder();
		boolean isMail = false;
		table.append(TABLE_HEADLINE_START + "CAD Worker Status" + TABLE_HEADLINE_END);
		
		try{


			table.append(START_TABLE);

			String[] columns ={" Worker "," Status "};
			table.append(getHeader(columns));

			String Output = START_TR + START_TD_S;
			Output += "{displayname}";
			Output += END_TD + START_TD_S;
			Output += "{status}" + END_TD;
			Output += END_TR;


			String nonceReplacementValue = "_CSRF_NONCE"; 
			System.out.println("nonceReplacementValue -->"+nonceReplacementValue);
			//String nonce = CSRFProtector.getNonce(session);
			String nonceKeyValue = "000DM0000000M000";
			// String nonceKeyValue = "CSRFProtector.NONCE_KEY+"="+java.net.URLEncoder.encode(nonce,"UTF-8")";
			 //String nonceKeyValue = CSRFProtector.NONCE_KEY+"="+java.net.URLEncoder.encode(nonce,"UTF-8");
			 System.out.println("nonceKeyValue -->"+nonceKeyValue);
			String stateString = "<A ID=\"ssVizAdminCad:IconLink:{trailid}\" HREF=\"admincad.jsp?{action}={shapetype}&host={host}&instanceNumber={instancenumber}&starttime={starttime}&" + nonceKeyValue + "\"><IMG SRC=../../../wt/clients/images/wvs/{action}.gif hspace=0 vspace=0 border=0 {alttag}></A>~"
					+ " ALT=\"_CAD_START\" TITLE=\"_CAD_START\" ~"
					+ " ALT=\"_CAD__STOP\" TITLE=\"_CAD__STOP\" ~"
					+ " ALT=\"_CAD_MAKE_OFFLINE\" TITLE=\"_CAD_MAKE_OFFLINE\" ~"
					+ " ALT=\"_CAD_MAKE_ONLINE\" TITLE=\"_CAD_MAKE_ONLINE\" ~"
					+ "javascript:createDialogWindow('admintestworkermain.jsp?shapeType={shapetype}&host={host}&instanceNumber={instancenumber}', 'workertest', '800', '600', '0')~"
					+ " ALT=\"_CAD_TEST_WORKER\" TITLE=\"_CAD_TEST_WORKER\" ~"
					+ "javascript:createDialogWindow('adminlogworkerlist.jsp?shapeType={shapetype}&host={host}&instanceNumber={instancenumber}', 'workerlog', '600', '600', '0')~"
					+ " ALT=\"_CAD_LOG_WORKER\" TITLE=\"_CAD_LOG_WORKER\" ~"
					+ "admincad.jsp?stopmonitor=1&shapeType={shapetype}&host={host}&"+com.ptc.core.appsec.CSRFProtector.NONCE_KEY+"="+nonceReplacementValue+"~"
					+ " ALT=\"_CAD_STOP_MONITOR\" TITLE=\"_CAD_STOP_MONITOR\" ~"
					+ " ALT=\"_CAD_EXE_NOT_SET\" TITLE=\"_CAD_EXE_NOT_SET\" ~"
					+ " ALT=\"_CAD_EXE_NOT_SAFE\" TITLE=\"_CAD_EXE_NOT_SAFE\" ";
System.out.println("stateString -->"+stateString);

			CadProxy proxy=new CadProxy();
			
			result = proxy.reportStatus(Output, stateString);
			System.out.println("Initial Result -->"+result);
						
			result = Util.escapeFromCADAgentRUN(result);
			result=result.replace("<tr><td>VDCAWQ05272.rd.ds.ge.com-SOLIDWORKS:1</td><td>Off</td></tr>","");
			result=result.replace("<tr><td>VDCAWQ05269.rd.ds.ge.com-APE:1</td><td>Off</td></tr>","");
			
	//	if(result.contains("<tr><td>VDCAWQ05272.rd.ds.ge.com-PROE:1</td><td>Off</td></tr>") || result.contains("<tr><td>VDCAWQ05269.rd.ds.ge.com-OFFICE:1</td><td>Off</td></tr>")){

			System.out.println("Result -->"+result);

			if ( ! result.startsWith("$ERROR") ) {
				result = result.replaceAll(nonceReplacementValue, java.net.URLEncoder.encode("UTF-8"));
			System.out.println("result.startsWith ERROR"+result);
			}

			if(result.contains("Off") || result.contains("Fails to Start"))
			{
				String resetReport = resetWorkers(result, proxy);
				System.out.println("ResetReport -->"+resetReport);
			//	Thread.sleep(300000); // pausing the execution for 300 seconds || Change : From 5 second to 300 second
				
				verify = proxy.reportStatus(Output, stateString);

				verify = Util.escapeFromCADAgentRUN(verify);
				System.out.println("result.contains Fails to Start");

				if ( ! verify.startsWith("$ERROR") ) {
					verify = verify.replaceAll(nonceReplacementValue, java.net.URLEncoder.encode("UTF-8"));
				System.out.println("verify.startsWith ERROR");
				}
				
				 // if reset workers does not work, this block will trigger it in mail 
					if (verify.contains("Fails to Start") ||verify.contains("Off")) {                        
					String[] td = getTD(1, true);
					//result.replaceAll(START_TD_S + "Fails to Start" + END_TD,td[0] +"Fail to Start"+td[1]);
					result.replaceAll(START_TD_S + "Off" + END_TD,td[0] +"Off"+td[1]);
					isMail= true;
					System.out.println("verify.contains Fails to Start-->Triggers Mail");
					result.replaceAll(END_TR + START_TD_S, END_TR + START_TD_EVEN);
					table.append(result);

					table.append(END_TABLE);
					table.append("<p style = \"color:red;\"><b> Failed to restart worker, Kindly manually reset the worker </b><p><br>");
					table.append(resetReport);
					
				}
				else {
					table.append("<p style = \"color:green;\"><b> Workers successfully Reset!! </b><p>");
					table.append(resetReport);				
				}
			//}
			}else{
				result.replaceAll(END_TR + START_TD_S, END_TR + START_TD_EVEN);
				table.append(result);
				table.append(END_TABLE);
			}
			
			System.out.println("End Result"+result);
			System.out.println("Inside");
		}
		catch(Exception e)
		{
			System.out.println("EXCEption");
			e.printStackTrace();
			table.append(e.fillInStackTrace());
		}
		table.append(END_DIV);
/*
	boolean distributedCadAgentEnabled = localWVSProperties.isDistributedCadAgentEnabled();
		String iniFile = localWVSProperties.getInifileName();
		StringBuilder workerResult= new StringBuilder();
		boolean isMailWorker =false;
		try {
			Inifile localInifile = new Inifile();
			localInifile.load(iniFile);

			int masterPort = localInifile.getInt("[agent]", "port");

			int numberOfWorkers = localInifile.getInt("agent", "numworkers");

			TelnetClient conn=new TelnetClient();


			workerResult.append(TABLE_HEADLINE_START + "GS Worker Services Status" + TABLE_HEADLINE_END);
			workerResult.append(BR+" Windchill Port : "+masterPort);
			workerResult.append(BR+" Number Of Workers : "+numberOfWorkers);
			workerResult.append(BR+" Distributed Worker : "+distributedCadAgentEnabled+BR);					
			workerResult.append(START_TABLE);

			String[] gs_columns ={" Worker ID "," Host "," Type ","Service Port "," Status "};
			workerResult.append(getHeader(gs_columns));

			for (int j = 1; j <= numberOfWorkers; j++)
			{
				String worker = "[worker" + j + "]";
				int workerPort = localInifile.getInt(worker, "port");						   
				String workerHost = localInifile.getStr(worker, "host");
				String workerType = localInifile.getStr(worker, "shapetype");
				
				try
				{
					conn.connect(workerHost,workerPort);
					System.out.println("Host -->"+workerHost+ " Port -->"+workerPort);
					conn.disconnect();
					String td[] = getTD(j, false);					   
					workerResult.append(START_TR);
					workerResult.append(td[0] + worker + td[1]);
					workerResult.append(td[0] + workerHost + td[1]);
					workerResult.append(td[0] + workerType + td[1]);
					workerResult.append(td[0] + workerPort + td[1]);
					workerResult.append(td[0] + "Ok" + td[1]);
					workerResult.append(END_TR);	
				}
				catch (Exception e)
				{							    
					isMailWorker = true;
					String td[] = getTD(j, true);					   
					workerResult.append(START_TR);
					workerResult.append(td[0] + worker + td[1]);
					workerResult.append(td[0] + workerHost + td[1]);
					workerResult.append(td[0] + workerType + td[1]);
					workerResult.append(td[0] + workerPort + td[1]);
					workerResult.append(td[0] + "Down" + td[1]);
					workerResult.append(END_TR);							   
				}
			}
			workerResult.append(END_TABLE);
			workerResult.append(END_DIV);
		}
		catch(Exception e)
		{
			System.out.println("\n Error while Checking GS Worker Services");
		}
*/


		
		StringBuilder TibcoResult= new StringBuilder();
		boolean isMailWorker =false;
		String TibcoURL="http://vdcawq05272.rd.ds.ge.com:8426/administrator/servlet/tibco_administrator";
		try {
				URL url1 = new URL("http://vdcawq05272.rd.ds.ge.com:8426/administrator/servlet/tibco_administrator");
     
			


			TibcoResult.append(TABLE_HEADLINE_START + "Tibco URL Status" + TABLE_HEADLINE_END);
			
							
			TibcoResult.append(START_TABLE);

			String[] gs_columns ={" Tibco URL "," Status "};
			TibcoResult.append(getHeader(gs_columns));

			
				
				//String td[]=new String[2];
				int j=1;
			try
				{
					HttpURLConnection huc = (HttpURLConnection) url1.openConnection();
 
                    int responseCode = huc.getResponseCode();
                    System.out.println(responseCode);

                     System.out.println("responding");
						String td[] = getTD(j, false);			   
					TibcoResult.append(START_TR);
					TibcoResult.append(td[0] + TibcoURL+ td[1]);
					
					TibcoResult.append(td[0] + "Ok" + td[1]);
					TibcoResult.append(END_TR);	
				}
			catch (Exception e)
				{							    
					isMailWorker = true;
						String td[] = getTD(j, true);			   
					TibcoResult.append(START_TR);
					
					TibcoResult.append(td[0] + TibcoURL + td[1]);
					
					TibcoResult.append(td[0] + "Down" + td[1]);
					TibcoResult.append(END_TR);	
                    				}
			
			TibcoResult.append(END_TABLE);
			//TibcoResult.append("<p style = \"color:red;\"><b> Tibco URL is not responding,Please restart the Tibco services </b><p><br>");					

			TibcoResult.append(END_DIV);
		}
		catch(Exception e)
		{
			System.out.println("\n Please check Tibco Services");
		}

		HashMap<?, ?>[] returnValue = new HashMap<?, ?>[2];
		returnValue[0] = getReturnMap(table, isMail);
		//returnValue[1] = getReturnMap(workerResult, isMailWorker);
		returnValue[1] = getReturnMap(TibcoResult, isMailWorker);
		return returnValue;
	}


	public static HashMap<String,String> getQueueRunningStatus() throws WTException
	{
		List<WtQueue> allQueues = MVCQueuePage.getQueues();
		StringBuilder table=new StringBuilder();
		HashSet<String> skipQueues = new HashSet<String>();
		table.append(TABLE_HEADLINE_START +"Windchill Queues" + TABLE_HEADLINE_END);
		table.append("This table will highlight the queues not started/starting , disabled and carry forwarded severe entries");
		if(skipQ.contains(","))
		{
			skipQueues.addAll(Arrays.asList(skipQ.split(",")));
			table.append("\nQueues specified in skip list are "+skipQueues);
		}
		else if (!skipQ.equalsIgnoreCase(""))
		{
			skipQueues.add(skipQ);
			table.append("\nQueue name specified in skip list is "+skipQueues);
		}
		else
		{
			skipQueues.add("");
			table.append("\n No Queue name specified in skip list");
		}

		table.append(START_TABLE);

		String[] columns ={" Queue Name "," Type "," Status "," Enable /Disable "," Total Entries "," Waiting Entries "," Severe Entries "};
		table.append(getHeader(columns));

		boolean isMail=false;
		int rowCount = 1;
		for(Iterator<WtQueue> it= allQueues.listIterator();it.hasNext();  )
		{
			WtQueue localWtQueue = it.next();

			boolean isActionReq = false;
			boolean isEnabled = !localWtQueue.isEnabled();
			boolean isQueueState = false;
			//if(localWtQueue.getSevereFailedEntryCount() > 0)
			//	isActionReq=true;
			//else
			//	isActionReq=false;
			rowCount++;
			if (!(((localWtQueue.getQueueState().equals("STARTED")) && (localWtQueue.isEnabled())) 
					|| ((localWtQueue.getQueueState().equals("STARTING")) && (localWtQueue.isEnabled()))))
			{
				isQueueState = true;
			}
			table.append( START_TR);
			String td[] = getTD(rowCount, false);
			String tdName[] = getTD(rowCount, isActionReq||isEnabled||isQueueState);
			String tdStatus[] = getTD(rowCount, isQueueState);
			String tdEnabled[] = getTD(rowCount, isEnabled);
			String tdSevere[] = getTD(rowCount, false);
			if(! (skipQueues.contains(localWtQueue.getName()))){
			table.append( tdName[0] + localWtQueue.getName() + tdName[1]);
			table.append( td[0] + localWtQueue.getType() + td[1]);
			table.append( tdStatus[0] + localWtQueue.getQueueState() + tdStatus[1]);
			table.append( tdEnabled[0] + getEnabledDisplay(localWtQueue.isEnabled()) + tdEnabled[1]);
			table.append( td[0] + localWtQueue.getTotalEntryCount() + td[1]);
			table.append( td[0] + localWtQueue.getWaitingEntryCount() + td[1]);				
			table.append( tdSevere[0] + localWtQueue.getSevereFailedEntryCount() + tdSevere[1]);
			table.append( END_TR);
			}

			if((!checkCollection(skipQueues, localWtQueue.getName())) && (isEnabled||isQueueState))
			{
				isMail=true;
			}

		}

		table.append( END_TABLE);
		table.append(END_DIV);
		return getReturnMap(table, isMail);
	}

	public static HashMap<?, ?>[] getVaultStatus(Connection conn) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, SQLException, IOException, WTException{

		Statement stmt = conn.createStatement();   
		String query =""; 

		String folderOverviewOracleQuery = "(SELECT s.name as Site, fv.name as Vault, fh.hostName as Hostname, fm.status as MountStatus, fm.path as Path FROM Site s INNER JOIN FvVault fv ON s.idA2A2=fv.idA3A5 INNER JOIN FvFolder ff ON fv.idA2A2=ff.idA3A5 INNER JOIN FvMount fm ON ff.idA2A2=fm.idA3A5 INNER JOIN FvHost fh ON fh.idA2A2=fm.idA3B5) UNION ALL (SELECT s.name as Site, rv.name as Vault, fh.hostName as Hostname,fm.status as MountStatus, fm.path FROM Site s INNER JOIN ReplicaVault rv ON s.idA2A2=rv.idA3A5 INNER JOIN ReplicaFolder rf ON rv.idA2A2=rf.idA3A5 INNER JOIN FvMount fm ON rf.idA2A2=fm.idA3A5 INNER JOIN FvHost fh ON fh.idA2A2=fm.idA3B5) ORDER BY Site,Vault,Path";



		if (localDatastore instanceof Oracle)
		{

			query=folderOverviewOracleQuery;
		}

		else if(localDatastore instanceof SQLServer)
		{

			query=folderOverviewOracleQuery;
		}
		else
		{
			throw new WTException("Error while reading localDatastore");
		}

		ResultSet rs = stmt.executeQuery(query);
		StringBuilder table = new StringBuilder();


		table.append(TABLE_HEADLINE_START + "Vault Mount Status" +TABLE_HEADLINE_END);
		table.append("Note: Remote file servers disk space check is not applicable for master server OS other than Windows");
		table.append(BR+"Minimum space limit specified "+fvMinFreeSpaceLimitInGB+" GB \t Minimum space percentage "+fvMinFreeSpaceInPercentage+"%");
		table.append(START_TABLE);
		String[] columns ={ " Site "," Vault "," Host Name "," Mount Path "," Mount Status "};
		table.append(getHeader(columns));

		int rowCount=1;
		boolean isMail=false;
		HashMap <String,Object[]> vaultSize =new HashMap<String,Object[]> ();

		while(rs.next())
		{
			String[] td = getTD(rowCount, false);
			table.append(START_TR);
			String hostName = rs.getString("Hostname").equalsIgnoreCase("masterDefault") ? wtHostName : rs.getString("Hostname");
			String site = rs.getString("Site");			

			String vault = rs.getString("Vault");
			String path = rs.getString("Path");
			String tempPath = path;
			String shortPath = tempPath.substring(0,tempPath.lastIndexOf(File.separator));
			tempPath = path;

			String mountStatus = rs.getString("MountStatus");
			table.append(td[0] + site +td[1]);
			table.append(td[0] + vault +td[1]);
			table.append(td[0] + hostName +td[1]);
			table.append(td[0] + tempPath +td[1]);
			String key = hostName + tempPath.substring(0,tempPath.lastIndexOf(File.separator));
			if(!mountStatus.equalsIgnoreCase("VALID")){
				String[] td_h= getTD(rowCount, true);
				table.append(td_h[0] + mountStatus +td_h[1]);
				isMail=true;						
				if(vaultSize.get(key) == null){
					vaultSize.put(key,new Object[] {false,NA,NA,NA,NA,hostName,site,vault,shortPath});
				}
			}

			else if(System.getProperty("os.name").contains("Windows"))
			{
				table.append(td[0] + mountStatus +td[1]);
				if(vaultSize.get(hostName + tempPath.substring(0,tempPath.lastIndexOf(File.separator))) == null){
					Object[] obj= getVaultFolderDiskSpace(hostName,tempPath,fvMinFreeSpaceLimitInGB,fvMinFreeSpaceInPercentage);
					vaultSize.put((String)obj[0],new Object[] { obj[1],obj[2],obj[3],obj[4],obj[5],site,vault,hostName,shortPath});
				}
			}

			else
			{
				table.append(td[0] + mountStatus +td[1]);
				if(vaultSize.get(key) == null){
					vaultSize.put(key,new Object[] {false,NA,NA,NA,NA,hostName,site,vault,shortPath});
				}
			}

			table.append(END_TR);
			rowCount++;

		}
		table.append(END_TABLE);
		table.append(END_DIV);


		HashMap<?, ?>[] returnValue = new HashMap<?, ?>[2];
		returnValue[0] = getReturnMap(table, isMail);
		returnValue[1] = getAllVaultRootFoldersDiskSpace(vaultSize,fvMinFreeSpaceLimitInGB,fvMinFreeSpaceInPercentage);
		return returnValue;
	}

	public static HashMap<?, ?>[] getVaultStatus() throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, SQLException, IOException, WTException
	{
		return getVaultStatus(getSQLConnection());
	}

	public static HashMap<String, String> getQueueEntryDetails() throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, SQLException, WTException
	{
		return getQueueEntryDetails(getSQLConnection());
	}


	public static Connection getSQLConnection() throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, SQLException, WTException
	{
		Connection conn = SQLCommandTool.getConnection(SQLCommandTool.getWindchillDBUser(),DBProperties.DB_PASSWORD);
		setSession(conn);
		return conn;
	}

	public static HashMap<String,String> getAllVaultRootFoldersDiskSpace(HashMap<String,Object[]> vaultSize,long minFreeSpaceLimitInGB,long minFreeSpaceInPercentage)
	{
		int rowCount=1;
		Set<String> pathKey = vaultSize.keySet();

		StringBuilder table = new StringBuilder();
		table.append(TABLE_HEADLINE_START + "Vault Disk Space" +TABLE_HEADLINE_END);
		table.append("Note: Remote file servers disk space check is not applicable for master server OS other than Windows");
		table.append(BR+"Minimum space limit specified "+minFreeSpaceLimitInGB+" GB");
		table.append(BR+"Minimum space percentage "+minFreeSpaceInPercentage+"%");
		table.append(START_TABLE);
		String[] columns ={ " Site "," Vault "," Host Name "," Root Mount Path "," Disk Total Space "," Disk Free Space "," Disk Free Space in Percentage "," Disk Status "};
		table.append(getHeader(columns));

		boolean isMail = false;
		Iterator<String> keyit = pathKey.iterator();
		while(keyit.hasNext())
		{
			Object[] objFromMap =  vaultSize.get(keyit.next());
			String[] td = getTD(rowCount, false);
			table.append(START_TR);
			boolean isLow =  (Boolean) objFromMap[0];

			table.append(td[0] + objFromMap[5] +td[1]);
			table.append(td[0] + objFromMap[6] +td[1]);
			table.append(td[0] + objFromMap[7] +td[1]);
			table.append(td[0] + objFromMap[8] +td[1]);

			if(isLow)
			{
				String[] td_h= getTD(rowCount, true);
				isMail=true;

				if((Boolean) objFromMap[4])
				{
					table.append(td[0] + objFromMap[1] +td[1]);
					table.append(td_h[0] + objFromMap[2] +td_h[1]);
					table.append(td_h[0] + objFromMap[3] +td_h[1]);
					table.append(td_h[0] + "Low Disk Space "+td_h[1]);
				}
				else
				{
					table.append(td[0] + NA +td[1]);	
					table.append(td[0] + NA +td[1]);	
					table.append(td[0] + NA +td[1]);
					table.append(td_h[0] + "Not able to Ping Server"+td_h[1]);
				}
			}
			else
			{
				table.append(td[0] + objFromMap[1] +td[1]);
				table.append(td[0] + objFromMap[2] +td[1]);
				table.append(td[0] + objFromMap[3] +td[1]);
				table.append(td[0] + "Disk space above limit"+td[1]);						
			}

			table.append(END_TR);
			rowCount++;
		}


		table.append(END_TABLE);
		table.append(END_DIV);

		return getReturnMap(table, isMail);
	}

	public static Object[] getVaultFolderDiskSpace(String hostName, String path, long minFreeSpaceLimitInGB, long minFreeSpaceInPercentage) throws IOException {

		boolean isRemote= !wtHostName.equalsIgnoreCase(hostName);
		Object[] obj= new Object[6];
		File localVault = null;
		String localPath=path;
		boolean isReachable = true;
		if(isRemote)
		{

			String remotePath = File.separator +File.separator + hostName +File.separator;
			if (localPath.charAt(1) == ':') {
				localPath =	localPath.charAt(0)+"$"+localPath.substring(localPath.indexOf(File.separator));
			}
			String temp=localPath.substring(0,localPath.lastIndexOf(File.separator));
			localVault= new File(remotePath + temp);

			if(monitoringVerbose)
				System.out.println("Remote File path :"+localVault.getCanonicalPath());

			InetAddress inet = InetAddress.getByName(hostName);//ByAddress(new byte[] { 127, 0, 0, 1 });
			if(inet.isReachable(5000))
			{
				isReachable = true;
			}
			else
			{
				isReachable = false;
				System.out.println(hostName+" is NOT reachable");
			}
		}
		else
		{
			localVault= new File(localPath.substring(0,localPath.lastIndexOf(File.separator)));
		}

		long freeSpaceInDrive=localVault.getFreeSpace();
		float freeSpaceInPercentage = 0;

		if( localVault.getFreeSpace() != 0)
		{
			freeSpaceInPercentage = getPercentage(freeSpaceInDrive,localVault.getTotalSpace());
		}

		obj[0] = hostName + path.substring(0,path.lastIndexOf(File.separator));

		if(freeSpaceInDrive<= minFreeSpaceLimitInGB ||freeSpaceInPercentage <= minFreeSpaceInPercentage){				
			obj[1] = true;
		}
		else
		{
			obj[1] = false;
		}	
		obj[2] =  convertBytetoGB(localVault.getTotalSpace()) + GB;
		obj[3] =  convertBytetoGB(freeSpaceInDrive) + GB;
		obj[4] =  freeSpaceInPercentage + "%";
		obj[5] =  isReachable;

		return obj;
	}



	public static HashMap<String,String> getQueueEntryDetails(Connection conn ) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, SQLException, WTException{

		Statement stmt = conn.createStatement();       
		String query =""; 

		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
		Timestamp currenttime=new Timestamp(cal.getTimeInMillis());

		cal.add(Calendar.MINUTE, -queueEntrylastMinutes);
		Timestamp starttime=  new Timestamp(cal.getTimeInMillis());

		if (localDatastore instanceof Oracle)
		{
			/*(select pq.name,qe.codec5, count(qe.ida2a2) as entries from queueentry qe, processingqueue pq where pq.ida2a2=qe.IDA3A5 and codec5 != 'COMPLETED' 
			 * and qe.createstampa2 BETWEEN '28-05-2012 15:29:15' and '28-05-2015 15:29:15' group by pq.name, qe.codec5) UNION (select sq.name,sqe.codec5, count(sqe.ida2a2)
			 *  as entries from schedulequeueentry sqe, schedulequeue sq where sq.ida2a2=sqe.IDA3A5 and codec5 != 'RESCHEDULE' and sqe.createstampa2 BETWEEN '28-05-2012 15:29:15'
			 *   and '28-05-2015 15:29:15' group by sq.name, sqe.codec5)
			 */
			/*			query="(select pq.name,qe.codec5, count(qe.ida2a2) as entries from queueentry qe, processingqueue pq where pq.ida2a2=qe.IDA3A5 and codec5 != "+"'"+"COMPLETED"+"'"+
					" and qe.createstampa2 BETWEEN '" +todispayTime.format(starttime)+"' and '"+todispayTime.format(currenttime)+"' group by pq.name, qe.codec5) UNION "+
					"(select sq.name,sqe.codec5, count(sqe.ida2a2) as entries from schedulequeueentry sqe, schedulequeue sq where sq.ida2a2=sqe.IDA3A5 and codec5 != "+"'"+"RESCHEDULE"+"'"+
					" and sqe.createstampa2 BETWEEN '" +todispayTime.format(starttime)+"' and '"+todispayTime.format(currenttime)+"' group by sq.name, sqe.codec5)";
			 */	


			query="(select pq.name,qe.codec5, count(qe.ida2a2) as entries from queueentry qe, processingqueue pq where pq.ida2a2=qe.IDA3A5 and"+
					" qe.createstampa2 BETWEEN '" +todispayTime.format(starttime)+"' and '"+todispayTime.format(currenttime)+"' group by pq.name, qe.codec5) UNION "+
					"(select sq.name,sqe.codec5, count(sqe.ida2a2) as entries from schedulequeueentry sqe, schedulequeue sq where sq.ida2a2=sqe.IDA3A5 and codec5 != "+"'"+"RESCHEDULE"+"'"+
					" and sqe.createstampa2 BETWEEN '" +todispayTime.format(starttime)+"' and '"+todispayTime.format(currenttime)+"' group by sq.name, sqe.codec5)";

		}

		else if(localDatastore instanceof SQLServer)
		{
			query = "(select pq.name,qe.codeC5, count(qe.idA2A2) as entries from QueueEntry qe, ProcessingQueue pq where pq.idA2A2=qe.idA3A5 and CONVERT(char(10),qe.createStampA2,105) BETWEEN '" + 
					todispayTime.format(starttime) + "' and '" + todispayTime.format(currenttime) + "' group by pq.name, qe.codeC5) UNION " + 
					"(select sq.name,sqe.codeC5, count(sqe.idA2A2) as entries from ScheduleQueueEntry sqe, ScheduleQueue sq where sq.idA2A2=sqe.idA3A5 and codeC5 != " + "'" + "RESCHEDULE" + "'" + " and CONVERT(char(10),sqe.createStampA2,105) BETWEEN '" + 
					todispayTime.format(starttime) + "' and '" + todispayTime.format(currenttime) + "' group by sq.name, sqe.codeC5)";
		}
		else
		{
			throw new WTException("Error while reading localDatastore");
		}

		ResultSet rs = stmt.executeQuery(query);
		StringBuilder table = new StringBuilder();
		table.append(TABLE_HEADLINE_START + " Queue Entry (last "+wvslastMinutes+" Minutes)" +TABLE_HEADLINE_END );
		table.append("This table will highlight the new queue entries status after "+ todispayTime.format(cal.getTimeInMillis()) + " GMT");
		table.append(START_TABLE);

		String[] columns ={ " Queue Name " ," Status "," Count "};
		table.append(getHeader(columns));
		int tableFlagForAllCases=1;
		boolean isActionReq = false;
		while(rs.next())
		{
			tableFlagForAllCases++;
			table.append(START_TR);
			String[] td=getTD(tableFlagForAllCases, false);
			table.append(td[0] + rs.getString("NAME") + td[1] );
			table.append(td[0] + rs.getString("CODEC5") + td[1] );
			String[] td_h=getTD(tableFlagForAllCases, true);

			if(rs.getString("CODEC5").equalsIgnoreCase("severe")){
				isActionReq = true;
				table.append(td_h[0] + rs.getString("entries") + td_h[1] );
			}else if(rs.getString("CODEC5").equalsIgnoreCase("failed")){
				isActionReq = true;
				table.append(td_h[0] + rs.getString("entries") + td_h[1] );
			}else{
				table.append(td[0] + rs.getString("entries") + td[1] );
			}
			table.append(END_TR);
		}
		table.append(END_TABLE);
		table.append(END_DIV);
		return getReturnMap(table,isActionReq);
	}

	public static HashMap<?,?>[] getJobStatus() throws Exception{



		QueryResult qrExecuting = PublishQueueHelper.getProcessedQueueEntries(-1, -1L,-1L, null, null, new String[] { "EXECUTING" });
		QueryResult qrFailed = PublishQueueHelper.getProcessedQueueEntries(-1, -1L,-1L, null, null, new String[] { "NONEVENTFAILED", "FAILED", "SEVERE" });

		int rowCount = 1;
		String[] td = getTD(rowCount++, false);
		HashMap<?,?>[] returnValue = new HashMap<?,?>[3];

		if(isCompleteQueueCounEnabled)
		{
			QueryResult qrWaiting = PublishQueueHelper.getWaitingQueueEntries(-1);
			QueryResult qrSuccess = PublishQueueHelper.getProcessedQueueEntries(-1, -1L,-1L, null, null,  new String[] { "COMPLETED" });

			StringBuilder table=new StringBuilder();
			table.append(TABLE_HEADLINE_START + "Publishing Count" +TABLE_HEADLINE_END);
			table.append("This table will show count from start.");
			table.append(START_TABLE);

			String[] columns ={ " Status " ," Complete Count "};
			table.append(getHeader(columns));

			table.append(START_TR);

			table.append(td[0] + "Waiting Jobs"+ td[1] +td[0]+ qrWaiting.size() +td[1]);
			table.append(END_TR);

			table.append(START_TR);
			td = getTD(rowCount++, false);
			table.append(td[0] + "Executing Jobs"+ td[1] +td[0]+ qrExecuting.size() +td[1]);
			table.append(END_TR);

			table.append(START_TR);
			td = getTD(rowCount++, false);
			table.append(td[0] + "Failed Jobs"+ td[1] +td[0]+ qrFailed.size() +td[1]);
			table.append(END_TR);

			table.append(START_TR);
			td = getTD(rowCount++, false);
			table.append(td[0] + "Successfull Jobs"+ td[1] +td[0]+ qrSuccess.size() +td[1]);
			table.append(END_TR);
			table.append(END_TABLE);

			table.append(END_DIV);
			returnValue[0] = getReturnMap(table, false);
		}

		rowCount = 1;
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
		StringBuilder tableEXE=new StringBuilder();
		tableEXE.append(TABLE_HEADLINE_START + "Executing Jobs (last "+wvslastMinutes+" Minutes)" +TABLE_HEADLINE_END);
		tableEXE.append("Current Time "+ todispayTime.format(cal.getTimeInMillis()) + " GMT");
		cal.add(Calendar.MINUTE, -wvsMaxExeTime);
		tableEXE.append(BR+"This table will highlight the objects started executing before "+ todispayTime.format(cal.getTimeInMillis()) + " GMT");
		tableEXE.append(START_TABLE);

		String[] columnsEXE ={ " Object " ," Object Version "," Start Time "," Submited Time "};
		tableEXE.append(getHeader(columnsEXE));

		boolean isMailExe =false;
		while(qrExecuting.hasMoreElements())
		{
			tableEXE.append(START_TR);
			Persistable[] arrayOfPersistable = (Persistable[]) qrExecuting.nextElement();
			QueueEntry queueEntry = (QueueEntry) arrayOfPersistable[1];

			WVSProcessingJob wvsProcessingJob = PublishQueueHelper.extractProcessingJob(queueEntry);
			if(queueEntry.getModifyTimestamp().getTime() < cal.getTimeInMillis())
			{
				String[] td_h = getTD(rowCount, true);
				tableEXE.append(td_h[0] + wvsProcessingJob.getTargetNumber() +td_h[1] );
				tableEXE.append(td_h[0] + wvsProcessingJob.getTargetDisplayVersion(Locale.US)+ td_h[1]);
				tableEXE.append(td_h[0] + todispayTime.format(queueEntry.getModifyTimestamp().getTime()) + " GMT"+ td_h[1]);
				tableEXE.append(td_h[0] + todispayTime.format(wvsProcessingJob.getSubmitTime()) + " GMT"+ td_h[1]);		
				isMailExe=true;

			}
			else
			{
				tableEXE.append(td[0] + wvsProcessingJob.getTargetNumber() +td[1] );
				tableEXE.append(td[0] + wvsProcessingJob.getTargetDisplayVersion(Locale.US)+ td[1]);
				tableEXE.append(td[0] + todispayTime.format(queueEntry.getModifyTimestamp().getTime()) + " GMT"+ td[1]);
				tableEXE.append(td[0] + todispayTime.format(wvsProcessingJob.getSubmitTime()) + " GMT"+ td[1]);					
			}
			tableEXE.append(END_TR);
			rowCount++;
		}
		tableEXE.append(END_TABLE);
		tableEXE.append(END_DIV);

		rowCount = 1;

		cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));

		StringBuilder tableFailed=new StringBuilder();
		StringBuilder logsFailed=new StringBuilder();

		tableFailed.append(TABLE_HEADLINE_START + "Failed Jobs (last "+wvslastMinutes+" Minutes)" +TABLE_HEADLINE_END);
		tableFailed.append("Current Time "+ todispayTime.format(cal.getTimeInMillis()) + " GMT");
		cal.add(Calendar.MINUTE, -wvslastMinutes);
		tableFailed.append(BR+"This table will highlight the objects failed after "+ todispayTime.format(cal.getTimeInMillis()) + " GMT");

		tableFailed.append(START_TABLE);

		String[] columnsFailed ={ " Object " ," Version "," Start Time "," Submited Time "," Queue Number", " Error Msg"};
		tableFailed.append(getHeader(columnsFailed));


		boolean isMailFail = false;
		while(qrFailed.hasMoreElements())
		{
			Persistable[] arrayOfPersistable = (Persistable[]) qrFailed.nextElement();
			QueueEntry queueEntry = (QueueEntry) arrayOfPersistable[1];
			WVSProcessingJob wvsProcessingJob = PublishQueueHelper.extractProcessingJob(queueEntry);	
			if(queueEntry.getModifyTimestamp().getTime() >= cal.getTimeInMillis())
			{
				String log = wvsProcessingJob.getLogBuffer();
				if(!log.contains("Processing Returned: $ERROR$ Failure to retrieve") 
						&& !log.contains("No Processing Required") 
						&& !log.contains("Error Finalizing Representation")
						&& !log.contains("No EPMDocument found, Publishing can not be performed")
						)
				{
					tableFailed.append(START_TR);
					String[] td_h = getTD(rowCount, true);
					tableFailed.append(td_h[0] + wvsProcessingJob.getTargetNumber() +td_h[1] );
					tableFailed.append(td_h[0] + wvsProcessingJob.getTargetDisplayVersion(Locale.US)+ td_h[1]);
					tableFailed.append(td_h[0] + todispayTime.format(queueEntry.getModifyTimestamp()) + " GMT"+ td_h[1]);
					tableFailed.append(td_h[0] + todispayTime.format(wvsProcessingJob.getSubmitTime()) + " GMT"+ td_h[1]);	
					tableFailed.append(td_h[0] + queueEntry.getNumber() + td_h[1]);
					if(log.contains("Timeout exceeded waiting for a reply from the CadAgent - Time out"))
						tableFailed.append(td_h[0] + "Timeout exceeded waiting for a reply from the CadAgent - Time out"+ td_h[1]);
					else if(log.contains("Returned file does not exist"))
						tableFailed.append(td_h[0] + "Returned file does not exist"+ td_h[1]);
					else
						tableFailed.append(td_h[0] + "New error - ref logs"+ td_h[1]);
					isMailFail=true;
					tableFailed.append(END_TR);
					rowCount++;
					logsFailed.append(D_BR+"<font color='red'>"+B_START +"Log of entry "+queueEntry.getNumber() +"\tNumber :"+wvsProcessingJob.getTargetNumber()+
							"\t Version "+wvsProcessingJob.getTargetDisplayVersion(Locale.US)+B_END+"</font>"+D_BR);
					logsFailed.append(log);
				}
				else
				{
					tableFailed.append(START_TR);
					td = getTD(rowCount, false);
					tableFailed.append(td[0] + wvsProcessingJob.getTargetNumber() +td[1] );
					tableFailed.append(td[0] + wvsProcessingJob.getTargetDisplayVersion(Locale.US)+ td[1]);
					tableFailed.append(td[0] + todispayTime.format(queueEntry.getModifyTimestamp())+ td[1]);
					tableFailed.append(td[0] + todispayTime.format(wvsProcessingJob.getSubmitTime())+ td[1]);	
					tableFailed.append(td[0] + queueEntry.getNumber() + td[1]);

					if(log.contains("Processing Returned: $ERROR$ Failure to retrieve"))
						tableFailed.append(td[0] + "Processing Returned: $ERROR$ Failure to retrieve"+ td[1]);
					else if(log.contains("No Processing Required"))
						tableFailed.append(td[0] + "No Processing Required"+ td[1]);
					else if(log.contains("Error Finalizing Representation"))
						tableFailed.append(td[0] + "Error Finalizing Representation"+ td[1]);
					else if(log.contains("No EPMDocument found, Publishing can not be performed"))
						tableFailed.append(td[0] + "No EPMDocument found, Publishing can not be performed"+ td[1]);

					tableFailed.append(END_TR);
				}
			}
		}

		tableFailed.append(END_TABLE);
		tableFailed.append("*** Note: Failed objects which are not heighlighted will be failed due to Processing Returned: $ERROR$ Failure to retrieve (or) No Processing Required (or) Error Finalizing Representation (or) ForWTPart: No EPMDocument found, Publishing can not be performed."+BR);
		tableFailed.append(logsFailed);
		tableFailed.append(END_DIV);

		returnValue[1] = getReturnMap(tableEXE, isMailExe);
		returnValue[2] = getReturnMap(tableFailed, isMailFail);
		return returnValue;
	}


	public static HashMap<String, String> getFileServers() throws AddressException
	{
		int  rowCount = 0;
		CompositeData  vaultSiteStatusInfo = getVaultSiteStatusInfo();
		TabularData  siteStatusData = ( ( vaultSiteStatusInfo != null ) ? (TabularData) vaultSiteStatusInfo.get( "siteStatusData" ) : null );
		DecimalFormatSymbols  decimalSymbols = new DecimalFormatSymbols( Locale.ENGLISH );
		DecimalFormat  decimalFormat = new DecimalFormat( "0.###", decimalSymbols );
		SimpleDateFormat  dateFormat = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss.SSS Z" );
		dateFormat.setTimeZone( WTContext.getContext().getTimeZone() );
		StringBuilder table=new StringBuilder();
		table.append(TABLE_HEADLINE_START + "File Servers Status" +TABLE_HEADLINE_END);
		table.append(START_TABLE);

		String[] columns ={ "	Site URL " ," Name "," Status "," Time of Last Ping "," Availability "," Average Response Time "};
		table.append(getHeader(columns));

		table.append(START_TR);
		boolean isMail=false;
		int tableRowCount = 1 ;
		for ( CompositeData siteStatus : MBeanUtilities.getSortedData( siteStatusData ) )
		{
			String[] td = getTD(tableRowCount, false);			
			String  names[] = (String[]) siteStatus.get( "names" );
			Date  lastPingTime = (Date) siteStatus.get( "lastPing" );
			String  lastStatus = (String) siteStatus.get( "lastStatus" );
			@SuppressWarnings("unused")
			String temp=(( ++rowCount % 2 ) == 1 ? "o" : "e") ;
			if(MBeanUtilities.getStringArrayAsString( names ).equals("master")){
			table.append(START_TR);

			table.append(td[0] + xmlEscape(siteStatus.get( "url" )) + td[1]);
			table.append(td[0] + xmlEscape( ( names != null ) ? MBeanUtilities.getStringArrayAsString( names ) : null )+ td[1]);

			if(!SiteMonitorMBean.OK_STATUS.equals( lastStatus ))
			{
				String[] td_loc = getTD(tableRowCount, true);
				table.append(td_loc[0] + lastStatus + td_loc[1]);
				isMail=true;
			}
			else
			{
				table.append(td[0] + lastStatus + td[1]);
			}
			table.append(td[0] + xmlEscape( ( lastPingTime != null ) ? dateFormat.format( lastPingTime ) : null )+ td[1]);
			table.append(td[0] + xmlEscape( decimalFormat.format( siteStatus.get( "percentageUptime" ) ) )+ td[1]);
			table.append(td[0] + xmlEscape( decimalFormat.format( siteStatus.get( "averageResponseSeconds" ) ))+ td[1]);
			table.append(END_TR);
			}
			++rowCount;
			tableRowCount++;
		}
		table.append(END_TABLE);
		table.append(END_DIV);
		return getReturnMap(table, isMail);
	}


	private static void autoCleanOlderJobs(Connection conn) throws Exception
	{
		Statement stmt = conn.createStatement();   
		String query =""; 
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
		cal.add(Calendar.DATE, -autoCleanJobsOlderThan);
		Timestamp starttime=  new Timestamp(cal.getTimeInMillis());

		String autoCleanOlderJobs="Delete from queueentry where targetmethod='doJob' and createstampa2 <'"+ todispayTime.format(starttime) +"'";
		if (localDatastore instanceof Oracle)
		{
			query=autoCleanOlderJobs;
		}

		else if(localDatastore instanceof SQLServer)
		{
			autoCleanOlderJobs = "Delete from QueueEntry where targetMethod='doJob' and CONVERT(char(10),createStampA2,105) < '" + todispayTime.format(starttime) + "'";

			query = autoCleanOlderJobs;
		}

		else
		{
			throw new WTException("Error while reading localDatastore");
		}

		stmt.executeUpdate(query);
	}

	private static void autoCleanOlderReports() throws Exception
	{
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DAY_OF_YEAR, -autoCleanReportOlderThan);
		File[] filesOfDir = new File(reportDir).listFiles();
		for(File singledir: filesOfDir) {	    	
			if(singledir.isDirectory() && singledir.lastModified() < cal.getTimeInMillis())
			{
				if(monitoringVerbose){
					System.out.println(singledir.getCanonicalPath() + " will be deleted ");
					//singledir.delete();
					FileUtils.deleteDirectory(singledir);
					System.out.println("Deleted");
				}
				
			}
			else if (singledir.isDirectory() && monitoringVerbose)
			{
				System.out.println(singledir.getCanonicalPath() + " is younger than "+autoCleanReportOlderThan +" days");
			}
		}
	}

	private static CompositeData  getVaultSiteStatusInfo()
	{
		try
		{
			// Use SelfAwareMBean.getSelfAwareMBean() and caste as this will be faster than going through MBeanServer
			return ( ((SiteMonitorMBean)SelfAwareMBean.getSelfAwareMBean( vaultSitesMBeanName )).getSiteStatusInfo() );
		}
		catch ( VirtualMachineError e )
		{
			throw e;
		}
		catch ( Throwable t )
		{
			System.out.printf( "Failed to retrieve vault site status info", t );
			return ( null );
		}
	}

	private static long convertBytetoGB(long size)
	{
		return size/1024/1024/1024;
	}

	private static String getEnabledDisplay(boolean isEnabled)
	{
		return isEnabled?"Enabled":"Disabled";
	}


	private static String[] getTD (int rowCount, boolean highI)
	{
		String[] td = new String[2];
		if (rowCount%2 == 0)
		{
			if(highI)
			{
				td[0] = START_TD_EVEN + START_HIGH;
				td[1] = END_HIGH + END_TD;
			}
			else
			{
				td[0] = START_TD_EVEN;
				td[1] = END_TD;
			}
		}

		else
		{
			if(highI)
			{
				td[0] = START_TD_ODD + START_HIGH;
				td[1] = END_HIGH + END_TD;
			}
			else
			{
				td[0] = START_TD_ODD;
				td[1] = END_TD;
			}
		}
		return td;
	}

	private static boolean checkCollection(HashSet<String> set,String name)
	{
		for(Iterator<String> it=set.iterator();it.hasNext();)
			if(it.next().equalsIgnoreCase(name))
				return true;
		return false;
	}

	private  static void setSession(Connection conn) throws SQLException, WTException{
		String setSessionQuery="alter SESSION set NLS_DATE_FORMAT = 'DD-MM-YYYY HH24:MI:SS'";
		Statement stmt = conn.createStatement();       
		String query =""; 
		if (localDatastore instanceof Oracle)
		{
			query=setSessionQuery;
		}
		else if(localDatastore instanceof SQLServer)
		{
			System.out.println("Do noting for MS SQL Server");
		}
		else
		{
			throw new WTException("Error while reading localDatastore");
		}
		stmt.executeUpdate(query);
	}

	private static HashMap<String,String> getReturnMap(StringBuilder table,boolean isMail)
	{
		HashMap<String,String> map =new HashMap<String,String>();
		if(isMail){
			map.put(MAIL,table.toString());
		}else{
			map.put(MAIL,"");
		}
		map.put(REPORT,table.toString());
		return map;
	}


	private static ObjectName  newObjectName( final String objectNameString )
	{
		try
		{
			return ( new ObjectName( objectNameString ) );
		}
		catch ( Exception e )
		{
			System.out.printf("Could not create ObjectName from " + objectNameString, e );
			if ( e instanceof RuntimeException )
				throw (RuntimeException) e;
			throw new RuntimeException( e );
		}
	}


	private static StringBuilder getHeader(String[] columns)
	{
		StringBuilder header = new StringBuilder();
		header.append(START_TR);
		for(String column:columns )
		{
			header.append(START_TD_H);
			header.append(column);
			header.append(END_TD_H);
		}
		header.append(END_TR);
		return header;
	}

	private static void appendMessagetoMail(HashMap<?, ?> map, String newSubject, StringBuilder subject, StringBuilder message) {

		if(!((String)map.get(MAIL)).equalsIgnoreCase(""))
		{
			if(isEmpty(subject))
				subject.append(newSubject);
			else
				subject.append(" ,"+ newSubject);
			message.append(map.get(MAIL));
		}

	}

	private static String  xmlEscape( final String string )
	{
		return ( MBeanUtilities.xmlEscape( string ) );
	}

	private static String  xmlEscape( final Object object )
	{
		if(object instanceof java.lang.reflect.UndeclaredThrowableException && ((java.lang.reflect.UndeclaredThrowableException) object).getMessage().contains("nested exception is:")){
			String msg = ((java.lang.reflect.UndeclaredThrowableException) object).getMessage();
			msg = msg.substring(msg.indexOf("nested exception is:")+"nested exception is:".length());
			return xmlEscape(msg.substring(0, msg.indexOf("Exception")+"Exception".length()));
		}else{
			return ( ( object != null ) ? xmlEscape( object.toString() ) : null );
		}
	}

	private static boolean isEmpty(StringBuilder table)
	{
		return table.toString().equalsIgnoreCase("");
	}

	private static float getPercentage(float current,float total)
	{
		return ((Float.valueOf(current)*100)/Float.valueOf(total));
	}




	public static String processMethods(String requestid) throws ClassNotFoundException, InstantiationException, IllegalAccessException, NoSuchMethodException, SecurityException, IllegalArgumentException, InvocationTargetException, SQLException, IOException {
		wtproperties = WTProperties.getLocalProperties();
		Class<?> cls = Class.forName(BHDailyMonitor.class.getName());
		Object obj = cls.newInstance();
		Object returnObj = null;

		Class<?>[] paramClass = { };
		Object[] paramObj = {  };
		Method method = cls.getDeclaredMethod("get" + requestid, paramClass);
		returnObj = method.invoke(obj, paramObj);   

		if(returnObj instanceof HashMap<?,?>)
		{
			return (String)((HashMap<?,?>)returnObj).get(REPORT);
		}
		else if (returnObj instanceof HashMap<?,?> [])
		{
			HashMap<?,?> [] collection = (HashMap<?,?>[])returnObj;
			String returnString ="";
			for(HashMap<?,?> report:collection)
			{
				returnString += (String)report.get(REPORT) + BR;
			}
			return returnString;
		}
		else
		{
			return "No Output";
		}
	}


}