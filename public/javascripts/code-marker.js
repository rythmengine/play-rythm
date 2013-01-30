$(window).keydown(function (e) {
    // ctrl
    if (e.which == 17) {
        $('body .code-marker').addClass('code-marker-active');
    }
}).keyup(function(e) {
    if (e.which == 17) {
        $('body .code-marker').removeClass('code-marker-active');
    }
});

$(function(){
    $('body').on('click', '.code-marker', function(e){
        e.stopPropagation();
        e.stopImmediatePropagation();
        var $me = $(this), src = $me.data('src'), line = $me.data('line');
        $.get('http://localhost:8091?message='+ src + ":" + line);
        return false;
    });
})