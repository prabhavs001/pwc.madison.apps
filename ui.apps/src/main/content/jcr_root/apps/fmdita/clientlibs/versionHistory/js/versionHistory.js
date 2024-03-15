(function(document, $) {
    var MESSAGE = ".versionHistory_msg";
    var LABEL_EXIST_ERRMSG =
      '<coral-alert class="label_exist_errmsg" variant="error"><coral-alert-header>INFO</coral-alert-header> <coral-alert-content>' +
      Granite.I18n.get(
        "This label has been applied to another version. Please type a unique label."
      ) +
      "</coral-alert-content></coral-alert>";
    var CANNOT_REVERT_ERRMSG =
      '<coral-alert class="cannot_revert_errmsg" variant="error"><coral-alert-header>INFO</coral-alert-header> <coral-alert-content>' +
      Granite.I18n.get(
        "Cannot revert version. File is checked out by another user."
      ) +
      "</coral-alert-content></coral-alert>";
    var LABEL_EXIST_ERRMSG_CLASS = ".label_exist_errmsg";
    var CANNOT_REVERT_ERRMSG_CLASS = ".cannot_revert_errmsg";
    var isPreviewPage = isPreview();
    var AEMVERSION_6_1 = "6.1";
    var AEMVERSION_6_2 = "6.2";
    var AEMVERSION_6_3 = "6.3";
    var warning_msg_1 =
      '<div id="revertVersionWarning" class="coral-Modal revert_version_update">  <div class="coral-Modal-header">' +
      '<i class="coral-Modal-typeIcon coral-Icon coral-Icon--sizeS"></i>' +
      '<h2 class="coral-Modal-title coral-Heading coral-Heading--2">' +
      Granite.I18n.get(" Revert to this Version") +
      "</h2>" +
      '</div>  <div class="coral-Modal-body"><p>' +
      Granite.I18n.get("Reverting to this version will create a new branch") +
      "</p> " +
      "</div>" +
      '<input is="coral-textfield" class="revert_version_comment" placeholder="Enter comments" name="field" value="">' +
      '<label class="coral-Checkbox">' +
      '<input class="coral-Checkbox-input" type="checkbox">' +
      '<span class="coral-Checkbox-checkmark upversion_check"></span>' +
      "</label>" +
      "<span>" +
      Granite.I18n.get("Save the current working copy as a new version") +
      "</span>" +
      ' <div class="coral-Modal-footer">  ' +
      '<button type="button" class="coral-Button coral-Button--primary confirmRevertVersion" >' +
      Granite.I18n.get("Ok") +
      "</button>" +
      '<button type="button" class="coral-Button coral-Button--primary closeRevertVersionWarning">' +
      Granite.I18n.get("Cancel") +
      "</button>" +
      " </div></div>";
  
    var warning_msg_2 =
  
      '<div id="revertVersionWarning" class="coral-Dialog-wrapper revert_version_update" style="position: absolute; left: 50%; top: 50%; margin-top: -82.5px; margin-left: -160px; border: .125rem solid #c8c8c8;"><div class="coral-Dialog-header"><coral-icon class="coral-Icon coral-Dialog-typeIcon coral-Icon--sizeS" icon="" size="S" handle="icon"></coral-icon>' +
      '<div handle="headerContent" class=" coral-Dialog-title coral-Heading coral-Heading--2"><coral-dialog-header>' +
      Granite.I18n.get("Revert to this Version") +
      "</coral-dialog-header></div>" +
      '</div><coral-dialog-content class="coral-Dialog-content">' +
      Granite.I18n.get("Reverting to this version will create a new branch") +
      "</br> </br> </br>" +
      '<input is="coral-textfield" class="revert_version_comment" placeholder="Enter comments" name="field" value="">' +
      '<coral-checkbox class="coral-Checkbox upversion_check">' +
      '<input class="coral-Checkbox-input" type="checkbox" value="upversion">' +
      '<span class="coral-Checkbox-description">' +
      Granite.I18n.get("Save the current working copy as a new version") +
      "</span>" +
      "</coral-checkbox>" +
      "</coral-dialog-content>" +
      '<coral-dialog-footer class="coral-Dialog-footer"><button is="coral-button" variant="primary" coral-close="" class="coral-Button coral-Button--primary confirmRevertVersion" size="M" data-dismiss="modal"><coral-button-label>' +
      Granite.I18n.get("Ok") +
      "</coral-button-label><" +
      "/" +
      ' button><button is="coral-button" variant="primary" coral-close="" class="coral-Button coral-Button--primary closeRevertVersionWarning" size="M" data-dismiss="modal"><coral-button-label>' +
      Granite.I18n.get("Cancel") +
      "</coral-button-label><" +
      "/" +
      " button></coral-dialog-footer></div>";
  
    var warning_msg_3 =
  
      '<div id="revertVersionWarning" class="coral-Dialog-wrapper revert_version_update" style="position: absolute; left: 50%; top: 50%; margin-top: -82.5px; margin-left: -160px;  border: .125rem solid #c8c8c8;"><div class="coral-Dialog-header"><coral-icon class="coral-Icon coral-Dialog-typeIcon coral-Icon--sizeS" icon="" size="S" handle="icon"></coral-icon>' +
      '<div handle="headerContent" class=" coral-Dialog-title coral-Heading coral-Heading--2"><coral-dialog-header>' +
      Granite.I18n.get("Revert to this Version") +
      "</coral-dialog-header></div>" +
      '</div><coral-dialog-content class="coral-Dialog-content">' +
      Granite.I18n.get("Reverting to this version will create a new branch") +
      "</br> </br> </br>" +
      '<input is="coral-textfield" class="revert_version_comment" placeholder="Enter comments" name="field" value="">' +
      '<coral-checkbox class="coral-Form-field upversion_check" name="name" value="checkboxValue" labelledby="label-vertical-checkbox-0">' +
      Granite.I18n.get("Save the current working copy as a new version") +
      "</coral-checkbox>" +
      "</coral-dialog-content>" +
      '<coral-dialog-footer class="coral-Dialog-footer"><button is="coral-button" variant="primary" coral-close="" class="coral-Button coral-Button--primary confirmRevertVersion" size="M" data-dismiss="modal"><coral-button-label>' +
      Granite.I18n.get("Ok") +
      "</coral-button-label><" +
      "/" +
      ' button><button is="coral-button" variant="primary" coral-close="" class="coral-Button coral-Button--primary closeRevertVersionWarning" size="M" data-dismiss="modal"><coral-button-label>' +
      Granite.I18n.get("Cancel") +
      "</coral-button-label><" +
      "/" +
      " button></coral-dialog-footer></div>";
  
    var modalRemoveLabel =
      '<div id="deleteLabelModal" class="coral-Modal">' +
      '<div class="coral-Modal-header">' +
      '<i class="coral-Modal-typeIcon coral-Icon coral-Icon--sizeS"></i>' +
      '<h2 class="coral-Modal-title coral-Heading coral-Heading--2">' +
      Granite.I18n.get("Remove Label") +
      "</h2>" +
      '<button type="button" class="coral-MinimalButton coral-Modal-closeButton" title="Close" data-dismiss="modal">' +
      '<i class="coral-Icon coral-Icon--sizeXS coral-Icon--close coral-MinimalButton-icon "></i>' +
      "</button>" +
      "</div>" +
      '<div class="coral-Modal-body">' +
      "<p>" +
      Granite.I18n.get("Remove Selected Label ?") +
      "</p>" +
      "</div>" +
      '<div class="coral-Modal-footer">' +
      '<button type="button" class="coral-Button" data-dismiss="modal">' +
      Granite.I18n.get("Cancel") +
      "</button>" +
      '<button type="button" class="coral-Button coral-Button--warning coral-Button--primary"  id="btn-bl-remove-confirm" data-dismiss="modal">' +
      Granite.I18n.get("Remove") +
      "</button>" +
      "</div>" +
      "</div>";
  
    (function() {
      var aemVer = getAEMVersion();
      if ($("#revertVersionWarning").length == 0) {
        if (aemVer == AEMVERSION_6_1) {
          $("body").append(warning_msg_1);
        } else if (aemVer == AEMVERSION_6_2) {
          $("body").append(warning_msg_2);
        } else {
          $("body").append(warning_msg_3);
        }
      }
      $("body").append(modalRemoveLabel);
    })();
  
    $("#revertVersionWarning").hide();
  
    function setCreationDetails(item) {
	  var versionCreatedDetails = "";
       if(item.versionCreator && item.versionCreator != item.createdBy){
           versionCreatedDetails = ", Created by "+ item.versionCreator;
       }
        
      if (item.creationTime) {
        try {
          item.creationDetails =
            new Date(item.creationTime).toLocaleString() +
            " Modified by " +
            item.createdBy + versionCreatedDetails;
        } catch (e) {}
      }
    }
  
    var PREVIEW_ERRMSG =
      '<div id="error-alert" class="coral-Alert coral-Alert--error coral-Alert--large"><button type="button" class="coral-MinimalButton coral-Alert-closeButton" title=' +
      Granite.I18n.get("Close") +
      ' data-dismiss="alert"><i class="coral-Icon coral-Icon--sizeXS coral-Icon--close coral-MinimalButton-icon"></i></button> <i class="coral-Alert-typeIcon coral-Icon coral-Icon--sizeS coral-Icon--alert"></i> <strong class="coral-Alert-title"></strong>  <div class="coral-Alert-message"></div> </div> ';
    $(".versionHistory").css("display", "none");
    if (isPreviewPage) {
      var path = getPathPreview();
      var opts = {
        type: "get",
        url: "/bin/referencelistener",
        cache: false,
        data: {
          operation: "baseVersion",
          _charset_: "UTF-8",
          source: path
        }
      };
      $.ajax(opts).done(function(response) {
        var aemVersion = "6.2";
        var title = $(".granite-title div[role='heading']").text();
        if (title.length == 0) {
          aemVersion = "6.1";
        }
        var currentVersion =
          " (" + Granite.I18n.get("Version") + " " + response.versionName + ")";
        var docTitle = "AEM Assets" + " - " + title;
        if (!title.endsWith(currentVersion)) {
          title = title + currentVersion;
        }
        $(".granite-title div[role='heading']").text(title);
        if (aemVersion == "6.1") {
          title = $(".endor-BlackBar-title").text();
          docTitle = "AEM Assets" + " - " + title;
          if (!title.endsWith(currentVersion)) {
            title = title + currentVersion;
          }
          $(".endor-BlackBar-title").text(title);
        }
        rh.model.publish("docTitle", docTitle);
        document.title = docTitle;
        History.options.initialTitle = docTitle;
      });
      $(".versionHistory").css("display", "block");
      getVersionHistory(path);
      setHrefRevertLog(path);
    } else {
      setHrefRevertLog("");
      $("#granite-shell-actionbar").append(PREVIEW_ERRMSG);
      setMessage("Select an item to display its version history");
    }
  
    $(document).on("foundation-selections-change", function(e) {
      setMessage("");
      var isPreviewPage = isPreview();
      $(".versionHistory").css("display", "none");
      var selections = $(
        ".foundation-collection-item.foundation-selections-item"
      );
      if (selections.length == 0) {
        if (isPreviewPage) {
          var path = getPathPreview();
          getVersionHistory(path);
          setHrefRevertLog(path);
        } else {
          setHrefRevertLog("");
          setMessage("Select an item to display its version history");
        }
      } else if (selections.length > 1 && !isPreviewPage) {
        setMessage("Version History is not available for multiple items");
        $(".revert-log-link").hide();
      } else {
        $(".versionHistory").css("display", "block");
        var path = selections.attr("data-foundation-collection-item-id");
        getVersionHistory(path);
        $(".revert-log-link").show();
        setHrefRevertLog(path);
      }
    });
  
    $(document).on("click", ".version_details_section", function(e) {
      $(".version_details_section").removeClass("is-active");
      $(this).addClass("is-active");
      var expanded_version_details = $(this).children(".expand_version_details");
      var expand = !expanded_version_details.hasClass("is-active");
      $(".expand_version_details").removeClass("is-active");
      if (expand || $(e.originalEvent.srcElement).hasClass("version_add_label")) {
        $(this)
          .children(".expand_version_details")
          .addClass("is-active");
      }
    });
  
    $(document).on("keypress", ".version_add_label", function(event) {
      if (event.which == 13) {
        event.preventDefault();
        if (isPreview()) {
          var path = getPathPreview();
        } else {
          var path = getAssetPath();
        }
        var opts = {
          type: "post",
          url: "/bin/referencelistener",
          data: {
            operation: "setlabel",
            _charset_: "UTF-8"
          }
        };
        var version = $(
          ".version_details_section.is-active .version_name"
        ).text();
        opts.data.topiclist = JSON.stringify([
          { path: path, version: version, label: event.target.value }
        ]);
        $.ajax(opts).done(function(response) {
          var versionData = response.allVersionHistoryData[path];
          if (response.labelExistList.length != 0) {
            showErrMsg(LABEL_EXIST_ERRMSG, LABEL_EXIST_ERRMSG_CLASS);
          } else {
            if (versionData) {
              $.each(versionData, function(index, item) {
                setCreationDetails(item);
              });
            }
            rh.model.publish(
              ".g.versionHistory",
              response.allVersionHistoryData[path]
            );
          }
        });
      }
    });
  
    var label_modal = new CUI.Modal({
      element: "#deleteLabelModal",
      visible: false
    });
    $("#btn-bl-remove-confirm").click(function() {
      removeLabel();
    });
    function removeLabel() {
      var label = rh.model.get(".g.versionHistory.removeLabel");
      if (isPreview()) {
        var path = getPathPreview();
      } else {
        var path = getAssetPath();
      }
      var opts = {
        type: "post",
        url: "/bin/referencelistener",
        data: {
          operation: "deletelabel",
          _charset_: "UTF-8"
        }
      };
      opts.data.topiclist = JSON.stringify([{ path: path, label: label }]);
      $.ajax(opts).done(function(response) {
        var versionData = response.allVersionHistoryData[path];
        if (versionData) {
          $.each(versionData, function(index, item) {
            setCreationDetails(item);
          });
        }
  
        rh.model.publish(
          ".g.versionHistory",
          response.allVersionHistoryData[path]
        );
      });
      rh.model.publish(".g.versionHistory.removeLabel", undefined);
    }
    $(document).on("click", ".label_remove", function(e) {
      var label = $(this)
        .parent()
        .children("span")
        .text();
      rh.model.publish(".g.versionHistory.removeLabel", label);
      label_modal.show();
      $(".expand_version_details").toggleClass("is-active");
    });
  
    $(document).on("click", ".closeRevertVersionWarning", function(e) {
      $("#revertVersionWarning").hide();
    });
  
    $(document).on("click", ".upversion_check", function(e) {
      if ($(".upversion_check").hasClass("checked")) {
        $(".upversion_check").removeClass("checked");
      } else {
        $(".upversion_check").addClass("checked");
      }
    });
  
    $(document).on("click", ".confirmRevertVersion", function(e) {
      $("#revertVersionWarning").hide();
      var upVer = $(".upversion_check").hasClass("checked");
      var version = $(".version_details_section.is-active .version_name").text();
      var revertVerComment = $(".revert_version_comment")[0].value;
      if (isPreview()) {
        var path = getPathPreview();
      } else {
        var path = getAssetPath();
      }
      var opts = {
        type: "post",
        url: "/bin/referencelistener",
        data: {
          operation: "restoreVersion",
          _charset_: "UTF-8",
          path: path,
          version: version,
          upVersion: upVer,
          revertVerComment: revertVerComment
        }
      };
      $.ajax(opts).done(function(response) {
        var versionData = response.allVersionHistoryData[path];
        if (versionData) {
          $.each(versionData, function(index, item) {
            setCreationDetails(item);
          });
        }
        rh.model.publish(
          ".g.versionHistory",
          response.allVersionHistoryData[path]
        );
      });
    });
  
    $(document).on("click", ".revert_version", function(e) {
      if (isPreview()) {
        var path = getPathPreview();
      } else {
        var path = getAssetPath();
      }
      fmdita.getLockStatusAll(path, function(data) {
        if (data.hasOwnProperty(path)) {
          var lockProps = data[path];
          if (
            (lockProps.status == "locked" && lockProps.isOwner == "yes") ||
            lockProps.status == "unlocked"
          ) {
            showUpVersionWarning();
          } else {
            showErrMsg(CANNOT_REVERT_ERRMSG, CANNOT_REVERT_ERRMSG_CLASS);
          }
        }
      });
    });
  
    function getVersionHistory(path) {
      var opts = {
        type: "get",
        url: "/bin/referencelistener",
        cache: false,
        data: {
          operation: "getVersionHistory",
          addCurrent:true,
          path: path,
          _charset_: "UTF-8"
        }
      };
      $.ajax(opts).done(function(response) {
        if (response.versionHistoryData.length == 0) {
          setMessage("There are currently no versions");
        } else {
          $(MESSAGE).hide();
          $(".versionHistory").css("display", "block");
          var versionData = response.versionHistoryData;
          if (versionData) {
            $.each(versionData, function(index, item) {
              setCreationDetails(item);
            });
          }
          rh.model.publish(".g.versionHistory", versionData);
        }
      });
    }
  
    function isPreview() {
      var pathname = window.location.pathname;
      return pathname.startsWith("/assetdetails.html");
    }
    function getPathPreview() {
      var pathname = window.location.pathname;
      var index = pathname.indexOf("/assetdetails.html");
      var topicPath = "";
      if (index > -1) {
        var srchpath = "/assetdetails.html";
        var topicPath = decodeURIComponent(
          pathname.substring(index + srchpath.length)
        );
      }
      return topicPath;
    }
  
    function setMessage(msg) {
      $(MESSAGE).show();
      $(".versionHistory").css("display", "none");
      $(MESSAGE).text(Granite.I18n.get(msg));
    }
  
    function showErrMsg(msg, className) {
      if ($(className).length != 0) {
        $(className).show();
      } else {
        if (isPreview()) {
          showErrMsgPreview(msg, className);
        } else {
          showErrMsgAsset(msg, className);
        }
      }
      setTimeout(function() {
        $(className).hide();
      }, 3000);
    }
  
    function showErrMsgPreview(msg, className) {
      if ($(".granite-actionbar").length > 0) {
        $(".granite-actionbar").append(msg); //AEM6.2
      } else if ($("betty-titlebar-primary").length > 0) {
        $("betty-titlebar-primary").append(msg); //AEM6.3
      } else {
        $(".foundation-content-current").append(msg); //AEM6.1
        $(className).prepend(
          '<coral-icon class="coral-Icon coral3-Alert-typeIcon coral-Icon--sizeXS coral-Icon--alert" icon="alert" size="XS" handle="icon" role="img" aria-label="alert"></coral-icon>'
        );
        $(className).css({
          position: "absolute",
          left: "100px",
          top: "50px",
          "z-index": "5",
          "background-color": "#fa7d73",
          "box-sizing": "content-box",
          margin: ".5rem 0",
          border: "1px",
          padding: ".5rem"
        });
      }
    }
  
    function showErrMsgAsset(msg, className) {
      if (
        $("#granite-shell-actionbar").length > 0 &&
        $("#granite-shell-actionbar").find("betty-titlebar-primary").length == 0
      ) {
        $("#granite-shell-actionbar").append(msg); //AEM 6.2
      } else if ($("#granite-shell-actionbar").length == 0) {
        $(".foundation-content-current").append(msg); //AEM6.1
        $(className).prepend(
          '<coral-icon class="coral-Icon coral3-Alert-typeIcon coral-Icon--sizeXS coral-Icon--alert" icon="alert" size="XS" handle="icon" role="img" aria-label="alert"></coral-icon>'
        );
        $(className).css({
          position: "absolute",
          left: "100px",
          top: "50px",
          "z-index": "5",
          "background-color": "#fa7d73",
          "box-sizing": "content-box",
          margin: ".5rem 0",
          border: "1px",
          padding: ".5rem"
        });
      } else {
        $("#granite-shell-actionbar betty-titlebar-primary").append(msg); //AEM6.3
      }
    }
  
    function getAEMVersion() {
      if (isPreview()) {
        if ($("betty-titlebar-primary").length > 0) {
          return AEMVERSION_6_3;
        } else if ($(".granite-actionbar").length > 0) {
          return AEMVERSION_6_2;
        } else {
          return AEMVERSION_6_1;
        }
      } else {
        if (
          $("#granite-shell-actionbar").length > 0 &&
          $("#granite-shell-actionbar").find("betty-titlebar-primary").length > 0
        ) {
          return AEMVERSION_6_3;
        } else if ($("#granite-shell-actionbar").length > 0) {
          return AEMVERSION_6_2;
        } else {
          return AEMVERSION_6_1;
        }
      }
    }
  
    function getAssetPath() {
      var selections = $(
        ".foundation-collection-item.foundation-selections-item"
      );
      var path = selections.attr("data-foundation-collection-item-id");
      return path;
    }
  
    function showUpVersionWarning() {
      $(".revert_version_comment")[0].value = "";
      if ($(".upversion_check").hasClass("checked")) {
        $(".upversion_check").removeClass("checked");
        $(".upversion_check").removeAttr("checked");
      }
      $("#revertVersionWarning").show();
    }
  
    function setHrefRevertLog(path) {
      var href = "/libs/fmdita/report/revertversionhistory.html" + path;
      $(".revert-log-link").attr("href", href);
    }
  })(document, Granite.$);