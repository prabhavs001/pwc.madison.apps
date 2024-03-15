package com.pwc.madison.core.models;

import java.util.List;

public class StandardSetterBean {

    private List<String> fullCycleWorkflowSetters;
    private List<String> simpleWorkflowSetters;

    public List<String> getFullCycleWorkflowSetters() {
        return fullCycleWorkflowSetters;
    }

    public void setFullCycleWorkflowSetters(List<String> fullCycleWorkflowSetters) {
        this.fullCycleWorkflowSetters = fullCycleWorkflowSetters;
    }

    public List<String> getSimpleWorkflowSetters() {
        return simpleWorkflowSetters;
    }

    public void setSimpleWorkflowSetters(List<String> simpleWorkflowSetters) {
        this.simpleWorkflowSetters = simpleWorkflowSetters;
    }
}
