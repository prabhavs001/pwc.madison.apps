package com.pwc.madison.core.userreg.models.request;

import java.util.List;

/**
 *
 * Model to represent data to send to UserReg Rest edit content preferences request API.
 *
 */
public class EditContentPreferencesRequest extends UserRegRequest {

    private List<String> preferredGaas;
    private List<String> preferredGaap;
    private List<String> preferredTopic;
    private List<String> preferredIndustry;

    public List<String> getPreferredGaas() {
        return preferredGaas;
    }

    public void setPreferredGaas(final List<String> preferredGaas) {
        this.preferredGaas = preferredGaas;
    }

    public List<String> getPreferredGaap() {
        return preferredGaap;
    }

    public void setPreferredGaap(final List<String> preferredGaap) {
        this.preferredGaap = preferredGaap;
    }

    public List<String> getPreferredTopic() {
        return preferredTopic;
    }

    public void setPreferredTopic(final List<String> preferredTopic) {
        this.preferredTopic = preferredTopic;
    }

    public List<String> getPreferredIndustry() {
        return preferredIndustry;
    }

    public void setPreferredIndustry(final List<String> preferredIndustry) {
        this.preferredIndustry = preferredIndustry;
    }

    public EditContentPreferencesRequest(final List<String> preferredGaas, final List<String> preferredGaap,
            final List<String> preferredTopic, final List<String> preferredIndustry) {
        super();
        this.preferredGaas = preferredGaas;
        this.preferredGaap = preferredGaap;
        this.preferredTopic = preferredTopic;
        this.preferredIndustry = preferredIndustry;
    }

}
