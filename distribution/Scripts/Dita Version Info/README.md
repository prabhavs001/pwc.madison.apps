# About script

the groovy to get Dita versions created by different users. The one with fmdita-serviceuser is system generated.

## Variables

Path 
CSV generate path


## How to run (Ignore step 1 - 3 if not done)

1. Install the groovy script console for apache felix: http://felix.apache.org/documentation/subprojects/apache-felix-script-console-plugin.html

2. Download these two jars and install them to the /system/console/bundles UI 	        http://repo1.maven.org/maven2/org/codehaus/groovy/groovy-all/2.1.6/groovy-all-2.1.6.jar http://apache.mirrors.tds.net//felix/org.apache.felix.webconsole.plugins.scriptconsole-1.0.2.jar

3. Go to http://host/system/console/configMgr/org.apache.sling.jcr.base.internal.LoginAdminWhitelist
   Add org.apache.felix.webconsole.plugins.scriptconsole to "Whitelist regexp" and save

4. Go to http://host/system/console/sc

5. Select "Groovy" as the language

6. Copy / paste the contents of script *.groovy to the console and run it - output goes to the error.log by default

# Expected output 

Scripts generate CSV on tmp location 

output cantains below

Label	Count
admin-qa	4752
admin-pwc	183
fmdita-serviceuser	3688
chd	2488
saravanan.sellathurai@pwc.com	1
vinaykumar	8
