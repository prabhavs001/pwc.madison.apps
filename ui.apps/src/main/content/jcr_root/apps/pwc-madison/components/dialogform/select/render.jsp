<%@include file="/libs/granite/ui/global.jsp" %><%
%><%@page session="false"
          import="java.text.Collator,
                  java.util.Collections,
                  java.util.Comparator,
                  java.util.Iterator,
                  java.util.List,
                  javax.servlet.jsp.JspWriter,
                  org.apache.commons.collections4.IteratorUtils,
                  org.apache.commons.lang3.StringUtils,
                  com.adobe.granite.ui.components.AttrBuilder,
                  com.adobe.granite.ui.components.Config,
                  com.adobe.granite.ui.components.ComponentHelper,
                  com.adobe.granite.ui.components.Field,
                  com.pwc.madison.core.util.TemplateRenderConditionUtil,
                  com.adobe.granite.ui.components.Tag" %><%--###
Select
======

.. granite:servercomponent:: /apps/pwc-madison/components/dialogform/select
   :supertype: /libs/granite/ui/components/coral/foundation/form/select

   Select is a component to represent a concept of selection of some options.

   It extends :granite:servercomponent:`Field </libs/granite/ui/components/coral/foundation/form/select>` component.

   It has the following content structure apart from [granite:FormSelect]:

      /**
       * Disable the field for the defined templates
       */
      - disabledForTemplates (String [])

      /**
       * When disabledForTemplates property is defined as well as satisfied then set the defined value as default value
       */
      - disabledDefaultValue (String)

###--%><%

    Config cfg = cmp.getConfig();
    ValueMap vm = (ValueMap) request.getAttribute(Field.class.getName());

    String name = cfg.get("name", String.class);
    boolean disabled = cfg.get("disabled", false);

    String[] disabledForTemplates = cfg.get("disabledForTemplates", String[].class);
    isDisabled = TemplateRenderConditionUtil.isTemplate(slingRequest, request, disabledForTemplates);

    disabledDefaultValue = cfg.get("disabledDefaultValue", String.class);

    if(isDisabled)
        disabled = true;

    Iterator<Resource> itemIterator = cmp.getItemDataSource().iterator();

    if (cfg.get("ordered", false)) {
        List<Resource> items = IteratorUtils.toList(itemIterator);
        final Collator langCollator = Collator.getInstance(request.getLocale());

        Collections.sort(items, new Comparator<Resource>() {
            public int compare(Resource o1, Resource o2) {
                return langCollator.compare(getOptionText(o1, cmp), getOptionText(o2, cmp));
            }
        });

        itemIterator = items.iterator();
    }

    Tag tag = cmp.consumeTag();
    AttrBuilder attrs = tag.getAttrs();
    cmp.populateCommonAttrs(attrs);

    attrs.add("name", name);
    attrs.addMultiple(cfg.get("multiple", false));
    attrs.addDisabled(disabled);
    attrs.add("placeholder", i18n.getVar(cfg.get("emptyText", String.class)));
    attrs.addBoolean("required", cfg.get("required", false));
    attrs.add("variant", cfg.get("variant", String.class));

    String fieldLabel = cfg.get("fieldLabel", String.class);
    String fieldDesc = cfg.get("fieldDescription", String.class);
    String labelledBy = null;

    if (fieldLabel != null && fieldDesc != null) {
        labelledBy = vm.get("labelId", String.class) + " " + vm.get("descriptionId", String.class);
    } else if (fieldLabel != null) {
        labelledBy = vm.get("labelId", String.class);
    } else if (fieldDesc != null) {
        labelledBy = vm.get("descriptionId", String.class);
    }

    if (StringUtils.isNotBlank(labelledBy)) {
        attrs.add("labelledby", labelledBy);
    }

    String validation = StringUtils.join(cfg.get("validation", new String[0]), " ");
    attrs.add("data-foundation-validation", validation);
    attrs.add("data-validation", validation); // Compatibility

