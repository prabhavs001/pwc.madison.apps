$.validator.register({
  selector: '[data-validation=root-path-validator]',
  validate: function(el) {
    var field,
        value,
        rootPath;

    field = el;
    value = el.val();

    rootPath = el.closest(".coral-Form-field").data("root-path");
    if(!rootPath){
          rootPath="/content/dam";
    }

    if (!value.startsWith(rootPath)) {
      return Granite.I18n.get('The field must start with '+rootPath);
    }
  },
  show: function (el, message) {
    var fieldErrorEl,
        field,
        error,
        arrow;

    fieldErrorEl = $("<span class='coral-Form-fielderror coral-Icon coral-Icon--alert coral-Icon--sizeS' data-init='quicktip' data-quicktip-type='error' />");
    field = el;

    field.attr("aria-invalid", "true")
      .toggleClass("is-invalid", true);

    error = field.closest(".coral-Form-field").nextAll(".coral-Form-fielderror");

    if (error.length === 0) {
      arrow = field.closest("form").hasClass("coral-Form--vertical") ? "right" : "top";

      fieldErrorEl
        .attr("data-quicktip-arrow", arrow)
        .attr("data-quicktip-content", message)
        .insertAfter(field.closest(".coral-Form-field"));
    } else {
      error.data("quicktipContent", message);
    }
  },
  clear: function (el) {
    var field = el;

    field.removeAttr("aria-invalid").removeClass("is-invalid");

    field.closest(".coral-Form-field").nextAll(".coral-Form-fielderror").tooltip("hide").remove();
  }
});