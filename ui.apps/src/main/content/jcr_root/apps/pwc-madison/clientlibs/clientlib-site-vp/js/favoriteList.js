function paginate(totalItems, currentPage, pageSize, maxPages) {
	var totalPages, startPage, endPage, startIndex, endIndex, pages, maxPagesBeforeCurrentPage, maxPagesAfterCurrentPage;
	if (currentPage === void 0) {
		currentPage = 1;
	}
	if (pageSize === void 0) {
		pageSize = 10;
	}
	if (maxPages === void 0) {
		maxPages = 10;
	}
	// calculate total pages
	totalPages = Math.ceil(totalItems / pageSize);
	// ensure current page isn't out of range
	if (currentPage < 1) {
		currentPage = 1;
	} else if (currentPage > totalPages) {
		currentPage = totalPages;
	}
	if (totalPages <= maxPages) {
		// total pages less than max so show all pages
		startPage = 1;
		endPage = totalPages;
	} else {
		// total pages more than max so calculate start and end pages
		maxPagesBeforeCurrentPage = Math.floor(maxPages / 2);
		maxPagesAfterCurrentPage = Math.ceil(maxPages / 2) - 1;
		if (currentPage <= maxPagesBeforeCurrentPage) {
			// current page near the start
			startPage = 1;
			endPage = maxPages;
		} else if (currentPage + maxPagesAfterCurrentPage >= totalPages) {
			// current page near the end
			startPage = totalPages - maxPages + 1;
			endPage = totalPages;
		} else {
			// current page somewhere in the middle
			startPage = currentPage - maxPagesBeforeCurrentPage;
			endPage = currentPage + maxPagesAfterCurrentPage;
		}
	}
	// calculate start and end item indexes
	startIndex = (currentPage - 1) * pageSize;
	endIndex = Math.min(startIndex + pageSize - 1, totalItems - 1);
	// create an array of pages to repeat in the pager control
	pages = Array.from(new Array((endPage + 1) - startPage).keys()).map(function (i) {
		return startPage + i;
	});
	// return object with all pager properties required by the view
	return {
		totalItems: totalItems,
		currentPage: currentPage,
		pageSize: pageSize,
		totalPages: totalPages,
		startPage: startPage,
		endPage: endPage,
		startIndex: startIndex,
		endIndex: endIndex,
		pages: pages
	};
}

