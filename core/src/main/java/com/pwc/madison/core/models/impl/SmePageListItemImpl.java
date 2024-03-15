/*
 * Model class for populating the authorable SME Page Detail component fields.
 */
package com.pwc.madison.core.models.impl;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import com.pwc.madison.core.models.SmeListItem;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.day.cq.wcm.api.Page;
import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import javax.inject.Inject;

@Model(adaptables = Resource.class, adapters = SmeListItem.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class SmePageListItemImpl implements SmeListItem {

    public static final String PN_REDIRECT_TARGET = "cq:redirectTarget";

    public static final String SME_FIRST_NAME = "firstname";

    public static final String SME_LAST_NAME = "lastname";

    public static final String SME_STAFF_LEVEL = "stafflevel";

    public static final String SME_ORG = "organization";

    public static final String SME_PHONE = "phone";

    public static final String SME_EMAIL = "email";

    public static final String SME_LINKEDIN = "linkedin";

    public static final String SME_PHOTO = "photo";

    public static final String SME_BIO = "biography";

    public static final String SME_NAMED_IMAGE_TRANS_RULE = ".transform/sme-bio/image.jpg";

    @Self
    private SlingHttpServletRequest request;

    @Nonnull
    protected Page page;

    @ScriptVariable
    private ValueMap properties;

    @SlingObject
    private Resource resource;

    @Inject
    private String firstname = StringUtils.EMPTY;

    @Inject
    private String lastname;

    @Inject
    private String fullname;

    @Inject
    private String stafflevel;

    @Inject
    private String tooltipName;

    @Inject
    private String organization;

    @Inject
    private String phone;

    @Inject
    private String email;

    @Inject
    private String linkedin;

    @Inject
    private String biography;


    @PostConstruct
    private void init() {
        this.page = resource.adaptTo(Page.class);
        if (page.getContentResource() != null && page.getContentResource().getValueMap() != null) {
            this.properties = page.getContentResource().getValueMap();

        }
    }

    public String getFirstname() {
        firstname = properties.get(SME_FIRST_NAME, String.class);
        return firstname;
    }

    public String getLastname() {
        // Rules for LastName if Full Name exceeds 20 characters limit to be displayed in single line.
        lastname = properties.get(SME_LAST_NAME, String.class);
        String fname = StringUtils.join(new String[]{firstname, lastname}, " ");
        /* The UI for the SME Full Name and Last Name allows for only 20 characters before it starts to wrap.
            Adding a condition here so that we can truncate the Last Name.
         */
        if (StringUtils.length(fname) > 20) {
            lastname = StringUtils.left(lastname, 1) + ".";
        }
        return lastname;
    }

    public String getFullname() {
        fullname = StringUtils.join(new String[]{getFirstname(), properties.get(SME_LAST_NAME, String.class)}, " ");
        return fullname;
    }

    public String getTooltipName() {
        tooltipName = StringUtils.lowerCase(StringUtils.deleteWhitespace(properties.get(SME_FIRST_NAME, String.class)))
            + "-"
            + StringUtils.substringBefore(StringUtils.lowerCase(StringUtils.deleteWhitespace(properties.get(SME_LAST_NAME, String.class))), ".");
        return tooltipName;
    }

    public String getStafflevel() {
        stafflevel = properties.get(SME_STAFF_LEVEL, String.class);
        return stafflevel;
    }

    public String getOrganization() {
        organization = properties.get(SME_ORG, String.class);
        return organization;
    }

    public String getPhone() {
        phone = properties.get(SME_PHONE, String.class);
        return phone;
    }

    public String getEmail() {
        email = properties.get(SME_EMAIL, String.class);
        return email;
    }

    public String getLinkedin() {
        linkedin = properties.get(SME_LINKEDIN, String.class);
        return linkedin;
    }

    public String getPhoto() {
        String photoPath = properties.get(SME_PHOTO, String.class);
        return StringUtils.isNotEmpty(photoPath) ?  photoPath + SME_NAMED_IMAGE_TRANS_RULE : "";
    }

    public String getBiography() {
        biography = properties.get(SME_BIO, String.class);
        return biography;
    }

    public Character getFirstCharacterName(){
        Character firstchar = getFirstname().charAt(0);
        return firstchar;
    }
}