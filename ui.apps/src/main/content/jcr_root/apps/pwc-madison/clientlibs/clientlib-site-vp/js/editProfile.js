(function (document, $, UserRegistration, Vue, CustomEvent, getUserRegErrorMessage, headerDropdowns) {

	function hideEditModals() {
        $('#editPreferencesTerritoryLanguage').hide();
        $('#editPreferencesContent').hide();
        $('#editProfileModalOne').hide();
	}
	
	hideEditModals();
	
    function initializeEditProfile(componentContext, callIndustry) {
       var editProfile = {};
        if (UserRegistration.userProfile){
            editProfile = {
                email: UserRegistration.userProfile.email,
                firstName: UserRegistration.userProfile.firstName,
                lastName: UserRegistration.userProfile.lastName,
                title: (UserRegistration.userProfile.title || ''),
                company: UserRegistration.userProfile.company,
                subscribeToWeeklyNewsLetter: (UserRegistration.userProfile.subscribeToWeeklyNewsLetter ? true : false),
                showMultimediaSubtitle: (UserRegistration.userProfile.showMultimediaSubtitle ? true : false),
                preferenceView: (UserRegistration.userProfile.preferenceView || 'LIST'),
                country: (UserRegistration.userProfile.country || ''),
                isInternalUser: UserRegistration.userProfile.isInternalUser,
                tncAccepted: false,
                industry: (UserRegistration.userProfile.preferredIndustry.length === 1 ? UserRegistration.userProfile.preferredIndustry[0] : ''),
                locale: $("input[name=pageLocale]").val(),
                userAcceptTnc: {
                    localeAccepted: $("input[name=pageLocale]").val(),
                    referrerAccepted: window.location.href,
                    territoryCode: $("input[name=pageTerritoryCode]").val()
                  }
            };
        }
        return {
            editProfile: editProfile,
            validation: {
                firstName: {
                    hasError: false,
                    errorMessage: ''
                },
                lastName: {
                    hasError: false,
                    errorMessage: ''
                },
                company: {
                    hasError: false
                },
                country: {
                    hasError: false
                }
            },
            config: {
                industry: {
                    attempt : false
                },
                tncAccepted: {
                    attempt : false
                }
            },
            buttonDisabled: false,
            attemptSubmit: false,
            hasEditProfileError: false,
            editProfileErrorMessage: '',
            titleList: editProfile.isInternalUser ? window.getInternalTitleList() : window.getExternalTitleList(),
            locale: window.getLocale(),
            isCompleteProfile:  UserRegistration.getCookie(UserRegistration.COMPLETE_PROFILE_COOKIE) ? true : false,
            industryList: callIndustry ? componentContext.getIndustryList() : [],
            isValidIndustry: false
        };
    }

    var editProfileModal = new Vue({
        el: '#editProfileModalOne',
        data: function() {
            return initializeEditProfile(this);
        },
        methods: {
            saveEditProfile: function (allowExit) {
                // refresh page if complete profile is still open in another tab but action has been taken on 1st tab
                if(this.isCompleteProfile && !UserRegistration.getCookie(UserRegistration.COMPLETE_PROFILE_COOKIE)){
                    window.location.reload();
                }
                this.hasEditProfileError = false;
                if (!this.attemptSubmit) {
                    this.validateForm();
                    this.attemptSubmit = true;
                }
                if (this.isFormValid()) {
                    this.buttonDisabled = true;
                    if(UserRegistration.getCookie(UserRegistration.COMPLETE_PROFILE_COOKIE)){
                    // pass isCompleteProfile = true in request
                        this.editProfile.isCompleteProfile=true;
                    }
                    $.ajax({
                        url: UserRegistration.EDIT_PROFILE_API_END,
                        data: JSON.stringify(this.editProfile),
                        type: "PUT",
                        contentType: 'application/json; charset=utf-8',
                        success: function (result) {
                            this.buttonDisabled = false;
                            this.attemptSubmit = false;
                            if (result.editProfileStatus === 200) {
                                    if (allowExit) {
                                         $(".edit-profile-modal").removeClass("is-active");
                                         $('body').removeClass('show-account-menu');
                                         window.hideBlur();
                                    }
                                    else{
                                      $('#editProfileModalOne').hide();
                                      $('#editPreferencesTerritoryLanguage').show();
                                      window.editPreferencesTerritoryLanguage.resetModal();
                                    }
                                if(UserRegistration.getCookie(UserRegistration.COMPLETE_PROFILE_COOKIE)){
                                        //NewsLetter Opt
                                        $(document).trigger("newsletterOptIn", [{"newsletterOptIn" : this.editProfile.subscribeToWeeklyNewsLetter}]);

										//Registration Complete
										$(document).trigger("registrationComplete", [{"email" : result.editProfileResponse.data.userProfile.email,
											"country" : result.editProfileResponse.data.userProfile.country,
											"industryTags" : result.completeProfileAnalyticResponse.industryTags,
											"titleTags" : result.completeProfileAnalyticResponse.titleTags,
											"functionRoleTitle" : result.completeProfileAnalyticResponse.functionalRoleTitle,
											"industryTitle" : result.completeProfileAnalyticResponse.industryTitle}]);

                                        UserRegistration.setCookie(UserRegistration.COMPLETE_PROFILE_COOKIE, undefined, 0);
                                        this.isCompleteProfile = false;
                                      }
                                UserRegistration.userProfile = result.editProfileResponse.data.userProfile;
                                UserRegistration.setUserInfo();
                            }
                        }.bind(this),
                        error: function (xhr, httpStatusMessage, customErrorMessage) {
                            if (xhr.status === UserRegistration.STATUS_CODE_UNAUTHORIZED) {
                                UserRegistration.performActionOnUnauthorized();
                            }
                            else {
                                this.hasEditProfileError = true;
                                this.editProfileErrorMessage = getUserRegErrorMessage("internalError");
                            }
                            this.buttonDisabled = false;
                            this.attemptSubmit = false;
                        }.bind(this)
                    });
                }
            },
            validateRequired: function (field) {
                this.editProfile[field] = this.editProfile[field] !== undefined ? this.editProfile[field].trim() : this.editProfile[field];
                this.validation[field].hasError = this.editProfile[field] !== undefined && this.editProfile[field] === '';
                if((field === 'firstName') || (field === 'lastName')) {
		            if(this.validation[field].hasError){
		                this.validation[field].errorMessage = getUserRegErrorMessage("required");
		            }
		            if(!UserRegistration.isNameValid(this.editProfile[field])) {
		                this.validation[field].hasError = true;
		                this.validation[field].errorMessage = getUserRegErrorMessage("invalidName");
		            }
                }
            },
            resetEditProfileModal: function () {
                $('#editPreferencesTerritoryLanguage').hide();
                $('#editPreferencesContent').hide();
                if($("#editProfileModalOne").css('display') === 'none'){
                    $("#editProfileModalOne").css('display','block');
                }
                Object.assign(this.$data, initializeEditProfile(this, true));
                $(this.$refs.title).val(this.editProfile.title).trigger('change.select2');
                $(this.$refs.country).val(this.editProfile.country).trigger("change.select2");
                if($(this.$refs.country).val() === null){
                    this.editProfile.country = 'XX';
                    $(this.$refs.country).val(this.editProfile.country).trigger("change.select2");
                }
                $(this.$refs.industry).val(this.editProfile.industry);
            },
            validateForm: function () {
                $.each(this.validation, function (field, value) {
                    this.validateRequired(field);
                }.bind(this));
                this.validateCompany();
            },
            isFormValid: function () {
                var isFormValid = true;
                $.each(this.validation, function (field, value) {
                    if (value.hasError) {
                        isFormValid = false;
                    }
                });
                if(UserRegistration.getCookie(UserRegistration.COMPLETE_PROFILE_COOKIE)){
                    this.config.industry.attempt = true;
                    if(!this.editProfile.tncAccepted){
                        isFormValid = false;
                        this.config.tncAccepted.attempt = true;
                    }
                }
                return isFormValid;
            },
            validateCompany: function() {
                this.editProfile.company = this.editProfile.company !== undefined ? this.editProfile.company.trim() : this.editProfile.company;
                this.validation.company.hasError = this.editProfile.company !== undefined && this.editProfile.company.length < 2;
            },
            validateIndustry: function(event) {
                this.config.industry.attempt = true;
	        },
            validateTnC: function(){
                this.config.tncAccepted.attempt = true;
            },
	        cancelModal: function(){
                // refresh page if complete profile is still open in another tab but action has been taken on 1st tab
                if(this.isCompleteProfile && !UserRegistration.getCookie(UserRegistration.COMPLETE_PROFILE_COOKIE)){
                    window.location.reload();
                }
                this.resetEditProfileModal();
                $(".edit-profile-modal").removeClass("is-active");
                $('body').removeClass('show-account-menu');
                window.hideBlur();
                if(UserRegistration.getCookie(UserRegistration.COMPLETE_PROFILE_COOKIE)){
                    headerDropdowns.logout();
	            }
	        },
            getIndustryList: function (){
				var industryFieldInLocalStorage, self = this;
				industryFieldInLocalStorage = "industry|"+$("input[name=pageTerritoryCode]").val()+'-'+$("input[name=pageLocale]").val();
				if(localStorage.getItem(industryFieldInLocalStorage)){
				    return localStorage.getItem(industryFieldInLocalStorage).split("|").map(JSON.parse);
				}else{
                    $.ajax({
                        url: UserRegistration.INDUSTRY_LIST_API_END,
                        type: "GET",
                        data: {
                            currentPageTerritoryCode : $("input[name=pageTerritoryCode]").val(),
                            currentPageLocale : $("input[name=pageLocale]").val()
                        },
                        contentType: 'application/json; charset=utf-8',
                        success: function(result, status, xhr) {
                            if(xhr.status === 200){
                                self.industryList = result;
                                localStorage.setItem(industryFieldInLocalStorage,result.map(JSON.stringify).join("|"));
                            }
                        },
                        error: function (xhr, httpStatusMessage, customErrorMessage) {
                            if (xhr.status === UserRegistration.STATUS_CODE_UNAUTHORIZED) {
                                UserRegistration.performActionOnUnauthorized();
                            }
                            else {
                                this.hasEditProfileError = true;
                                this.editProfileErrorMessage = getUserRegErrorMessage("internalError");
                            }
                            this.buttonDisabled = false;
                            this.attemptSubmit = false;
                        }
                    });
                }
            }
        }
    });

    function onEditProfileGetUserSuccess(result) {
        editProfileModal.resetEditProfileModal();
    }

    $(".edit-profile").on("click", function(e) {
        e.preventDefault();
        $(".body").removeClass("show-account-menu");
        UserRegistration.getCurrentUser(onEditProfileGetUserSuccess);
        $(".regActivationModal").removeClass("is-active");
        $(".edit-profile-modal").addClass("is-active");
        window.showBlur();
    });
    $(document).on("click", ".edit-modal-close", function(event){
        // refresh page if complete profile is still open in another tab but action has been taken on 1st tab
        if(editProfileModal.isCompleteProfile && !UserRegistration.getCookie(UserRegistration.COMPLETE_PROFILE_COOKIE)){
            window.location.reload();
        }
        window.closeModal(event.target);
        if(UserRegistration.getCookie(UserRegistration.COMPLETE_PROFILE_COOKIE)){
            headerDropdowns.logout();
	    }
    });
        
    $(".edit-profile-modal").on("click", ".modal-close-edit", function(e) {
        e.preventDefault();
        hideEditModals();
        $(".edit-profile-modal").removeClass("is-active");
        window.hideBlur();
    }); 
    
    function openCompleteProfileModal(){
        if(UserRegistration.getCookie(UserRegistration.COMPLETE_PROFILE_COOKIE)&& $(".disableCompleteProfile").length===0){
            $(".edit-profile").trigger('click');
        }
    }
    openCompleteProfileModal();
    window.hideEditModals = hideEditModals;

    window.editProfileModal = editProfileModal;

}(document, $, window.UserRegistration, window.Vue, window.CustomEvent, window.getUserRegErrorMessage, window.headerDropdowns));

