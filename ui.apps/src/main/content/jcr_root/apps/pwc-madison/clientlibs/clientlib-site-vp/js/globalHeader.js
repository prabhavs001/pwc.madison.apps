(function ($, Vue, UserRegistration, showBlur) {
		//homepage suggested content slider
        // $(".technical-slider").on('init', function() {
        //     var containerEl = $(this).parents('.technical-box');
        //     containerEl.children('.technical-slider-loader').remove();
        //     containerEl.addClass('slider-showing');
        //     containerEl.removeClass('slider-loading');
        //     containerEl.css('height', containerEl.parents('.technical-box').outerHeight());
        //     setTimeout(function() {
        //         containerEl.css('height', '');
        //         containerEl.removeClass('slider-showing');
        //     }, 100);
        // });
        $('.technical-slider').slick({
            slidesToShow: 3,
            slidesToScroll: 3,
            dots: false,
            infinite: false,
            speed: 300,
            focusOnSelect: false,
            prevArrow: $('.prev'),
            nextArrow: $('.next'),
            responsive: [{
                    breakpoint: 1024,
                    settings: {
                        slidesToShow: 3,
                        slidesToScroll: 3,
                        infinite: true,
                        dots: false
                    }
                },
                {
                    breakpoint: 600,
                    settings: {
                        slidesToShow: 2,
                        slidesToScroll: 2
                    }
                },
                {
                    breakpoint: 480,
                    settings: {
                        slidesToShow: 1,
                        slidesToScroll: 1
                    }
                }
            ]
        });
    var headerDropdowns = document.getElementsByClassName('header-dropdown'), i, header;
		
	function initializeHeaderDropdown(el) {
		header = new Vue({
			el: el,
			data: {
				isUserLoggedIn: UserRegistration.isUserLoggedIn,
				helloMessage: '',
				helloMessagePlaceHolder: ''
			},
			mounted: function () {
				var $helloMessage = this.$refs.helloMessage;
				this.helloMessagePlaceHolder = $helloMessage ? $helloMessage.dataset.helloMessage : '';
				this.dataChange();
			},
			methods: { 
				logout: function () {
					$.ajax({
						url: UserRegistration.LOGOUT_API_PATH,
						type: 'POST',
						contentType: 'application/json; charset=utf-8',
						success: function (result) {
							var redirectionUrl = $("#redirect-logout-url").val();	
                            UserRegistration.setCookie(UserRegistration.USER_REIRECTION_PATH_COOKIE, redirectionUrl ? redirectionUrl : window.location.href);
                            window.location.href = UserRegistration.LOGOUT_REDIRECT_API_PATH;
						},
						error: function (xhr, httpStatusMessage, customErrorMessage) {
							if (xhr.status === UserRegistration.STATUS_CODE_UNAUTHORIZED) {
								UserRegistration.performActionOnUnauthorized();
							}
						}
					});
				},
                openProfileMenu: function() {
                    $(".body").removeClass("show-language-menu");
                    $(".body").removeClass("show-favorites-menu");
                    $(".body").toggleClass("show-account-menu");
                },
                dataChange: function() {
                    var userFrequentData = UserRegistration.getUserInfo(), firstName = userFrequentData ? userFrequentData.firstName : '';
                    try {
                        firstName = decodeURIComponent(window.escape(window.atob(firstName)));
                    } catch(err) {

                    }
                    this.helloMessage = this.helloMessagePlaceHolder.replace('{0}', firstName);
                }
			}
		});
	}

	$(document).on("userDataChange", function(){
		header.dataChange();
	});
		
	for(i = 0; i < headerDropdowns.length; i++){
		initializeHeaderDropdown(headerDropdowns[i]);
	}
	
	window.headerDropdowns = header;
}(jQuery, window.Vue, window.UserRegistration, window.showBlur));
