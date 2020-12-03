$Servers = Get-Content "D:\CPU\servers.txt"
$Array = @()

$date = (get-date).ToString('ddhhmmss')
$fileName = "SystemAnalysisReport$date"

#$smtpto = "kishanchandra.prajapati@bhge.com" 
$smtpto = "ExpertAssistWindchill@ge.com" 
$smtpfrom = "bh.engg_app_support_team@ge.com"
$smtpHost = "mail.ad.ge.com"
$smtp = New-Object Net.Mail.SmtpClient($smtpHost)

[decimal]$cputhreshold = 1
[decimal]$memorythreshold = 25


ForEach ($Server in $Servers) {
    
	$isMail = $false
	$messageSubject = "System Health Report - $Server" 
	$message = New-Object System.Net.Mail.MailMessage $smtpfrom, $smtpto 
	$message.Subject = $messageSubject 
	$message.IsBodyHTML = $true
	$message.Body = " "
    $Server = $Server.trim()
	$Processor = $ComputerMemory = $RoundMemory = $Object = $null
	
    Try {
        # Processor utilization
        $Processor = (Get-WmiObject -ComputerName $Server -Class win32_processor -ErrorAction Stop | Measure-Object -Property LoadPercentage -Average | Select-Object Average).Average
 
        # Memory utilization
        $ComputerMemory = Get-WmiObject -ComputerName $Server -Class win32_operatingsystem -ErrorAction Stop
        $Memory = ((($ComputerMemory.TotalVisibleMemorySize - $ComputerMemory.FreePhysicalMemory)*100)/ $ComputerMemory.TotalVisibleMemorySize)
        $RoundMemory = [math]::Round($Memory, 2)		
		
         
        # Creating custom object
        $Object = New-Object PSCustomObject
        $Object | Add-Member -MemberType NoteProperty -Name "Server name" -Value $Server
        $Object | Add-Member -MemberType NoteProperty -Name "CPU %" -Value $Processor
        $Object | Add-Member -MemberType NoteProperty -Name "Memory %" -Value $RoundMemory
 
        $Object
        $Array += $Object
    }
    Catch {
        Write-Host "Something went wrong ($Server): "$_.Exception.Message
        Continue
    }
	
	$CPUHTMLmessage = @" 
		 <font color="Red" face="Microsoft Tai le"> 
		 <body BGCOLOR="White"> 
		<h2>High CPU Utilization Alert</h2> </font> 
		  
		 <font face="Microsoft Tai le"> 
		  You are receiving this alert because the server mentioned in Subject Line is having CPU utilization of  <font color="Red"><b>$Processor % </b></font>which is higher than defined threshold <font color="Red"><b>($cputhreshold %)</b></font>.<br> Please take appropriate action to clear this alert!!!.
		</font> 
		<br> <br>   
"@
	$MemoryHTMLmessage = @" 
		 <font color="Red" face="Microsoft Tai le"> 
		 <body BGCOLOR="White"> 
		<h2>High Memory Utilization Alert</h2> </font>   
		 <font face="Microsoft Tai le"> 
		 You are receiving this alert because the server mentioned in Subject Line is having Memory utilization of <font color="Red"><b>$RoundMemory %</b></font> which is higher than defined threshold <font color="Red"><b>($memorythreshold %) </b></font>.<br> Please take appropriate action to clear this alert!!!.
		</font> 
		<br> <br> 
"@ 
	$messagefooter =@"
		 <h2></h2>
		<body BGCOLOR=""white"">
		<br> <br> <font face="Microsoft Tai le"><center> <i> *** This Alert was triggered by a monitoring script ***</i> </center></font> 
		</body>
"@
	if($cputhreshold -lt $processor){
	$isMail = $true
	$message.Body += $CPUHTMLmessage 
	}
	if($memorythreshold -lt $RoundMemory){
	$isMail = $true
	$message.Body += $MemoryHTMLmessage
	}
	$message.Body += $messagefooter
	
	if($isMail){
	$smtp.Send($message)
	}
}

If ($Array) {
   $Array | Export-Csv -Path "D:\CPU\output\$fileName.csv" -NoTypeInformation -Force
	
}
 
