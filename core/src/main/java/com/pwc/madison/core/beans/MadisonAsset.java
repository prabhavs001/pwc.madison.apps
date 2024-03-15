package com.pwc.madison.core.beans;

public class MadisonAsset implements Comparable<MadisonAsset> {

    private String path;

    private String title;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public int compareTo(MadisonAsset asset) {
        return this.getTitle().compareTo(asset.getTitle());
    }

}
