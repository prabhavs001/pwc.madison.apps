(function (document, $, Coral) {

   $(document).on('dialog-ready', function(e) {
      $(".global-navigation-group .coral-Form-fieldwrapper input[type^='text'].coral-Form-field.coral3-Textfield").each(function(index, e) {
          $(e).parents('.coral-Collapsible-content:first').siblings('.coral-Collapsible-header').find('.coral-Collapsible-title').text($(e).val());
      });

   });

   $(document).on('foundation-contentloaded', function(e) {
       $(".global-navigation-group .coral-Form-fieldwrapper input[type^='text'].coral-Form-field.coral3-Textfield").keyup(function(index, e) {
          $(this).each(function(index, e) {
             $(e).parents('.coral-Collapsible-content:first').siblings('.coral-Collapsible-header').find('.coral-Collapsible-title').text($(e).val());
          });
       });
   });
}(document, Granite.$, Coral));