%><coral-select <%= attrs.build() %>><%
    if (cfg.get("emptyOption", false)) {
        String value = "";

        AttrBuilder opAttrs = new AttrBuilder(null, xssAPI);
        opAttrs.add("value", value);
        opAttrs.addSelected(cmp.getValue().isSelected(value, false));

        out.println("<coral-select-item " + opAttrs.build() + "></coral-select-item>");
    }

    for (Iterator<Resource> items = itemIterator; items.hasNext();) {
        printOption(out, items.next(), cmp);
    }

    if (!StringUtils.isBlank(name) && cfg.get("deleteHint", true)) {
        AttrBuilder deleteAttrs = new AttrBuilder(request, xssAPI);
        deleteAttrs.addClass("foundation-field-related");
        deleteAttrs.add("type", "hidden");
        deleteAttrs.add("name", name + "@Delete");
        deleteAttrs.addDisabled(disabled);

        %><input <%= deleteAttrs %>><%
    }
%></coral-select><%!

    boolean isDisabled;
    String disabledDefaultValue;

    private void printOption(JspWriter out, Resource option, ComponentHelper cmp) throws Exception {
        if (!cmp.getRenderCondition(option, false).check()) {
            return;
        }
        I18n i18n = cmp.getI18n();
        XSSAPI xss = cmp.getXss();

        Config optionCfg = new Config(option);
        String value = cmp.getExpressionHelper().getString(optionCfg.get("value", String.class));

        AttrBuilder opAttrs = new AttrBuilder(null, cmp.getXss());
        cmp.populateCommonAttrs(opAttrs, option);

        opAttrs.add("value", value);
        opAttrs.addDisabled(optionCfg.get("disabled", false));

        // if the item is an optgroup, render the <optgroup> and all its containing items
        if (optionCfg.get("group", false)) {
            opAttrs.add("label", i18n.getVar(optionCfg.get("text", String.class)));

            out.println("<coral-select-group> " + opAttrs.build() + ">");
            for (Iterator<Resource> options = option.listChildren(); options.hasNext();) {
                printOption(out, options.next(), cmp);
            }
            out.println("</coral-select-group>");
        } else {
            // otherwise, render the <option>

            //check if the field is disabled for the current page (due to disabledForTemplates property)
            if(isDisabled){
                opAttrs.addSelected(cmp.getValue().isSelected(value, value.equals(disabledDefaultValue)));
            } else {
                opAttrs.addSelected(cmp.getValue().isSelected(value, optionCfg.get("selected", false)));
            }

            out.print("<coral-select-item " + opAttrs.build() + ">");

            printIcon(out, optionCfg.get("icon", String.class), cmp);
            // we print it first due to float right
            printStatusIcon(out, optionCfg, cmp);
            out.print(xss.encodeForHTML(getOptionText(option, cmp)));

            out.println("</coral-select-item>");
        }
    }

    private void printIcon(JspWriter out, String icon, ComponentHelper cmp) throws Exception {
        if (icon == null || "".equals(icon)) return;

        AttrBuilder attrs = new AttrBuilder(null, cmp.getXss());
        attrs.add("icon", icon);

        out.print("<coral-icon " + attrs + "></coral-icon>");
    }

    private void printStatusIcon(JspWriter out, Config cfg, ComponentHelper cmp) throws Exception {
        String icon = cfg.get("statusIcon", "");

        if ("".equals(icon)) return;

        AttrBuilder attrs = new AttrBuilder(null, cmp.getXss());
        attrs.add("icon", icon);
        attrs.add("aria-label", cfg.get("statusText", String.class));

        attrs.addClass("granite-Select-statusIcon");

        String statusVariant = cfg.get("statusVariant", "");
        if (!"".equals(statusVariant)) {
          attrs.addClass("granite-Select-statusIcon--" + statusVariant);
        }

        out.print("<coral-icon " + attrs + "></coral-icon>");
    }

    private String getOptionText(Resource option, ComponentHelper cmp) {
        Config optionCfg = new Config(option);
        String text = optionCfg.get("text", "");

        if (cmp.getConfig().get("translateOptions", true)) {
            text = cmp.getI18n().getVar(text);
        }

        return text;
    }
%>