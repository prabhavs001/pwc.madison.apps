package com.pwc.madison.core.models.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.annotation.PostConstruct;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;

import com.adobe.cq.export.json.ExporterConstants;
import com.day.cq.tagging.Tag;
import com.day.cq.tagging.TagManager;
import com.day.cq.wcm.api.Page;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.models.ContentCopyright;

/**
 * Sling model for Content Copyright component.
 */
@Model(adaptables = SlingHttpServletRequest.class, adapters = ContentCopyright.class)
@Exporter(name = ExporterConstants.SLING_MODEL_EXPORTER_NAME, extensions = ExporterConstants.SLING_MODEL_EXTENSION)

public class ContentCopyrightImpl implements ContentCopyright {

	@ScriptVariable
	private Page currentPage;

	private List<String> copyrightText = new ArrayList<>();

	@ScriptVariable
	private ResourceResolver resolver;

	/**
	 * Fetches the copyright tag references from page property and sets the
	 * copyright text from the copyright tag.
	 */
	@PostConstruct
	protected void init() {
		if (null != currentPage) {
			ValueMap pageProperties = currentPage.getProperties();
			if (pageProperties.containsKey(DITAConstants.META_COPYRIGHT)) {
				String[] copyrightReferences = pageProperties.get(DITAConstants.META_COPYRIGHT, String[].class);
				TagManager tagManager = resolver.adaptTo(TagManager.class);
				setCopyrightTextFromTag(tagManager, copyrightReferences);
			}

		}

	}

	/**
	 * Sets the copyright text from tag.
	 *
	 * @param tagManager          the tag manager
	 * @param copyrightReferences the copyright references
	 */
	private void setCopyrightTextFromTag(TagManager tagManager, String[] copyrightReferences) {
		if (tagManager != null && copyrightReferences != null && copyrightReferences.length != 0) {
			for (String copyrightReference : copyrightReferences) {
				Tag copyrightTag = tagManager.resolve(copyrightReference);
				if (null != copyrightTag) {
					Resource copyrightResource = copyrightTag.adaptTo(Resource.class);
					if (copyrightResource != null) {
						setCopyrightText(copyrightResource);
					}
				}
			}
		}

	}

	/**
	 * Sets the copyright text.
	 *
	 * @param copyrightResource the new copyright text
	 */
	private void setCopyrightText(Resource copyrightResource) {
		ValueMap tagProps = copyrightResource.adaptTo(ValueMap.class);
		if (null != tagProps) {
			Locale locale = currentPage.getLanguage();
			String languageCode = locale.getLanguage();
			String tagTitle = getLocalizedTagTitle(tagProps, languageCode);
			copyrightText.add(tagTitle);
		}
	}

	/**
	 * Gets the localized tag title.
	 *
	 * @param tagProps     the tag props
	 * @param languageCode the language code
	 * @return the localized tag title
	 */
	private String getLocalizedTagTitle(ValueMap tagProps, String languageCode) {
		if (null != tagProps.get("jcr:title." + languageCode, String.class) && !languageCode.equals("en")) {
			return tagProps.get("jcr:title." + languageCode, String.class);
		} else {
			return tagProps.get("jcr:title", String.class);
		}

	}

	/**
	 * Gets the copyright text.
	 *
	 * @return the copyright text
	 */
	@Override
	public List<String> getCopyrightText() {
		return copyrightText;
	}

}
