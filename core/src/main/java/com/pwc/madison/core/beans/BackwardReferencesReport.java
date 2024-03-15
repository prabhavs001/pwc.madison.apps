package com.pwc.madison.core.beans;

import java.util.List;

public class BackwardReferencesReport {

    private List<BackwardReference> backwardRefs;

    public List<BackwardReference> getBackwardRefs() {
        return backwardRefs;
    }

    public void setBackwardRefs(List<BackwardReference> backwardRefs) {
        this.backwardRefs = backwardRefs;
    }

}
