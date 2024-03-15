/*
 * Model  class for populating the authorable footer component fields. 
 */
package com.pwc.madison.core.models;

import java.util.List;

public interface Footer {

	String COPYRIGHT_PROPERTY_NAME = "copyRight";
	String PUBLISH_YEAR_PROPERTY_NAME = "publishingYear";
	String FOOTER_MENU_LINK_PROPERTY_NAME = "footerMenuLinks";
	String FOOTER_SECOND_ROW_LINK_PROPERTY_NAME = "footerSecondRowLinks";
	String TUTORIAL_LINK_TEXT_PROPERTY_NAME = "tutorialLinkText";

	/***
	 * @return a list of MultiFieldBean for Footer Links.
	 */
	public List<FooterLink> getFooterLinks();

	/***
	 * @return copyright text.
	 */
	public String getCopyRight();

	/***
	 * @return copyright text.
	 */
	public String getCookieEnabled();

	/**
	 * @return current page territory
	 */
	public String getCurrentPageTerritory();

	//Added
	/***
	 * @return a list of MultiFieldBean for second row Footer Links.
	 */

	public List<FooterLink> getSecondRowLinks();
	//Added

	String getTutorialLinkText();
	
    /**
     * Returns the analytics component's name.
     * 
     * @return {@link String}
     */
	public String getComponentName();

}
