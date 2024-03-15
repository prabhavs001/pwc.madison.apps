(function(document, $, Vue, _, getUserRegErrorMessage, UserRegistration) {
	function initializeEditPreferencesTwo(territoryGaapMap, territoryGaasMap, territoryIndustryMap, territoryTopicMap) {
		var editPreferencesTwo = {
			selectedGaap : [],
			selectedGaas : [],
			gaapList : [],
			gaasList : [],
			buttonDisabled : false,
			hasEditPreferencesError : false,
			editEditPreferencesErrorMessage : "",
			selectedIndustry : [],
			selectedTopic : [],
			industryList : [],
			topicList : []
		}, gaap, gaas, industry, topic, key, locale = window.getLocale();
		if (territoryGaapMap && territoryGaasMap && territoryIndustryMap && territoryTopicMap) {
			for (key in territoryGaapMap) {
				gaap = territoryGaapMap[key];
				gaap.title = gaap.translatedTitles[locale] || gaap.title;
				editPreferencesTwo.gaapList.push(gaap);
			}
			for (key in territoryGaasMap) {
				gaas = territoryGaasMap[key];
				gaas.title = gaas.translatedTitles[locale] || gaas.title;
				editPreferencesTwo.gaasList.push(gaas);
			}
			for (key in territoryIndustryMap) {
				industry = territoryIndustryMap[key];
				industry.title = industry.translatedTitles[locale] || industry.title;
				editPreferencesTwo.industryList.push(industry);
			}
			for (key in territoryTopicMap) {
				topic = territoryTopicMap[key];
				topic.title = topic.translatedTitles[locale] || topic.title;
				editPreferencesTwo.topicList.push(topic);
			}
			if (UserRegistration.userProfile) {
				editPreferencesTwo.selectedGaap = UserRegistration.userProfile.preferredGaap;
				editPreferencesTwo.selectedGaas = UserRegistration.userProfile.preferredGaas;
				editPreferencesTwo.selectedIndustry = UserRegistration.userProfile.preferredIndustry;
				editPreferencesTwo.selectedTopic = UserRegistration.userProfile.preferredTopic;
			}
		}
		return editPreferencesTwo;
	}

	var editPreferencesContent = new Vue(
			{
				el : "#editPreferencesContent",
				data : function() {
					return initializeEditPreferencesTwo();
				},
				methods : {
					resetModal : function(territoryGaapMap, territoryGaasMap, territoryIndustryMap, territoryTopicMap) {
						Object.assign(this.$data, initializeEditPreferencesTwo(
								territoryGaapMap, territoryGaasMap, territoryIndustryMap, territoryTopicMap));
					},
					changeSelectedGaap : function(selectedItems) {
						this.selectedGaap = selectedItems;
						this.hasEditPreferencesError = false;
					},
					changeSelectedGaas : function(selectedItems) {
						this.selectedGaas = selectedItems;
						this.hasEditPreferencesError = false;
					},
					changeSelectedIndustry : function(selectedItems) {
						this.selectedIndustry = selectedItems;
						this.hasEditPreferencesError = false;
					},
					changeSelectedTopic : function(selectedItems) {
						this.selectedTopic = selectedItems;
						this.hasEditPreferencesError = false;
					},
					saveEditPreferencesPageTwo : function() {
						var data = {
							preferredGaap : this.selectedGaap,
							preferredGaas : this.selectedGaas,
							preferredIndustry : this.selectedIndustry,
							preferredTopic : this.selectedTopic
						};
						$.ajax({
								url : UserRegistration.EDIT_PREFERENCES_CONTENT_API_END,
								data : JSON.stringify(data),
								type : "PUT",
								contentType : 'application/json; charset=utf-8',
								success : function(result) {
									this.buttonDisabled = false;
									UserRegistration.userProfile = result.data.userProfile;
	                                UserRegistration.setUserInfo();
	                                window.hideEditModals();
									$(".edit-profile-modal").removeClass("is-active");
									window.hideBlur();
								}.bind(this),
								error : function(xhr, httpStatusMessage,
										customErrorMessage) {
									if (xhr.status === UserRegistration.STATUS_CODE_UNAUTHORIZED) {
										UserRegistration.performActionOnUnauthorized();
									} else {
										this.hasEditPreferencesError = true;
										this.editEditPreferencesErrorMessage = getUserRegErrorMessage("internalError");
									}
									this.buttonDisabled = false;
								}.bind(this)
							});
					},
					previous : function() {
						$('#editPreferencesTerritoryLanguage').show();
						$('#editPreferencesContent').hide();
						window.editPreferencesTerritoryLanguage.resetModal();
					}
				}
			});
	window.editPreferencesContent = editPreferencesContent;
}(document, $, window.Vue, window._, window.getUserRegErrorMessage,
		window.UserRegistration));