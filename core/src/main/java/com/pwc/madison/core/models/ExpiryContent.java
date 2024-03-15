package com.pwc.madison.core.models;

import com.day.cq.search.QueryBuilder;
import com.day.crx.JcrConstants;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.util.DITAUtils;
import com.pwc.madison.core.util.MadisonUtil;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.settings.SlingSettingsService;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.Date;
import java.util.Map;

/**
 * model for expiration content
 */
@Model(adaptables = Resource.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class ExpiryContent implements Comparable<ExpiryContent> {

    @SlingObject
    private ResourceResolver resourceResolver;

    @SlingObject
    private Resource currentResource;

    @Inject
    SlingSettingsService slingSettingsService;

    @OSGiService
    private QueryBuilder queryBuilder;

    @Inject
    @Named(DITAConstants.PROPERTY_TITLE)
    private String title;

    private String author;

    @Inject
    @Named(DITAConstants.META_EXPIRY_DATE)
    private Date expiration;

    @Inject
    @Named(DITAConstants.META_EXPIRY_DATE)
    private String expirationString;

    @Inject
    @Named(DITAConstants.META_PUBLICATION_DATE)
    private String publication;

    @Inject
    @Named(DITAConstants.META_EFFECTIVE_AS_OF_DATE)
    private String effective;

    @Inject
    @Named(JcrConstants.JCR_LASTMODIFIED)
    private Date lastModified;

    @Inject
    @Named(DITAConstants.META_STANDARD_SETTERS)
    private String issuingBody;

    @Inject
    @Named(DITAConstants.META_CONTENT_TYPE)
    private String contentType;

    private boolean expired;

    @Inject
    @Named(DITAConstants.META_EXPIRATION_CONFIRMATION_STATUS)
    private String confirmationState;

    private String expirationDate;

    private String publicationDate;

    private String effectiveDate;

    private String lastModifiedDate;

    private String path;

    private String ditaAssetDetailsPath;

    private Map<String, String> ditaMapPath;

    private String externalDitaMapPath;

    private String publishingDitaMapPath;

    private String language;

    private String country;

    private boolean active;

    private String approvedRejectedBy;

    @PostConstruct
    protected void init() {
        if (null != expiration) {
            expirationDate = DITAUtils.formatDate(expirationString, MadisonConstants.COMPONENTS_DATE_FORMAT);
        }
        if (null != publication) {
            publicationDate = DITAUtils.formatDate(publication, MadisonConstants.COMPONENTS_DATE_FORMAT);
        }
        if (null != effective) {
            effectiveDate = DITAUtils.formatDate(effective, MadisonConstants.COMPONENTS_DATE_FORMAT);
        }
        if (null != lastModified) {
            lastModifiedDate = MadisonUtil.getDate(lastModified, MadisonConstants.COMPONENTS_DATE_FORMAT);
        }
        if (null != expiration) {
            expired = (expiration.compareTo(new Date())) < 0 ? true : false;
        }
        //** traversing up to dita node from metadata node */
        if (null != currentResource.getParent() && null != currentResource.getParent().getParent()) {
            path = currentResource.getParent().getParent().getPath();
            ditaAssetDetailsPath = MadisonConstants.DITA_EDITOR_PATH + path;
            country = MadisonUtil.getTerritoryCodeForPath(path);
            language = MadisonUtil.getLanguageCodeForPath(path);
        }
    }




    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(String expirationDate) {
        this.expirationDate = expirationDate;
    }

    public String getPublicationDate() {
        return publicationDate;
    }

    public void setPublicationDate(String publicationDate) {
        this.publicationDate = publicationDate;
    }

    public String getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(String effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public String getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(String lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    public String getIssuingBody() {
        return issuingBody;
    }

    public void setIssuingBody(String issuingBody) {
        this.issuingBody = issuingBody;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public boolean isExpired() {
        return expired;
    }

    public void setExpired(boolean expired) {
        this.expired = expired;
    }

    public String getConfirmationState() {
        return confirmationState;
    }

    public void setConfirmationState(String confirmationState) {
        this.confirmationState = confirmationState;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Map<String, String> getDitaMapPath() {
        return ditaMapPath;
    }

    public void setDitaMapPath(Map<String, String> ditaMapPath) {
        this.ditaMapPath = ditaMapPath;
    }

    public String getPublishingDitaMapPath() {
        return publishingDitaMapPath;
    }

    public void setPublishingDitaMapPath(String publishingDitaMapPath) {
        this.publishingDitaMapPath = publishingDitaMapPath;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getDitaAssetDetailsPath() {
        return ditaAssetDetailsPath;
    }

    public void setDitaAssetDetailsPath(String ditaAssetDetailsPath) {
        this.ditaAssetDetailsPath = ditaAssetDetailsPath;
    }

    public String getExternalDitaMapPath() {
        return externalDitaMapPath;
    }

    public void setExternalDitaMapPath(String externalDitaMapPath) {
        this.externalDitaMapPath = externalDitaMapPath;
    }

    public Date getExpiration() {
        return expiration;
    }

    public String getApprovedRejectedBy() {
        return approvedRejectedBy;
    }

    public void setApprovedRejectedBy(String approvedRejectedBy) {
        this.approvedRejectedBy = approvedRejectedBy;
    }

    /**
     * Method to compare objects with same type
     *
     * @param expiryContent the object to be compared.
     * @return a negative integer, zero, or a positive integer as this object
     * is less than, equal to, or greater than the specified object.
     * @throws NullPointerException if the specified object is null
     * @throws ClassCastException   if the specified object's type prevents it
     *                              from being compared to this object.
     */
    @Override
    public int compareTo(ExpiryContent expiryContent) {
        if(null == expiration || null == expiryContent.expiration){
            return 0;
        }
        return expiryContent.expiration.compareTo(this.expiration);
    }

}
