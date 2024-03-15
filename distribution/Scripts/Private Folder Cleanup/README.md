# About script

The groovy to remove private folder permissions and move users to parent private folder(level 6).
The groovy will create two files under tmp location as below, these will have the folder level permissions details before and after the private acls are cleaned.

before-move-file.csv
after-move-file.csv

Please update the folder level path at below line
getInfo("/content/dam/pwc-madison/ditaroot/us/en/sec");

To disable dry run and perform the actual operation set private boolean isDryRun to false.

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

Before move and After move 

Path	Role	Group	Access	Members	IsMoved	Moved To
/content/dam/pwc-madison/ditaroot/us/en/sec	owner	mac-default-sec13-owner	crx:replicate,jcr:lockManagement,jcr:modifyAccessControl,jcr:read,jcr:readAccessControl,jcr:versionManagement,rep:write	admin	FALSE	

