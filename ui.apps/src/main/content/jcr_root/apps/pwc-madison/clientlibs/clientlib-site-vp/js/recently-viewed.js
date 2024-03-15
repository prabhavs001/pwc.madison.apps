$(document).ready(function () {
  var recentlyTiles, limitedword,
    showmoreText = $('.recently-show-more span').data('showmoreText'),
    showlessText = $('.recently-show-more span').data('showlessText');
  recentlyTiles = $('.recently-viewed-slider .tile').length;
  limitedword = $('.limitedword');
  $('.recently-viewed-slider').slick({
    dots: false,
    infinite: false,
    prevArrow: "<span class='icon-caret-left' />",
    nextArrow: "<span class='icon-caret-right' />",
    speed: 300,
    focus: false,
    slidesToShow: Math.min(4, recentlyTiles),
    slidesToScroll: 4,
    rows: 0,
    responsive: [{
      breakpoint: 1024,
      settings: {
        slidesToShow: 3,
        slidesToScroll: 3,
        infinite: false,
        dots: false
      }
    },
    {
      breakpoint: 769,
      settings: "unslick"
    }
    ]
  });
  limitedword.each(function (index, value) {
    $clamp(value, {
      clamp: 4
    });
  });
  /* toggle the length of recently viewed boxes */
  $('.recently-show-more').on("click", function (e) {
    e.preventDefault();
    var toggleDiv, recentlyToggleParent;
    toggleDiv = $(this);
    toggleDiv.parent(".recently-viewed-slider").toggleClass("recently-more");
    recentlyToggleParent = toggleDiv.parents(".recently-viewed-slider").hasClass("recently-more");
    if (recentlyToggleParent) {
      $(".recently-viewed-slider > .tile").show();
      toggleDiv.find("span").text(showlessText);
      toggleDiv.find(".arrow").addClass("arrowUp");
    } else {
      toggleDiv.find("span").text(showmoreText);
      $('.recently-viewed-slider > .tile').hide();
      $('.recently-viewed-slider > .tile:lt(2)').show();
      toggleDiv.find(".arrow").removeClass("arrowUp");
    }
  });
});
