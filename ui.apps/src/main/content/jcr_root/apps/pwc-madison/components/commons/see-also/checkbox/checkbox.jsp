<%--
  Custome checkbox to render see also in tabular format.
--%><%
%><%@ include file="/libs/granite/ui/global.jsp" %><%
%><%@ page session="false"
          import="java.util.UUID,
                  org.apache.commons.lang3.StringUtils,
                  com.adobe.granite.ui.components.AttrBuilder,
                  com.adobe.granite.ui.components.Config,
                  com.adobe.granite.ui.components.Field,
                  com.adobe.granite.ui.components.Tag" %><%--###
Checkbox
========

.. granite:servercomponent:: /libs/granite/ui/components/coral/foundation/form/checkbox
   :supertype: /libs/granite/ui/components/coral/foundation/form/field

   A checkbox component.

   It extends :granite:servercomponent:`Field </libs/granite/ui/components/coral/foundation/form/field>` component.

   It has the following content structure:

   .. gnd:gnd::

      [granite:FormCheckbox] > granite:commonAttrs, granite:renderCondition

      /**
       * The name that identifies the field when submitting the form.
       */
      - name (String)

      /**
       * ``true`` to generate the `SlingPostServlet @Delete <http://sling.apache.org/documentation/bundles/manipulating-content-the-slingpostservlet-servlets-post.html#delete>`_ hidden input based on the name.
       */
      - deleteHint (Boolean) = true

      /**
       * The value of the field.
       *
       * It is RECOMMENDED that this property is set.
       */
      - value (String)

      /**
       * The submit value of the field when it is unchecked.
       *
       * It is RECOMMENDED that this property is set.
       */
      - uncheckedValue (String)

      /**
       * Indicates if the field is in disabled state.
       */
      - disabled (Boolean)

      /**
       * Indicates if the field is mandatory to be filled.
       */
      - required (Boolean)

      /**
       * The name of the validator to be applied. E.g. ``foundation.jcr.name``.
       * See :doc:`validation </jcr_root/libs/granite/ui/components/coral/foundation/clientlibs/foundation/js/validation/index>` in Granite UI.
       */
      - validation (String) multiple

      /**
       * ``true`` to pre-check this field, ``false`` otherwise.
       */
      - checked (BooleanEL)

      /**
       * If ``false``, the checked state is based on matching the form values by ``name`` and ``value`` properties.
       * Otherwise, the form values are ignored, and the checked state is based on ``checked`` property specified.
       */
      - ignoreData (Boolean)

      /**
       * The text of the checkbox.
       */
      - text (String) i18n

      /**
       * ``true`` to automatically submit the form when the checkbox is checked/unchecked.
       *
       * Pressing "enter" in the text field will submit the form (when everything is configured properly). This is the equivalence of that for checkbox.
       */
      - autosubmit (Boolean)

      /**
       * The description of the component.
       */
      - fieldDescription (String) i18n

      /**
       * The position of the tooltip relative to the field. Only used when fieldDescription is set.
       */
      - tooltipPosition (String) = 'right' < 'left', 'right', 'top', 'bottom'

      /**
       * Renders the read-only markup as well.
       *
       * .. warning:: The read-only mode is deprecated.
       */
      - renderReadOnly (Boolean)

      /**
       * Shows read-only version even when it is unchecked.
       */
      - showEmptyInReadOnly (Boolean)

      /**
       * The class for the wrapper element.
       */
      - wrapperClass (String)
