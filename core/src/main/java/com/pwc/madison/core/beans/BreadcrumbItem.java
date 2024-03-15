package com.pwc.madison.core.beans;

/**
 * Bean to set properties of page object into a BreadcrumbItem object
 */
public class BreadcrumbItem implements Comparable{

    private String title;
    private String href;
    private String alt;
    private Integer order;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }

    public String getAlt() {
        return alt;
    }

    public void setAlt(String alt) {
        this.alt = alt;
    }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    @Override
    public int compareTo(Object o) {
        int compareOrder = ((BreadcrumbItem)o).getOrder();
        return this.getOrder()-compareOrder;
    }

}