(function($, Vue, UserRegistration) {	
	
	var FavoriteListComponent, favoriteListController, favoriteListDropdown, favListPage, isFavoriteListPage = $(".favorite-list-page").length ? true : false;
	FavoriteListComponent = Vue.component("favorite-list-component", {
		props : {
			pagePath : String,
			pageHref : String,
			isActive : Boolean,
			isUserLoggedIn : Boolean,
			list : Object,
			parent : Object,
			isWhite : Boolean,
			isContentPage : Boolean,
			controllerError : Object,
			hasError : Boolean,
			showError : Boolean
		},
		computed : {
			isActive : function(){
				var isPresent = false, self = this;
				$.each(this.list.favoriteFolders, function(favoriteFolderId, favoriteFolder) {
					favoriteFolder.list.every(function(favoriteList) {
						if (favoriteList.pagePath === self.pagePath) {
							isPresent = true ;
							return false;
						}
						else {
							return true;
						}
					});
				    if(isPresent){
				        return false;
				    }
				});
				return isPresent;
			},
			hasError : function(){
                if(this.controllerError.componentUid === this._uid && this.controllerError.errorOccured) {
                    return true;
                }
                else {
                    return false;
                }
			}
		},
        watch: { 
            isActive: {
                immediate : true,
                handler : function(newValue, oldValue) {
                    if(this.isContentPage){
                        if(newValue){
                            $(".badge-favorited").addClass("active");
                        }
                        else {
                            $(".badge-favorited").removeClass("active");
                        }
                    }
                }
            },
            hasError : function(){
				var $self = this;
                if(this.hasError){
                    this.showError = true;
                    setTimeout(function() {
                        $self.showError = false;
                    }, 3500);
                }
            }
        },
		methods : {
			handleClick : function() {
				if(this.isUserLoggedIn){
					if(this.isActive){
						this.parent.openRemoveConfirmModal(this.pagePath, this.pageHref, this._uid);
					}
					else{
						this.parent.addFavoriteList(this.pagePath, this.pageHref, this._uid);
					}
				}
				else{
					this.parent.openLoginModel();
				}
			}
		},
		template : "#favorite-list-component-template"
	});
		
	favoriteListController = new Vue({
		el : "#favorite-list-controller",
		data: {
			list : {
				favoriteFolders : {
				}
			},
			isUserLoggedIn : UserRegistration.isUserLoggedIn,
			updatePagesData : false,
			error: {
				componentUid: -1,
				errorOccured: false
			},
			favoriteListComponentSelector: ".favorite-list-component",
			deleteRequest: {
				pageHref: "",
				pagePath: "",
				componentUid: -1
			},
			cacheTime: 10
		},
		mounted: function () {
			this.cacheTime = this.$el.getAttribute('data-limit') || 10;
			var self = this;
			window.addEventListener("initFavoriteListComponent", this.addFavoriteListComponents, self);
			if(this.isUserLoggedIn){
				this.updatePagesData = isFavoriteListPage;
				this.checkForListValidity();
			}
			else{
				window.localStorage.removeItem(UserRegistration.FAVORITE_LIST_LOCAL_STORAGE);
			}
			this.addFavoriteListComponents();
		},
		methods: {
			checkForListValidity: function() {
				var invalidate = true, currTerritory = $("input[name=pageTerritoryCode]").val(), favoriteListData = window.localStorage.getItem(UserRegistration.FAVORITE_LIST_LOCAL_STORAGE);
				if(favoriteListData){
					favoriteListData = JSON.parse(favoriteListData);
					if(((this.updatePagesData && favoriteListData.updatePagesData) || !this.updatePagesData) && (favoriteListData && (UserRegistration.currentTimeInMinutes() - favoriteListData.lastUpdated < this.cacheTime)) && currTerritory===favoriteListData.currentTerritory){
						this.list.favoriteFolders = favoriteListData.favoriteFolders;
						invalidate = false;
					}
				}
				if(invalidate){
					this.getAllList();
				}
			},
			getAllList : function(){
	            $.ajax({
	                url: UserRegistration.FAVORITE_LIST_GETALL_END,
	                type: "GET",
	                data: {
                        updatePages : this.updatePagesData,
                        currentPageTerritoryCode : $("input[name=pageTerritoryCode]").val()
	                },
	                contentType: "application/json; charset=utf-8",
	                success: function(result) {
                       this.updateFavoriteListStorage(result, this.updatePagesData);
	                }.bind(this),
	                error: function(xhr, httpStatusMessage, customErrorMessage) {
						if (xhr.status === UserRegistration.STATUS_CODE_UNAUTHORIZED) {
							UserRegistration.performActionOnUnauthorized();
						}
	                }.bind(this)
	            });
			},
			addFavoriteList : function(pagePath, pageHref, componentUid){
				this.error.errorOccured = false;
				var data = {
						pagePath : pagePath,
						pageHref : pageHref,
						updatePagesData : this.updatePagesData
				};
	            $.ajax({
	                url: UserRegistration.FAVORITE_LIST_ADD_END,
	                type: "POST",
	                data: JSON.stringify(data),
	                contentType: "application/json; charset=utf-8",
	                success: function(result) {
                        this.updateFavoriteListStorage(result, this.updatePagesData);
                        if(result && result.data){
                            if(result.data.status === "LIMIT_EXCEED"){
                                $(".modal-favorite-limit").addClass("is-active");
                            }
                            else if(result.data.status === "SUCCESS"){
                                window.dispatchEvent(new window.CustomEvent("favoriteListAdded"));
                            }
                        }
	                }.bind(this),
	                error: function(xhr, httpStatusMessage, customErrorMessage) {
						if (xhr.status === UserRegistration.STATUS_CODE_UNAUTHORIZED) {
							UserRegistration.performActionOnUnauthorized();
						} else {
							this.error.errorOccured = true;
							this.error.componentUid = componentUid;
						}
	                }.bind(this)
	            });
			},
			deleteFavoriteList : function(pagePath, pageHref, componentUid){
				var data = {
						pagePaths : [pagePath],
						updatePagesData : this.updatePagesData,
						currentPageTerritoryCode : $("input[name=pageTerritoryCode]").val()
				};
	            $.ajax({
	                url: UserRegistration.FAVORITE_LIST_DELETE_END,
	                type: "DELETE",
	                data: JSON.stringify(data),
	                contentType: "application/json; charset=utf-8",
	                success: function(result) {
                        this.updateFavoriteListStorage(result);
	                }.bind(this),
	                error: function(xhr, httpStatusMessage, customErrorMessage) {
						if (xhr.status === UserRegistration.STATUS_CODE_UNAUTHORIZED) {
							UserRegistration.performActionOnUnauthorized();
						} else {
							this.error.errorOccured = true;
							this.error.componentUid = componentUid;
						}
	                }.bind(this)
	            });
			},
			updateFavoriteListStorage : function(result){
				if(result && result.data && result.data.favoriteFolders){
					this.list.favoriteFolders = result.data.favoriteFolders;
					this.list.lastUpdated = UserRegistration.currentTimeInMinutes();
					this.list.currentTerritory = $("input[name=pageTerritoryCode]").val();
					this.list.updatePagesData = this.updatePagesData;
					window.localStorage.setItem(UserRegistration.FAVORITE_LIST_LOCAL_STORAGE, JSON.stringify(this.list));
				}
			},
			addFavoriteListComponents : function(event){
				var $controllerSelf = this;
				$(this.favoriteListComponentSelector + (!event ? ':not(.dynamic)' : '')).each(function() {
					var favoriteList = new FavoriteListComponent({
						el : this,
						propsData : {
							pagePath : this.dataset.pagePath,
							pageHref : this.dataset.pageHref,
							isUserLoggedIn : $controllerSelf.isUserLoggedIn,
							list : $controllerSelf.list,
							parent: $controllerSelf,
							isWhite : this.dataset.isWhite === "true",
							isContentPage : this.dataset.isContentPage === "true",
							controllerError : $controllerSelf.error,
							hasError : false,
							showError : false
						}
					});
				});
			},
			confirmRemove : function(){
				window.closeModal(".modal-confirm-remove");
				this.deleteFavoriteList(this.deleteRequest.pagePath, this.deleteRequest.pageHref, this.deleteRequest.componentUid);
			},
			openRemoveConfirmModal : function(pagePath, pageHref, componentUid){
				$(".modal-confirm-remove").addClass("is-active");
				this.deleteRequest.pagePath = pagePath;
				this.deleteRequest.pageHref = pageHref;
				this.deleteRequest.componentUid = componentUid;
				this.error.errorOccured = false;
			},
			openLoginModel : function(){
				$(".modal-favorite-login").addClass("is-active");
			}
		}
	});
	
	window.favoriteListController = favoriteListController;
	
	favoriteListDropdown = new Vue({
		el : "#favorite-list-dropdown",
		data: {
			isUserLoggedIn : UserRegistration.isUserLoggedIn,
			list : favoriteListController.list,
			isDotActive : false
		},
		mounted: function () {
			if(this.isUserLoggedIn){
				var self = this;
				$(".favorite-list-dropdown-spacer").css("display", "block");
				if($(window).width() >= 768){
				    $(".navbar-favorite-list.scrollbar-outer").scrollbar({"autoScrollSize": true});
				}
				this.isDotActive = window.localStorage.getItem(UserRegistration.FAVORITE_LIST_DOT_STATUS_LOCAL_STORAGE) ? true : false;
                window.addEventListener("favoriteListAdded", this.favoriteListAdded, self);
			}
			else{
				window.localStorage.removeItem(UserRegistration.FAVORITE_LIST_DOT_STATUS_LOCAL_STORAGE);
			}
		},
		watch : {
			hasNoFavorites: function(){
				if(this.hasNoFavorites){
					$(".navbar-favorite-list.scroll-wrapper").addClass("hide");
				}
				else{
					$(".navbar-favorite-list.scroll-wrapper").removeClass("hide");
				}
			},
			defaultList: function(){
				var favoriteHeight = 0, noOfVisibleItems = window.innerWidth > 768 ? 6 : 4;
				$(".navbar-favorite-list li").each(function (index, element) {
			        $clamp(element, {
			            clamp: 2
			        });
			    });
				$('ul.navbar-favorite-list a').each(function(index) {
				    if(index < noOfVisibleItems) {
				        favoriteHeight = favoriteHeight + $(this).outerHeight();
				    }
				    else{
                        return false;
				    }
			    });
				if(this.defaultList.length >= noOfVisibleItems) {
                    $('.navbar-favorite-list').css('height', favoriteHeight + "px");
				}
			}
		},
		computed : {
			defaultList : function(){
				return this.list.favoriteFolders.DEFAULT ? this.list.favoriteFolders.DEFAULT.list.slice(0, 10) : [];
			},
			hasNoFavorites : function(){
				return this.defaultList.length ? false : true;
			}
		},
		methods : {
			openFavoriteListDropdown : function(){
				if(!isFavoriteListPage){
	                if(!$(".body").hasClass("show-favorites-menu")){
					    favoriteListController.checkForListValidity();
					    this.isDotActive = false;
						window.localStorage.removeItem(UserRegistration.FAVORITE_LIST_DOT_STATUS_LOCAL_STORAGE);
	                }
	                $(".body").removeClass("show-language-menu");
	                $(".body").removeClass("show-account-menu");
	                $(".body").toggleClass("show-favorites-menu");
				}
			},
			favoriteListAdded : function(){
				this.isDotActive = true;
				window.localStorage.setItem(UserRegistration.FAVORITE_LIST_DOT_STATUS_LOCAL_STORAGE, true);
			}
		}
	});
	
	if(isFavoriteListPage) {

        favListPage = new Vue({
            el: "#favorite-list-page",
            data: {
                isUserLoggedIn: UserRegistration.isUserLoggedIn,
                list: favoriteListController.list,
                numberOfItems: '',
                pageOfItems: [],
                pager: {},
                items: [],
                initialPage: 1,
                itemsPerPage: '',
                maxPages: 10,
                controllerError: favoriteListController.error,
                error: {
                    componentUid: -1,
                    errorOccured: false
                },
                indexElement: '-1'
    
            },
            mounted: function () {
                if (this.isUserLoggedIn) {
                    this.itemsPerPage = parseInt(this.$el.getAttribute('data-favorite-list-items-per-page'), 10) || 0;
                    $('body').addClass('favorites_list');
                }
            },
            updated: function () {
                $('.favorites_list .module-heading').each(function (index, value) {
                    $clamp(value, {
                        clamp: 2
                    });
                });
                var clampLines = window.matchMedia("only screen and (max-width: 768px)").matches ? 3 : 2;
                $('.favorites_list .feature-tile-content').each(function (index, value) {
                    $clamp(value, {
                        clamp: clampLines
                    });
                });
                if (this.numberOfItems === 0) {
                    $('.favorites_list .back-to-top').hide();
                } else {
                    $('.favorites_list .back-to-top').show();
                }
    
            },
            computed: {
                favoritedList: function () {
                    var currentFavItems = this.list.favoriteFolders.DEFAULT ? this.list.favoriteFolders.DEFAULT.list : [];
                    this.pageOfItems = currentFavItems;
                    this.items = currentFavItems;
                    this.numberOfItems = currentFavItems.length;
                },
                isError: function () {
                    var errorExist = this.controllerError.DEFAULT ? '' : this.controllerError.errorOccured;
                    if (this.controllerError.componentUid === this.indexElement && errorExist) {
                        return true;
                    } else {
                        return false;
                    }
                }
            },
            methods: {
                setPage: function (page) {
                    this.pager = paginate(this.items.length, page, this.itemsPerPage, this.maxPages);
                    this.pageOfItems = this.items.slice(this.pager.startIndex, this.pager.endIndex + 1);
                },
                deleteItem: function (pagePath, pageHref, indexElement) {
                    this.indexElement = indexElement;
                    favoriteListController.openRemoveConfirmModal(pagePath, pageHref, indexElement);
                }
            },
            watch: {
                items: function () {
                    this.setPage(this.initialPage);
                },
                isError: function () {
                    var $self = this;
                    if (this.controllerError.errorOccured) {
                        setTimeout(function () {
                            $self.controllerError.errorOccured = false;
                        }, 3500);
                    }
                }
            }
        });
    }

    $(".modal-favorite-login .sign-in").click(function(e) {
         $(".modal-favorite-login").removeClass("is-active");
         window.showBlur();
         $(".user-login").trigger("click");
    });

	
}($, window.Vue, window.UserRegistration));