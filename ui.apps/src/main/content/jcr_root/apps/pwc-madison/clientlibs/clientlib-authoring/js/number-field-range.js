(function (document, $) {
  "use strict";
  $(document).on('blur focusout change','coral-numberinput[name="./jcr:content/metadata/pwc-sortOrder"]', function (event) {
    var numberValue, min, max, numberField = $(event.target);
    if (numberField.length > 0) {
      numberValue = parseInt($(numberField).val(),10);
      min = parseInt($(numberField).attr('min'),10);
      max = parseInt($(numberField).attr('max'),10);

      if (numberValue < min || numberValue > max) {
            event.target.value = '';
      }
    }
  });
    
    $(document).on('blur focusout change', 'coral-numberinput[name="./jcr:content/metadata/pwc-seeAlsoMaxDisplayCount"]', function (event) {
   var numberValue, min, max, numberField = $(event.target);
   if (numberField.length > 0) {
      numberValue = parseInt($(numberField).val(), 10);
      min = parseInt($(numberField).attr('min'), 10);
      max = parseInt($(numberField).attr('max'), 10);

      if (numberValue < min || numberValue > max) {
         event.target.value = '';
      }
   }
});

}(document, Granite.$));
