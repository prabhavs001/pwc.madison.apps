package com.pwc.madison.core.models;

import com.pwc.madison.core.models.impl.Link;

public interface PreviousAndNextLinkModel {

  public Link getNextPage();

  public Link getPrevPage();

}
