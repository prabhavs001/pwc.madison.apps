(function(document, $, Vue, _, getUserRegErrorMessage, UserRegistration) {
	function initializeEditPreferences(componentContext) {
		var editPreferences = {
		    fullTerritoryMap : [],
			territoryMap : {},
			countriesAndLanguageList : {},
			countriesAndLanguageMap : {},
			territoryList : [],
			selectedCountries : [],
			selectedLanguages : [],
			hasEditPreferencesTerritoryError : false,
			editEditPreferencesTerritoryErrorMessage : "",
			hasEditPreferencesLanguageError : false,
			editEditPreferencesLanguageErrorMessage : "",
			buttonDisabled : false,
			primaryTerritory : '',
			primaryLanguage : '',
			isChanged : true
		}, key, localeKey, territory, localeToLanguageMap, temporaryPreferredLanguages = [];
		if (UserRegistration.userProfile) {
			editPreferences.fullTerritoryMap=componentContext.getTerrotoryAndLanugageMap(editPreferences.fullTerritoryMap);
            editPreferences.territoryMap = UserRegistration.userProfile.isInternalUser ? editPreferences.fullTerritoryMap : Object.keys(editPreferences.fullTerritoryMap).filter(function(key) {
              return editPreferences.fullTerritoryMap[key].designation !== "Internal Only";
            }).reduce(function(res, key) {
              res[key] = editPreferences.fullTerritoryMap[key];
              return res;
            }, {});

            Object.keys(editPreferences.territoryMap).forEach(function(territory) {
                Object.keys(editPreferences.territoryMap[territory].localeToLanguageMap).forEach(function(lang) {
                    if (typeof editPreferences.countriesAndLanguageMap[lang] === 'undefined') {
                        editPreferences.countriesAndLanguageMap[lang] = Object.assign({}, editPreferences.territoryMap[territory]);
                        editPreferences.countriesAndLanguageMap[lang].localeToLanguageMap = {};
                    }
                    editPreferences.countriesAndLanguageMap[lang].localeToLanguageMap = editPreferences.territoryMap[territory].localeToLanguageMap[lang];
                });
            });

			editPreferences.primaryTerritory = UserRegistration.userProfile
					.hasOwnProperty("primaryTerritory") ? UserRegistration.userProfile.primaryTerritory
					: (window.getCountryToTerritoryMap()[UserRegistration.userProfile.country] ? window
							.getCountryToTerritoryMap()[UserRegistration.userProfile.country].territoryCode
							: window.getDefaultTerritoryCode());
			editPreferences.selectedCountries = JSON.parse(JSON.stringify(UserRegistration.userProfile.preferredLanguages)).filter(function(selectedCountry){
						return editPreferences.countriesAndLanguageMap[selectedCountry];
					});
			temporaryPreferredLanguages = JSON.parse(JSON
					.stringify(UserRegistration.userProfile.preferredLanguages));

            editPreferences.primaryLanguage = UserRegistration.userProfile
                                    .hasOwnProperty("primaryLanguage") ? UserRegistration.userProfile.primaryLanguage
                                    : UserRegistration.userProfile.language;
			if (editPreferences.countriesAndLanguageMap[editPreferences.primaryLanguage]) {
				editPreferences.selectedCountries.push(editPreferences.primaryLanguage);
				editPreferences.primaryLanguage = editPreferences.countriesAndLanguageMap[editPreferences.primaryLanguage].localeToLanguageMap.locale ? editPreferences.primaryLanguage : "";
				temporaryPreferredLanguages.push(editPreferences.primaryLanguage);
			}
			else {
				editPreferences.primaryTerritory = "";
			}
			for (key in editPreferences.countriesAndLanguageMap) {
				territory = editPreferences.countriesAndLanguageMap[key];
				if (territory.territoryCode === editPreferences.primaryLanguage) {
					territory.primary = true;
					territory.setAsPrimary = false;
				} else if (editPreferences.selectedCountries
						.indexOf(territory.territoryCode) !== -1) {
					territory.setAsPrimary = true;
					territory.primary = false;
				} else {
					territory.setAsPrimary = false;
					territory.primary = false;
				}
				editPreferences.territoryList
						.push(editPreferences.countriesAndLanguageMap[key]);
			}
		}
		return editPreferences;
	}

	Vue.component('madison-columns', {
		props : {
			title : String,
			countryAndLangList : Array,
			columnList : Array,
			languageList : Array,
			isDoubleColumned : Boolean,
			propertyName : String,
			columnWidth : Number,
			propertyValue : String,
			localelistPropertyName : String,
			langtitlePropertyName : String,
			langtitlePropertyValue : String,
			selectedItems : Array,
			showErrorBlock : Boolean,
			errorMessage : String,
			divId : String,
			isChanged : Boolean
		},
		updated : function() {
			if (this.isChanged) {
				this.isChanged = false;
				this.updateValue();
				if (window.editPreferencesTerritoryLanguage) {
					window.editPreferencesTerritoryLanguage.isChanged = false;
				}
			}
		},
		methods : {
			getCutOff : function() {
				if (!this.isDoubleColumned) {
				    return this.columnList.length;
				}
				var rows = this.columnList.length;
				if (rows <= 12) {
					return rows;
				}
				if (rows <= 24) {
					return 12;
				} else {
					return rows / 2;
				}
			},
			updateValue : function() {
				this.$emit('change-selected', this.selectedItems);
			},
			setAsPrimary : function() {
				this.$emit('set-primary', event.currentTarget.id);
			}
		},
		template : '#edit-preference-list'
	});

	var editPreferencesTerritoryLanguage = new Vue(
			{
				el : "#editPreferencesTerritoryLanguage",
				data : function() {
					return initializeEditPreferences(this);
				},
				computed : {
					languageList : function() {
						this.hasEditPreferencesLanguageError = false;
						var languageList = [], index, localeToLanguageMap, key, territory, language, localeList = [];
						return languageList;
					}
				},
				methods : {
					validateCountries : function(selectedItems) {
						this.hasEditPreferencesTerritoryError = false;
						this.selectedCountries = selectedItems;
						$("#preference-country-list .remember-me").removeClass("panel-disabled");
						if (selectedItems.length === 3) {
							$("#preference-country-list label.check input:not(:checked)")
									.closest(".remember-me").addClass("panel-disabled");
						}
						if (this.selectedCountries.indexOf(this.primaryLanguage) < 0) {
							this.primaryTerritory = '';
							this.primaryLanguage = '';
						}
						this.updateTerritoriesList();
					},
					validateLanguages : function() {
						var index, localeCodes;
						if (this.primaryLanguage === '') {
							this.hasEditPreferencesLanguageError = true;
							this.editEditPreferencesLanguageErrorMessage = getUserRegErrorMessage("noPrimaryLangaugeSelected");
							return false;
						}
                        localeCodes = Object.keys(this.countriesAndLanguageMap);
                        if (_.intersection(localeCodes, this.selectedCountries).length === 0) {
                            this.hasEditPreferencesLanguageError = true;
                            this.editEditPreferencesLanguageErrorMessage = getUserRegErrorMessage("invalidLanguageList");
                            return false;
                        }
						this.hasEditPreferencesLanguageError = false;
						return true;
					},
					changeSelectedLanguages : function(selectedItems) {
						this.hasEditPreferencesLanguageError = false;
						this.selectedLanguages = selectedItems;
					},
					saveEditPreferencesPageOne : function(allowExit) {
						this.hasEditPreferencesTerritoryError = false;
						this.hasEditPreferencesLanguageError = false;
						if (this.validatePrimaryTerritory() && this.validateLanguages()) {
							var countries = JSON.parse(JSON
									.stringify(this.selectedCountries)), languages = JSON
									.parse(JSON
											.stringify(this.selectedCountries)), data, locale = window
									.getLocale();
							countries.splice(countries.indexOf(this.primaryLanguage), 1);
                            countries = countries.map(function(country) {
                                return country.split("_")[1];
                            });
							languages.splice(languages.indexOf(this.primaryLanguage), 1);
							data = {
								preferredTerritories : countries,
								preferredLanguages : languages,
								primaryTerritory : this.primaryTerritory,
								primaryLanguage : this.primaryLanguage,
								localeString : locale
							};
							$.ajax({
									url : UserRegistration.EDIT_PREFERENCES_TERRITORY_LANGUAGE_END,
									data : JSON.stringify(data),
									type : "PUT",
									contentType : 'application/json; charset=utf-8',
									success : function(result) {
										this.buttonDisabled = false;
										UserRegistration.userProfile = result.data.userProfile;
		                                UserRegistration.setUserInfo();
										if (allowExit) {
			                                window.hideEditModals();
											$(".edit-profile-modal").removeClass("is-active");
											window.hideBlur();
										} else {
											$('#editPreferencesTerritoryLanguage')
													.hide();
											$('#editPreferencesContent')
													.show();
											window.editPreferencesContent
													.resetModal(
															result.territoryToGaapPreferencesMap,
															result.territoryToGaasPreferencesMap,
															result.industryPreferencesMap,
															result.topicPreferencesMap);
										}
									}.bind(this),
									error : function(xhr,
											httpStatusMessage,
											customErrorMessage) {
										if (xhr.status === UserRegistration.STATUS_CODE_UNAUTHORIZED) {
											UserRegistration.performActionOnUnauthorized();
										} else {
											this.hasEditPreferencesTerritoryError = true;
											this.editEditPreferencesTerritoryErrorMessage = getUserRegErrorMessage("internalError");
										}
										this.buttonDisabled = false;
									}.bind(this)
								});
							}
					},
					validatePrimaryTerritory : function() {
						if (this.primaryTerritory === '') {
							this.hasEditPreferencesTerritoryError = true;
							this.editEditPreferencesTerritoryErrorMessage = getUserRegErrorMessage("noPrimaryTerritorySelected");
							return false;
						} else {
							this.hasEditPreferencesTerritoryError = false;
							return true;
						}
					},
					updateTerritoriesList : function() {
						var index, territory;
						for (index = 0; index < this.territoryList.length; index++) {
							territory = this.territoryList[index];
							if (territory.localeToLanguageMap.locale === this.primaryLanguage) {
								territory.primary = true;
								territory.setAsPrimary = false;
							} else if (this.selectedCountries.length === 1 && this.selectedCountries.indexOf(territory.localeToLanguageMap.locale) !== -1) {
                                this.primaryLanguage = territory.localeToLanguageMap.locale;
                                this.primaryTerritory = territory.territoryCode;
                                territory.primary = true;
                                territory.setAsPrimary = false;
                            } else if (this.selectedCountries.indexOf(territory.localeToLanguageMap.locale) !== -1) {
								territory.setAsPrimary = true;
								territory.primary = false;
							} else {
								territory.setAsPrimary = false;
								territory.primary = false;
							}
							this.territoryList[index] = territory;
						}
					},
					setPrimary : function(id) {
                        this.setPrimaryTerritory(id);
                        this.setPrimaryLanguage(id);
                        this.updateTerritoriesList();
                    },
					setPrimaryTerritory : function(id) {
						this.primaryTerritory = id.split("_")[1];
						this.updateTerritoriesList();
					},
					setPrimaryLanguage : function(id) {
						this.primaryLanguage = id;
						if (this.selectedLanguages.indexOf(id) < 0) {
							this.selectedLanguages.push(id);
						}
					},
					previous : function() {
						$('#editProfileModalOne').show();
						$('#editPreferencesTerritoryLanguage').hide();
						window.editProfileModal.resetEditProfileModal();
					},
					resetModal : function() {
						Object.assign(this.$data, initializeEditPreferences(this));
					},
                    getTerrotoryAndLanugageMap: function (fullMap){
                         var self = this;
                         $.ajax({
                             url: UserRegistration.EDIT_PREFERENCES_TERRITORY_LANGUAGE_END,
                             type: "GET",
                             async: false,
                             contentType: 'application/json; charset=utf-8',
                             success: function(result, status, xhr) {
                                 if(xhr.status === 200){
                                     fullMap = result;
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
                        return fullMap;
                     }
				},
				watch : {
				}
			});

	window.editPreferencesTerritoryLanguage = editPreferencesTerritoryLanguage;

}(document, $, window.Vue, window._, window.getUserRegErrorMessage,
		window.UserRegistration));
