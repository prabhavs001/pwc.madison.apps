//This file is a copy of "http://s7d2.scene7.com/s7viewers/html5/js/VideoViewer.js" with video player control bar fix.
// Made controlsDivID property specific to VideoViewer object instead of assign it globally (at line no: 167).
// Also modified the logic of s7viewers.VideoViewer.getCodeBase.

if (typeof s7viewers == "undefined") {
    s7viewers = {}
} else {
    if (typeof s7viewers != "object") {
        throw new Error("Cannot initialize a root 's7viewers' package. s7viewers is not an object")
    }
}
if (!s7viewers.VideoViewer) {
    (function() {
        var a;
        s7viewers.VideoViewer = function(b) {
            this.sdkBasePath = "../../s7viewersdk/3.11/VideoViewer/";
            this.viewerFileName = "VideoViewer.js";
            this.cssSrcURL = "VideoViewer.css";
            this.utilsFilePath = "js/s7sdk/utils/Utils.js";
            this.containerId = null;
            this.params = {};
            this.handlers = [];
            this.onInitComplete = null;
            this.onInitFail = null;
            this.initializationComplete = false;
            this.initCalled = false;
            this.firstMediasetParsed = false;
            this.isDisposed = false;
            this.utilsScriptElm = null;
            this.fixinputmarker = null;
            this.sdkProvided = false;
            if (typeof b == "object") {
                if (b.containerId) {
                    this.setContainerId(b.containerId)
                }
                if (b.params) {
                    for (var c in b.params) {
                        if (b.params.hasOwnProperty(c) && b.params.propertyIsEnumerable(c)) {
                            this.setParam(c, b.params[c])
                        }
                    }
                }
                if (b.handlers) {
                    this.setHandlers(b.handlers)
                }
                if (b.localizedTexts) {
                    this.setLocalizedTexts(b.localizedTexts)
                }
            }
        }
        ;
        s7viewers.VideoViewer.cssClassName = "s7videoviewer";
        s7viewers.VideoViewer.prototype.modifiers = {};
        s7viewers.VideoViewer.prototype.setContainerId = function(b) {
            if (this.isDisposed) {
                return
            }
            this.containerId = b || null
        }
        ;
        s7viewers.VideoViewer.getCodeBase = function() {
            var COMP_SELECTOR = '.s7dm-dynamic-media';
            var viewerRootPath = $(COMP_SELECTOR).data('viewer-path');

            var h = viewerRootPath + "html5/";
            var c = h;
            var d = /\/etc\/dam\/viewers\//;
            s7viewers.VideoViewer.codebase = {
                contentUrl: h,
                isDAM: d.test(c)
            }
        }
        ;
        s7viewers.VideoViewer.getCodeBase();
        s7viewers.VideoViewer.prototype.getContentUrl = function() {
            return s7viewers.VideoViewer.codebase.contentUrl
        }
        ;
        s7viewers.VideoViewer.prototype.includeViewer = function() {
            a.Util.lib.include("s7sdk.common.Button");
            a.Util.lib.include("s7sdk.common.Container");
            a.Util.lib.include("s7sdk.event.Event");
            a.Util.lib.include("s7sdk.video.VideoControls");
            a.Util.lib.include("s7sdk.video.VideoPlayer");
            a.Util.lib.include("s7sdk.common.ControlBar");
            a.Util.lib.include("s7sdk.set.MediaSet");
            a.Util.lib.include("s7sdk.share.Share");
            this.s7params = new a.ParameterManager(null,null,{
                asset: "MediaSet.asset"
            },this.getContentUrl() + this.cssSrcURL);
            var f = "";
            if (this.s7params.params.config && (typeof (this.s7params.params.config) == "string")) {
                f = ",";
                if (this.s7params.params.config.indexOf("/") > -1) {
                    f += this.s7params.params.config.split("/")[1]
                } else {
                    f += this.s7params.params.config
                }
            }
            this.s7params.setViewer("506,5.12.1" + f);
            var d = {
                en: {
                    "Container.LABEL": "Video viewer"
                },
                defaultLocale: "en"
            };
            this.s7params.setDefaultLocalizedTexts(d);
            for (var b in this.params) {
                if (b != "localizedtexts") {
                    this.s7params.push(b, this.params[b])
                } else {
                    this.s7params.setLocalizedTexts(this.params[b])
                }
            }
            this.trackingManager = new a.TrackingManager();
            this.mediaSet = null;
            this.container = null;
            this.videoplayer = null;
            this.controls = null;
            this.playPauseButton = null;
            this.videoScrubber = null;
            this.videoTime = null;
            this.mutableVolume = null;
            this.fullScreenButton = null;
            this.closedCaptionButton = null;
            this.storedPlayingState = false;
            this.socialShare = null;
            this.emailShare = null;
            this.embedShare = null;
            this.linkShare = null;
            this.twitterShare = null;
            this.facebookShare = null;
            this.captionButtonPosition = null;
            this.volumeButtonPosition = null;
            this.videoTimePosition = null;
            this.isCaption = true;
            this.curCaption = null;
            this.storedCaptionEnabled = null;
            this.isNavigation = null;
            this.isPosterImage = null;
            this.fixTrackCSS = false;
            this.controlsDivID = null;
            this.storedSocialShareDisplayProp = null;
            this.supportsInline = null;
            this.isOrientationMarkerForcedChanged = false;
            var c = this;
            function g() {
                c.s7params.push("aemmode", s7viewers.VideoViewer.codebase.isDAM ? "1" : "0");
                var i = c.containerId + "_container";
                c.controlsDivID = c.containerId + "_controls";
                c.s7params.push("autoplay", "0");
                c.s7params.push("singleclick", "playPause");
                c.s7params.push("iconeffect", "1,-1,0.3,0");
                c.s7params.push("bearing", "fit-vertical");
                c.s7params.push("initialbitrate", "1400");
                var j = c.getParam("fixinputmarker");
                if (j) {
                    c.fixinputmarker = (j == "s7touchinput" || j == "s7mouseinput") ? c.fixinputmarker = j : null
                }
                var h = c.getURLParameter("fixinputmarker");
                if (h) {
                    c.fixinputmarker = (h == "s7touchinput" || h == "s7mouseinput") ? c.fixinputmarker = h : null
                }
                if (c.fixinputmarker) {
                    if (c.fixinputmarker === "s7mouseinput") {
                        c.addClass(c.containerId, "s7mouseinput")
                    } else {
                        if (c.fixinputmarker === "s7touchinput") {
                            c.addClass(c.containerId, "s7touchinput")
                        }
                    }
                } else {
                    if (a.browser.supportsTouch()) {
                        c.addClass(c.containerId, "s7touchinput")
                    } else {
                        c.addClass(c.containerId, "s7mouseinput")
                    }
                }
                c.parseMods();
                c.container = new a.common.Container(c.containerId,c.s7params,i);
                if (c.container.isInLayout()) {
                    e()
                } else {
                    c.container.addEventListener(a.event.ResizeEvent.ADDED_TO_LAYOUT, e, false)
                }
            }
            function e() {
                c.container.removeEventListener(a.event.ResizeEvent.ADDED_TO_LAYOUT, e, false);
                var s = document.getElementById(c.containerId);
                var C = s.style.minHeight;
                s.style.minHeight = "1px";
                var E = document.createElement("div");
                E.style.position = "relative";
                E.style.width = "100%";
                E.style.height = "100%";
                s.appendChild(E);
                var h = E.offsetHeight;
                if (E.offsetHeight <= 1) {
                    s.style.height = "100%";
                    h = E.offsetHeight
                }
                s.removeChild(E);
                s.style.minHeight = C;
                var i = false;
                switch (c.s7params.get("responsive", "auto")) {
                case "fit":
                    i = false;
                    break;
                case "constrain":
                    i = true;
                    break;
                default:
                    i = h == 0;
                    break
                }
                c.updateCSSMarkers();
                c.updateOrientationMarkers();
                if (c.container.isFixedSize()) {
                    c.viewerMode = "fixed"
                } else {
                    if (i) {
                        c.viewerMode = "ratio"
                    } else {
                        c.viewerMode = "free"
                    }
                }
                c.mediaSet = new a.MediaSet(null,c.s7params,c.containerId + "_mediaSet");
                c.videoplayer = new a.video.VideoPlayer(c.container,c.s7params,c.containerId + "_videoPlayer");
                c.trackingManager.attach(c.videoplayer);
                c.socialShare = new a.share.SocialShare(c.container,c.s7params,c.containerId + "_socialShare");
                c.emailShare = new a.share.EmailShare(c.containerId + "_socialShare",c.s7params,c.containerId + "_emailShare");
                c.embedShare = new a.share.EmbedShare(c.containerId + "_socialShare",c.s7params,c.containerId + "_embedShare");
                c.linkShare = new a.share.LinkShare(c.containerId + "_socialShare",c.s7params,c.containerId + "_linkShare");
                c.twitterShare = new a.share.TwitterShare(c.containerId + "_socialShare",c.s7params,c.containerId + "_twitterShare");
                c.facebookShare = new a.share.FacebookShare(c.containerId + "_socialShare",c.s7params,c.containerId + "_facebookShare");
                c.emailShare.addEventListener(a.event.SocialEvent.NOTF_SOCIAL_ACTIVATED, t, false);
                c.embedShare.addEventListener(a.event.SocialEvent.NOTF_SOCIAL_ACTIVATED, t, false);
                c.linkShare.addEventListener(a.event.SocialEvent.NOTF_SOCIAL_ACTIVATED, t, false);
                c.twitterShare.addEventListener(a.event.SocialEvent.NOTF_SOCIAL_ACTIVATED, t, false);
                c.facebookShare.addEventListener(a.event.SocialEvent.NOTF_SOCIAL_ACTIVATED, t, false);
                c.socialShare.addEventListener("mouseover", function(O) {
                    c.controls.allowAutoHide(false)
                });
                c.socialShare.addEventListener("mouseout", function(O) {
                    c.controls.allowAutoHide(true)
                });
                c.linkShare.setContentUrl(document.URL);
                c.emailShare.setOriginUrl(window.location.hostname);
                c.emailShare.setContentUrl(document.URL);
                c.supportsInline = c.videoplayer.supportsInline();
                c.controls = new a.common.ControlBar(c.container,c.s7params,c.controlsDivID);
                c.controls.setCSS(".s7controlbar", "visibility", "hidden");
                c.controls.attachView(c.videoplayer, false);
                if (a.browser.device.name != "iphone") {
                    c.controls.attach(c.socialShare)
                }
                c.socialShare.addTrackedComponent(c.controls);
                c.playPauseButton = new a.common.PlayPauseButton(c.controlsDivID,c.s7params,c.containerId + "_playPauseButton");
                c.videoScrubber = new a.video.VideoScrubber(c.controlsDivID,c.s7params,c.containerId + "_videoScrubber");
                c.fixTrackCSS = (a.Util.getStyle(c.videoScrubber.component.track, "width") == "310px") || (a.Util.getStyle(c.videoScrubber.component.track, "width") == "365px");
                c.videoTime = new a.VideoTime(c.controlsDivID,c.s7params,c.containerId + "_videoTime");
                c.closedCaptionButton = new a.common.ClosedCaptionButton(c.controlsDivID,c.s7params,c.containerId + "_closedCaptionButton");
                c.closedCaptionButton.addEventListener("click", w);
                c.closedCaptionButton.setSelected(c.videoplayer.getCaptionEnabled());
                c.videoplayer.setCaptionEnabled(c.videoplayer.getCaptionEnabled());
                c.storedCaptionEnabled = c.videoplayer.getCaptionEnabled();
                c.captionButtonPosition = D(document.getElementById(c.containerId + "_closedCaptionButton"), "right");
                c.captionButtonPosition = Number(c.captionButtonPosition.substring(0, c.captionButtonPosition.length - 2));
                c.mutableVolume = new a.video.MutableVolume(c.controlsDivID,c.s7params,c.containerId + "_mutableVolume");
                c.mutableVolume.setSelected(c.videoplayer.muted());
                c.volumeButtonPosition = D(document.getElementById(c.containerId + "_mutableVolume"), "right");
                c.volumeButtonPosition = Number(c.volumeButtonPosition.substring(0, c.volumeButtonPosition.length - 2));
                c.videoTimePosition = D(document.getElementById(c.containerId + "_videoTime"), "right");
                c.videoTimePosition = Number(c.videoTimePosition.substring(0, c.videoTimePosition.length - 2));
                if (!c.s7params.get("caption")) {
                    c.isCaption = false
                } else {
                    c.curCaption = c.s7params.params.caption
                }
                if (c.s7params.get("navigation")) {
                    c.isNavigation = c.s7params.get("navigation")
                }
                c.fullScreenButton = new a.common.FullScreenButton(c.controlsDivID,c.s7params,c.containerId + "_fullScreenButton");
                c.mediaSet.addEventListener(a.AssetEvent.NOTF_SET_PARSED, x, false);
                c.container.addEventListener(a.event.ResizeEvent.COMPONENT_RESIZE, l, false);
                c.container.addEventListener(a.event.ResizeEvent.FULLSCREEN_RESIZE, n, false);
                c.container.addEventListener(a.event.ResizeEvent.REMOVED_FROM_LAYOUT, k, false);
                c.container.addEventListener(a.event.ResizeEvent.ADDED_TO_LAYOUT, G, false);
                c.container.addEventListener(a.event.ResizeEvent.SIZE_MARKER_CHANGE, z, false);
                c.videoplayer.addEventListener(a.event.CapabilityStateEvent.NOTF_VIDEO_CAPABILITY_STATE, u, false);
                c.videoplayer.addEventListener(a.event.VideoEvent.NOTF_DURATION, y, false);
                c.videoplayer.addEventListener(a.event.VideoEvent.NOTF_LOAD_PROGRESS, M, false);
                c.videoplayer.addEventListener(a.event.VideoEvent.NOTF_CURRENT_TIME, A, false);
                c.videoplayer.addEventListener(a.event.VideoEvent.NOTF_NAVIGATION, H, false);
                c.playPauseButton.addEventListener("click", I);
                c.videoScrubber.addEventListener(a.SliderEvent.NOTF_SLIDER_UP, q, false);
                c.mutableVolume.addEventListener("click", m);
                c.mutableVolume.addEventListener(a.SliderEvent.NOTF_SLIDER_DOWN, N, false);
                c.mutableVolume.addEventListener(a.SliderEvent.NOTF_SLIDER_MOVE, j, false);
                c.mutableVolume.addEventListener(a.SliderEvent.NOTF_SLIDER_UP, j, false);
                c.fullScreenButton.addEventListener("click", o);
                c.trackingManager.setCallback(B);
                if ((typeof (AppMeasurementBridge) == "function") && (c.isConfig2Exist == true)) {
                    c.appMeasurementBridge = new AppMeasurementBridge(c.trackingParams);
                    c.appMeasurementBridge.setVideoPlayer(c.videoplayer)
                }
                if (c.viewerMode == "ratio") {
                    s.style.height = "auto"
                }
                if (c.container.getWidth() > 0 && c.container.getHeight() > 0) {
                    v(c.container.getWidth(), c.container.getHeight())
                }
                function B(Q, P, S, O, R) {
                    if (c.appMeasurementBridge) {
                        c.appMeasurementBridge.track(Q, P, S, O, R)
                    }
                    if (c.handlers.trackEvent) {
                        if (typeof window.s7sdk == "undefined") {
                            window.s7sdk = a
                        }
                        c.handlers.trackEvent(Q, P, S, O, R)
                    }
                    if ("s7ComponentEvent"in window) {
                        s7ComponentEvent(Q, P, S, O, R)
                    }
                }
                function t(O) {
                    c.videoplayer.pause()
                }
                function x(P) {
                    var O = P.s7event.asset;
                    if (O instanceof a.MediaSetDesc) {
                        if (c.viewerMode == "ratio") {
                            var S = O.items[0];
                            var Q = S.width / S.height;
                            c.container.setModifier({
                                aspect: Q
                            })
                        }
                        if (O.type == a.ItemDescType.VIDEO_SET || O.type == a.ItemDescType.VIDEO_GROUP) {
                            c.videoplayer.setItem(O)
                        } else {
                            c.videoplayer.setItem(O.items[0])
                        }
                        var R = {};
                        R.navigation = c.isNavigation ? c.isNavigation : ",0";
                        R.posterimage = c.isPosterImage ? c.isPosterImage : "";
                        R.caption = c.curCaption ? c.curCaption : ",0";
                        c.videoplayer.setModifier(R)
                    } else {
                        throw new Error("Failed to get meta data for video: " + P.s7event.asset)
                    }
                    L();
                    v(c.container.getWidth(), c.container.getHeight());
                    if (c.emailShare) {
                        c.emailShare.setThumbnail(P.s7event.asset.name)
                    }
                    if (c.embedShare) {
                        c.embedShare.setEmbedCode(r())
                    }
                    if ((c.handlers.initComplete != null) && (typeof c.handlers.initComplete == "function") && !c.firstMediasetParsed) {
                        if (typeof window.s7sdk == "undefined") {
                            window.s7sdk = a
                        }
                        c.handlers.initComplete()
                    }
                    c.firstMediasetParsed = true;
                    if (c.controls) {
                        c.controls.setCSS(".s7controlbar", "visibility", "inherit")
                    }
                }
                function l(O) {
                    if ((typeof (O.target) == "undefined") || (O.target == document.getElementById(c.containerId + "_container"))) {
                        if (!c.container.isInLayout()) {
                            return
                        }
                        v(O.s7event.w, O.s7event.h);
                        c.fullScreenButton.setSelected(c.container.isFullScreen())
                    }
                }
                function n(O) {
                    v(O.s7event.w, O.s7event.h);
                    c.fullScreenButton.setSelected(c.container.isFullScreen());
                    if (!c.container.isFullScreen()) {
                        c.onFullScreenExit(O)
                    } else {
                        c.onFullScreenEnter(O)
                    }
                }
                function z(O) {
                    c.updateCSSMarkers()
                }
                function G(O) {
                    if (a.browser.device.name != "desktop") {} else {
                        if (c.storedPlayingState) {
                            c.videoplayer.play();
                            c.storedPlayingState = false
                        }
                    }
                }
                function k(O) {
                    if (a.browser.device.name != "desktop") {} else {}
                    if (c.videoplayer.getCapabilityState().hasCapability(a.VideoCapabilityState.PAUSE)) {
                        c.storedPlayingState = true;
                        a.Logger.log(a.Logger.INFO, "Pause video");
                        c.videoplayer.pause()
                    }
                }
                function u(P) {
                    var O = P.s7event.state;
                    if (O.hasCapability(a.VideoCapabilityState.PAUSE)) {
                        c.playPauseButton.setSelected(false)
                    } else {
                        if (O.hasCapability(a.VideoCapabilityState.PLAY) || O.hasCapability(a.VideoCapabilityState.REPLAY)) {
                            c.playPauseButton.setSelected(true)
                        }
                    }
                    c.playPauseButton.enableReplay(O.hasCapability(a.VideoCapabilityState.REPLAY))
                }
                function y(O) {
                    c.videoTime.setDuration(O.s7event.data);
                    c.videoScrubber.setDuration(O.s7event.data)
                }
                function M(O) {
                    c.videoScrubber.setLoadedPosition(O.s7event.data)
                }
                function A(O) {
                    c.videoTime.setPlayedTime(O.s7event.data);
                    c.videoScrubber.setPlayedTime(O.s7event.data)
                }
                function H(O) {
                    c.videoScrubber.setNavigation(O.s7event.data)
                }
                function I(O) {
                    if (!c.playPauseButton.isSelected()) {
                        var P = c.videoplayer.getDuration() - c.videoplayer.getCurrentTime();
                        if (P <= 1) {
                            c.videoplayer.seek(0)
                        }
                        c.videoplayer.play()
                    } else {
                        c.videoplayer.pause()
                    }
                }
                function q(O) {
                    c.videoplayer.seek(O.s7event.position * c.videoplayer.getDuration())
                }
                function m(O) {
                    if (c.mutableVolume.isSelected()) {
                        c.videoplayer.mute()
                    } else {
                        c.videoplayer.unmute();
                        c.videoplayer.setVolume(c.mutableVolume.getPosition())
                    }
                }
                function N(O) {
                    c.videoplayer.unmute()
                }
                function j(O) {
                    c.videoplayer.setVolume(O.s7event.position)
                }
                function o(O) {
                    if (!c.container.isFullScreen()) {
                        c.container.requestFullScreen()
                    } else {
                        c.container.cancelFullScreen()
                    }
                }
                function w() {
                    c.videoplayer.setCaptionEnabled(c.closedCaptionButton.isSelected())
                }
                function r() {
                    var O = "";
                    if (c.s7params.params.style != "" && c.s7params.params.style != undefined) {
                        O = '    videoViewer.setParam("style", "' + c.s7params.params.style + '"); \n'
                    }
                    if (c.isCaption && c.curCaption != "" && c.curCaption != undefined) {
                        O += '    videoViewer.setParam("caption", "' + c.curCaption + '"); \n'
                    }
                    if (c.isNavigation && c.isNavigation != "" && c.isNavigation != undefined) {
                        O += '    videoViewer.setParam("navigation", "' + c.isNavigation + '"); \n'
                    }
                    var P = "";
                    if (c.s7params.params.config != "" && c.s7params.params.config != undefined) {
                        P = '    videoViewer.setParam("config", "' + c.s7params.params.config + '"); \n'
                    }
                    var R = "";
                    if (c.s7params.params.config2 != "" && c.s7params.params.config2 != undefined) {
                        R = '		videoViewer.setParam("config2", "' + c.s7params.params.config2 + '"); \n'
                    }
                    var Q = '<script language="javascript" type="text/javascript" src="' + (s7viewers.VideoViewer.codebase.contentUrl + "js/" + c.viewerFileName) + '"><\/script> \n<div id="' + c.containerId + '"></div> \n<script type="text/javascript"> \n    var videoViewer = new s7viewers.VideoViewer(); \n    videoViewer.setParam("videoserverurl", "' + F(c.videoplayer.component.videoServerUrl) + '"); \n    videoViewer.setParam("serverurl", "' + F(c.videoplayer.component.serverUrl) + '"); \n    videoViewer.setParam("contenturl", "' + F(c.s7params.get("contenturl", "/is/content")) + '"); \n' + O + "    videoViewer." + (c.mediaSet.component.asset != "" ? 'setAsset("' + c.mediaSet.component.asset : 'setVideo("' + c.videoplayer.getCurrentAsset()) + '"); \n    videoViewer.setParam("stagesize", "$EMBED_WIDTH$,$EMBED_HEIGHT$"); \n	 videoViewer.setParam("emailurl", "' + F(c.emailShare.component.emailurl) + '"); \n' + ((c.videoplayer.component.assetSpecificPosterImage || c.videoplayer.component.posterimage) ? '	 videoViewer.setParam("posterimage", "' + (c.videoplayer.component.assetSpecificPosterImage || c.videoplayer.component.posterimage) + '"); \n' : "") + P + R + '	 videoViewer.setContainerId("' + c.containerId + '"); \n	 videoViewer.init(); \n<\/script> \n';
                    return Q
                }
                function F(O) {
                    if (O && ((O.indexOf("http://") == 0) || (O.indexOf("https://") == 0))) {
                        return O
                    }
                    var P = document.location.protocol + "//" + document.location.host;
                    if (!O || O.indexOf("/") != 0) {
                        P += "/"
                    }
                    if (O) {
                        P += O
                    }
                    return P
                }
                function v(O, P) {
                    c.updateOrientationMarkers();
                    c.videoplayer.resize(O, P);
                    c.videoScrubber.resize(0, 0);
                    c.controls.resize(O, c.controls.getHeight());
                    J(O)
                }
                function J(P) {
                    if (c.supportsInline != true) {
                        return
                    }
                    var T = document.getElementById(c.containerId + "_controls");
                    var S = a.Util.getStyle(T, "display");
                    T.style.display = "block";
                    c.videoTime.autoSize();
                    var R = document.getElementById(c.containerId + "_playPauseButton").getBoundingClientRect();
                    var Q = document.getElementById(c.containerId + "_videoTime").getBoundingClientRect();
                    var O = document.getElementById(c.containerId + "_videoScrubber").getBoundingClientRect();
                    c.videoScrubber.resize(Q.left - R.right - 10, (O.bottom - O.top));
                    T.style.display = S;
                    p();
                    L()
                }
                function p() {
                    if (c.container.isPopup() && !c.container.isFixedSize() && !c.container.supportsNativeFullScreen()) {
                        c.fullScreenButton.setCSS(".s7fullscreenbutton", "display", "none")
                    }
                }
                function L() {
                    var O = c.videoplayer.supportsVolumeControl();
                    var Q;
                    var P = document.getElementById(c.containerId + "_playPauseButton").getBoundingClientRect();
                    var Q;
                    if (!O && !c.isCaption) {
                        c.mutableVolume.setCSS(".s7mutablevolume", "display", "none");
                        c.closedCaptionButton.setCSS(".s7closedcaptionbutton", "display", "none");
                        Q = c.volumeButtonPosition
                    } else {
                        if (!O) {
                            c.mutableVolume.setCSS(".s7mutablevolume", "display", "none");
                            Q = c.captionButtonPosition;
                            c.closedCaptionButton.setCSS(".s7closedcaptionbutton", "right", c.volumeButtonPosition + "px")
                        }
                        if (!c.isCaption) {
                            c.closedCaptionButton.setCSS(".s7closedcaptionbutton", "display", "none");
                            Q = c.captionButtonPosition
                        } else {
                            c.closedCaptionButton.setCSS(".s7closedcaptionbutton", "display", "block");
                            if (!O) {
                                Q = c.captionButtonPosition
                            } else {
                                Q = c.videoTimePosition
                            }
                        }
                    }
                    c.videoTime.setCSS(".s7videotime", "right", Q + "px");
                    if (c.fixTrackCSS) {
                        c.videoScrubber.setCSS(".s7videoscrubber .s7track", "width", (document.getElementById(c.containerId + "_videoTime").getBoundingClientRect().left - P.right - 10) + "px")
                    }
                    c.videoScrubber.resize(document.getElementById(c.containerId + "_videoTime").getBoundingClientRect().left - P.right - 10, document.getElementById(c.containerId + "_videoScrubber").getBoundingClientRect().height)
                }
                function D(R, Q) {
                    var P, O, S;
                    if (R && R.style) {
                        Q = Q.toLowerCase();
                        O = Q.replace(/\-([a-z])/g, function(U, T) {
                            return T.toUpperCase()
                        });
                        S = R.style[O];
                        if (!S) {
                            P = document.defaultView || window;
                            if (P.getComputedStyle) {
                                S = P.getComputedStyle(R, "").getPropertyValue(Q)
                            } else {
                                if (R.currentStyle) {
                                    S = R.currentStyle[O]
                                }
                            }
                        }
                    }
                    return S || ""
                }
                if (c.supportsInline) {
                    var K = c.container.getWidth();
                    J(K)
                } else {
                    c.controls.setCSS(".s7controlbar", "display", "none")
                }
                if ((c.onInitComplete != null) && (typeof c.onInitComplete == "function")) {
                    c.onInitComplete()
                }
                if (!c.s7params.get("asset", null, "MediaSet")) {
                    L();
                    if (c.embedShare) {
                        c.embedShare.setEmbedCode(r())
                    }
                    if ((c.handlers.initComplete != null) && (typeof c.handlers.initComplete == "function")) {
                        c.handlers.initComplete()
                    }
                    c.controls.setCSS(".s7controlbar", "visibility", "inherit")
                }
            }
            this.s7params.addEventListener(a.Event.SDK_READY, function() {
                c.initSiteCatalyst(c.s7params, g)
            }, false);
            this.s7params.setProvidedSdk(this.sdkProvided);
            this.s7params.init()
        }
        ;
        s7viewers.VideoViewer.prototype.setParam = function(b, c) {
            if (this.isDisposed) {
                return
            }
            this.params[b] = c
        }
        ;
        s7viewers.VideoViewer.prototype.getParam = function(c) {
            var d = c.toLowerCase();
            for (var b in this.params) {
                if (b.toLowerCase() == d) {
                    return this.params[b]
                }
            }
            return null
        }
        ;
        s7viewers.VideoViewer.prototype.setParams = function(b) {
            if (this.isDisposed) {
                return
            }
            var e = b.split("&");
            for (var c = 0; c < e.length; c++) {
                var d = e[c].split("=");
                if (d.length > 1) {
                    this.setParam(d[0], decodeURIComponent(e[c].split("=")[1]))
                }
            }
        }
        ;
        s7viewers.VideoViewer.prototype.s7sdkUtilsAvailable = function() {
            if (s7viewers.VideoViewer.codebase.isDAM) {
                return typeof (s7viewers.s7sdk) != "undefined"
            } else {
                return (typeof (s7classic) != "undefined") && (typeof (s7classic.s7sdk) != "undefined")
            }
        }
        ;
        s7viewers.VideoViewer.prototype.resize = function(b, c) {
            this.container.resize(b, c)
        }
        ;
        s7viewers.VideoViewer.prototype.init = function() {
            if (this.isDisposed) {
                return
            }
            if (this.initCalled) {
                return
            }
            this.initCalled = true;
            if (this.initializationComplete) {
                return this
            }
            var i = document.getElementById(this.containerId);
            if (i) {
                if (i.className != "") {
                    if (i.className.indexOf(s7viewers.VideoViewer.cssClassName) != -1) {} else {
                        i.className += " " + s7viewers.VideoViewer.cssClassName
                    }
                } else {
                    i.className = s7viewers.VideoViewer.cssClassName
                }
            }
            this.s7sdkNamespace = s7viewers.VideoViewer.codebase.isDAM ? "s7viewers" : "s7classic";
            var d = this.getContentUrl() + this.sdkBasePath + "js/s7sdk/utils/Utils.js?namespace=" + this.s7sdkNamespace;
            var f = null;
            if (document.scripts) {
                f = document.scripts
            } else {
                f = document.getElementsByTagName("script")
            }
            if (this.s7sdkUtilsAvailable()) {
                a = (s7viewers.VideoViewer.codebase.isDAM ? s7viewers.s7sdk : s7classic.s7sdk);
                this.sdkProvided = true;
                if (this.isDisposed) {
                    return
                }
                a.Util.init();
                this.includeViewer();
                this.initializationComplete = true
            } else {
                if (!this.s7sdkUtilsAvailable() && (s7viewers.VideoViewer.codebase.isDAM ? s7viewers.S7SDK_S7VIEWERS_LOAD_STARTED : s7viewers.S7SDK_S7CLASSIC_LOAD_STARTED)) {
                    this.sdkProvided = true;
                    var h = this;
                    var g = setInterval(function() {
                        if (h.s7sdkUtilsAvailable()) {
                            clearInterval(g);
                            a = (s7viewers.VideoViewer.codebase.isDAM ? s7viewers.s7sdk : s7classic.s7sdk);
                            if (h.isDisposed) {
                                return
                            }
                            a.Util.init();
                            h.includeViewer();
                            h.initializationComplete = true
                        }
                    }, 100)
                } else {
                    this.utilsScriptElm = document.createElement("script");
                    this.utilsScriptElm.setAttribute("language", "javascript");
                    this.utilsScriptElm.setAttribute("type", "text/javascript");
                    var e = document.getElementsByTagName("head")[0];
                    var c = this;
                    function b() {
                        if (!c.utilsScriptElm.executed) {
                            c.utilsScriptElm.executed = true;
                            a = (s7viewers.VideoViewer.codebase.isDAM ? s7viewers.s7sdk : s7classic.s7sdk);
                            if (c.s7sdkUtilsAvailable() && a.Util) {
                                if (c.isDisposed) {
                                    return
                                }
                                a.Util.init();
                                c.includeViewer();
                                c.initializationComplete = true;
                                c.utilsScriptElm.onreadystatechange = null;
                                c.utilsScriptElm.onload = null;
                                c.utilsScriptElm.onerror = null
                            }
                        }
                    }
                    if (typeof (c.utilsScriptElm.readyState) != "undefined") {
                        c.utilsScriptElm.onreadystatechange = function() {
                            if (c.utilsScriptElm.readyState == "loaded") {
                                e.appendChild(c.utilsScriptElm)
                            } else {
                                if (c.utilsScriptElm.readyState == "complete") {
                                    b()
                                }
                            }
                        }
                        ;
                        c.utilsScriptElm.setAttribute("src", d)
                    } else {
                        c.utilsScriptElm.onload = function() {
                            b()
                        }
                        ;
                        c.utilsScriptElm.onerror = function() {}
                        ;
                        c.utilsScriptElm.setAttribute("src", d);
                        e.appendChild(c.utilsScriptElm);
                        c.utilsScriptElm.setAttribute("data-src", c.utilsScriptElm.getAttribute("src"));
                        c.utilsScriptElm.setAttribute("src", "?namespace=" + this.s7sdkNamespace)
                    }
                    if (s7viewers.VideoViewer.codebase.isDAM) {
                        s7viewers.S7SDK_S7VIEWERS_LOAD_STARTED = true
                    } else {
                        s7viewers.S7SDK_S7CLASSIC_LOAD_STARTED = true
                    }
                }
            }
            return this
        }
        ;
        s7viewers.VideoViewer.prototype.getDomScriptTag = function(b) {
            var d;
            if (document.scripts) {
                d = document.scripts
            } else {
                d = document.getElementsByTagName("script")
            }
            for (var c = 0; c < d.length; c++) {
                if (d[c] && d[c].getAttribute("src") != null && d[c].getAttribute("src").indexOf(b) != -1) {
                    return d[c];
                    break
                }
            }
            return null
        }
        ;
        s7viewers.VideoViewer.prototype.getDomain = function(b) {
            var c = /(^http[s]?:\/\/[^\/]+)/i.exec(b);
            if (c == null) {
                return ""
            } else {
                return c[1]
            }
        }
        ;
        s7viewers.VideoViewer.prototype.setAsset = function(b, e) {
            if (this.isDisposed) {
                return
            }
            var d = null
              , c = null
              , f = null;
            if (e) {
                if (Object.prototype.toString.apply(e) === "[object String]") {
                    d = e
                } else {
                    if (typeof e == "object") {
                        if (e.caption) {
                            d = e.caption
                        }
                        if (e.navigation) {
                            c = e.navigation
                        }
                        if (e.posterimage) {
                            f = e.posterimage
                        }
                    }
                }
            }
            if (this.mediaSet) {
                this.mediaSet.setAsset(b);
                if (d) {
                    this.isCaption = true;
                    this.curCaption = d + ",1";
                    this.videoplayer.setCaption(d);
                    this.videoplayer.setCaptionEnabled(this.storedCaptionEnabled)
                } else {
                    this.isCaption = false;
                    this.curCaption = null;
                    this.videoplayer.setCaptionEnabled(false)
                }
                this.isNavigation = (c) ? c : null;
                this.isPosterImage = (f) ? f : null;
                this.closedCaptionButton.setSelected(this.storedCaptionEnabled);
                if (this.emailShare) {
                    this.emailShare.setThumbnail(b)
                }
            } else {
                this.setParam("asset", b)
            }
        }
        ;
        s7viewers.VideoViewer.prototype.setVideo = function(b, e) {
            if (this.isDisposed) {
                return
            }
            var d = null
              , c = null
              , f = null;
            if (e) {
                if (Object.prototype.toString.apply(e) === "[object String]") {
                    d = e
                } else {
                    if (typeof e == "object") {
                        if (e.caption) {
                            d = e.caption
                        }
                        if (e.navigation) {
                            c = e.navigation
                        }
                        if (e.posterimage) {
                            f = e.posterimage
                        }
                    }
                }
            }
            if (this.videoplayer) {
                this.videoplayer.setVideo(b, f);
                if (d) {
                    this.isCaption = true;
                    this.curCaption = d + ",1";
                    this.videoplayer.setCaption(d);
                    this.videoplayer.setCaptionEnabled(this.storedCaptionEnabled)
                } else {
                    this.isCaption = false;
                    this.curCaption = null;
                    this.videoplayer.setCaptionEnabled(false)
                }
                this.isNavigation = (c) ? c : null;
                this.isPosterImage = (f) ? f : null;
                this.closedCaptionButton.setSelected(this.storedCaptionEnabled)
            } else {
                if (b) {
                    this.setParam("video", b)
                }
                if (d) {
                    this.setParam("caption", d)
                }
                if (c) {
                    this.setParam("navigation", c)
                }
                if (f) {
                    this.setParam("posterimage", f)
                }
            }
        }
        ;
        s7viewers.VideoViewer.prototype.setLocalizedTexts = function(b) {
            if (this.isDisposed) {
                return
            }
            if (this.s7params) {
                this.s7params.setLocalizedTexts(b)
            } else {
                this.setParam("localizedtexts", b)
            }
        }
        ;
        s7viewers.VideoViewer.prototype.initSiteCatalyst = function(i, c) {
            if (i.get("asset", null, "MediaSet")) {
                var f = i.get("asset", null, "MediaSet").split(",")[0].split(":")[0];
                this.isConfig2Exist = false;
                if (f.indexOf("/") != -1) {
                    var d = a.MediaSetParser.findCompanyNameInAsset(f);
                    var h = i.get("config2");
                    this.isConfig2Exist = (h != "" && typeof h != "undefined");
                    if (this.isConfig2Exist) {
                        this.trackingParams = {
                            siteCatalystCompany: d,
                            config2: h,
                            isRoot: i.get("serverurl"),
                            contentUrl: this.getContentUrl()
                        };
                        var b = this.getContentUrl() + "../AppMeasurementBridge.jsp?company=" + d + (h == "" ? "" : "&preset=" + h);
                        if (i.get("serverurl", null)) {
                            b += "&isRoot=" + i.get("serverurl")
                        }
                        var g = document.createElement("script");
                        g.setAttribute("language", "javascript");
                        g.setAttribute("type", "text/javascript");
                        g.setAttribute("src", b);
                        var e = document.getElementsByTagName("head");
                        g.onload = g.onerror = function() {
                            if (!g.executed) {
                                g.executed = true;
                                if (typeof c == "function") {
                                    c()
                                }
                                g.onreadystatechange = null;
                                g.onload = null;
                                g.onerror = null
                            }
                        }
                        ;
                        g.onreadystatechange = function() {
                            if (g.readyState == "complete" || g.readyState == "loaded") {
                                setTimeout(function() {
                                    if (!g.executed) {
                                        g.executed = true;
                                        if (typeof c == "function") {
                                            c()
                                        }
                                    }
                                    g.onreadystatechange = null;
                                    g.onload = null;
                                    g.onerror = null
                                }, 0)
                            }
                        }
                        ;
                        e[0].appendChild(g)
                    } else {
                        if (typeof c == "function") {
                            c()
                        }
                    }
                }
            } else {
                if (typeof c == "function") {
                    c()
                }
            }
        }
        ;
        s7viewers.VideoViewer.prototype.onFullScreenEnter = function(b) {
            this.storedSocialShareDisplayProp = a.Util.getStyle(this.socialShare.getObj(), "display");
            this.socialShare.setCSS(".s7socialshare", "display", "none")
        }
        ;
        s7viewers.VideoViewer.prototype.onFullScreenExit = function(b) {
            this.socialShare.setCSS(".s7socialshare", "display", this.storedSocialShareDisplayProp)
        }
        ;
        s7viewers.VideoViewer.prototype.getComponent = function(b) {
            if (this.isDisposed) {
                return
            }
            switch (b) {
            case "container":
                return this.container || null;
            case "mediaSet":
                return this.mediaSet || null;
            case "videoPlayer":
                return this.videoplayer || null;
            case "controls":
                return this.controls || null;
            case "videoScrubber":
                return this.videoScrubber || null;
            case "videoTime":
                return this.videoTime || null;
            case "mutableVolume":
                return this.mutableVolume || null;
            case "playPauseButton":
                return this.playPauseButton || null;
            case "closedCaptionButton":
                return this.closedCaptionButton || null;
            case "fullScreenButton":
                return this.fullScreenButton || null;
            case "twitterShare":
                return this.twitterShare || null;
            case "facebookShare":
                return this.facebookShare || null;
            case "linkShare":
                return this.linkShare || null;
            case "socialShare":
                return this.socialShare || null;
            case "emailShare":
                return this.emailShare || null;
            case "embedShare":
                return this.embedShare || null;
            case "parameterManager":
                return this.s7params || null;
            default:
                return null
            }
        }
        ;
        s7viewers.VideoViewer.prototype.setHandlers = function(c) {
            if (this.isDisposed || this.initCalled) {
                return
            }
            this.handlers = [];
            for (var b in c) {
                if (!c.hasOwnProperty(b)) {
                    continue
                }
                if (typeof c[b] != "function") {
                    continue
                }
                this.handlers[b] = c[b]
            }
        }
        ;
        s7viewers.VideoViewer.prototype.getModifiers = function() {
            return this.modifiers
        }
        ;
        s7viewers.VideoViewer.prototype.setModifier = function(f) {
            if (this.isDisposed) {
                return
            }
            var h, c, j, b, g, e;
            for (h in f) {
                if (!this.modifiers.hasOwnProperty(h)) {
                    continue
                }
                c = this.modifiers[h];
                try {
                    b = f[h];
                    if (c.parseParams === false) {
                        g = new a.Modifier([b != "" ? b : c.defaults[0]])
                    } else {
                        g = a.Modifier.parse(b, c.defaults, c.ranges)
                    }
                    if (g.values.length == 1) {
                        this[h] = g.values[0];
                        this.setModifierInternal(h)
                    } else {
                        if (g.values.length > 1) {
                            j = {};
                            for (e = 0; e < g.values.length; e++) {
                                j[c.params[e]] = g.values[e]
                            }
                            this[h] = j;
                            this.setModifierInternal(h)
                        }
                    }
                } catch (d) {
                    throw new Error("Unable to process modifier: '" + h + "'. " + d)
                }
            }
        }
        ;
        s7viewers.VideoViewer.prototype.setModifierInternal = function(b) {
            switch (b) {
            default:
                break
            }
        }
        ;
        s7viewers.VideoViewer.prototype.parseMods = function() {
            var g, c, h, b, f, e;
            for (g in this.modifiers) {
                if (!this.modifiers.hasOwnProperty(g)) {
                    continue
                }
                c = this.modifiers[g];
                try {
                    b = this.s7params.get(g, "");
                    if (c.parseParams === false) {
                        f = new a.Modifier([b != "" ? b : c.defaults[0]])
                    } else {
                        f = a.Modifier.parse(b, c.defaults, c.ranges)
                    }
                    if (f.values.length == 1) {
                        this[g] = f.values[0]
                    } else {
                        if (f.values.length > 1) {
                            h = {};
                            for (e = 0; e < f.values.length; e++) {
                                h[c.params[e]] = f.values[e]
                            }
                            this[g] = h
                        }
                    }
                } catch (d) {
                    throw new Error("Unable to process modifier: '" + g + "'. " + d)
                }
            }
        }
        ;
        s7viewers.VideoViewer.prototype.updateCSSMarkers = function() {
            var c = this.container.getSizeMarker();
            var b;
            if (c == a.common.Container.SIZE_MARKER_NONE) {
                return
            }
            if (c == a.common.Container.SIZE_MARKER_LARGE) {
                b = "s7size_large"
            } else {
                if (c == a.common.Container.SIZE_MARKER_SMALL) {
                    b = "s7size_small"
                } else {
                    if (c == a.common.Container.SIZE_MARKER_MEDIUM) {
                        b = "s7size_medium"
                    }
                }
            }
            if (this.containerId) {
                this.setNewSizeMarker(this.containerId, b)
            }
            this.reloadInnerComponents()
        }
        ;
        s7viewers.VideoViewer.prototype.reloadInnerComponents = function() {
            var c = this.s7params.getRegisteredComponents();
            for (var b = 0; b < c.length; b++) {
                if (c[b] && c[b].restrictedStylesInvalidated()) {
                    c[b].reload()
                }
            }
        }
        ;
        s7viewers.VideoViewer.prototype.setNewSizeMarker = function(f, c) {
            var b = document.getElementById(f).className;
            var d = /^(.*)(s7size_small|s7size_medium|s7size_large)(.*)$/gi;
            var e;
            if (b.match(d)) {
                e = b.replace(d, "$1" + c + "$3")
            } else {
                e = b + " " + c
            }
            if (b != e) {
                document.getElementById(f).className = e
            }
        }
        ;
        s7viewers.VideoViewer.prototype.dispose = function() {
            if (this.appMeasurementBridge) {
                this.appMeasurementBridge.dispose();
                this.appMeasurementBridge = null
            }
            if (this.trackingManager) {
                this.trackingManager.dispose();
                this.trackingManager = null
            }
            if (this.videoplayer) {
                this.videoplayer.dispose();
                this.videoplayer = null
            }
            if (this.facebookShare) {
                this.facebookShare.dispose();
                this.facebookShare = null
            }
            if (this.twitterShare) {
                this.twitterShare.dispose();
                this.twitterShare = null
            }
            if (this.linkShare) {
                this.linkShare.dispose();
                this.linkShare = null
            }
            if (this.embedShare) {
                this.embedShare.dispose();
                this.embedShare = null
            }
            if (this.emailShare) {
                this.emailShare.dispose();
                this.emailShare = null
            }
            if (this.socialShare) {
                this.socialShare.dispose();
                this.socialShare = null
            }
            if (this.closedCaptionButton) {
                this.closedCaptionButton.dispose();
                this.closedCaptionButton = null
            }
            if (this.fullScreenButton) {
                this.fullScreenButton.dispose();
                this.fullScreenButton = null
            }
            if (this.mutableVolume) {
                this.mutableVolume.dispose();
                this.mutableVolume = null
            }
            if (this.videoTime) {
                this.videoTime.dispose();
                this.videoTime = null
            }
            if (this.videoScrubber) {
                this.videoScrubber.dispose();
                this.videoScrubber = null
            }
            if (this.playPauseButton) {
                this.playPauseButton.dispose();
                this.playPauseButton = null
            }
            if (this.controls) {
                this.controls.dispose();
                this.controls = null
            }
            if (this.mediaSet) {
                this.mediaSet.dispose();
                this.mediaSet = null
            }
            if (this.s7params) {
                this.s7params.dispose();
                this.s7params = null
            }
            if (this.container) {
                var e = [s7viewers.VideoViewer.cssClassName, "s7touchinput", "s7mouseinput", "s7size_large", "s7size_small", "s7size_medium"];
                var c = document.getElementById(this.containerId).className.split(" ");
                for (var d = 0; d < e.length; d++) {
                    var b = c.indexOf(e[d]);
                    if (b != -1) {
                        c.splice(b, 1)
                    }
                }
                document.getElementById(this.containerId).className = c.join(" ");
                this.container.dispose();
                this.container = null
            }
            this.params = {};
            this.handlers = [];
            this.isDisposed = true
        }
        ;
        s7viewers.VideoViewer.prototype.updateOrientationMarkers = function() {
            if (!this.isOrientationMarkerForcedChanged) {
                var b;
                if (window.innerWidth > window.innerHeight) {
                    b = "s7device_landscape"
                } else {
                    b = "s7device_portrait"
                }
                if (document.getElementById(this.containerId).className.indexOf(b) == -1) {
                    this.setNewOrientationMarker(this.containerId, b);
                    this.reloadInnerComponents()
                }
            }
        }
        ;
        s7viewers.VideoViewer.prototype.setNewOrientationMarker = function(f, c) {
            var b = document.getElementById(f).className;
            var d = /^(.*)(s7device_landscape|s7device_portrait)(.*)$/gi;
            var e;
            if (b.match(d)) {
                e = b.replace(d, "$1" + c + "$3")
            } else {
                e = b + " " + c
            }
            if (b != e) {
                document.getElementById(f).className = e
            }
        }
        ;
        s7viewers.VideoViewer.prototype.forceDeviceOrientationMarker = function(b) {
            switch (b) {
            case "s7device_portrait":
            case "s7device_landscape":
                this.isOrientationMarkerForcedChanged = true;
                if (this.containerId) {
                    this.setNewOrientationMarker(this.containerId, b)
                }
                this.reloadInnerComponents();
                break;
            case null:
                this.isOrientationMarkerForcedChanged = false;
                this.updateOrientationMarkers();
                break;
            default:
                break
            }
        }
        ;
        s7viewers.VideoViewer.prototype.getURLParameter = function(b) {
            return decodeURIComponent((new RegExp("[?|&]" + b + "=([^&;]+?)(&|#|;|$)","gi").exec(location.search) || [, ""])[1].replace(/\+/g, "%20")) || null
        }
        ;
        s7viewers.VideoViewer.prototype.addClass = function(d, c) {
            var b = document.getElementById(d).className.split(" ");
            if (b.indexOf(c) == -1) {
                b[b.length] = c;
                document.getElementById(d).className = b.join(" ")
            }
        }
    }
    )()
}
;
