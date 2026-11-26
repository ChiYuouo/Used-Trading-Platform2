/**
 * Home page carousel & quick add-to-cart
 */
$(function () {
    var $slides = $('.my_slide');
    var currentIdx = $slides.index($('.my_slide.current'));
    if (currentIdx < 0) currentIdx = 0;

    function showSlide(idx) {
        $slides.removeClass('current').hide();
        $slides.eq(idx).addClass('current').css({display: 'flex', opacity: 0}).animate({opacity: 1}, 350);
        currentIdx = idx;
    }

    var timer = setInterval(function () {
        showSlide((currentIdx + 1) % $slides.length);
    }, 4500);

    $('.right_border, .right_turn').click(function (e) {
        e.stopPropagation();
        clearInterval(timer);
        showSlide((currentIdx + 1) % $slides.length);
        timer = setInterval(function () { showSlide((currentIdx + 1) % $slides.length); }, 4500);
    });

    $('.left_border, .left_turn').click(function (e) {
        e.stopPropagation();
        clearInterval(timer);
        showSlide((currentIdx - 1 + $slides.length) % $slides.length);
        timer = setInterval(function () { showSlide((currentIdx + 1) % $slides.length); }, 4500);
    });

    $('.buy').click(function () {
        var id = $(this).attr('value');
        $.ajax({
            url: '/insertGoodsCar.do',
            dataType: 'JSON',
            type: 'post',
            data: {id: id},
            success: function (data) {
                var result = data.result;
                if (result == '2') {
                    alert('您还未登录，请先登录！！！');
                    window.location.href = '/login.do';
                } else if (result == '1') {
                    alert(data.msg || '加入购物车成功');
                } else {
                    alert(data.msg || '加入购物车失败');
                }
            }
        });
    });
});
