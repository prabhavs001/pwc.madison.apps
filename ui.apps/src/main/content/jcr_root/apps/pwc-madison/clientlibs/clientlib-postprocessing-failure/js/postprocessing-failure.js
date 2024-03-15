function checkForFailures() {
    $("td.spectrum-Table-cell.fm_tdtOutputTitle").each(function() {
        if ($(this).find(".failure").length !== 0) {
            var $postProcLogCell = $(this).siblings(".fm_tdtpostprocessinglog");
            if ($postProcLogCell.html() !== "Log File") {
                $postProcLogCell.html("Log File");
                $postProcLogCell.css("color","#4178cd");
            }
        }
    });
}

$(document).ready(setTimeout(function() {

    function prePopulateOutputs(json) {
        var $rows = $("tr.spectrum-Table-row.output_finished.fm_tdtReviewRow"),
            servletPath = '/bin/pwc-madison/post-processing-failure-log?path=';
        $.each(json.outputs, function(key, item){
            var logPath = item.ditaotLogFile.replace('logs.txt', 'postprocessing-logs.txt'),
                $postProcLogCell = $rows.eq(key).children(".fm_tdtpostprocessinglog");
            item.postprocessingLogPath = logPath;
            $postProcLogCell.attr("ischecked","true");
            $postProcLogCell.on("click",function() {
                window.open(servletPath + logPath, "_blank");
            });
        });
    }


    var tHeader = '<th class="spectrum-Table-headCell fm_tdtpostprocessinglogHead">Postprocessing Log</th>',
        pProcCell = '<td class="spectrum-Table-cell fm_tdtpostprocessinglog"></td>',
        payload = (window.rh.model.get(".d.payload")),
        ditamap = window.encodeURIComponent(payload);

    $("th.spectrum-Table-headCell.fm_tdtGeneratedIn").after(tHeader);
    $("td.spectrum-Table-cell.fm_tdtGeneratedIn").after(pProcCell);

    checkForFailures();

    $.ajax({
        type: "GET",
        url: "/bin/publishlistener?operation=PUBLISHBEACON&source=" + ditamap,
        cache: false,
        success: function(data, status, jqXhr) {
            if (jqXhr.status === 200 && jqXhr.responseText) {
                var response = JSON.parse(jqXhr.responseText);
                prePopulateOutputs(response);
            }
        }
    });
},1000));

function addPostProcCells() {
    var tHeader = '<th class="spectrum-Table-headCell fm_tdtpostprocessinglogHead">Postprocessing Log</th>',
        pProcCell = '<td class="spectrum-Table-cell fm_tdtpostprocessinglog"></td>',
        $thEnd = $("th.spectrum-Table-headCell.fm_tdtGeneratedIn"),
        $trEnd = $("td.spectrum-Table-cell.fm_tdtGeneratedIn");

    $thEnd.each(function() {
        if (!($(this).next().is('.fm_tdtpostprocessinglogHead'))) {
            $(this).after(tHeader);
        }
    });

    $trEnd.each(function() {
        if (!($(this).next().is('.fm_tdtpostprocessinglog'))) {
            //TODO: Determine why the new cell isn't being added after the
            // first time this is called by the .g.report listener
            $(this).after(pProcCell);
        }
    });
}

(function(){
    'use strict';
    var servletPath = '/bin/pwc-madison/post-processing-failure-log?path=';

    window.rh.model.subscribe(".g.report", function(r) {
        addPostProcCells();
        checkForFailures();
        if(r !== undefined){

            var $rows = $("tr.spectrum-Table-row.output_finished.fm_tdtReviewRow");
            $.each(r.outputs, function(key, item){
                var logPath = item.ditaotLogFile.replace('logs.txt', 'postprocessing-logs.txt'),
                    $postProcLogCell = $rows.eq(key).children(".fm_tdtpostprocessinglog"),
                    attr = $postProcLogCell.attr('ischecked');
                item.postprocessingLogPath = logPath;

                if (typeof attr === 'undefined' || attr === false) {
                    $postProcLogCell.attr("ischecked", "true");
                    $postProcLogCell.on("click",function() {
                        window.open(servletPath + logPath, "_blank");
                    });

                }
            });
        }
    });
}());
