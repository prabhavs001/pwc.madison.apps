package com.pwc.madison.core.models;

/**
 * Model to hold properties for inline review button
 */
public class ReviewButtonModel {

    /**
     * inline review page URL
     */
    public String reviewPage;

    /**
     * flag to render inline review button
     */
    public boolean isRender;

    /**
     * @return  review page url
     */
    public String getReviewPage() {
        return reviewPage;
    }

    /**
     * set review page url
     * @param reviewPage
     */
    public void setReviewPage(String reviewPage) {
        this.reviewPage = reviewPage;
    }

    /**
     * @return render flag for review button
     */
    public boolean isRender() {
        return isRender;
    }

    /**
     * set render flag for review button
     * @param render
     */
    public void setRender(boolean render) {
        isRender = render;
    }
}
