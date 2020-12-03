set WT_HOME=D:\ptc\Windchill_11.0\Windchill
set WC_JAVAHOME=D:\ptc\Windchill_11.0\Java
set PATH=%WC_JAVAHOME%\bin;%PATH%
set CLASSPATH=%WT_HOME%\codebase;%WT_HOME%\codebase\WEB-INF\lib\*;%WC_JAVAHOME%\lib\tools.jar;%WT_HOME%\tomcat\bin\bootstrap.jar;%WT_HOME%\tomcat\bin\tomcat-juli.jar
java com.BH.monitor.BHDailyMonitoringStarter %WT_HOME%

