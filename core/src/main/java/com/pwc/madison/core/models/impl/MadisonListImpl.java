package com.pwc.madison.core.models.impl;

import com.adobe.cq.export.json.ExporterConstants;
import com.day.cq.commons.RangeIterator;
import com.day.cq.search.Predicate;
import com.day.cq.search.SimpleSearch;
import com.day.cq.search.result.Hit;
import com.day.cq.search.result.SearchResult;
import com.day.cq.tagging.TagManager;
import com.day.cq.wcm.api.NameConstants;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.designer.Style;
import com.pwc.madison.core.models.*;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;

@Model(adaptables = SlingHttpServletRequest.class,adapters = MadisonList.class,resourceType = MadisonListImpl.RESOURCE_TYPE,
    defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
@Exporter(name = ExporterConstants.SLING_MODEL_EXPORTER_NAME, extensions = ExporterConstants.SLING_MODEL_EXTENSION)
public class MadisonListImpl implements MadisonList {
    protected static final String RESOURCE_TYPE = "pwc-madison/components/content/madison-list";

    private static final Logger LOGGER = LoggerFactory.getLogger(SmeListImpl.class);
    private static final int PN_DEPTH_DEFAULT = 1;
    private static final int LIMIT_DEFAULT = 100;
    private static final String TAGS_MATCH_ANY_VALUE = "any";

    /**
     * Name of the resource property indicating how the list will be built. Possible values are:
     *
     * <ul>
     *     <li><code>children</code> - the list will be built from the child pages of the page identified by {@link #PN_PARENT_PAGE}</li>
     *     <li><code>static</code> - the list will be built from a custom set of pages, stored by the {@link #PN_PAGES} property</li>
     *     <li><code>search</code> - the list will be built from the result of a search query</li>
     *     <li><code>tags</code> - the list will be built from the sub-pages of the page identified by {@link #PN_TAGS_PARENT_PAGE}
     *     which are tagged with the tags stored by the {@link #PN_TAGS} property</li>
     * </ul>
     *
     */
    String PN_SOURCE = "listFrom";

    /**
     * Name of the resource property storing the list of pages to be rendered if the source of the list is <code>static</code>.
     *
     * @see #PN_SOURCE
     */
    String PN_PAGES = "pages";

    /**
     * Name of the resource property storing the root page from which to build the list if the source of the list is <code>children</code>.
     *
     * @see #PN_SOURCE
     */
    String PN_PARENT_PAGE = "parentPage";

    /**
     * Name of the resource property storing the root from where the tag search is performed.
     *
     * @see #PN_SOURCE
     */
    String PN_TAGS_PARENT_PAGE = "tagsSearchRoot";

    /**
     * Name of the resource property storing the tags that will be used for building the list if the source of the list is
     * <code>tags</code>.
     *
     * @see #PN_SOURCE
     */
    String PN_TAGS = "tags";

    /**
     * Name of the resource property indicating if the matching against tags can accept any tag from the tag list. The accepted value is
     * <code>any</code>.
     *
     * @see #PN_SOURCE
     */
    String PN_TAGS_MATCH = "tagsMatch";


    /**
     * Name of the resource property storing where a search should be performed if the source of the list is <code>search</code>.
     *
     * @see #PN_SOURCE
     */
    String PN_SEARCH_IN = "searchIn";

    /**
     * Name of the resource property indicating how the list items should be sorted. Possible values: <code>asc</code>, <code>desc</code>.
     *
     */
    String PN_SORT_ORDER = "sortOrder";

    /**
     * Name of the resource property indicating by which criterion the sort is performed. Possible value: <code>title</code>,
     * <code>modified</code>.
     *
     */
    String PN_ORDER_BY = "orderBy";

    

    @Self
    private SlingHttpServletRequest request;

    private java.util.List<Page> listItems;

    @ScriptVariable
    private PageManager pageManager;

    @ScriptVariable
    private ValueMap properties;

    @ScriptVariable
    private Page currentPage;

    @SlingObject
    private ResourceResolver resourceResolver;

    @SlingObject
    private Resource resource;

    private String query;

    @ValueMapValue
    @Default(intValues = LIMIT_DEFAULT)
    private int limit;

    @ValueMapValue
    @Default(intValues = PN_DEPTH_DEFAULT)
    private int childDepth;

    @ValueMapValue
    @Default(intValues = 0)
    private int maxItems;


    @ScriptVariable
    private Style currentStyle;

    private java.util.List<MadisonListItem> mListItems;

    public Collection<MadisonListItem> getListItems() {
        mListItems = new ArrayList<MadisonListItem>();
        int count = 1;
        Collection<Page> pages = getPages();
        for (Page page : pages) {
            // The UI allows for only 3 items to be displayed under the SME Accordion
            if (page != null && count <= 3) {
                mListItems.add(new MadisonPageListItemImpl(request, page));
            }
            count ++;
        }
        return mListItems;
    }

    protected Collection<Page> getPages() {
        if (listItems == null) {
            MadisonListImpl.Source listType = getListType();
            populateListItems(listType);
        }
        return listItems;
    }

    protected enum Source {
        CHILDREN("children"),
        STATIC("static"),
        SEARCH("search"),
        TAGS("tags"),
        EMPTY(StringUtils.EMPTY);

        private String value;

        Source(String value) {
            this.value = value;
        }

        public static MadisonListImpl.Source fromString(String value) {
            for (MadisonListImpl.Source s : values()) {
                if (StringUtils.equals(value, s.value)) {
                    return s;
                }
            }
            return null;
        }
    }

    protected void populateListItems(MadisonListImpl.Source listType) {
        switch (listType) {
            case STATIC:
                populateStaticListItems();
                break;
            case CHILDREN:
                populateChildListItems();
                break;
            case TAGS:
                populateTagListItems();
                break;
            case SEARCH:
                populateSearchListItems();
                break;
            default:
                listItems = new ArrayList<>();
                break;
        }
        sortListItems();
        setMaxItems();
    }

    private void populateStaticListItems() {
        listItems = new ArrayList<>();
        String[] pagesPaths = properties.get(PN_PAGES, new String[0]);
        for (String path : pagesPaths) {
            Page page = pageManager.getContainingPage(path);
            if (page != null) {
                listItems.add(page);
            }
        }
    }

    private void populateChildListItems() {
        listItems = new ArrayList<>();
        Page rootPage = getRootPage(PN_PARENT_PAGE);
        if (rootPage != null) {
            collectChildren(rootPage.getDepth(), rootPage);
        }
    }

    private void collectChildren(int startLevel, Page parent) {
        Iterator<Page> childIterator = parent.listChildren();
        while (childIterator.hasNext()) {
            Page child = childIterator.next();
            listItems.add(child);
            if (child.getDepth() - startLevel < childDepth) {
                collectChildren(startLevel, child);
            }
        }
    }

    private void populateTagListItems() {
        listItems = new ArrayList<>();
        String[] tags = properties.get(PN_TAGS, new String[0]);
        boolean matchAny = properties.get(PN_TAGS_MATCH, TAGS_MATCH_ANY_VALUE).equals(TAGS_MATCH_ANY_VALUE);
        Page rootPage = getRootPage(PN_TAGS_PARENT_PAGE);
        if (rootPage != null) {
            TagManager tagManager = resourceResolver.adaptTo(TagManager.class);
            if (tagManager != null && ArrayUtils.isNotEmpty(tags)) {
                RangeIterator<Resource> resourceRangeIterator = tagManager.find(rootPage.getPath(), tags, matchAny);
                if (resourceRangeIterator != null) {
                    while (resourceRangeIterator.hasNext()) {
                        Page containingPage = pageManager.getContainingPage(resourceRangeIterator.next());
                        if (containingPage != null) {
                            listItems.add(containingPage);
                        }
                    }
                }
            }
        }
    }

    private void populateSearchListItems() {
        listItems = new ArrayList<>();
        if (!StringUtils.isBlank(query)) {
            SimpleSearch search = resource.adaptTo(SimpleSearch.class);
            if (search != null) {
                search.setQuery(query);
                search.setSearchIn(properties.get(PN_SEARCH_IN, currentPage.getPath()));
                search.addPredicate(new Predicate("type", "type").set("type", NameConstants.NT_PAGE));
                search.setHitsPerPage(limit);
                try {
                    collectSearchResults(search.getResult());
                } catch (RepositoryException e) {
                    LOGGER.error("Unable to retrieve search results for query.", e);
                }
            }
        }
    }

    private void collectSearchResults(SearchResult result) throws RepositoryException {
        for (Hit hit : result.getHits()) {
            Page containingPage = pageManager.getContainingPage(hit.getResource());
            if (containingPage != null) {
                listItems.add(containingPage);
            }
        }
    }

    protected MadisonListImpl.Source getListType() {
        String listFromValue = properties.get(PN_SOURCE, currentStyle.get(PN_SOURCE, StringUtils.EMPTY));
        return MadisonListImpl.Source.fromString(listFromValue);
    }

    private enum SortOrder {
        ASC("asc"),
        DESC("desc");

        private String value;

        SortOrder(String value) {
            this.value = value;
        }

        public static MadisonListImpl.SortOrder fromString(String value) {
            for (MadisonListImpl.SortOrder s : values()) {
                if (StringUtils.equals(value, s.value)) {
                    return s;
                }
            }
            return ASC;
        }
    }

    private enum OrderBy {
        TITLE("title"),
        MODIFIED("modified");

        private String value;

        OrderBy(String value) {
            this.value = value;
        }

        public static MadisonListImpl.OrderBy fromString(String value) {
            for (MadisonListImpl.OrderBy s : values()) {
                if (StringUtils.equals(value, s.value)) {
                    return s;
                }
            }
            return null;
        }
    }

    private Page getRootPage(String fieldName) {
        String parentPath = properties.get(fieldName, currentPage.getPath());
        return pageManager.getContainingPage(resourceResolver.getResource(parentPath));
    }

    private void sortListItems() {
        if (MadisonListImpl.OrderBy.fromString(properties.get(PN_ORDER_BY, StringUtils.EMPTY)) != null) {
            listItems.sort(new MadisonListImpl.ListSort(MadisonListImpl.OrderBy.fromString(properties.get(PN_ORDER_BY, StringUtils.EMPTY)), MadisonListImpl.SortOrder.fromString(properties.get(PN_SORT_ORDER, MadisonListImpl.SortOrder.ASC.value))));
        }
    }

    private void setMaxItems() {
        if (maxItems != 0) {
            java.util.List<Page> tmpListItems = new ArrayList<>();
            for (Page item : listItems) {
                if (tmpListItems.size() < maxItems) {
                    tmpListItems.add(item);
                } else {
                    break;
                }
            }
            listItems = tmpListItems;
        }
    }

    private static class ListSort implements Comparator<Page>, Serializable {


        private static final long serialVersionUID = 204096578105548876L;
        private MadisonListImpl.SortOrder sortOrder;
        private MadisonListImpl.OrderBy orderBy;

        ListSort(MadisonListImpl.OrderBy orderBy, MadisonListImpl.SortOrder sortOrder) {
            this.orderBy = orderBy;
            this.sortOrder = sortOrder;
        }

        public int compare(Page item1, Page item2) {
            int i = 0;
            if (orderBy == MadisonListImpl.OrderBy.MODIFIED) {
                // getLastModified may return null, define null to be after nonnull values
                i = ObjectUtils.compare(item1.getLastModified(), item2.getLastModified(), true);
            } else if (orderBy == MadisonListImpl.OrderBy.TITLE) {
                // getTitle may return null, define null to be greater than nonnull values
                i = ObjectUtils.compare(item1.getTitle(), item2.getTitle(), true);
            }

            if (sortOrder == MadisonListImpl.SortOrder.DESC) {
                i = i * -1;
            }
            return i;
        }
    }
}