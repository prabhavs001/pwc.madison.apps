# About script

Groovy to get the list of all images having an aspect ratio of height > 1.5 times the width

## Variables


## How to run (Ignore step 1 - 3 if done already)

1. Install the groovy script console for apache felix: http://felix.apache.org/documentation/subprojects/apache-felix-script-console-plugin.html

2. Download these two jars and install them to the /system/console/bundles UI 	        http://repo1.maven.org/maven2/org/codehaus/groovy/groovy-all/2.1.6/groovy-all-2.1.6.jar http://apache.mirrors.tds.net//felix/org.apache.felix.webconsole.plugins.scriptconsole-1.0.2.jar

3. Go to http://host/system/console/configMgr/org.apache.sling.jcr.base.internal.LoginAdminWhitelist
   Add org.apache.felix.webconsole.plugins.scriptconsole to "Whitelist regexp" and save

4. Go to http://host/system/console/sc

5. Select "Groovy" as the language

6. Copy / paste the contents of script *.groovy to the console and run it

# Expected output 
Sample output below:
Asset: /content/dam/pwc-madison/ditaroot/us/en/sec/z_archive/final_rules/2012/assets/0000017878830756.jpg/jcr:content/metadata
Asset: /content/dam/pwc-madison/ditaroot/us/en/sec/financial_reporting_m/topic_12_reverse_acq/12200_reporting_issu/assets/FRM_12200 Reporting Issues.jpg/jcr:content/metadata
Asset: /content/dam/pwc-madison/ditaroot/us/en/sec/financial_reporting_m/topic_1_registrants_/1100_financial_state/assets/Financial Statements and schedules in registration and proxy statements - 1140 Proxy Statemen.jpg/jcr:content/metadata
Asset: /content/dam/pwc-madison/ditaroot/us/en/sec/regulations/regulation_sk/600_exhibits/assets/Item-601_table1.jpg/jcr:content/metadata