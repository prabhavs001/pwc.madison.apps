package com.pwc.madison.core.userreg.models.request;

import java.util.List;

public class RecentViewRequest extends UserRegRequest {

	private Long userId;
	private String territory;
	private String locale;
	private String itemPath;    
	private String createdDate;
	private String itemViewedDate;
	private String modifiedDate;
	private List<String> listOfPaths;

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public List<String> getListOfPaths() {
		return listOfPaths;
	}

	public void setListOfPaths(List<String> listOfPaths) {
		this.listOfPaths = listOfPaths;
	}

	public String getTerritory() {
		return territory;
	}

	public void setTerritory(String territory) {
		this.territory = territory;
	}

	public String getLocale() {
		return locale;
	}

	public void setLocale(String locale) {
		this.locale = locale;
	}

	public String getItemPath() {
		return itemPath;
	}

	public void setItemPath(String itemPath) {
		this.itemPath = itemPath;
	}

	public String getCreatedDate() {
		return createdDate;
	}

	public void setCreatedDate(String createdDate) {
		this.createdDate = createdDate;
	}

	public String getItemViewedDate() {
		return itemViewedDate;
	}

	public void setItemViewedDate(String itemViewedDate) {
		this.itemViewedDate = itemViewedDate;
	}

	public String getModifiedDate() {
		return modifiedDate;
	}

	public void setModifiedDate(String modifiedDate) {
		this.modifiedDate = modifiedDate;
	}

	@Override
	public String toString() {
		return "RecentViewRequest [userId=" + userId + ", territory=" + territory + ", locale=" + locale + ", itemPath="
				+ itemPath + ", createdDate=" + createdDate + ", itemViewedDate=" + itemViewedDate + ", modifiedDate="
				+ modifiedDate + "]";
	}
}
