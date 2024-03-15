(function($, undefined) {
    "use strict";

    window.fmdita = window.fmdita || {};
    var createWizardRel = ".apps-dita-publish-createreview";
    var collaboratorsList = [],validateAll = null , TITLE_XPATH = '[name="title"]', allTopicsList = [];
    var EXCLUDE_TOPIC_CHECKBOX = ".ditmap-excludetopics";

    var TOPICS_SELECTALL_SELECTOR = '#topics-checkAll';
    var TOPICS_CHECKBOX_SELECTOR = 'input[name="review-topics"]';
    var SELECTED_TOPICS_SELECTOR = TOPICS_CHECKBOX_SELECTOR + ':checked';
    var REVIEW_PAYLOAD_KEY = "fmdita.review_topics_data";

    var ui = $(window).adaptTo("foundation-ui");

    var payload = window.fmdita.payload = window.fmdita.payload || JSON.parse(decodeURIComponent(sessionStorage.getItem(REVIEW_PAYLOAD_KEY)));
    sessionStorage.removeItem(REVIEW_PAYLOAD_KEY);
    var payloadExists = payload && Array.isArray(payload.asset) && (payload.asset.length > 0)
    var isDitamap = payloadExists && payload.asset[0].endsWith(".ditamap");
    var selectedAssets = payloadExists && payload.asset;

    // Handle the add task back button
    $(document).fipo("tap.foundation-wizard-control", "click.foundation-wizard-control", ".foundation-wizard-control", function(e) {
        if ($(this).data("foundation-wizard-control-action") != "cancel") return;

        var ref = $(".urlParameters").data("ref");
        if (!ref || ref.length == 0) return;

        e.preventDefault();
        window.location = Granite.HTTP.externalize(ref);
    });

    function doSubmit(form) {

        var targetUrl = "/bin/pwc-madison/createcollaboration";
        if(!targetUrl){
            var ui = $(window).adaptTo("foundation-ui");
            ui.alert(Granite.I18n.get("Incomplete Form"),
                Granite.I18n.get("Enter a valid project name"),
                "error");
            return;
        }

        if (!payload)
        {
            var ui = $(window).adaptTo("foundation-ui");
            ui.alert(Granite.I18n.get("Incomplete Form"),
                Granite.I18n.get("Select a content payload to start collaboration."),
                "error");
            return;
        } else {
            $("input[name='contentPath']").val(JSON.stringify(payload));
        }

        var dueDate = $($("input[name=taskDueDate]")[0]).val();
        if (dueDate == null || dueDate.length == 0)
        {
            var ui = $(window).adaptTo("foundation-ui");
            ui.alert(Granite.I18n.get("Incomplete Form"),
                Granite.I18n.get("Select due date for review."),
                "error");
            return;
        }

        if(collaboratorsList.length == 0){
            var ui = $(window).adaptTo("foundation-ui");
            ui.alert(Granite.I18n.get("Incomplete Form"),
                Granite.I18n.get("Add a collaborator."),
                "error");
            return;
        }

        if(isDitamap && $(SELECTED_TOPICS_SELECTOR).length == 0) {
            var ui = $(window).adaptTo("foundation-ui");
            ui.alert(Granite.I18n.get("Incomplete Form"),
                Granite.I18n.get("Select topic(s) OR ditamap(s) for collaboration."),
                "error");
            return;
        }

        if(payload.excludeTopics && $(EXCLUDE_TOPIC_CHECKBOX).val() != "on"){
            var ui = $(window).adaptTo("foundation-ui");
            ui.alert(Granite.I18n.get("Incomplete Form"),
                Granite.I18n.get("Some of the Ditamap's components are already under review.Please exclude the topics for review."),
                "error");
            return;
        }
        var referrer = payload.referrer;


        var data = [];
        setDataValues(form, data);
        data.push({name: 'collaborators', value: collaboratorsList});

        var revTopics = [], selectedTopics = [];;
        if(selectedAssets.length == 1){
        /* Handle single selection */
            var selectedAsset = selectedAssets[0];
            if(selectedAsset.endsWith(".dita")){
                revTopics.push(selectedAsset);
            }else if(selectedAsset.endsWith(".ditamap")){
                //this add all the topics to the revTopics list
                $(TOPICS_CHECKBOX_SELECTOR).each(function(i,v){
                        revTopics.push(v.value);
                });
                //this add only the selected topics to the selectedTopics list
                $(SELECTED_TOPICS_SELECTOR).each(function(i,v){
                    selectedTopics.push(v.value);
                });
            }
        }else if(selectedAssets.length > 1){
        /*  Handle multiple topic selection */
            $.each(selectedAssets, function(index, item){
                if(item.endsWith(".dita")){
                    revTopics.push(item);
                }
            });
        }

        if(revTopics.length > 0) {
            data.push({name: 'review-topics', value: revTopics.join("|")});
        }
        if(selectedTopics.length > 0) {
            data.push({name: 'selectedTopics', value: selectedTopics.join("|")});
        }
        data.push({name:'sendEmailNotification', value: 'true'});//always send email
        data.push({name:'allowAllReviewers', value: 'true'});
        data.push({name:'reviewVersion', value: '2.0'});
        var reviewerList = [];
        collaboratorsList.forEach(function(item){
            reviewerList.push(item);
        });
        var versionError = false;
        $.post("/bin/publishlistener", {
            ':operation': "visit",
            traverse: 'list',
            visitor: 'version',
            paths: allTopicsList.map(e=>e.path).join("|"),
            "_charset_": "UTF-8"
        }, null, 'json').then(function(resp) {
            var versionJson = [];
            resp.forEach(function(topicsList, index) {
                var topic = allTopicsList.find(item=>item.path===topicsList.path);
                var versions = topicsList.versions;
                if (versions.length > 0) {
                    topic.version = versions[versions.length - 1].name;
                }else{
                    versionError = true;
                }
                if(referencedDitamaps.length>0){
                    topic.review = true;
                }else{
                    topic.review = selectedTopics.indexOf(topic.path)>=0;
                }
                topic.reviewers = reviewerList;
                versionJson.push(topic);
            });
            if(versionError){
                var ui = $(window).adaptTo("foundation-ui");
                var  message = Granite.I18n.get("No version exists for the selected topic(s). Please go back and create version for the topic(s) to create a review task.");
                var actions = [{text: Granite.I18n.get("OK"),id: "ok"}];
                ui.prompt(Granite.I18n.get("No Version Created"), message, "error", actions, handleVersionAction());
                return false;
            }else{
                getMapHierarchy(payload.asset[0]).then(mapHierarchy => {
                    data.push({name:'ditamapHierarchy', value: JSON.stringify(mapHierarchy)});
                    data.push({
                        name: 'versions',
                        value: JSON.stringify(allTopicsList)
                    });
                    var ajaxOptions = {
                        type: "post",
                        data: $.param(data),
                        url: targetUrl
                    };

                    var jqxhr = $.ajax(ajaxOptions);
                    jqxhr.done(function(html) {
                        var title = $('[name="title"]').val();
                        var modal = CQ.projects.modal.successTemplate.clone();
                        modal.find(".coral-Modal-header h2").html(Granite.I18n.get("Success"));
                        var strLocalize = Granite.I18n.get("Workflow '{}' has been created.");
                        strLocalize = strLocalize.replace("{}", title);
                        modal.find(".coral-Modal-body").html(strLocalize);

                        var footer = modal.find(".coral-Modal-footer");

                        $('<a class="coral-Button coral-Button--primary"></a>')
                            .prop("href", referrer)
                            .text(Granite.I18n.get("Close"))
                            .appendTo(footer);

                        modal.appendTo("body").modal("show");
                    });
                    jqxhr.fail(function(xhr, error, errorThrown) {
                        var ui = $(window).adaptTo("foundation-ui");
                        ui.alert(Granite.I18n.get("Error"), Granite.I18n.get("Failed to start collaboration."), "error");
                        //window.location.href = document.referrer;
                    });
                });
            }
        });
    }

    var clearAllUsers = null;

    function initValidation() {
        var DISABLE_CLASS = "button-disabled";
        var titleUI = $(".review-title"), projectUI = $(".review-project-path");
        var create = $(".create-review-button"),dueDateUI = $(".review-due-date");;
        create.addClass(DISABLE_CLASS);
        validateAll = function() {
            var bValid = true, dueDate = moment($('[name="taskDueDate"]').val());
            var title = titleUI.val();
            if(title == null || title == '')
                bValid = false;
            if(collaboratorsList.length == 0)
                bValid = false;
            if(isDitamap && $(SELECTED_TOPICS_SELECTOR).length == 0)
                bValid = false;
            if(!dueDate.isValid())
                bValid = false;
            bValid? create.removeClass(DISABLE_CLASS):create.addClass(DISABLE_CLASS);
        };
        titleUI.keyup(validateAll);
        var subs = false;
        projectUI.click(function(){
            if(!subs)
                projectUI.find(".coral-SelectList-item").click(function() {
                    if(clearAllUsers) clearAllUsers();
                    rh._.defer(validateAll);
                });
            subs = true;
        });
        
        $(document).on('change', TOPICS_CHECKBOX_SELECTOR + ", " + TOPICS_SELECTALL_SELECTOR, validateAll);
        dueDateUI.change(validateAll);
        return validateAll;
    };

    function handleVersionAction(){
    }

    function setDataValues(form, data){
        data.push({name: 'title', value: $(form).find("input[name='title']").val()});
        data.push({name: 'description', value: $(form).find("textarea[name='description']").val()});
        data.push({name: 'contentPath', value: $(form).find("input[name='contentPath']").val()});
        data.push({name: 'taskDueDate', value: $(form).find("input[name='taskDueDate']").val()});
        data.push({name: 'projectPath', value: $(form).find("input[name='projectPath']").val()});
    }

    var checkCollaboratorChange = (function(){
        var bInitCollaborator = false, tagList = null, TAGLISTID = 'approverTagList';
        var userList = null;

            return function() {
                var NULLVAL = "null";
                var assigneeField = $("input[name=collaborators]");
                var assignee = assigneeField.val();

                if(!userList)
                    userList = $('.collaboratorsList');

                if(userList.find('[data-value]').length > 0)
                    for(var i=0;i<collaboratorsList.length;i++){
                        userList.find('[data-value="'+collaboratorsList[i]+'"]').hide();
                    }

                if (assignee != null && assignee.length != 0 && assignee != NULLVAL)
                {
                    if(collaboratorsList.indexOf(assignee) != -1){
                        assigneeField.val("");
                        assigneeField.prev().val("");
                        return;
                    }
                    var USER_TAGLIST_TEMPLATE = '<ol class="coral-TagList" role="list" id="'+ TAGLISTID +'"  ></ol>';
                    var APPROVER_BUTTON_PARENT="#aem-asset-reviewtask-approvers";
                    var displayName = assigneeField.prev().val();
                    assigneeField.val("");
                    assigneeField.prev().val("");
                    if(!bInitCollaborator) {
                        bInitCollaborator = true;
                        $(APPROVER_BUTTON_PARENT).append($.parseHTML(USER_TAGLIST_TEMPLATE));
                         tagList = new CUI.TagList({ element:'#' +TAGLISTID, values:[] });
                        $('#'+ TAGLISTID).on('itemremoved', function(event, item) {
                            collaboratorsList = jQuery.grep(collaboratorsList, function(value) {
                              return value != item.value;
                            });
                            validateAll();
                        })
                    }
                    tagList.addItem( {'display': displayName, 'value': assignee} );
                    collaboratorsList.push(assignee);
                    if(!clearAllUsers){
                        clearAllUsers = function() {
                            collaboratorsList.forEach(function(item){
                                tagList.removeItem(item);
                            });
                            collaboratorsList = [];
                        };
                    }
                    validateAll();
                }
            }
    })();


    $(document).on("foundation-contentloaded" + createWizardRel, function(e) {
        if(!payloadExists) {
            ui.alert(Granite.I18n.get("Payload not found"),
                "Payload value: " + JSON.stringify(payload) + ". Please go back and select file for review",
                "error");
        }
        setRootMapPath(payload.asset[0]);
        showOrHideDitamapCheck();
        initValidation();
        var $form = $("form" + createWizardRel);
        setInterval(function() {
            checkCollaboratorChange();
        }, 100);
        $form.submit(function(e) {
            e.preventDefault();
            var DISABLE_CLASS = "button-disabled";
            var createButton = $(".create-review-button");
            if(createButton.hasClass(DISABLE_CLASS)) {
                return false;
            }
            const isValueExists = (key, value) => {
              return allTopicsList.some(obj => obj[key] === value);
            };
            $(TOPICS_CHECKBOX_SELECTOR).each(function(item){
                if(!isValueExists("path", $(this).attr('value'))){
	                var topic = {};
    	            topic.path = $(this).attr('value');
        	        allTopicsList.push(topic);
                }
            });
            doSubmit($form);
        });
        $(TITLE_XPATH).focus();
    });

    function setRootMapPath(path) {
        $('[name="root_map_path"]').val(path)
    }

    function showOrHideDitamapCheck() {
        var $form = $("form" + createWizardRel);
        $form.find(EXCLUDE_TOPIC_CHECKBOX).closest('.coral-Form-fieldwrapper').hide();
        return; //hide the Ditamap Checkbox right now

        if (isDitamap) {
            $form.find(EXCLUDE_TOPIC_CHECKBOX).closest('.coral-Form-fieldwrapper').show();
        }
    }

})(Granite.$);
