package com.pwc.madison.core.models;

public interface SearchWithinDocModel {

    /***
     * @return search url.
     */
    String getSearchURL();

    /**
     * @return publishing point search Doc Context.
     */
    String getPubPointDocContext();

    /**
     * @return title
     */
    String getTitle();
}
