/*global fmxml */
function generateRCL() {
    var dxml = window.dxml,
        currentPath = dxml.curEditor.filePath;

    $.ajax({
        type: 'GET',
        url: '/bin/pwc-madison/auto-generate-rcl?basePath=' + currentPath,
        success: function() {
            location.reload();
        },
        error: function() {
            dxml.appGet('util').showError('Copy Links to RCL', 'RCL copy failed');
        }
    });
}

function confirmAndGenerateRCL() {
    var dxml = window.dxml;
    dxml.eventHandler.next(dxml.eventHandler.KEYS.APP_SHOW_DIALOG, {
        id: 'prompt',
        onSuccess: generateRCL,
        inputModel: {
            label: 'Copy Links to RCL',
            message: 'Prior to performing this action, make sure you have saved your changes in the editor. Any unsaved changes will be lost in the editor. If you have not saved your work, please cancel and save the document before performing this action. If you have already saved, click to proceed',
            doneLabel: 'Proceed',
            cancelLabel: 'Cancel'
        }
    });
}

window.dxml.ready(function() {
    //Subscribe on APP_GENERATE_RCL
    window.dxml.eventHandler.subscribe({key: 'APP_GENERATE_RCL', next: confirmAndGenerateRCL});
});
