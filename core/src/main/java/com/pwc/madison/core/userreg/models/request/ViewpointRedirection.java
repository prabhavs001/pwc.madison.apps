package com.pwc.madison.core.userreg.models.request;

/**
 * 
 * Model to represent VP redirection Entry.
 *
 */
public class ViewpointRedirection {

    public ViewpointRedirection(String oldPath, String newPath, Boolean disabled, Boolean isAutomatic) {
        super();
        this.oldPath = oldPath;
        this.newPath = newPath;
        this.disabled = disabled;
        this.isAutomatic = isAutomatic;
    }

    @Override
    public String toString() {
        return "ViewpointRedirection [oldPath=" + oldPath + ", newPath=" + newPath + ", disabled=" + disabled
                + ", isAutomatic=" + isAutomatic + "]";
    }

    String oldPath;
    String newPath;
    Boolean disabled;
    Boolean isAutomatic;

    public String getOldPath() {
        return oldPath;
    }

    public void setOldPath(String oldPath) {
        this.oldPath = oldPath;
    }

    public String getNewPath() {
        return newPath;
    }

    public void setNewPath(String newPath) {
        this.newPath = newPath;
    }

    public Boolean getDisabled() {
        return disabled;
    }

    public void setDisabled(Boolean disabled) {
        this.disabled = disabled;
    }

    public Boolean getIsAutomatic() {
        return isAutomatic;
    }

    public void setIsAutomatic(Boolean isAutomatic) {
        this.isAutomatic = isAutomatic;
    }

}
