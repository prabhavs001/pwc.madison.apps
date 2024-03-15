$(document).ready(function() {
    if (window.location.href.indexOf('joined.html') !== -1) {
        var obj = {
            event: "",
            searchResultCount: '',
            searchTerm: '',
            searchType: "",
            joinViewTitle: ''
        };
        $("body").on('focusout', '#cst-search', function() {
            obj.searchResultCount = window.finder.resultsCount;
            obj.searchTerm = $('#cst-search').val();
			obj.event = "joinview-search-success";
			obj.searchType = "search-within-joinview";
            window.digitalData.joinView = obj;

        });
        obj.joinViewTitle = window.pageTitle;
        window.digitalData.joinView = obj;
    }
});