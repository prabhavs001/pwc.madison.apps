package com.pwc.madison.core.models;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Vijay
 */

public class BodyCalloutListPojo {

    private List<BodyCalloutPojo> bodyCalloutItems = new ArrayList<>();

	public List<BodyCalloutPojo> getBodyCalloutItems() {
		return bodyCalloutItems;
	}

	public int getSize() { return bodyCalloutItems.size(); };

	public void setBodyCalloutItems(List<BodyCalloutPojo> bodyCalloutItems) {
		this.bodyCalloutItems = bodyCalloutItems;
	}
}
