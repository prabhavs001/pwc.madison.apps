package com.pwc.madison.core.models;

import java.util.List;

public interface RelatedContentLinks {

	List<RelatedContent> getRelatedContents();

	String getItemType();

	String getPwcSourceValue();
}
