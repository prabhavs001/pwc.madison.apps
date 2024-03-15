document.getElementById("btn-op-edit").onclick = function() {
    //TBD - The defaultOutputPath needs to be changed once the Madison hierarchy is finalized
    if (window.frames.report_iframe === null) {
        return;
    }
    //Add decodeURIComponent to decode the encoded url after click on editmap and close.
    var pageUrl = decodeURIComponent(window.location.href),
        defaultOutputPath = pageUrl.substring(pageUrl.indexOf("/content/dam/pwc-madison/"), pageUrl.length),
        damPath, ditaMapDamPath, finalRelativePath, extractedMAdisonPath, ditaPath,
        currentOutputPath = $("#report_iframe").contents().find("form").first().find("input[name='targetpath']").val();
    defaultOutputPath = "/content/pwc-madison/ditaroot/" + defaultOutputPath.split("/")[5] + "/" + defaultOutputPath.split("/")[6];

    if (defaultOutputPath === currentOutputPath) {
        //pageUrl example - http://localhost:4502/libs/fmdita/report/report.html/content/dam/pwc-madison/aicpa/ara-ebp_2017.ditamap
        damPath = pageUrl.substring(pageUrl.indexOf("/content/dam/pwc-madison/"));
        //damPath example - content/dam/pwc-madison/aicpa/ara-ebp_2017.ditamap
        extractedMAdisonPath = damPath.replace("/content/dam/pwc-madison/", "/content/pwc-madison/");
        //extractedMAdisonPath extracted - pwc-madison/aicpa/ara-ebp_2017.ditamap
        ditaMapDamPath = extractedMAdisonPath.substring(extractedMAdisonPath.indexOf('/'));
        //ditaMapDamPath extracted - /aicpa/ara-ebp_2017.ditamap
        ditaPath = ditaMapDamPath.substring(0, ditaMapDamPath.lastIndexOf('/'));
        //ditaPath extracted - /aicpa
        finalRelativePath = ditaPath;
        //finalRelativePath generated - /content/output/sites/aicpa/ara-ebp_2017.ditamap
        //coral-85 is the id used to target the destination path input field
        if ($("input[name=targetpath]").length > 0) {
            $("input[name=targetpath]").val(finalRelativePath);
        } else {
            $("#report_iframe").contents().find("form").first().find("input[name='targetpath']").val(finalRelativePath);
        }
        document.getElementById('report_iframe').contentWindow.$('input[name="targetpath"]').trigger('change');
    }
};