package com.pwc.madison.core.models.impl;

import javax.inject.Inject;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;

import com.day.cq.wcm.api.Page;
import com.pwc.madison.core.models.FavoriteListModel;
import com.pwc.madison.core.userreg.services.FavoriteListService;

@Model(adaptables = SlingHttpServletRequest.class, adapters = FavoriteListModel.class)
public class FavoriteListModelImpl implements FavoriteListModel {

    @ScriptVariable
    private Page currentPage;

    @Self
    private SlingHttpServletRequest request;

    @Inject
    FavoriteListService favoriteListService;

    @Override
    public String getFavoriteListPage() {
        return favoriteListService.getFavoriteListPage(request, currentPage.getPath());
    }

}
