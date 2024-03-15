# About script

This groovy script adds the specified version tag to the dita (dita and ditamap files) content under the specified folder. 

## Variables

1. path: AEM folder path under which the dita content needs to be versionized.
2. labelName: Name of the label to be tagged.
3. comment: Comment to be added with the label.

## Install Groovy Console 
`(Note: These steps can be skipped if groovy console is already installed.).`
1. Download the groovy console AEM package from [here](https://github.com/icfnext/aem-groovy-console/releases/download/15.1.0/aem-groovy-console-15.1.0.zip).

2. Install the package in the package manager.

3. Verify the installation by opening the following link, http://<host:port>/apps/groovyconsole.html.

## How to run the script
1. Go to http://<host:port>/apps/groovyconsole.html

2. Copy / paste the contents of script `add-dita-version.groovy` to the "script editor".

3. In the last line of the script, add appropriate values<br>
`bulkCreateVersion("<path>", "<labelBame>", "<comment>");`

4. Click "Run Script" - output will be shown below.

## Expected output
Script will add the specified label and comment to the dita (dita and ditamap files) content under the specified folder.