###--%><%

    if (!cmp.getRenderCondition(resource, false).check()) {
        return;
    }

    Config cfg = cmp.getConfig();

    Field field = new Field(cfg);
    boolean mixed = field.isMixed(cmp.getValue());

    String name = cfg.get("name", String.class);
    String value = cfg.get("value", String.class);
    String uncheckedValue = cfg.get("uncheckedValue", String.class);
    boolean disabled = cfg.get("disabled", false);
    String text = i18n.getVar(cfg.get("text", String.class));
    String fieldDesc = cfg.get("fieldDescription", String.class);
    String descriptionId = "description_" + UUID.randomUUID().toString();
	String title = text.substring(0, StringUtils.ordinalIndexOf(text, "|", 1));
	String contentId = text.substring(StringUtils.ordinalIndexOf(text, "|", 1) + 1, StringUtils.ordinalIndexOf(text, "|", 2));
	String ditaPath = text.substring(StringUtils.ordinalIndexOf(text, "|", 2) + 1, StringUtils.ordinalIndexOf(text, "|", 3));
	String publicationDate = text.substring(StringUtils.ordinalIndexOf(text, "|", 3) + 1);

    boolean required = cfg.get("required", false);

    boolean checked = cmp.getValue().isSelected(value, cmp.getExpressionHelper().getBoolean(cfg.get("checked", String.valueOf(false))));

    Tag tag = cmp.consumeTag();
    AttrBuilder attrs = tag.getAttrs();
    cmp.populateCommonAttrs(attrs);

    attrs.add("name", name);
    attrs.add("value", value);
    attrs.addDisabled(disabled);
    attrs.addChecked(checked);

    if (StringUtils.isBlank(text) && StringUtils.isNotBlank(fieldDesc)) {
        attrs.add("labelledby", descriptionId);
    }

    if (cfg.get("autosubmit", false)) {
        attrs.addClass("foundation-field-autosubmit");
    }

    attrs.addBoolean("required", required);

    String validation = StringUtils.join(cfg.get("validation", new String[0]), " ");
    attrs.add("data-foundation-validation", validation);
    attrs.add("data-validation", validation); // Compatibility

    if (mixed || cfg.get("partial", false)) {
        attrs.add("indeterminate", "");
    }

    if (mixed) {
        attrs.addClass("foundation-field-mixed");
    }

    AttrBuilder deleteAttrs = new AttrBuilder(request, xssAPI);
    deleteAttrs.addClass("foundation-field-related");
    deleteAttrs.add("type", "hidden");
    deleteAttrs.addDisabled(disabled);

    AttrBuilder defaultValueAttrs = new AttrBuilder(request, xssAPI);
    defaultValueAttrs.addClass("foundation-field-related");
    defaultValueAttrs.add("type", "hidden");
    defaultValueAttrs.addDisabled(disabled);
    defaultValueAttrs.add("value", uncheckedValue);

    AttrBuilder defaultValueWhenMissingAttrs = new AttrBuilder(request, xssAPI);
    defaultValueWhenMissingAttrs.addClass("foundation-field-related");
    defaultValueWhenMissingAttrs.add("type", "hidden");
    defaultValueWhenMissingAttrs.addDisabled(disabled);
    defaultValueWhenMissingAttrs.add("value", true);

    if (!StringUtils.isBlank(name)) {
        deleteAttrs.add("name", name + "@Delete");
        defaultValueAttrs.add("name", name + "@DefaultValue");
        defaultValueWhenMissingAttrs.add("name", name + "@UseDefaultWhenMissing");
    }

    if (cfg.get("renderReadOnly", false)) {
    	attrs.addClass("coral-Form-field");

        %><div class="foundation-field-editable"><%
            if (!mixed && (checked || cfg.get("showEmptyInReadOnly", false))) {
                AttrBuilder roAttrs = new AttrBuilder(request, xssAPI);
                roAttrs.addClass("coral-Form-field");
                roAttrs.addDisabled(true);
                roAttrs.addChecked(checked);

                AttrBuilder wrapperAttrs = new AttrBuilder(request, xssAPI);
                wrapperAttrs.addClass("foundation-field-readonly coral-Form-fieldwrapper coral-Form-fieldwrapper--singleline");
                wrapperAttrs.addClass(cfg.get("wrapperClass", String.class));

                %><div <%= wrapperAttrs %>>
                    <coral-checkbox <%= roAttrs.build() %>>
                        <coral-checkbox-label>
                            <div class="see-also-row">
                                <span class="see-also-column"><%= xssAPI.encodeForHTML(title) %></span>
                                <span class="see-also-column"><%= xssAPI.encodeForHTML(contentId) %></span>
                                <span class="see-also-column"><%= xssAPI.encodeForHTML(ditaPath) %></span>
                                <span class="see-also-column"><%= xssAPI.encodeForHTML(publicationDate) %></span>
                            </div>
                        </coral-checkbox-label>
                    </coral-checkbox><%

                    if (fieldDesc != null) {
                        %><coral-icon class="coral-Form-fieldinfo" icon="infoCircle" tabindex="0" aria-describedby="<%= xssAPI.encodeForHTMLAttr(descriptionId) %>" alt="<%= xssAPI.encodeForHTMLAttr(i18n.get("description")) %>"></coral-icon>
                        <coral-tooltip target="_prev" placement="<%= xssAPI.encodeForHTMLAttr(cfg.get("tooltipPosition", "right")) %>" id="<%= xssAPI.encodeForHTMLAttr(descriptionId) %>">
                            <coral-tooltip-content><%= outVar(xssAPI, i18n, fieldDesc) %></coral-tooltip-content>
                        </coral-tooltip><%
                    }
                %></div><%
            }

            AttrBuilder wrapperAttrs = new AttrBuilder(request, xssAPI);
            wrapperAttrs.addClass("foundation-field-edit coral-Form-fieldwrapper coral-Form-fieldwrapper--singleline");
            wrapperAttrs.addClass(cfg.get("wrapperClass", String.class));

            %><div <%= wrapperAttrs %>>
	            <coral-checkbox <%= attrs.build() %>>
                    <coral-checkbox-label><coral-checkbox-label>
                            <div class="see-also-row">
                                <span class="see-also-column"><%= xssAPI.encodeForHTML(title) %></span>
                                <span class="see-also-column"><%= xssAPI.encodeForHTML(contentId) %></span>
                                <span class="see-also-column"><%= xssAPI.encodeForHTML(ditaPath) %></span>
                                <span class="see-also-column"><%= xssAPI.encodeForHTML(publicationDate) %></span>
                            </div>
                        </coral-checkbox-label><%= required ? " *" : "" %></coral-checkbox-label>
                </coral-checkbox><%

	            if (cfg.get("deleteHint", true)) {
	                %><input <%= deleteAttrs.build() %>><%
	            }

                if (uncheckedValue != null) {
                    %><input <%= defaultValueAttrs.build() %>><%
                    %><input <%= defaultValueWhenMissingAttrs.build() %>><%
                }

                if (fieldDesc != null) {
                    %><coral-icon class="coral-Form-fieldinfo" icon="infoCircle" tabindex="0" aria-describedby="<%= xssAPI.encodeForHTMLAttr(descriptionId)%>" alt="<%= xssAPI.encodeForHTMLAttr(i18n.get("description")) %>"></coral-icon>
                    <coral-tooltip target="_prev" placement="<%= xssAPI.encodeForHTMLAttr(cfg.get("tooltipPosition", "right")) %>" id="<%= xssAPI.encodeForHTMLAttr(descriptionId)%>">
                        <coral-tooltip-content><%= outVar(xssAPI, i18n, fieldDesc) %></coral-tooltip-content>
                    </coral-tooltip><%
                }
            %></div>
        </div><%
    } else {
        boolean renderWrapper = false;

        if (cmp.getOptions().rootField()) {
            attrs.addClass("coral-Form-field");

            renderWrapper = fieldDesc != null;
        }

        if (renderWrapper) {
            AttrBuilder wrapperAttrs = new AttrBuilder(request, xssAPI);
            wrapperAttrs.addClass("coral-Form-fieldwrapper coral-Form-fieldwrapper--singleline");
            wrapperAttrs.addClass(cfg.get("wrapperClass", String.class));

            %><div <%= wrapperAttrs %>><%
        }

        %><coral-checkbox <%= attrs.build() %>>
            <coral-checkbox-label><coral-checkbox-label>
                            <div class="see-also-row">
                                <span class="see-also-column"><%= xssAPI.encodeForHTML(title) %></span>
                                <span class="see-also-column"><%= xssAPI.encodeForHTML(contentId) %></span>
                                <span class="see-also-column"><%= xssAPI.encodeForHTML(ditaPath) %></span>
                                <span class="see-also-column"><%= xssAPI.encodeForHTML(publicationDate) %></span>
                            </div>
                        </coral-checkbox-label><%= required ? " *" : "" %></coral-checkbox-label>
        </coral-checkbox><%

        if (cfg.get("deleteHint", true)) {
            %><input <%= deleteAttrs.build() %>><%
        }

        if (uncheckedValue != null) {
            %><input <%= defaultValueAttrs.build() %>><%
            %><input <%= defaultValueWhenMissingAttrs.build() %>><%
        }

        if (renderWrapper) {
            if (fieldDesc != null) {
                %><coral-icon class="coral-Form-fieldinfo" icon="infoCircle" tabindex="0" aria-describedby="<%= xssAPI.encodeForHTMLAttr(descriptionId) %>" alt="<%= xssAPI.encodeForHTMLAttr(i18n.get("description")) %>"></coral-icon>
                <coral-tooltip target="_prev" placement="<%= xssAPI.encodeForHTMLAttr(cfg.get("tooltipPosition", "right")) %>" id="<%= xssAPI.encodeForHTMLAttr(descriptionId) %>">
                    <coral-tooltip-content><%= outVar(xssAPI, i18n, fieldDesc) %></coral-tooltip-content>
                </coral-tooltip><%
            }
            %></div><%
        }
    }
%>