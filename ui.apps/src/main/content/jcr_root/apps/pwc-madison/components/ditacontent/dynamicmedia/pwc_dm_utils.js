use( function(){

	var title = resourcePage.properties['jcr:description'] ? resourcePage.properties['jcr:description'] : "";
    title = resourcePage.properties['pwc-contentId'] ? resourcePage.properties['pwc-contentId'] + " " + title : title;

    var revisedDate = Packages.com.pwc.madison.core.util.MadisonUtil.getPageRevisedDate(resourcePage);

    function getDuration(seconds) {
        var sec_num = parseInt(seconds, 10);
        var hours   = Math.floor(sec_num / 3600);
        var minutes = Math.floor((sec_num - (hours * 3600)) / 60);
        var seconds = sec_num - (hours * 3600) - (minutes * 60);

        if (hours   < 10) {hours   = "0"+hours;}
        if (minutes < 10) {minutes = "0"+minutes;}
        if (seconds < 10) {seconds = "0"+seconds;}
        return minutes + ':' + seconds;
    }

    return {
        title : title,
        revisedDate : revisedDate,
        getDuration : getDuration
    }
});