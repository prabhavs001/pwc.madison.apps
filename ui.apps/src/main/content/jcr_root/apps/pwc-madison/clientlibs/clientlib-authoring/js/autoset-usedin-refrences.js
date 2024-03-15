(function (document, $) {
   "use strict";
   $(document).on("foundation-contentloaded", function () {
      var autoSetReferenncesButton, currentPublishingPointPath, _ui = $(window).adaptTo("foundation-ui");
      currentPublishingPointPath = $('form').attr('data-formid');
      autoSetReferenncesButton = $("#autoSetReferences");
      try {
         function autoSetSuccess() {
            autoSetReferenncesButton.attr('disabled', '');
            _ui.notify(Granite.I18n.get("Success"), Granite.I18n.get("Default Used in references are set"), "success");
         }

         function autoSetFail() {
            autoSetReferenncesButton.attr('enabled', '');
            _ui.notify(Granite.I18n.get("Fail"), Granite.I18n.get("Error while auto setting references"), "fail");
         }

         autoSetReferenncesButton.on('click', function () {
            var refCallAjax;
            if (currentPublishingPointPath !== undefined) {
               refCallAjax = $.post("/bin/pwc-madison/autoSetUsedInReferences", {
                     "item": currentPublishingPointPath
                  }, autoSetSuccess(), 'text')
                  .fail(function () {
                     autoSetFail();
                  });
            }
         });

      } catch (error) {}

   });

}(document, Granite.$));