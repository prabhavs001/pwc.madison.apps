(function (document, $) {
    $.userInfo = {};
    const INTERNAL_PRIVATE = 'i_p';
    const INTERNAL_NORMAL = 'i_n';
    const EXTERNAL = 'i_e';
    const GB_TERRITORY_CODE = "gb";
    const UK_TERRITORY_CODE = "uk";

    $.userInfo.getUserLocale = function () {
        var user = {};
        var locale = {};
        var audience = {};
        var countryList = [];
        if ($("#search-results-vue") !== undefined && $("#search-results-vue").length > 0) {
            locale.col = $("#search-results-vue").data('cc');
            locale.list = $("#search-results-vue").data('pagelocale');
            audience.param = $("#search-results-vue").data('audience');
            audience.value = EXTERNAL;
            countryList.push(changeGBTerritoryCode(locale.list.split('_')[1]));
        }
        else{
        	locale.col = $('input[name="cc"]').val() !== undefined ? $('input[name="cc"]').val() : '';
        	locale.list = $('input[name="pageLocale"]').val() !== undefined ? $('input[name="pageLocale"]').val() : '';
        	 audience.param = $('input[name="audience"]').val() !== undefined ? $('input[name="audience"]').val() : '';
             audience.value = EXTERNAL;
             countryList.push(changeGBTerritoryCode(locale.list.split('_')[1]));
        }

        /*Set user locale based on preferences */
        if (window.UserRegistration.userInfo !== undefined) {
            var userProfile = window.UserRegistration.userInfo;
            let lang = getLanguage(locale.list);
            let primaryLocale = lang.concat('_').concat(userProfile.primaryTerritory);
            if (locale.list !== undefined && locale.list !== '') {
                locale.list = locale.list.concat('|').concat(primaryLocale);
            }
            else {
                locale.list = primaryLocale;
            }
            if(!countryList.includes(userProfile.primaryTerritory))
                countryList.push(changeGBTerritoryCode(userProfile.primaryTerritory));
            let preferredTerritories = userProfile.preferredTerritories;
            if (preferredTerritories) {
                $.each(preferredTerritories, function( index, value ) {
					 locale.list = locale.list.concat('|');
                     let preferedLocale = lang.concat('_').concat(value);
					 locale.list = locale.list.concat(preferedLocale);
					 if(!countryList.includes(value))
					     countryList.push(changeGBTerritoryCode(value));
				});
            }
            if (userProfile.isInternalUser) {
                if (userProfile.contentAccessInfo.privateGroups !== undefined && userProfile.contentAccessInfo.privateGroups.length > 0) {
                    audience.value = INTERNAL_PRIVATE;
                } else {
                    audience.value = INTERNAL_NORMAL;
                }
            }
            else {
                audience.value = EXTERNAL;
            }
            user.locale = locale;
            user.audience = audience;
            user.countryList = countryList;
            return user;
        }
        /*Set default page locale */
        else {
            user.locale = locale;
            user.countryList = countryList;
            return user;
        }

    };

    function getLanguage(locale) {
        let langauge = '';
        let localeArray = locale.split('_');
        if (localeArray.length > 1) {
            langauge = localeArray[0];
        }
        return langauge;
    }

    function changeGBTerritoryCode(territoryCode) {
      return territoryCode === GB_TERRITORY_CODE ? UK_TERRITORY_CODE : territoryCode ;
    }


}(document, $));