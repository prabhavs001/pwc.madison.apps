$(function() {
    $('body').on('click','#image-modal-link', function() {
    $('.pwc-image-modal').addClass("is-active");
    var srcImage = $(this).parent().find('img').attr('src'),
    //for example scrImage = /content/dam/pwc-madison/ditaroot/us/en/pwc/206H.tif/_jcr_content/renditions/cq5dam.web.1280.1280.jpeg
    originalSrcImage = srcImage.substr(0,srcImage.lastIndexOf(srcImage.match(/\/([_])?jcr[:|_]content/gm)[0]));
    //for example originalSrcImage = /content/dam/pwc-madison/ditaroot/us/en/pwc/206H.tif
    $('#image-modal').find('img').attr('src',originalSrcImage);
    });
});
