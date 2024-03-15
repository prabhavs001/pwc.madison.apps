<%@include file="/libs/granite/ui/global.jsp"%>
<link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/font-awesome/4.4.0/css/font-awesome.min.css">
<%@page import="org.osgi.service.cm.ConfigurationAdmin, org.osgi.service.cm.Configuration "%>
<%
	Configuration conf = sling.getService(org.osgi.service.cm.ConfigurationAdmin.class).getConfiguration("com.pwc.madison.core.servlets.RedirectManagerServlet");
	String oldUrlPattern = (String) conf.getProperties().get("old.url.pattern");
	String newUrlPattern = (String) conf.getProperties().get("new.url.pattern");
%>
<ui:includeClientLib categories="pwc.redirectmanager" />
<div id="redirect-manager">
    <div class="row">
        <div class="col-md-6">
            <div class="title-text">Search Redirects</div>
            <div class="form-group">
                <label class="control-label">Redirect Status:</label>
                <select name="searchstatus" id="searchstatus">
                    <option value="301">301 Moved Permanently</option>
                    <option value="302">302 Found</option>
                    <option value="200">Internal Redirect</option>
                    <option value="all">All</option>
                </select>
            </div>
            <div class="form-group">
                <label for="searcholdurl" class="control-label">Old Url: <small>(Start with)</small></label>
                <input type="text" id="searcholdurl" name="searcholdurl" data-url-pattern='<%=oldUrlPattern%>'/>
            </div>
            <div class="form-group">
                <label for="searchnewurl" class="control-label">New Url: <small>(Start with)</small></label>
                <input type="text" id="searchnewurl" name="searchnewurl" data-url-pattern='<%=newUrlPattern%>'/>
            </div>
            <div class="form-group">
                <button type="button" id="btnsearch" class="btnredirect" title="Search" disabled>
                    <span class="fa fa-search"></span><span>Search</span>
                </button>
                <button type="button" id="btnnew" class="btnredirect" title="Add New Redirect">
                    <span class="fa fa-plus"></span><span>Add New Redirect</span>
                </button>
            </div>
            <div id="searchresult" class="form-group">
                <br />
                <div class="title-text">Search Results: <small id="searchresulttotal"></small></div>
            </div>
        </div>
        <div id="actionPanel" class="col-md-5">
            <div id="actionTitle" class="title-text">Add New Redirect</div>
            <form class="form-horizontal" id="addForm">
                <div class="form-group">
                    <label for="status" class="control-label">Redirect Status:</label>
                    <select name="status" id="status" disabled="true">
                        <option value="">[Select a redirect status]</option>
                        <option value="301">301 Moved Permanently</option>
                        <option value="302">302 Found</option>
                        <option value="200">Internal Redirect</option>
                    </select>
                    <label id="statusmsg" class="error">error</label>
                </div>
                <div class="form-group">
                    <label for="oldurl" class="control-label">Old Url:</label>
                    <input type="text" id="oldurl" name="oldurl" placeholder="" data-url-pattern='<%=oldUrlPattern%>'/>
                    <label id="oldurlmsg" class="error">error</label>
                </div>
                <div class="form-group">
                    <label for="newurl" class="control-label">New Url:</label>
                    <input type="text" id="newurl" name="newurl" placeholder="" data-url-pattern='<%=newUrlPattern%>'/>
                    <label id="newurlmsg" class="error">error</label>
                </div>
                <div class="form-group">
                    <button type="button" id="btnsave" class="btnredirect" title="Save Redirect">
                        <span class="fa fa-save"></span><span>Save</span>
                    </button>
                    <button type="button" id="btnupdate" class="btnredirect" title="Update Redirect">
                        <span class="fa fa-save"></span><span>Update</span>
                    </button>
                    <button type="button" id="btndelete" class="btnredirect" title="Delete Redirect">
                        <span class="fa fa-eraser"></span><span>Delete</span>
                    </button>
                    <button type="button" id="btncancel" class="btnredirect" title="Cancel">
                        <span class="fa fa-times"></span><span>Cancel</span>
                    </button>
                </div>
                <p class="red"><b>Note: </b> Saved and deleted redirect url will be published instantly.</p>
            </form>
        </div>
    </div>
</div>
<script>redirectmanager();</script>
