package com.pwc.madison.core.services;

import com.day.cq.wcm.api.Page;

/**
 * The interface BreadcrumJson provides json as string of parent pages hierarchy of a given page
 */
public interface BreadcrumbJson {

    String getParentJsonHeirarchyForPagePath(Page page);
}
