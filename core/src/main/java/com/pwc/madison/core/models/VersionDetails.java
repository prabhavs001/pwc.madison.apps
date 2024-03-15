package com.pwc.madison.core.models;

/**
 * POJO which holds a ditamap version details
 * Any additional requirement in the future to compare more set of properties, this model should be augmented accordingly
 */
public class VersionDetails {
    private String version;
    private String[] topicRefs;
    private String frozenNodePath;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String[] getTopicRefs() {
        return topicRefs;
    }

    public void setTopicRefs(String[] topicRefs) {
        this.topicRefs = topicRefs;
    }

    public String getFrozenNodePath() {
        return frozenNodePath;
    }

    public void setFrozenNodePath(String frozenNodePath) {
        this.frozenNodePath = frozenNodePath;
    }
}
