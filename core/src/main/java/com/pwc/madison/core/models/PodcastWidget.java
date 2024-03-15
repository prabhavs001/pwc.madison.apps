package com.pwc.madison.core.models;

public class PodcastWidget {

    private String name;
    private String imgPath;
    private String url;

    public PodcastWidget(final String name, final String imgPath, final String url) {
        super();
        this.name = name;
        this.imgPath = imgPath;
        this.url = url;
    }

    public PodcastWidget() {
    }

    public String getName() {
        return name;
    }

    public String getImgPath() {
        return imgPath;
    }

    public String getUrl() {
        return url;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public void setImgPath(final String imgPath) {
        this.imgPath = imgPath;
    }

    public void setUrl(final String url) {
        this.url = url;
    }

}
