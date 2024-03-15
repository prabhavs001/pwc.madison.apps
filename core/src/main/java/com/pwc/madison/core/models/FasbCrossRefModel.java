package com.pwc.madison.core.models;

import java.util.ArrayList;

public class FasbCrossRefModel {
	private int totalCount;
	private ArrayList<CrossRefResultRowModel> searchResult;
	public int getTotalCount() {
		return totalCount;
	}
	public void setTotalCount(int totalCount) {
		this.totalCount = totalCount;
	}
	public ArrayList<CrossRefResultRowModel> getSearchResult() {
		return searchResult;
	}
	public void setSearchResult(ArrayList<CrossRefResultRowModel> searchResult) {
		this.searchResult = searchResult;
	}
}
