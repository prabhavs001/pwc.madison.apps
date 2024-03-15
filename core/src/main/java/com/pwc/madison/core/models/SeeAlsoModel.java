package com.pwc.madison.core.models;

import java.util.List;

import com.pwc.madison.core.beans.UsedInReferences;

public interface  SeeAlsoModel {

	boolean isSeeAlsoEnabledAtPublishingPoint();

	List<UsedInReferences> getAllPageReferencesList();

	List<UsedInReferences> getExternalReferencesList();

}
