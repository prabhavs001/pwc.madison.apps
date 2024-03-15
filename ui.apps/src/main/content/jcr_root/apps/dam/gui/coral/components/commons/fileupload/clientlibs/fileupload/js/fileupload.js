(function(document, $, UNorm) {
    "use strict";

    var uploadDialogEl;
    var currentUserId = getCurrentUserId();
    var ICON_PAUSE_SELECTOR = ".dam-asset-upload-list-item-pause-icon";
    var ICON_PLAY_SELECTOR = ".dam-asset-upload-list-item-play-icon";
    // @coral targeting inner component of coral-progress to be able to apply error styles
    var UPLOAD_PROGRESS_SELECTOR = "coral-progress div[handle='status']";
    var FileRejectionType = {
        FILE_SIZE_EXCEEDED: function(fileUpload) {
            return Granite.I18n.get("Following file(s) are too large. Only files smaller than {0} are allowed.",
                formatBytes(fileUpload.sizeLimit));
        },

        MIME_TYPE_REJECTED: function(fileUpload) {
            return Granite.I18n.get("Following file(s) are of file type(s) which are restricted.");
        }
    };

    var XHR_EVENT_NAMES = [
        "loadstart",
        "progress",
        "load",
        "error",
        "loadend",
        "readystatechange",
        "abort",
        "timeout"
    ];

    // used to drive nui uploads
    var workers = [];
    var objects;
    var objIndex = 0;
    var sliceCounter = 0;
    var uplCounter = new Map();
    var MAX_WORKERS = 6;

    function getCurrentUserId() {
        var currentUserId = "";
        var currentUserInfo;
        var CURRENT_USER_JSON_PATH = Granite.HTTP.externalize("/libs/granite/security/currentuser.json");
        var result = Granite.$.ajax({
            type: "GET",
            async: false,
            url: CURRENT_USER_JSON_PATH
        });
        if (result.status === 200) {
            currentUserInfo = JSON.parse(result.responseText);
            currentUserId = currentUserInfo.authorizableId;
        }
        return currentUserId;
    }

    function isColumnView() {
        var layoutData = JSON.parse($(".cq-damadmin-admin-childpages.foundation-collection")
            .attr("data-foundation-layout"));
        if (layoutData["name"] === "foundation-layout-columnview") {
            return true;
        } else {
            return false;
        }
    }

    function getCheckedOutBy(assetPath) {
        var checkedOutBy = "";
        var checkedOutInfo;
        var CHECKED_OUT_INFO_PATH = Granite.HTTP.externalize(assetPath + ".companioninfo.json");
        var result = Granite.$.ajax({
            type: "GET",
            async: false,
            url: CHECKED_OUT_INFO_PATH
        });
        if (result.status === 200) {
            checkedOutInfo = JSON.parse(result.responseText);
            checkedOutBy = checkedOutInfo.checkedOutBy;
        }
        return checkedOutBy;
    }

    function formatBytes(size) {
        var oneKB = 1024;
        var oneMB = 1048576;
        var oneGB = 1073741824;
        if (size < oneKB) {
            return Granite.I18n.get("{0} Bytes", size, "0 replaced by number");
        } else if (size < oneMB) {
            return Granite.I18n.get("{0} KB", parseFloat((size / oneKB).toFixed(3)), "0 replaced by number");
        } else if (size < oneGB) {
            return Granite.I18n.get("{0} MB", parseFloat((size / oneMB).toFixed(3)), "0 replaced by number");
        } else {
            return Granite.I18n.get("{0} GB", parseFloat((size / oneGB).toFixed(3)), "0 replaced by number");
        }
    }

    function isAllowedToAddAssets() {
        var isAllowed = true;
        var metaRel = $(".cq-damadmin-admin-childpages .foundation-collection-meta").last()
            .data("foundationCollectionMetaRel");
        if (metaRel && metaRel.indexOf("cq-damadmin-admin-createasset") === -1) {
            isAllowed = false;
        }
        return isAllowed;
    }

    var DamFileUpload = new Class({
        fileUpload: null,
        uploadDialog: null,
        contentPath: "",
        contentType: "",
        directUpload: false,
        _ILLEGAL_FILENAME_CHARS: [ '*', '/', ':', '[', '\\', ']', '|', '#', '%', '{', '}', '?', '&', '@', '!', '\'', '(', ')', '+', ';', '<', '>', '=', '^', '`', '~', '$', '£', ' ' ],

        set: function(prop, value) {
            this[prop] = value;
            return this;
        },

        initialize: function() {
            var dragCounter = 0;

            var self = this;

            // Create an upload dialog.
            self._createUploadDialog();

            document.body.appendChild(self.uploadDialog);

            var $foundationPath = $(".foundation-content-path");

            // Content path
            self.contentPath = self._getContentPath();
            // Content type: Folder or asset
            self.contentType = $foundationPath.data("foundationContentType");

            // Set the action attribute/
            self.fileUpload.setAttribute("action", self._getActionPath(self.contentPath));

            // Asynchronous upload by default
            self.fileUpload.setAttribute("async", true);

            // Container's Layout
            self.fileUpload.layoutId = $(".cq-damadmin-admin-childpages.foundation-collection")
                .data("foundationLayout").layoutId;

            self.duplicateAssets = [];

            self.restrictedFiles = [];

            self.forbiddenFiles = [];


            self.manualPause = false;
            self.dragDropEnabled = false;
            // by default for multiple files
            self.multiple = true;

            if (!self.fileUpload) {
                return;
            }

            function handleDrop(event) {
                dragCounter = 0;
                self.dragDropEnabled = true;
                event.preventDefault();
                if (isAllowedToAddAssets()) {
                    // Adding tracking data
                    var trackData = {
                        feature: "aem:assets:asset:upload"
                    };
                    $(window).adaptTo("foundation-tracker").trackEvent(trackData);
                    self._dropZoneDrop(event);
                    self.directUpload = true;
                    // workaround until Dam.ChunkFileUpload adds an API to add Files programmatically
                    self.fileUpload._onInputChange(event);
                }
            }

            function handleDragLeave(event) {
                dragCounter--;
                if (dragCounter === 0) {
                    self._dropZoneDragLeave(event);
                }
            }

            function handleDragEnter(event) {
                // Preventing dragover is also required to prevent the browser of loading the file
                event.preventDefault();
                dragCounter++;
                if (isAllowedToAddAssets()) {
                    self._dropZoneDragEnter(event);
                }
            }

            // Add the file-added event listener
            Coral.commons.ready(self.fileUpload, function() {
                var fileRejectionMessageFunction = null;

                self.fileUpload
                    .on("click", function() {
                        var parentAPI = $(self.fileUpload)
                            .closest(".foundation-toggleable")
                            .adaptTo("foundation-toggleable");
                        if (parentAPI) {
                            parentAPI.hide();
                        }
                    })
                    .off("dam-fileupload:fileadded")
                    .on("dam-fileupload:fileadded", function(event) {
                        // Add file name parameter out of the file
                        event.detail.item.name = event.detail.item.file.name;
                    })
                    .off("dam-fileupload:filesizeexceeded")
                    .on("dam-fileupload:filesizeexceeded", function(event) {
                        // Add to rejected list if filesizeexceeded
                        self.fileUpload.rejectedFiles = self.fileUpload.rejectedFiles || [];
                        self.fileUpload.rejectedFiles.push(event.detail.item);
                        fileRejectionMessageFunction = FileRejectionType.FILE_SIZE_EXCEEDED;
                    })
                    .off("dam-fileupload:filemimetyperejected")
                    .on("dam-fileupload:filemimetyperejected", function(event) {
                        // Add to rejected list if filemimetyperejected
                        self.fileUpload.rejectedFiles = self.fileUpload.rejectedFiles || [];
                        self.fileUpload.rejectedFiles.push(event.detail.item);
                        fileRejectionMessageFunction = FileRejectionType.MIME_TYPE_REJECTED;
                    })
                    .on("change", function(event) {
                        if (self.fileUpload.uploadQueue && self.fileUpload.uploadQueue.length) {
                            if (self.directUpload === true && self._allFilesHasValidNames()) {
                                self._submit();
                            } else {
                                self._confirmUpload();
                            }
                            self.directUpload = false;
                        }
                        if (self.fileUpload.rejectedFiles && self.fileUpload.rejectedFiles.length) {
                            self._showRejectedFiles(fileRejectionMessageFunction);
                        }
                    })
                    .on("dam-fileupload:fileremoved", function(event) {
                        event.detail.item.listDialogEL.parentNode.removeChild(event.detail.item.listDialogEL);
                    })
                    .off("dam-fileupload:loadend")
                    .on("dam-fileupload:loadend", function(event) {
                        self._fileUploaded(event);
                    })
                    .off("dam-fileupload:progress")
                    .on("dam-fileupload:progress", function(event) {
                        self._onUploadProgress(event);
                    })
                    .off("dam-fileupload:load")
                    .on("dam-fileupload:load", function(event) {
                        self._onFileLoaded(event);
                    })
                    .off("dam-fileupload:error")
                    .on("dam-fileupload:error", function(event) {
                        self._fileUploadedStatus(event);
                    })
                    .off("dam-fileupload:abort")
                    .on("dam-fileupload:abort", function(event) {
                        self._fileUploadCanceled(event);
                    })
                    .off("dam-chunkfileupload:loadstart")
                    .on("dam-chunkfileupload:loadstart", function(event) {
                        self._onChunkUpoadStart(event);
                    })
                    .off("dam-chunkfileupload:progress")
                    .on("dam-chunkfileupload:progress", function(event) {
                        self._onChunkLoaded(event);
                    })
                    .off("dam-chunkfileupload:loadend")
                    .on("dam-chunkfileupload:loadend", function(event) {
                        self._onFileLoaded(event);
                        self._refresh(event);
                    })
                    .off("dam-chunkfileupload:cancel")
                    .on("dam-chunkfileupload:cancel", function(event) {
                        self._fileChunkedUploadCanceled(event);
                    })
                    .off("dam-chunkfileupload:pause")
                    .on("dam-chunkfileupload:pause", function(event) {
                        self._pauseChunkUpload(event);
                    })
                    .off("dam-chunkfileupload:resume")
                    .on("dam-chunkfileupload:resume", function(event) {
                        self._queryChunkUploadStatus(event);
                    })
                    .off("dam-chunkfileupload:error")
                    .on("dam-chunkfileupload:error", function(event) {
                        self._fileChunkedUploadError(event);
                    })
                    .off("dam-chunkfileupload:timeout")
                    .on("dam-chunkfileupload:timeout", function(event) {
                        self._fileChunkedUploadTimeout(event);
                    })
                    .off("dam-chunkfileupload:querysuccess")
                    .on("dam-chunkfileupload:querysuccess", function(event) {
                        self._fileChunkedUploadQuerySuccess(event);
                    })
                    .off("dam-chunkfileupload:queryerror")
                    .on("dam-chunkfileupload:queryerror", function(event) {
                        self._fileChunkedUploadQueryError(event);
                    });
                if (self.fileUpload.dragDropSupported === true) {
                    $("coral-shell-content")[0].addEventListener("drop", handleDrop, false);

                    $("coral-shell-content")[0].addEventListener("dragover", function(event) {
                        // Preventing dragover is also required to prevent the browser of loading the file
                        event.preventDefault();
                    }, false);

                    $("coral-shell-content")[0].addEventListener("dragleave", handleDragLeave, false);

                    $("coral-shell-content")[0].addEventListener("dragenter", handleDragEnter, false);
                }
            });
        },
        _onUploadProgress: function(event) {
            var el = event.detail.item.element;
            var progressBar = $("coral-progress", el)[0];
            progressBar.value = Math.round((event.detail.loaded / event.detail.total) * 70);
        },
        _onFileLoaded: function(event) {
            var el = event.detail.item.element;
            var progressBar = $("coral-progress", el)[0];
            if (progressBar) {
                // FixMe: There is lag between progress bar status & refresh action, hence not removing the progress bar
                progressBar.value = 100;
                $("coral-icon", el).each(function(cnt, item) {
                    if (!item.classList.contains("coral-Icon--file")) {
                        $(item).remove();
                    }
                });
            }
            $(event.detail.item.listDialogEL).remove();
        },
        _fileUploaded: function(event) {
            var self = this;
            self._fileUploadedStatus(event);
        },

        _onError: function(event) {
            var self = this;

            var $el = $(document.getElementById("uploadRow_" + _g.XSS.getXSSValue(event.detail.item.name)));
            $(ICON_PAUSE_SELECTOR, $el).hide();
            $(UPLOAD_PROGRESS_SELECTOR, $el).addClass("progress-status-error");

            window.damUploadedFilesErrorCount = window.damUploadedFilesErrorCount || 0;
            window.damUploadedFilesErrorCount++;
            event.detail.item.isFailed = true;
            $(".dam-asset-upload-list-dialog-fileCount-failed", self.uploadDialog).show();
            $(".dam-asset-upload-list-dialog-fileCount-failed", self.uploadDialog)[0].innerText =
                window.damUploadedFilesErrorCount +
                Granite.I18n.get(" of ") +
                window.damTotalFilesForUpload +
                Granite.I18n.get(" assets failed");
            if (window.damUploadedFilesErrorCount + window.damUploadedFilesCount === window.damTotalFilesForUpload) {
                // upload is finished, change cancel button to OK button
                $(".dam-asset-upload-cancel-button", self.uploadDialog).text("OK");
            }
        },
        _allFilesHasValidNames: function() {
            var files = this.fileUpload.uploadQueue;
            if (files && files.length) {
                for (var i = 0; i < files.length; i++) {
                    //AEM guides: using 'name' instead of 'file.name'
                    //'file.name' contains original file name
                    if (!this.utils.validFileName(files[i].name, this._ILLEGAL_FILENAME_CHARS)) {
                        return false;
                    }
                }
                return true;
            }
            return false;
        },
        _detectDuplicateAssets: function(event) {
            var self = this;
            if (self.duplicateAssets.length > 0) {
                var firstDuplicate = self.duplicateAssets[0];
                var uploadedAsset = "<b>" + _g.XSS.getXSSValue(firstDuplicate[0]) + "</b>";
                var duplicatedBy = firstDuplicate[1].split(":");
                var duplicatedByMess = "<br><br>";

                for (var i = 0; i < duplicatedBy.length; i++) {
                    duplicatedByMess += _g.XSS.getXSSValue(duplicatedBy[i]) + "<br>";
                }

                var assetExistsMessage = Granite.I18n
                    .get(
                        "The asset uploaded {0} already exists in these location(s):",
                        uploadedAsset);
                assetExistsMessage += duplicatedByMess;
                var description = Granite.I18n
                    .get("Click 'Keep It' to keep or 'Delete' to delete the uploaded asset.");

                var applyToAllMessage = Granite.I18n.get("Apply to all");
                var content = assetExistsMessage + "<br><br>" + description;
                var applyToAll = '<coral-checkbox id="applyToAll">Apply to all</coral-checkbox>'; +applyToAllMessage;
                content = "<p>" + content + "</p>";
                if (self.duplicateAssets.length > 1) {
                    content += applyToAll;
                }
                var dialog = new Coral.Dialog().set({
                    id: "buyDialog",
                    header: {
                        innerHTML: Granite.I18n.get("Duplicate Asset Detected !")
                    },
                    backdrop: Coral.Dialog.backdrop.STATIC,
                    content: {
                        innerHTML: content
                    }
                });

                // Keep Button
                var keepButton = new Coral.Button().set({
                    variant: "default",
                    innerText: Granite.I18n.get("Keep")
                });
                keepButton.label.textContent = Granite.I18n.get("Keep");
                dialog.footer.appendChild(keepButton);
                keepButton.on("click", function() {
                    if (($(this.parentElement.previousElementSibling).find("#applyToAll").length &&
                        $(this.parentElement.previousElementSibling).find("#applyToAll")[0].checked) ||
                        self.duplicateAssets.length === 1) {
                        var dupPaths = [];
                        for (var i = 0; i < self.duplicateAssets.length; i++) {
                            dupPaths.push(self.duplicateAssets[i][0]);
                        }
                        self._initiateWorkflow(dupPaths);
                        self._cleanupAfterDeletingDuplicates();
                    } else {
                        // remove duplicates[0]
                        var duplicateAssets = self.duplicateAssets.splice(0, 1);
                        self._initiateWorkflow([ duplicateAssets[0][0] ]);
                        self._detectDuplicateAssets(event);
                        // self._refresh(event);
                    }
                    dialog.hide();
                    // self._pageRefresh(event);
                });

                // Delete Button
                var deleteButton = new Coral.Button().set({
                    variant: "primary"
                });
                deleteButton.label.textContent = Granite.I18n.get("Delete");
                dialog.footer.appendChild(deleteButton);
                deleteButton.on("click", function() {
                    var a = [];
                    if (($(this.parentElement.previousElementSibling).find("#applyToAll").length &&
                        $(this.parentElement.previousElementSibling).find("#applyToAll")[0].checked) ||
                        self.duplicateAssets.length === 1) {
                        for (var i = 0; i < self.duplicateAssets.length; i++) {
                            a[i] = self.duplicateAssets[i][0];
                        }
                        self._deleteAssetsAfterUpload(a);
                        self._cleanupAfterDeletingDuplicates();
                        // self._refresh(event);
                    } else {
                        a[0] = self.duplicateAssets[0][0];
                        self._deleteAssetsAfterUpload(a);
                        // remove duplicates[0]
                        self.duplicateAssets.splice(0, 1);
                        self._detectDuplicateAssets(event);
                    }
                    dialog.hide();
                    // self._pageRefresh(event);
                });

                document.body.appendChild(dialog);
                dialog.show();
            }
        },
        _showRestrictedFiles: function(event) {
            var self = this;
            var restrictedFiles = self.restrictedFiles;
            if (restrictedFiles.length > 0) {
                var message = FileRejectionType.MIME_TYPE_REJECTED(self.fileUpload);
                var heading = Granite.I18n.get("Restricted Files");

                var $content = $("<p class='item-list'>");
                var filesToList = (restrictedFiles.length > 10) ? 10 : restrictedFiles.length;
                for (var i = 0; i < filesToList; i++) {
                    $content.append($("<span>").text(restrictedFiles[i]["fileName"]).html());
                    $content.append("</br>");
                }
                if (filesToList !== restrictedFiles.length) {
                    $content.append("...</br>");
                }
                var content = "<p>" + message + "</p>" + $content[0].outerHTML;

                var dialog = new Coral.Dialog().set({
                    id: "buyDialog",
                    header: {
                        innerHTML: heading
                    },
                    backdrop: Coral.Dialog.backdrop.STATIC,
                    content: {
                        innerHTML: content
                    }
                });

                // Keep Button
                var okButton = new Coral.Button().set({
                    variant: "primary",
                    innerText: Granite.I18n.get("OK")
                });
                okButton.label.textContent = Granite.I18n.get("OK");
                dialog.footer.appendChild(okButton);
                okButton.on("click", function() {
                    self.restrictedFiles = [];
                    dialog.hide();
                    self._cleanupAfterRestrictedAssets();
                });

                document.body.appendChild(dialog);
                dialog.show();
            }
        },
        _onChunkUploadError: function(event) {
            var element = event.detail.item.element;
            $(ICON_PAUSE_SELECTOR, element)[0].hide();
            $(ICON_PLAY_SELECTOR, element)[0].show();
            $(UPLOAD_PROGRESS_SELECTOR, element).addClass("progress-status-error");
            new Coral.Dialog()
                .set({
                    variant: "error",
                    header: {
                        innerHTML: Granite.I18n.get("Error")
                    },
                    content: {
                        innerHTML: function() {
                            var contentHtml = Granite.I18n.get("Failed to upload the following file:");
                            contentHtml = contentHtml + "<br><br>";
                            contentHtml = contentHtml + event.detail.item.name;
                            contentHtml = contentHtml + "<br><br>";
                            contentHtml = contentHtml + Granite.I18n.get("You may retry.");
                            return contentHtml;
                        }()
                    },
                    footer: {
                        innerHTML: '<button is="coral-button" variant="primary" coral-close>Ok</button>'
                    }
                })
                .show();
        },
        _cleanupAfterDeletingDuplicates: function(event) {
            self.duplicateAssets = [];
            var contentApi = $(".foundation-content").adaptTo(
                "foundation-content");
            if (contentApi) {
                contentApi.refresh();
            } else {
                location.reload();
            }
        },
        _initiateWorkflow: function(assetPaths) {
            // making dummy update in asset to initiate set workflow(s)
            var payload = {
                "_charset_": "UTF-8"
            };
            for (var i = 0; i < assetPaths.length; i++) {
                payload["." + assetPaths[i].substring("/content/dam".length) +
                      "/jcr:content/renditions/original/jcr:content/jcr:mixinTypes"] = "mix:title";
            }
            Granite.$.ajax({
                async: false,
                url: Granite.HTTP.externalize("/content/dam"),
                type: "POST",
                data: payload
            });
        },
        _cleanupAfterRestrictedAssets: function(event) {
            var contentApi = $(".foundation-content").adaptTo(
                "foundation-content");
            if (contentApi) {
                contentApi.refresh();
            } else {
                location.reload();
            }
        },

        _prependErrorTooltip: function(item) {
            var errorIcon = new Coral.Icon().set({
                id: "dam-file-name-textfield-fielderror",
                icon: "infoCircle",
                size: "S"
            });
            errorIcon.className += " coral-Form-fielderror error-info-icon";
            item.prepend(errorIcon);
            var invalidCharSet;
            if (Dam.Util.NameValidation !== undefined) {
                invalidCharSet = Dam.Util.NameValidation.getInvalidFileCharSet();
            } else {
                // this block is to support backward compatibility
                invalidCharSet = this._ILLEGAL_FILENAME_CHARS.join(", ") + "space";
            }
            var errorToolTip = new Coral.Tooltip().set({
                variant: "error",
                content: {
                    innerHTML: Granite.I18n.get("<br> Characters {0}<br> are not allowed in the file name",
                        invalidCharSet)
                },
                target: "#dam-file-name-textfield-fielderror",
                placement: "left",
                id: "dam-file-name-textfield-fielderror-tooltip"
            });
            item.prepend(errorToolTip);
        },

        _onChunkUpoadStart: function(event) {
            var $el = $(document.getElementById("uploadRow_" + _g.XSS.getXSSValue(event.detail.item.name)));
            $(ICON_PAUSE_SELECTOR, $el)[0].show();
        },
        _onChunkLoaded: function(event) {
            var self = this;
            var el = event.detail.item.element;
            if (el) {
                var progress = ((event.detail.offset + event.detail.chunk.size) / (event.detail.item.file.size)) * 70;
                if (((event.detail.item.file.size - event.detail.offset) <= 2 * event.detail.chunk.size) &&
                    $("coral-progress", el)[0].getAttribute("autoProgressSet") !== "true") {
                    $("coral-progress", el)[0].setAttribute("autoProgressSet", "true");
                    var i = 1;
                    var timer = setInterval(function() {
                        if (i > 20 || $("coral-progress", el)[0].value === 100) {
                            clearInterval(timer);
                            return;
                        }
                        progress = progress + 1;
                        $("coral-progress", el)[0].value = progress;
                        i++;
                    }, 10000);
                }
                if (progress === 100) {
                    self._onFileLoaded(event);
                } else {
                    $("coral-progress", el)[0].value = progress;
                }
            }
        },
        /**
         * Creates a dialog which is not attached to the dom
         * */
        _createUploadDialog: function() {
            var self = this;
            if (!self.uploadDialog) {
                uploadDialogEl = new Coral.Dialog().set({
                    header: {
                        innerHTML: Granite.I18n.get("Upload Assets")
                    },
                    backdrop: Coral.Dialog.backdrop.STATIC
                });
                // $(uploadDialogEl).attr("id", "uploadListDialog");
                $(uploadDialogEl).addClass("uploadListDialog");
                var footer = uploadDialogEl.footer;
                var uploadFilesStatus = document.createElement("div");
                uploadFilesStatus.classList.add("dam-asset-upload-list-dialog-uploadStatus");
                // Uploaded asset count message
                var uploadFilesCount = document.createElement("div");
                uploadFilesCount.classList.add("foundation-layout-util-subtletext");
                uploadFilesCount.classList.add("dam-asset-upload-list-dialog-fileCount-uploaded");
                // Failed asset count message
                var failedFilesCount = document.createElement("div");
                failedFilesCount.classList.add("foundation-layout-util-subtletext");
                failedFilesCount.classList.add("dam-asset-upload-list-dialog-fileCount-failed");

                uploadFilesStatus.appendChild(uploadFilesCount);
                uploadFilesStatus.appendChild(failedFilesCount);
                footer.appendChild(uploadFilesStatus);
                // Cancel Button
                var cancelButton = new Coral.Button().set({
                    variant: "quiet",
                    label: {
                        innerText: Granite.I18n.get("Cancel")
                    }
                });
                cancelButton.label.textContent = Granite.I18n.get("Cancel");
                cancelButton.classList.add("dam-asset-upload-cancel-button");
                footer.appendChild(cancelButton);
                cancelButton.on("click", function() {
                    self.uploadDialog.hide();
                    self._cancelAll();
                });

                // Upload Button
                var uploadButton = new Coral.Button().set({
                    variant: "primary"
                });
                uploadButton.label.textContent = Granite.I18n.get("Upload");
                uploadButton.classList.add("dam-asset-upload-button");
                uploadButton.setAttribute("trackingfeature", "aem:assets:asset:upload");
                footer.appendChild(uploadButton);
                uploadButton.on("click", function() {
                    //AEM guides: Calling _opendialogAsPerConfig instead of _submit
                    self._opendialogAsPerConfig();
                });
            }
            self.uploadDialog = uploadDialogEl;
        },

        _cancelAll: function() {
            var self = this;
            self.fileUpload.uploadQueue.forEach(function(item, cnt) {
                item.isCancelled = true;
            });

            for (var i = 0; i < self.fileUpload.uploadQueue.length;) {
                var item = self.fileUpload.uploadQueue[i];
                i++;
                if (($("coral-progress", item.element)[0].value !== 100) || item.isFailed) {
                    i--;
                    self.fileUpload.cancel(item);
                }
            }
            $(self.uploadDialog.content).empty();
        },
        _addFileListToDialog: function(item) {
            var self = this;
            var fileName = item.file.name;
            var div = document.createElement("div");
            div.setAttribute("style", "margin-bottom:0.5rem;");
            let coralTextField = new Coral.Textfield()
                .set({
                    value: fileName,
                    id: "dam-asset-upload-rename-input"
                })
                .on("change", function(event) {
                    if (!self.utils.validFileName(this.value, self._ILLEGAL_FILENAME_CHARS)) {
                        this.className = this.className + " is-invalid";
                        Array.prototype.slice.call(this.parentElement.getElementsByTagName("coral-tooltip"))
                            .forEach(
                                function(item) {
                                    $(item).remove();
                                });
                        Array.prototype.slice.call(this.parentElement.getElementsByClassName("coral-Form-fielderror"))
                            .forEach(function(item) {
                                $(item).remove();
                            });
                        this.parentElement.style.position = "relative";
                        this.parentElement.style.marginTop = "10px";
                        self._prependErrorTooltip(this.parentElement);
                        event.preventDefault();
                        return false;
                    } else {
                        this.classList.remove("is-invalid");
                        this.parentElement.style.marginTop = "";
                        Array.prototype.slice.call(this.parentElement.getElementsByTagName("coral-tooltip"))
                            .forEach(function(item) {
                                $(item).remove();
                            });
                        Array.prototype.slice.call(this.parentElement.getElementsByClassName("coral-Form-fielderror"))
                            .forEach(function(item) {
                                $(item).remove();
                            });
                        self.utils.addOrReplaceInCustomPatameter(item,
                            "fileName",
                            this.value);
                    }
                })
                .on("keypress", function(event) {
                    if (!self.utils.validCharInput(event.charCode)) {
                        event.preventDefault();
                    }
                })

            //AEM Guides: Trigger on change after 100ms to show error state
            div.appendChild(coralTextField)
            setTimeout(() => {
                coralTextField.trigger('change')
            }, 100)

            div.appendChild(new Coral.Button()
                .set({
                    variant: "minimal",
                    icon: "closeCircle",
                    iconsize: "XS"
                })
                .on("click", function(event) {
                    self.fileUpload._clearFile(fileName);
                    if (self.fileUpload.uploadQueue.length === 0) {
                        self.uploadDialog.hide();
                        self._cancelAll();
                    }
                })
            );
            item.listDialogEL = div;
            self.uploadDialog.content.appendChild(div);
        },
        _getContentPath: function() {
            var self = this;
            var selectedFolders = $(".cq-damadmin-admin-childpages.foundation-collection " +
                ".foundation-collection-item.is-selected");

            if (selectedFolders.length && !self.dragDropEnabled) {
                return selectedFolders.data("foundationCollectionItemId");
            }
            return $(".cq-damadmin-admin-childpages.foundation-collection").data("foundationCollectionId");
        },
        _getNuiPath: function(path) {
            return this._getTrimPath(path) + ".initiateUpload.json";
        },
        _getTrimPath: function(path) {
            path = path.trim();
            if (path.charAt(path.length - 1) === "/") {
                path = path.substring(0, path.length - 1);
            }
            return Granite.HTTP.externalize(path);
        },
        _getActionPath: function(path) {
            path = path.trim();
            if (path.charAt(path.length - 1) === "/") {
                path = path.substring(0, path.length - 1);
            }
            return Granite.HTTP.externalize(path) + ".createasset.html";
        },

        _submit: function() {
            var self = this;
            self.contentPath = self._getContentPath();
            if (self.fileUpload.uploadQueue && self.fileUpload.uploadQueue.length) {
                // add browser warning for the refresh
                window.onbeforeunload = Granite.I18n.get(
                    "Upload is in Progress. Refreshing page will lose the files that have not completed the upload.");
                // normalizeWhitespaces(fileUpload);

                var allFileNamesValid = true;
                var firstInvalidElement = null;
                $("div input", self.uploadDialog.content).each(function(cnt, ech) {
                    if (!self.utils.validFileName(ech.value, self._ILLEGAL_FILENAME_CHARS)) {
                        allFileNamesValid = false;
                        if (!firstInvalidElement) {
                            firstInvalidElement = this;
                        }
                        this.className = this.className + " is-invalid";
                        Array.prototype.slice.call(this.parentElement.getElementsByTagName("coral-tooltip"))
                            .forEach(function(item) {
                                $(item).remove();
                            });
                        Array.prototype.slice.call(this.parentElement.getElementsByClassName("coral-Form-fielderror"))
                            .forEach(function(item) {
                                $(item).remove();
                            });
                        this.parentElement.style.position = "relative";
                        this.parentElement.style.marginTop = "10px";
                        self._prependErrorTooltip(this.parentElement);
                    }
                });

                if (firstInvalidElement) {
                    firstInvalidElement.focus();
                }

                if (allFileNamesValid) {
                    // get if any duplicate files are being uploaded
                    var duplicates = self.utils.getDuplicates(self.fileUpload.uploadQueue, self._getContentPath());
                    if (duplicates && duplicates.length) {
                        self.uploadDialog.hide();
                        self._showDuplicates(duplicates);
                    } else {
                        self._continueUpload();
                    }
                }
            }
        },

        _showDuplicates: function(duplicates) {
            var self = this;
            var xssName = _g.XSS.getXSSValue(duplicates[0]["name"]);
            var firstDuplicate = "<b>" + xssName + "</b>";
            var duplicateDialog = new Coral.Dialog().set({
                header: {
                    innerHTML: Granite.I18n.get("Name Conflict")
                }
            });
            document.body.appendChild(duplicateDialog);
            duplicateDialog.show();
            var assetExistsMessage;
            var description;
            var canOverwrite = self.utils.canOverwriteAssets(duplicates, self.contentPath);

            if (self.contentType === "folder" || (self
                .contentType === "asset" && isColumnView())) {
                assetExistsMessage = Granite.I18n.get(
                    "An asset named {0} already exists in this location.",
                    firstDuplicate);
                description = canOverwrite ? Granite.I18n
                    .get("Click 'Create Version' to create the version of the asset or 'Replace' to replace the asset or 'Keep Both' to keep both assets.")// eslint-disable-line max-len
                    : Granite.I18n.get("Click 'Keep Both' to keep both assets or 'Cancel' to cancel the upload.");
            } else if (self.contentType === "asset") {
                assetExistsMessage = Granite.I18n.get(
                    "A rendition named {0} already exists in this location",
                    firstDuplicate);
                description = Granite.I18n.get("Click 'Replace' to replace the rendition or 'Keep Both' to keep both renditions or 'X' to cancel the upload.");// eslint-disable-line max-len
            }
            duplicateDialog.content.innerHTML = "<p>" + assetExistsMessage + "<br><br>" + description + "</p>";
            if (duplicates.length > 1) {
                var checkbox = new Coral.Checkbox();
                checkbox.label.innerHTML = Granite.I18n.get("Apply to all");
                duplicateDialog.content.appendChild(checkbox);
            }

            // Add buttons to footer
            var cancelButton = new Coral.Button().set({
                variant: "secondary"
            });
            cancelButton.label.textContent = Granite.I18n.get("Cancel");
            duplicateDialog.footer.appendChild(cancelButton);
            cancelButton.on("click", function() {
                duplicateDialog.hide();
            });

            var keepBothButton = new Coral.Button().set({
                variant: "secondary"
            });
            keepBothButton.label.textContent = Granite.I18n.get("Keep Both");
            duplicateDialog.footer.appendChild(keepBothButton);
            keepBothButton.on("click", function() {
                self._duplicateOperations.keepBoth(duplicateDialog, self, duplicates);
                duplicateDialog.hide();
            });

            if (canOverwrite) {
                var replaceButton = new Coral.Button().set({
                    variant: "secondary"
                });
                replaceButton.label.textContent = Granite.I18n.get("Replace");
                duplicateDialog.footer.appendChild(replaceButton);
                replaceButton.on("click", function() {
                    self._duplicateOperations.replace(duplicateDialog, self, duplicates);
                    duplicateDialog.hide();
                });
            }

            if (self.contentType === "folder" && canOverwrite || (self
                .contentType === "asset" && isColumnView())) {
                var createVersionButton = new Coral.Button().set({
                    variant: "primary"
                });
                createVersionButton.setAttribute("trackingfeature", "aem:assets:asset:version:create");
                createVersionButton.label.textContent = Granite.I18n.get("Create Version");
                duplicateDialog.footer.appendChild(createVersionButton);
                createVersionButton.on("click", function() {
                    self._duplicateOperations.createVersion(duplicateDialog, self, duplicates);
                    duplicateDialog.hide();
                });
            }
        },

        _showRejectedFiles: function(fileRejectionMessageFunction) {
            var self = this;
            new Coral.Dialog()
                .set({
                    variant: "error",
                    header: {
                        innerHTML: Granite.I18n.get("Rejected Files")
                    },
                    content: {
                        innerHTML: function() {
                            var contentHtml = "";

                            if (fileRejectionMessageFunction !== null) {
                                contentHtml = fileRejectionMessageFunction(self.fileUpload);
                            }

                            contentHtml = contentHtml + "<br><br>";
                            for (var i = 0; i < self.fileUpload.rejectedFiles.length; i++) {
                                contentHtml = contentHtml + self.fileUpload.rejectedFiles[i].file.name + "<br>";
                            }
                            return contentHtml;
                        }()
                    },
                    footer: {
                        innerHTML: '<button is="coral-button" variant="primary" coral-close>Ok</button>'
                    }
                })
                .show();
            self.fileUpload.rejectedFiles = [];
        },

        //DXML CUSTOMIZATION

        _showRejectedFilesGuides: function(fileRejectionMessageFunction) {
            var self = this;
            var dialog = new Coral.Dialog()
                .set({
                    variant: "error",
                    header: {
                        innerHTML: Granite.I18n.get("Rejected Files")
                    },
                    content: {
                        innerHTML: function() {
                            var contentHtml = "";

                            if (fileRejectionMessageFunction !== null) {
                                contentHtml = fileRejectionMessageFunction(self.fileUpload);
                            }

                            contentHtml = contentHtml + "<br><br>";
                            for (var i = 0; i < self.fileUpload.rejectedFiles.length; i++) {
                                contentHtml = contentHtml + self.fileUpload.rejectedFiles[i].file.name + "<br>";
                            }
                            return contentHtml;
                        }()
                    }
                });
            var okButton = new Coral.Button().set({
                variant: "primary",
                innerText: Granite.I18n.get("OK")
            });
            okButton.label.textContent = Granite.I18n.get("OK");
            dialog.footer.appendChild(okButton);
            okButton.on("click", function() {
                window.location.reload();
            });
            document.body.appendChild(dialog);
            dialog.show();
            self.fileUpload.rejectedFiles = [];
        },

        _fileUploadCanceled: function(event) {
            var self = this;
            var item = event.detail.item;
            var cancelEvent = {};
            if (!item._xhr) {
                cancelEvent.item = item;
                cancelEvent.fileUpload = self.fileUpload;
                self._fileUploadItemCleanup(cancelEvent);
            } else if (self.fileUpload._canChunkedUpload(item) === true) {
                self._cleanUpChunkedUpload(item);
                self.fileUpload._cancelChunckedUpload(item);
                cancelEvent = {};
                cancelEvent.item = item;
                cancelEvent.fileUpload = self.fileUpload;
                self._fileUploadItemCleanup(cancelEvent);
            } else if (item._xhr.readyState === 0) {
                cancelEvent = {};
                cancelEvent.item = item;
                cancelEvent.fileUpload = self.fileUpload;
                self._fileUploadItemCleanup(cancelEvent);
            } else {
                self._cancelUpload(item);
            }
        },

        _fileUploadItemCleanup: function(event) {
            var self = this;
            // hide the item
            // Using document.getElementById as direct jquery selector behaves erroneously for xss prone string
            $(document.getElementById("uploadRow_" + _g.XSS.getXSSValue(event.item.name))).remove();

            self._refresh(event);
        },

        _changeIconPlayToPause: function(item) {
            var $el = $(document.getElementById("uploadRow_" + _g.XSS.getXSSValue(item.name)));
            $(ICON_PLAY_SELECTOR, $el).hide();
            $(ICON_PAUSE_SELECTOR, $el).removeAttr("hidden");
            $(ICON_PAUSE_SELECTOR, $el).show();
            $(UPLOAD_PROGRESS_SELECTOR, $el).removeClass("progress-status-error");
        },

        _changeIconPauseToPlay: function(item) {
            var $el = $(document.getElementById("uploadRow_" + _g.XSS.getXSSValue(item.name)));
            $(ICON_PAUSE_SELECTOR, $el).hide();
            $(ICON_PLAY_SELECTOR, $el).removeAttr("hidden");
            $(ICON_PLAY_SELECTOR, $el).show();
            $(UPLOAD_PROGRESS_SELECTOR, $el).addClass("progress-status-error");
        },

        /**
         Cancel upload of a file item
         @param {Object} item
         Object representing a file item
         */
        _cancelUpload: function(item) {
            item._xhr.abort();
        },

        _deleteAssetsAfterUpload: function(files) {
            // deletes the files from the server
            for (var i = 0; i < files.length; i++) {
                var filePath = Granite.HTTP.externalize(files[i]);
                Granite.$.ajax({
                    async: false,
                    url: filePath,
                    type: "POST",
                    data: {
                        ":operation": "delete"
                    }
                });
            }
        },

        _fileUploadedStatus: function(event) {
            var self = this;

            if (event.detail.item.statusChecked && event.detail.item.statusChecked === true) {
                return;
            }

            if (event.detail.item.isCancelled && event.detail.item.isCancelled === true) {
                var cancelEvent = {};
                cancelEvent.item = event.detail.item;
                cancelEvent.fileUpload = self.fileUpload;
                self._fileUploadItemCleanup(cancelEvent);
                return;
            }
            var response;
            if ((event.detail && event.detail.item.status && event.detail.item.status === 200)) {
                self._refresh(event);
                return;
            } else if ((event.detail && event.detail.item.status && event.detail.item.status === 409)) {
                var title;


                var changeLog;
                if (event.detail.item.status && event.detail.item.status === 409) {
                    response = event.detail.item.responseText;
                    title = $("#Path", response).text();
                    changeLog = $("#ChangeLog", response).text().trim();
                } else {
                    // this condition is to support IE9
                    title = event.message.headers["Path"];
                    changeLog = event.message.headers["ChangeLog"];
                }
                var indexOfDuplicates = changeLog.indexOf("duplicates") + "duplicates(\"".length;
                var duplicates = changeLog.substring(indexOfDuplicates, changeLog.indexOf("\"", indexOfDuplicates));
                var arr = [ title, duplicates ];
                self.duplicateAssets.push(arr);
                self._refresh(event);
            } else if ((event.detail && event.detail.item.status && event.detail.item.status === 415) ||
                (event.detail && event.detail.item.status && event.detail.item.status === 415)) {
                var msg;
                if (event.detail && event.detail.item.status && event.detail.item.status === 415) {
                    response = event.detail.item._xhr.responseText;
                    msg = $("#Message", response).text();
                } else {
                    // this condition is to support IE9
                    msg = $("#Message", response).text();
                }
                var file = msg.substr(0, msg.lastIndexOf(":"));
                var mimetype = msg.substr(msg.lastIndexOf(":") + 1);
                var obj = {
                    fileName: file,
                    mimeType: mimetype
                };
                self.restrictedFiles.push(obj);
                self._refresh(event);
            } else if ((event.detail && event.detail.item.status && event.detail.item.status === 403) ||
                (event.detail && event.detail.item.status && event.detail.item.status === 403)) {
                var forbiddenFile = {
                    fileName: event.item.fileName
                };
                self.forbiddenFiles.push(forbiddenFile);
                self._refresh(event);
            } else {
                self._onError(event);
            }

            event.detail.item.statusChecked = true;
            event.detail.item._xhr = null;
        },

        /**
         * will be called up on fileuploadsuccess, fileUploadedStatus and fileuploadcanceled.
         * when all the files have been processed, then refreshes the content and
         * returns true otherwise returns false;
         */
        _refresh: function(event) {
            var self = this;

            window.damUploadedFilesCount = window.damUploadedFilesCount || 0;
            if (event.item && event.item.isCancelled && event.item.isCancelled === true) {
                window.damTotalFilesForUpload--;
                if (event.item.isFailed && event.item.isFailed === true) {
                    window.damUploadedFilesErrorCount--;
                }
            } else {
                window.damUploadedFilesCount++;
            }
            $(".dam-asset-upload-list-dialog-fileCount-uploaded", self.uploadDialog)[0].innerText =
                Granite.I18n.get("{0} of {1} assets uploaded",
                    [ window.damUploadedFilesCount, window.damTotalFilesForUpload ], "Variables are numbers");

            if (window.damUploadedFilesErrorCount && window.damUploadedFilesErrorCount !== 0) {
                $(".dam-asset-upload-list-dialog-fileCount-failed", self.uploadDialog).show();
                $(".dam-asset-upload-list-dialog-fileCount-failed", self.uploadDialog)[0].innerText =
                    Granite.I18n.get("{0} of {1} assets failed",
                        [ window.damUploadedFilesErrorCount, window.damTotalFilesForUpload ], "Variables are numbers");
            } else {
                $(".dam-asset-upload-list-dialog-fileCount-failed", self.uploadDialog).hide();
            }

            if (window.damUploadedFilesErrorCount + window.damUploadedFilesCount === window.damTotalFilesForUpload) {
                // upload is finished, change cancel button to OK button
                $(".dam-asset-upload-cancel-button", self.uploadDialog).text("OK");
            }

            if (window.damUploadedFilesCount === window.damTotalFilesForUpload) {
                window.damUploadedFilesCount = 0;
                window.damTotalFilesForUpload = 0;
                // clean onbeforeunload
                window.onbeforeunload = null;

                if (self.duplicateAssets && self.duplicateAssets.length > 0) {
                    self._detectDuplicateAssets(event);
                } else if (self.restrictedFiles && self.restrictedFiles.length > 0) {
                    self._showRestrictedFiles(event);
                } else {
                    // refresh foundation-content
                    var contentApi = $(".foundation-content").adaptTo("foundation-content");
                    if (contentApi) {
                        contentApi.refresh();
                    } else {
                        location.reload();
                    }
                }


                self._cleanup(event);
                return true;
            } else {
                return false;
            }
        },

        _addFileUploadsToDialog: function(item) {
            var self = this;
            var div = document.createElement("div");

            div.appendChild(self._createFileUploadRow(item));
            self.uploadDialog.content.appendChild(div);
        },

        _confirmUpload: function(event) {
            var self = this;
            // refresh layoutId
            self.fileUpload.layoutId = $(".cq-damadmin-admin-childpages.foundation-collection")
                .data("foundationLayout").layoutId;

            // clear previous items
            $(self.uploadDialog.content).empty();

            self.fileUpload.uploadQueue.forEach(function(item, cnt) {
                item.parameters = item.parameters ? item.parameters : [];
                self._addFileListToDialog(item);
            });

            self.uploadDialog.getElementsByClassName("dam-asset-upload-button")[0].show();
            self.uploadDialog.show();
        },

        _continueUpload: function(event) {
            var self = this;
            // Hide the empty message banner before adding the cards
            $(".cq-damadmin-assets-empty-content").hide();
            // refresh layoutId
            self.fileUpload.layoutId = $(".cq-damadmin-admin-childpages.foundation-collection")
                .data("foundationLayout").layoutId;

            // clear previous items
            $(self.uploadDialog.content).empty();

            self.fileUpload.uploadQueue.forEach(function(item, cnt) {
                item.parameters = item.parameters ? item.parameters : [];
                // CQ-103490 - Trim leading and trailing spaces from file names
                if (item.parameters[0] && item.parameters[0].value) {
                    item.parameters[0].value = (item.parameters[0].value).trim();
                }
                self._addFileUploadsToDialog(item);
            });

            self.uploadDialog.show();
            self.uploadDialog.getElementsByClassName("dam-asset-upload-button")[0].hide();
            $(".dam-asset-upload-cancel-button", self.uploadDialog)[0].variant = "primary";
            // upload to nui or fall back
            self._uploadNui();
            window.damTotalFilesForUpload = window.damTotalFilesForUpload || 0;
            window.damTotalFilesForUpload = window.damTotalFilesForUpload + self.fileUpload.uploadQueue.length;
            $(".dam-asset-upload-list-dialog-fileCount-uploaded", self.uploadDialog)[0].innerText =
                Granite.I18n.get("0 of {0} assets uploaded", window.damTotalFilesForUpload, "Variables are numbers");
            self.fileUpload.uploadQueue = [];
        },
        /**
         * Upload to NUI
         */
        _uploadNui: function() {
            var self = this;
            var fileNames = [];
            var mimeTypes = [];
            var fileSizes = [];
            // reset
            objIndex = 0;
            objects = undefined;
            // 1. go through the list of files
            for (var ix = 0; ix < self.fileUpload.uploadQueue.length; ix++) {
                var item = self.fileUpload.uploadQueue[ix];
                fileNames[ix] = item.name;
                mimeTypes[ix] = item._originalFile.type;
                fileSizes[ix] = item._originalFile.size;
            }
            // 2. upload to init servlet
            $.post(this._getNuiPath(this._getContentPath()), {
                path: this._getContentPath(),
                fileName: fileNames,
                mimeType: mimeTypes,
                fileSize: fileSizes
            }, "json")
            // 3. completed all requests
                .done(function(data) {
                    // 4. submit to nui
                    self._parseResults(data);
                })
                // 5. on Failure fall back
                .fail(function() {
                    self.upload();
                });
        },
        /**
         * Parse results and send to nui or fall back
         */
        _parseResults: function(data) {
            var self = this;
            try {
                // 1. submit to nui
                objects = JSON.parse(data);
                self._sendAFile();
            } catch (e) {
                // 2. recover by falling back
                self.upload();
            }
        },
        /**
         * Send one file a time
         */
        _sendAFile: function() {
            var self = this;
            // 1. prepare one file at a time
            if (objIndex < objects.length) {
                // 2. clean up before we start work
                self._cleanUploadNui();
                var resObj = objects[objIndex];
                var fileName = resObj.fileName;
                // 3. get sorted keys
                var item = self.fileUpload.uploadQueue[objIndex];
                var uploadUris = resObj.uploadURIs;
                var completeUri = resObj.completeURI;
                // 4. fallback to regular upload they should match item name
                if (!completeUri || (item.name) !== fileName) {
                    self.upload();
                    return; // exit
                }
                // 5. submit requests to azure/s3
                self._sendToAzureS3(item, uploadUris, completeUri);
                // 6. update index
                objIndex++;
            }
        },
        /**
         * clean up shared resources
         */
        _cleanUploadNui: function() {
            // terminate all workers
            var self = this;
            self._clearAllWorkers(-1);
            uplCounter.clear();
            sliceCounter = 0;
        },
        /**
         * Upload using PUT, used for direct Azure / S3 transfer.
         * @private
         */
        _sendToAzureS3: function(item, uploadUris, completeUri) {
            var self = this;
            if (item.isCancelled && item.isCancelled === true) {
                return;
            }

            // 1. the size of the uploadurl[] is the slices we need for storage
            var end = uploadUris.length;
            var startByte = 0;
            var blob = item._originalFile;
            var bytesPerChunk = (blob.size / end);

            // 2. create workers < Max_workers
            while (sliceCounter < MAX_WORKERS) {
                var result = self._sendToWorker(startByte, blob, bytesPerChunk, uploadUris, item, completeUri, -1);
                if (result === true) {
                    return; // done
                } else {
                    // 3. update param to start = end
                    startByte = (startByte + bytesPerChunk);
                }
            }
        },

        _sendToWorker: function(startByte, blob, bytesPerChunk, uploadUris, item, completeUri, workerId) {
            var self = this;
            var end = uploadUris.length;
            var result = false;
            // 1. the last put request
            if (sliceCounter === (end - 1)) {
                self._putSlice(item, uploadUris[sliceCounter], completeUri, blob.slice(startByte, blob.size),
                    uploadUris, self._getWorkerId(workerId));
                result = true; // done with file
            } else {
                // 2. send the next slice
                var endByte = (startByte + bytesPerChunk);
                self._putSlice(item, uploadUris[sliceCounter], completeUri, blob.slice(startByte, endByte), uploadUris,
                    self._getWorkerId(workerId));
                result = false;
            }
            // 3. Increase slice counter
            sliceCounter++;
            return result;
        },

        _getWorkerId: function(workerId) {
            if (workerId >= 0 && workerId < MAX_WORKERS) {
                return workerId;
            }
            // slice counter
            return sliceCounter;
        },
        /**
         * Upload using PUT, used for direct Azure / S3 transfer.
         * @private
         */
        _putSlice: function(item, uploadUrl, completeUri, blob, uploadUris, workerId) {
            var self = this;
            var worker = self._getValidWorker(workerId);
            worker.postMessage(
                { topic: "blob", url: uploadUrl, ablob: blob }
            );
            // listen for response
            worker.addEventListener("message", function(ev) {
                if (ev.data >= 200 && ev.data < 300) {
                    // update the tracking of slices
                    uplCounter.set(self._getBlockId(uploadUrl), 1);
                    self._isPutComplete(item, self, completeUri, uploadUris, workerId);
                    worker.postMessage({ topic: "close" });
                }
            });
        },
        _getBlockId: function(uploadUrl) {
            return uploadUrl.match(/blockId=[\w]*/gi);
        },
        _getValidWorker: function(workerId) {
            var worker = new Worker("/libs/dam/gui/coral/components/commons/fileupload/clientlibs/putworker.js");
            workers[workerId] = worker;
            return worker;
        },
        _clearAllWorkers: function(workerId) {
            for (var idx = 0; idx < workers.length; idx++) {
                var worker = workers[idx];
                if (typeof (worker) !== "undefined") {
                    workers[idx] = undefined;
                    // close workers except current worker
                    if (idx !== workerId) {
                        worker.postMessage({ topic: "close" });
                    }
                }
            }
        },
        /**
         * check if put is complete otherwise process the next slice
         */
        _isPutComplete: function(item, self, completeUri, uploadUris, workerId) {
            // 1. completed transfer of file
            if (uplCounter.size === uploadUris.length) {
                self._clearAllWorkers(workerId);
                self._completeUri(completeUri, self, item);
            } else if (sliceCounter < uploadUris.length) {
                // 2. more slices need to be processed
                self._singleSlice(item, self, completeUri, uploadUris, workerId);
            }
        },

        _singleSlice: function(item, self, completeUri, uploadUris, workerId) {
            // 1. prepare variables
            var blob = item._originalFile;
            var end = uploadUris.length;
            var bytesPerChunk = (blob.size / end);
            var startByte = (sliceCounter * bytesPerChunk); // slice * bytes
            // 2. send to worker
            self._sendToWorker(startByte, blob, bytesPerChunk, uploadUris, item, completeUri, workerId);
        },
        /**
         * Call this servlet to complete the upload process via a post request
         */
        _completeUri: function(completeUri, self, item) {
            // 1. breakup the parameters
            var obj = completeUri.split("?");
            var obj2 = obj[1].split("&");
            var fileName = this._parseUrlParams(obj2[0]);
            var mimeType = this._parseUrlParams(obj2[1]);
            var uploadToken = this._parseUrlParams(obj2[2]);

            // 2. do a post request
            var requestMethod = "POST";
            var def = $.Deferred();
            $.ajax({
                xhr: function() {
                    var xhr = new XMLHttpRequest();
                    // 3. update progress
                    XHR_EVENT_NAMES.forEach(function(name) {
                        // Progress event is the only event among other ProgressEvents that can trigger multiple times.
                        // Hence it's the only one that gives away usable progress information.
                        var isProgressEvent = (name === "progress");
                        (isProgressEvent ? xhr.upload : xhr).addEventListener(name, function(event) {
                            var detail = {
                                item: item,
                                action: self._getTrimPath(obj[0]),
                                method: requestMethod
                            };

                            if (isProgressEvent) {
                                detail.lengthComputable = event.lengthComputable;
                                detail.loaded = event.loaded;
                                detail.total = event.total;
                            }
                            self.fileUpload.trigger("dam-chunkfileupload:" + name, detail);
                        });
                    });

                    xhr.onload = function() {
                        def.resolve();
                    };
                    return xhr;
                },
                type: requestMethod,
                url: self._getTrimPath(obj[0]),
                data: { fileName: fileName, mimeType: mimeType, uploadToken: uploadToken }
            }).done(function() {
                // process next file
                self._sendAFile();
            });
        },

        _parseUrlParams: function(params) {
            return params.split("=")[1];
        },

        /**
         Uploads the given filename, or all the files into the queue. It accepts extra parameters that are sent with the
         file.

         @param {String} [filename]
         The name of the file to upload.
         */
        upload: function(filename) {
            // file may be uploaded to a selected folder in current folder or
            // current folder, so update action path before upload
            this.fileUpload.setAttribute("action", this._getActionPath(this._getContentPath()));
            var self = this;
            if (!this.fileUpload.async) {
                if (typeof filename === "string") {
                    throw new Error("Coral.FileUpload does not support uploading " +
                        "a file from the queue on synchronous mode.");
                }

                var $form = this.fileUpload.$.parents("form");
                if (!$form.length) {
                    $form = $(document.createElement("form"));
                    $form
                        .attr({
                            // method is lowercase for consistency with other attributes
                            method: this.fileUpload.method.toLowerCase(),
                            enctype: "multipart/form-data",
                            action: this._getActionPath(this._getContentPath())
                        })
                        .css("display", "none");

                    $(this.fileUpload._elements.input).wrap($form);

                    Array.prototype.forEach.call(this.fileUpload.querySelectorAll('input[type="hidden"]'),
                        function(input) {
                            $form.append(input);
                        });
                }

                $form.append($(document.createElement("input")).attr({
                    type: "hidden",
                    name: "_charset_",
                    value: "utf-8"
                }));

                $form.trigger("submit");
            } else {
                var requests = [];
                if (this.fileUpload.parallelUploads === false) {
                    this.fileUpload._uploadQueue.forEach(function(item) {
                        self.fileUpload._abortFile(item.file.name);
                        requests.push(self.fileUpload._ajaxUpload(item, ""));
                    });

                    var p = requests[0](); // start off the chain
                    for (var i = 1; i < requests.length; i++) {
                        p = p.then(requests[i]);
                    }
                } else {
                    if (typeof filename === "string") {
                        this.fileUpload._uploadFile(filename);
                    } else {
                        this.fileUpload._uploadQueue.forEach(function(item) {
                            self.fileUpload._abortFile(item.file.name);
                            requests.push(self.fileUpload._ajaxUpload(item, ""));
                        });

                        for (i = 0; i < requests.length; i++) {
                            requests[i]();
                        }
                    }
                }
            }
        },
        // Creating item at ColumnMode
        _createFileUploadRow: function(item) {
            var self = this;
            var assetName = _g.XSS.getXSSValue(item.name);
            item.parameters.forEach(function(each) {
                if (each.name === "fileName") {
                    assetName = _g.XSS.getXSSValue(each.value);
                }
            });

            var uploadRow = document.createElement("div");
            uploadRow.classList.add("dam-asset-upload-list-dialog-item");
            uploadRow.id = "uploadRow_" + assetName;
            var title = document.createElement("div");
            if (assetName.length > 50) {
                $(title).text(assetName.substring(0, 50) + "...");
            } else {
                $(title).text(assetName);
            }
            var icon = new Coral.Icon().set({
                icon: "file",
                size: "XS"
            });
            icon.classList.add("dam-asset-upload-list-dialog-file-icon");
            uploadRow.appendChild(icon);

            var progress = new Coral.Progress();
            progress.size = Coral.Progress.size.SMALL;
            progress.classList.add("dam-asset-upload-list-item-progress");
            progress.id = "progress_" + assetName;

            var cancelIcon = new Coral.Icon().set({
                size: "XS",
                icon: "closeCircle"
            }).on("click", function() {
                self.fileUpload.cancel(item);
            });
            cancelIcon.classList.add("dam-asset-upload-list-item-cancel-icon");
            var pauseIcon = new Coral.Icon().set({
                size: "XS",
                icon: "pauseCircle"
            }).on("click", function() {
                self.fileUpload.pause(item);
            }).hide();
            pauseIcon.classList.add("dam-asset-upload-list-item-pause-icon");
            var playIcon = new Coral.Icon().set({
                size: "XS",
                icon: "playCircle"
            }).on("click", function() {
                self.fileUpload.resume(item);
            }).hide();
            playIcon.classList.add("dam-asset-upload-list-item-play-icon");
            title.appendChild(cancelIcon);

            if (self.fileUpload._canChunkedUpload(item)) {
                title.appendChild(pauseIcon);
                title.appendChild(playIcon);
            }
            if (assetName.length > 50) {
                var infoIcon = new Coral.Icon().set({
                    id: "dam-asset-name-icon",
                    icon: "infoCircle",
                    size: "XS"
                });
                var infoTooltip = new Coral.Tooltip().set({
                    variant: "inspect",
                    target: infoIcon,
                    placement: "right",
                    id: "dam-asset-name-fieldinfo-tooltip"
                });
                $(infoTooltip).text(assetName);
                title.appendChild(infoIcon);
                title.appendChild(infoTooltip);
            }
            uploadRow.appendChild(title);
            uploadRow.appendChild(progress);
            item.element = uploadRow;
            return uploadRow;
        },

        _cleanup: function(event) {
            var self = this;
            self.uploadDialog.hide();
            window.onbeforeunload = null;
            window.damFileUploadSucces = 0;
            self.fileUpload.list = [];
            self.fileUpload.uploadQueue.length = 0;
            self.fileUpload.rejectedFiles = [];
        },

        _fileChunkedUploadCanceled: function(event) {
            var self = this;
            var item = event.detail.item;
            self._changeIconPauseToPlay(item);
        },

        _cleanUpChunkedUpload: function(item) {
            var self = this;
            var filePath = self._getContentPath() + "/" + UNorm.normalize("NFC", item.name);
            var jsonPath = filePath + "/_jcr_content/renditions/original.1.json?ch_ck = " + Date.now();
            jsonPath = Granite.HTTP.externalize(jsonPath);
            var result = Granite.$.ajax({
                type: "GET",
                async: false,
                url: jsonPath
            });
            if (result.status === 200) {
                self._deleteChunks([ item ]);
            } else {
                self._deleteAssetsAfterUpload([ filePath ]);
            }
        },

        _deleteChunks: function(files) {
            // deletes the files from the server
            var self = this;
            var foundationContentPath = self._getContentPath();
            var foundationContentType = self.contentType;

            if (foundationContentType === "asset") {
                foundationContentPath += "/_jcr_content/renditions";
            }
            foundationContentPath = Granite.HTTP.externalize(foundationContentPath);
            for (var i = 0; i < files.length; i++) {
                var filePath = foundationContentPath + "/" + UNorm.normalize("NFC", files[i]["name"]);
                if (self.fileUpload._canChunkedUpload(files[i]) === true) {
                    self.fileUpload._deleteChunkedUpload(filePath);
                }
            }
        },


        // Pause
        _pauseChunkUpload: function(event) {
            var self = this;
            var item = event.detail.item;
            self.fileUpload._pauseChunckedUpload(item);
        },

        _queryChunkUploadStatus: function(event) {
            var self = this;
            var item = event.detail.item;
            self._changeIconPlayToPause(item);
            var url = self._getChunkInfoUrl(item, self.fileUpload);
            self.fileUpload._queryChunkedUpload(url, item);
        },

        _getChunkInfoUrl: function(item, fileupload) {
            var index = fileupload.attributes["action"].value.indexOf(".createasset.html");
            return fileupload.attributes["action"].value.substring(0, index) + "/" + item["name"] + ".3.json";
        },

        _fileChunkedUploadQuerySuccess: function(event) {
            var self = this;
            try {
                self._changeIconPlayToPause(event.detail.item);
                var json = JSON.parse(event.detail.originalEvent.target.responseText);
                var bytesUploaded = json["jcr:content"]["sling:length"];
                var uploadStatus = json["jcr:content"]["chunkUploadStatus"];
                if (uploadStatus) {
                    var status = uploadStatus.split("?")[0];
                }
                if (status && status === "inProgress") {
                    // merging in progress, check status after 5 seconds
                    setTimeout(function() {
                        self._queryChunkUploadStatus(event);
                    }, 5000);
                } else if (status && status === "Error") {
                    // merge failed
                    event.message = "Error in Uploading";
                    var item = event.detail.item;
                    var error = uploadStatus.split("?");
                    if (error.length > 1) {
                        var errorStatus = error[1];
                        json = JSON.parse(errorStatus);
                        if (json["status"] === 409) {
                            // handle duplicate asset error
                            var arr = [ json["path"], json["duplicates"] ];
                            self.duplicateAssets.push(arr);
                            self.fileUpload.trigger("dam-chunkfileupload:loadend", {
                                item: event.detail.item,
                                originalEvent: event,
                                fileUpload: self.fileUplaod
                            });
                        } else if (json["status"] === 415) {
                            // handle mime type unsupported error
                            var obj = {
                                fileName: json["file"],
                                mimeType: json["mimetype"]
                            };
                            self.restrictedFiles.push(obj);
                            self._cleanUpChunkedUpload(item);
                            self.fileUpload.trigger("dam-chunkfileupload:loadend", {
                                item: event.detail.item,
                                originalEvent: event,
                                fileUpload: self.fileUplaod
                            });
                        } else {
                            // handle any other error
                            self._handleChunkUploadFailed(event);
                        }
                    }
                    self.fileUpload.resolve();
                } else if (bytesUploaded) {
                    self.fileUpload._resumeChunkedUpload(event.detail.item, bytesUploaded);
                } else {
                    // merge was success
                    event.message = "Upload Success";
                    self.fileUpload.trigger("dam-chunkfileupload:loadend", {
                        item: event.detail.item,
                        originalEvent: event,
                        fileUpload: self.fileUplaod
                    });
                    self.fileUpload.resolve();
                }
            } catch (err) {
                event.message = "Error in query chunked upload.";
                self._handleChunkUploadFailed(event);
                self.fileUpload.resolve();
            }
        },

        _handleChunkUploadFailed: function(event) {
            var self = this;
            var item = event.detail.item;
            self._onError(event);
            if (self.fileUpload._canChunkedUpload(item) === true) {
                self._cleanUpChunkedUpload(item);
                self.fileUpload._cancelChunckedUpload(item);
            }
        },
        _fileChunkedUploadQueryError: function(event) {
            var self = this;
            if (event.detail.originalEvent.target.status === 404) {
                self.fileUpload._resumeChunkedUpload(event.detail.item, 0);
            } else {
                self._fileChunkedUploadError(event);
            }
        },

        _fileChunkedUploadTimeout: function(event) {
            var self = this;
            // last chunk timeout
            // query about chunk merge status after 5 second
            setTimeout(function() {
                self._queryChunkUploadStatus(event);
            }, 5000);
        },

        _fileChunkedUploadError: function(event) {
            var self = this;
            var response;
            if ((event.detail && event.detail.originalEvent.target.status &&
                event.detail.originalEvent.target.status === 200)) {
                self.fileUpload.resolve();
                return;
            } else if ((event.detail && event.detail.originalEvent.target.status &&
                event.detail.originalEvent.target.status === 415)) {
                var msg;
                if (event.detail.originalEvent.target.status && event.detail.originalEvent.target.status === 415) {
                    response = event.detail.item._xhr.responseText;
                    msg = $("#Message", response).text();
                } else {
                    // this condition is to support IE9
                    msg = $("#Message", response).text();
                }
                var file = event.detail.item.name;
                var mimetype = msg.substr(msg.lastIndexOf(":") + 1);
                var obj = {
                    fileName: file,
                    mimeType: mimetype
                };
                self.restrictedFiles.push(obj);
                self._cleanUpChunkedUpload(event.detail.item);
                self.fileUpload.trigger("dam-chunkfileupload:loadend", {
                    item: event.detail.item,
                    originalEvent: event,
                    fileUpload: self.fileUplaod
                });
                self.fileUpload.resolve();
            } else if ((event.detail &&
                event.detail.originalEvent.target.status &&
                event.detail.originalEvent.target.status === 403)) {
                var forbiddenFile = {
                    fileName: event.detail.item.file.name
                };
                self.forbiddenFiles.push(forbiddenFile);
                self._handleChunkUploadFailed(event);
                self.fileUpload.resolve();
            } else if ((event.detail &&
                event.detail.originalEvent.target.status &&
                event.detail.originalEvent.target.status === 409)) {
                var title;
                var changeLog;

                if (event.detail.originalEvent.target.status && event.detail.originalEvent.target.status === 409) {
                    response = event.detail.item._xhr.responseText;
                    title = $("#Path", response).text();
                    changeLog = $("#ChangeLog", response).text().trim();
                } else {
                    // this condition is to support IE9
                    title = event.message.headers["Path"];
                    changeLog = event.message.headers["ChangeLog"];
                }

                var indexOfDuplicates = changeLog.indexOf("duplicates") + "duplicates(\"".length;
                var duplicates = changeLog.substring(indexOfDuplicates, changeLog.indexOf("\"", indexOfDuplicates));
                var arr = [ title, duplicates ];
                self.duplicateAssets.push(arr);
                event.message = "Upload Success";
                self.fileUpload.trigger("dam-chunkfileupload:loadend", {
                    item: event.detail.item,
                    originalEvent: event,
                    fileUpload: self.fileUplaod
                });
                self.fileUpload.resolve();
            } else {
                self._changeIconPauseToPlay(event.detail.item);
                if (self.manualPause === false) {
                    // retry after 5 seconds
                    setTimeout(function() {
                        self._queryChunkUploadStatus(event);
                    }, 5000);
                }
            }
        },


        // Drop Zone implementation

        _dropZoneDragEnter: function(event) {
            var message = Granite.I18n.get("Drag and drop to upload");
            var dragAndDropMessage = $('<div class="drag-drop-message" style="text-align: center;">' +
                "<h1 > <span>{</span>" + message + "<span>}</span></h1></div>");
            $(".cq-damadmin-admin-childpages.foundation-collection").overlayMask("show", dragAndDropMessage);
        },

        _dropZoneDragLeave: function(event) {
            $(".cq-damadmin-admin-childpages.foundation-collection").overlayMask("hide");
        },

        _dropZoneDrop: function(event) {
            $(".cq-damadmin-admin-childpages.foundation-collection").overlayMask("hide");
        },

        _duplicateOperations: {
            keepBoth: function(duplicateDialog, damfileupload, duplicates) {
                var applyAllCheckbox = duplicateDialog.getElementsByTagName("coral-checkbox")[0];
                if ((applyAllCheckbox && applyAllCheckbox.checked) || duplicates.length === 1) {
                    this._autoResolveDuplicateFileNames(damfileupload, duplicates);
                    damfileupload._continueUpload(damfileupload.fileUpload);
                } else {
                    this._autoResolveDuplicateFileNames(damfileupload, [ duplicates[0] ]);
                    duplicates.splice(0, 1);
                    damfileupload._showDuplicates(duplicates);
                }
            },
            replace: function(duplicateDialog, damfileupload, duplicates) {
                var applyAllCheckbox = duplicateDialog.getElementsByTagName("coral-checkbox")[0];
                if ((applyAllCheckbox && applyAllCheckbox.checked) || duplicates.length === 1) {
                    this._addReplaceAssetParam(damfileupload.fileUpload, duplicates);
                    damfileupload._continueUpload(damfileupload.fileUpload);
                } else {
                    this._addReplaceAssetParam(damfileupload.fileUpload, [ duplicates[0] ]);
                    duplicates.splice(0, 1);
                    damfileupload._showDuplicates(duplicates);
                }
            },
            createVersion: function(duplicateDialog, damfileupload, duplicates) {
                var applyAllCheckbox = duplicateDialog.getElementsByTagName("coral-checkbox")[0];
                if ((applyAllCheckbox && applyAllCheckbox.checked) || duplicates.length === 1) {
                    damfileupload._continueUpload(damfileupload.fileUpload);
                } else {
                    duplicates.splice(0, 1);
                    damfileupload._showDuplicates(duplicates);
                }
            },
            _autoResolveDuplicateFileNames: function(damfileupload, duplicates) {
                var duplicatesIndex = 0;
                for (var i = 0; i < damfileupload.fileUpload.uploadQueue.length &&
                duplicatesIndex < duplicates.length; i++) {
                    if (duplicates[duplicatesIndex].name === damfileupload.fileUpload.uploadQueue[i].name) {
                        var params = damfileupload.fileUpload.uploadQueue[i].parameters;
                        damfileupload.fileUpload.uploadQueue[i].parameters = params ? params : [];
                        damfileupload.utils.addOrReplaceInCustomPatameter(damfileupload.fileUpload.uploadQueue[i],
                            "fileName",
                            this._resolveFileName(duplicates[duplicatesIndex].name, window.damDirectoryJson));
                        duplicatesIndex++;
                    }
                }
            },
            _resolveFileName: function(fileName, directoryJson) {
                var fn = fileName;
                var fileExtn = "";
                if (fileName.indexOf(".") !== -1) {
                    fn = fileName.substr(0, fileName.lastIndexOf("."));
                    fileExtn = fileName.substr(fileName.lastIndexOf(".") + 1);
                }
                var counter = 1;
                var tempFn;
                do {
                    tempFn = fn + counter;
                    counter++;
                } while (directoryJson[UNorm.normalize("NFC", tempFn) + "." + fileExtn]);
                return tempFn + "." + fileExtn;
            },
            _addReplaceAssetParam: function(fileUpload, duplicates) {
                for (var j = 0; j < duplicates.length; j++) {
                    var duplicateFileName = duplicates[j]["name"];
                    for (var i = 0; i < fileUpload.uploadQueue.length; i++) {
                        var fileName = fileUpload.uploadQueue[i].name;
                        if (duplicateFileName === fileName) {
                            var params = fileUpload.uploadQueue[i].parameters;
                            fileUpload.uploadQueue[i].parameters = params ? params : [];
                            fileUpload.uploadQueue[i].parameters.push({
                                name: ":replaceAsset",
                                value: "true"
                            });
                            break;
                        }
                    }
                }
            }
        },

        utils: {
            validFileName: function(fileName, illegalCharacters) {
                if (Dam.Util.NameValidation !== undefined) {
                    return Dam.Util.NameValidation.isValidFileName(fileName);
                } else {
                    // this block is to support backward incompatibility
                    return !this.contains(fileName, illegalCharacters);
                }
            },

            validCharInput: function(input) {
                var code = event.charCode;
                var restrictedCharCodes = [ 42, 47, 58, 91, 92, 93, 124, 35 ];
                if ($.inArray(code, restrictedCharCodes) > -1) {
                    return false;
                }
                return true;
            },
            contains: function(str, chars) {
                for (var i = 0; i < chars.length; i++) {
                    if (str.indexOf(chars[i]) > -1) {
                        return true;
                    }
                }
                return false;
            },
            getDuplicates: function(uploadFiles, contentPath) {
                var duplicates = [];
                var duplicateCount = 0;
                var jsonPath = "";
                var jsonResult;
                var foundationContent = $(".foundation-content-path");
                var foundationContentType = foundationContent
                    .data("foundationContentType");
                var resourcePath = encodeURIComponent(contentPath).replace(/%2F/g,
                    "/");
                if (foundationContentType === "folder") {
                    jsonPath = resourcePath + ".1.json?ch_ck = " + Date.now();
                } else if (foundationContentType === "asset") {
                    if (isColumnView()) {
                        jsonPath = resourcePath + ".1.json?ch_ck = " + Date.now();
                    } else {
                        jsonPath = resourcePath + "/_jcr_content/renditions.1.json?ch_ck = " + Date.now();
                    }
                }
                jsonPath = Granite.HTTP.externalize(jsonPath);
                var result = Granite.$.ajax({
                    type: "GET",
                    async: false,
                    url: jsonPath
                });
                if (result.status === 200) {
                    jsonResult = JSON.parse(result.responseText);
                    for (var i = 0; i < uploadFiles.length; i++) {
                        var name = uploadFiles[i].name ? uploadFiles[i].name : uploadFiles[i].file.name;
                        // Here we are trimming the spaces(at the end and beginning) from 'name' to make comparison
                        // agnostic of such spaces. CQ-4315863.
                        name = name.trim();
                        if (jsonResult[UNorm.normalize("NFKC", name)]) {
                            duplicates[duplicateCount] = uploadFiles[i];
                            duplicateCount++;
                        }
                    }
                }
                window.damDirectoryJson = jsonResult;
                return duplicates;
            },
            canOverwriteAsset: function(assetName, contentPath) {
                var assetPath = contentPath + "/" + assetName;
                var canOverwrite = true;
                var checkedOutBy = getCheckedOutBy(assetPath);
                if (checkedOutBy !== "") {
                    currentUserId = currentUserId !== "" ? currentUserId : getCurrentUserId();
                    canOverwrite = currentUserId === checkedOutBy;
                }
                return canOverwrite;
            },
            canOverwriteAssets: function(assets, contentPath) {
                var utls = this;
                for (var i = 0; i < assets.length; i++) {
                    if (utls.canOverwriteAsset(assets[i].name, contentPath) === false) {
                        return false;
                    }
                }
                return true;
            },
            addOrReplaceInCustomPatameter: function(item, name, value) {
                item.parameters = item.parameters ? item.parameters : [];
                var isPresent = false;
                item.parameters.forEach(function(itm) {
                    isPresent = true;
                    if (itm.name === name) {
                        itm.value = value;
                    }
                });
                if (!isPresent) {
                    item.parameters.push({
                        name: name,
                        value: value
                    });
                }

                // Specific handling for file name. As file object also contains name
                if (name === "fileName") {
                    item.name = value;
                }
            }
        }

    });

    $(document).on("foundation-contentloaded", function(e) {
        _intialize();
    });

    $(document).on("foundation-collection-navigate", function(e) {
        // FixMe: For Clumn View, content loaded event should be triggered on column navigation as well
        if ($(".cq-damadmin-admin-childpages.foundation-collection").data("foundationLayout").layoutId === "column") {
            _intialize();
        }
    });

    // NUI generic file upload for
    $(document).on("upload-blob", function(e) {
        // create DamFileUpload instance
        var dfu = new DamFileUpload();
        dfu.fileUpload.uploadQueue = [];
        dfu.fileUpload.uploadQueue.push(e.file);

        // upload file using NUI
        dfu._uploadNui(e.file);
    });

    function _intialize() {
        var iterate = function(cb) {
            var fileUploadSelectSelector = "dam-chunkfileupload";
            var fileUploadAll = document.querySelectorAll(fileUploadSelectSelector);

            for (var i = 0; i < fileUploadAll.length; i++) {
                cb(fileUploadAll[i]);
            }
        };

        iterate(function(fileUpload) {
            // Workaround GRANITE-11348
            if (fileUpload.getAttribute("is") === "coral-anchorlist-item") {
                var el = $(fileUpload.outerHTML.replace("is=\"coral-anchorlist-item\"", ""));
                $(fileUpload).replaceWith(el);
            }
        });

        // Wait for fileUpload element to be replaced before initialization
        Coral.commons.nextFrame(function() {
            iterate(function(fileUpload) {
                $(fileUpload).removeClass("coral-FileUpload");

                if (fileUpload.isInitialised === undefined || fileUpload.isInitialised === false) {
                    fileUpload.initialize();
                    var assetFU = new DamFileUpload().set("fileUpload", fileUpload);
                    assetFU.initialize();
                }
            });
        });
    }

    window.DamFileUpload = DamFileUpload;
})(document, Granite.$, UNorm);
