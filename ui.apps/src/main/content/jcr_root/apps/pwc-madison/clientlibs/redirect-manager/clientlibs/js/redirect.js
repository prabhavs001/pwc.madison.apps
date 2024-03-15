var redirectmanager = function() {

    var defaultOldUrlPattern = /^[0-9a-zA-Z_ \-\/.$]*$/,
        defaultNewUrlPattern = /^[0-9a-zA-Z_ \-\/.:]*$/,
        validateOldUrl, validateNewUrl, filterkeys, enablebtnsearch, showpanel;

    validateOldUrl = function($oldUrl) {
        var urlPattern = $oldUrl.data('urlPattern'),
            errorMsg = null;
        urlPattern = urlPattern && urlPattern !== 'null' ? urlPattern : defaultOldUrlPattern;
        if (!$oldUrl.val().match(urlPattern)) {
            errorMsg = "Invalid Old Url format! Old Url can only have alpha-numeric characters, underscore(_), hyphen(-), forward-slash(/), dot(.) and dollar-sign($).";
        }
        return errorMsg;
    };

    validateNewUrl = function($newUrl) {
        var urlPattern = $newUrl.data('urlPattern'),
            errorMsg = null;
        urlPattern = urlPattern && urlPattern !== 'null' ? urlPattern : defaultNewUrlPattern;
        if (!$newUrl.val().match(urlPattern)) {
            errorMsg = "Invalid New Url format! New Url can only have alpha-numeric characters, underscore(_), hyphen(-), forward-slash(/), dot(.) and colon(:).";
        }
        return errorMsg;
    };

    filterkeys = function(e) {
        if (e.keyCode === 32) { e.preventDefault(); return; }
    };
    enablebtnsearch = function(e) {
        if ($('#searcholdurl').val() === '' || $('#searchstatus').val() === '') {
            $('#btnsearch').attr('disabled', true);
        }
        else {
            $('#btnsearch').removeAttr('disabled');
        }
    };
    $('#searcholdurl').keydown(filterkeys).keyup(enablebtnsearch);
    $('#searchstatus').change(enablebtnsearch);


    $('#btnsearch').click(function() {
        $('#searchresult .row').remove();

        var $oldUrl = $('#searcholdurl'),
            $newUrl = $('#searchnewurl'),
            errorMsg = validateOldUrl($oldUrl) || validateNewUrl($newUrl);
        if (errorMsg) {
            window.alert(errorMsg);
            $('#searchresult').hide();
            return;
        }


        $('#searchresulttotal').text('Search....');
        $('#searchresult').show();

        $.post('/bin/redirectmanager', { actype: 'SEARCH', oldurl: $oldUrl.val(), newurl: $newUrl.val(), status: $('#searchstatus').val() },
            function(data) {
                var i;
                if (data.total > 100) {
                    $('#searchresulttotal').text('More than 100 matches found. Displaying first 100 matches.');
                }
                else if (data.total === 0) {
                    $('#searchresulttotal').text('No match found.');
                }
                else if (data.total === 1) {
                    $('#searchresulttotal').text('Only 1 match found.');
                }
                else if (!data.error) {
                    $('#searchresulttotal').text('Total of ' + data.total + ' matches found.');
                }

                for (i = 0; i < data.rws.length; i++) {
                    $('#searchresult').append(window.DOMPurify.sanitize('<div class="row"><button type="button" class="btnedit" title="Edit"><span class="fa fa-pencil-alt"></span></button>' +
                        '<div class="search-status" status=' + data.rws[i].status + '><strong>Status: </strong>' + (data.rws[i].status === '200' ? 'Internal Redirect' : (data.rws[i].status === '301' ? '301 Moved Permanently' : '302 Found')) + '</div>' +
                        '<div class="search-old-url"><strong>Old URL:  </strong><span>' + data.rws[i].oldurl + '</span></div><div class="search-new-url"><strong>New URL:  </strong><span>' + data.rws[i].newurl + '</span></div></div>'));
                }

                $('#searchresult .btnedit').click(function(e) {
                    $('#actionTitle').text('Edit Redirect');

                    $('#status').val($(this).parent().find('.search-status').attr('status'));
                    $('#oldurl').val($(this).parent().find('.search-old-url span').text()).attr('readonly', true);
                    $('#newurl').val($(this).parent().find('.search-new-url span').text());

                    $('#btndelete').show();
                    $('#btnupdate').show();
                    $('#btnsave').hide();
                    showpanel(e);
                });
            }
        );
    });

    $('#btnnew').click(function(e) {
        $('#actionTitle').text('Add New Redirect');
        $('#btndelete').hide();
        $('#btnupdate').hide();
        $('#btnsave').show();

        $('#oldurl').val('').removeAttr('readonly');
        $('#newurl').val('');
        $('#status').val('');
        showpanel(e);
    });

    showpanel = function(e) {
        var top = e.clientY;
        if (top < 100) {
            top = -150;
        }
        else {
            top = top - 300;
        }
        $('#actionPanel').css('top', top + $(document).scrollTop());
        $('#actionPanel').slideDown();

        $('#newurl').removeAttr('disabled');
        $('#oldurl').removeAttr('disabled');
        $('#status').removeAttr('disabled');
        $('#oldurlmsg').hide().text('');
        $('#newurlmsg').hide().text('');
        $('#statusmsg').hide().text('');
    };


    function saveMapping() {
        $.post("/bin/redirectmanager", { actype: "SAVE", oldurl: $('#oldurl').val(), newurl: $('#newurl').val(), status: $('#status').val() },
            function(data) {
                if (data.saved) {
                    $('#actionPanel').hide();
                    if (!$('#btnsearch').attr('disabled')) {
                        $('#btnsearch').click();
                    }
                } else {
                    window.alert('Cannot save it. Please try again. \nError: ' + data.error);
                }
            }
        );
    }

    function saveOrUpdateMapping(confirmOnUpdate) {
        var pass = true,
            oldUrl = $('#oldurl').val(),
            newUrl = $('#newurl').val(),
            oldUrlError, newUrlError;
        $('#oldurlmsg').hide().text('');
        $('#newurlmsg').hide().text('');
        $('#statusmsg').hide().text('');

        if (oldUrl.trim().length === 0) {
            $('#oldurlmsg').show().text('Old Url is required.');
            pass = false;
        } else {
            oldUrlError = validateOldUrl($('#oldurl'));
            if (oldUrlError) {
                $('#oldurlmsg').show().text(oldUrlError);
                pass = false;
            }
        }

        if (newUrl.trim().length === 0) {
            $('#newurlmsg').show().text('New Url is required.');
            pass = false;
        } else {
            newUrlError = validateNewUrl($('#newurl'));
            if (newUrlError) {
                $('#newurlmsg').show().text(newUrlError);
                pass = false;
            }
        }

        if ($('#status').val().length === 0) {
            $('#statusmsg').show().text('Redirect Status is required.');
            pass = false;
        }

        if (!pass) {
            return;
        }

        if(confirmOnUpdate) {
            $.post("/bin/redirectmanager", { actype: "CHECK", oldurl: $('#oldurl').val(), newurl: $('#newurl').val(), status: $('#status').val() },
                function(data) {
                    if (data.exists) {
                        if (window.confirm('Old URL already exists. Are you sure want to update it?')) {
                            saveMapping();
                        }
                    } else {
                        saveMapping();
                    }
                }
            );
        } else {
            saveMapping();
        }
    }

    $('#btnsave').click(function() {
        saveOrUpdateMapping(true);
    });

    $('#btnupdate').click(function() {
        saveOrUpdateMapping(false);
    });

    $('#btndelete').click(function() {
        var oldUrl, oldUrlError;
        if (window.confirm('Are you sure want to delete it?')) {

            oldUrl = $('#oldurl').val();

            if (oldUrl.trim().length === 0) {
                window.alert('Old Url is required.');
                return;
            } else {
                oldUrlError = validateOldUrl($('#oldurl'));
                if (oldUrlError) {
                    window.alert(oldUrlError);
                    return;
                }
            }
            $.post("/bin/redirectmanager", { actype: "DELETE", oldurl: $('#oldurl').val() },
                function(data) {
                    if (data.deleted) {
                        $('#actionPanel').hide();
                        if (!$('#btnsearch').attr('disabled')) {
                            $('#btnsearch').click();
                        }
                    } else {
                        window.alert('Cannot delete it. Please refresh the page and try again.\nError: ' + data.error);
                    }
                });
        }
    });
    $('#btncancel').click(function() {
        $('#actionPanel').hide();
    });

};
