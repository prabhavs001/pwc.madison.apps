Instruction for AEM packages in the Distribution

1. PWC-FASB-codificationDTD-1.2.0.zip

     - Install this package for FASB DITA maps to work on your AEM instance. The package should install the 'codificationDTD' for FASB

2. PWC-FMDITA-Profiles-1.0.0.zip
      - This is a customized DITA profile for FASB. Please make sure you install this on all AEM instances where you need FASB content to work.
      - Once imported, open 'com.adobe.fmdita.config.ConfigManager' and save it without any modifications at http://<server-url>:<port>/system/console/configMgr to enable the installed DITA profile.
	  The Output path for the Output preset is updated to “/content/pwc-madison/us/en”. also added PWC-Authored profile.

3. PWC-Authored-Content-DTD-1.0.0.zip
      - This package contains the Specialization DTD files needed for PWC Authored content. Please make sure to install this on your AEM instance, if you are working on the PWC authored content DITA files.
	  
4. PWC-AICPA-codificationDTD-1.0.0.zip
      - This package contains the Specialization DTD files needed for PWC AICPA content. Please make sure to install this on your AEM instance, if you are working on the PWC AICPA content DITA files.

Content Packages

1. Content-Packages/pwc-madison-base-content-1.0.0.zip

    - Install this package first when you setup the Madison Site content on a brand new instance, this will instal the '/content/pwc-madison' initial structure with all required properties.
    On production/Stage if you are doing the content copy as it is, you wouldn't require this package.

 User groups & Permissions

 The below packages should be installed on a new AEM instance when you want the Madisons ACLs need to be up and running for different user groups. Please ensure the
 content hierarchy is in place before installing these packages https://pwcwcmreplatform.atlassian.net/wiki/spaces/MP/pages/61014139/AEM+Content+Hierarchy

 1. UserGroupsAndAcls/madison-territory-user-groups-<latest-version>.zip

 2. UserGroupsAndAcls/madison-mac-user-groups-<latest-version>.zip

 3. UserGroupsAndAcls/madison-territory-groups-and-acls-<latest-version>.zip