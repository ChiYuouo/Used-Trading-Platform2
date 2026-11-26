/**
 * Header navigation & single-session WebSocket check
 */
$(function () {
    $('body').click(function (e) {
        if ($(e.target).closest('.personal_nav, .user_name_a').length === 0) {
            if ($('.personal_nav').is(":visible")) {
                $('.personal_nav').fadeOut(150);
            }
        }
    });

    $('.user_name_a').mouseenter(function () {
        if (!$('.personal_nav').is(":visible")) {
            $('.personal_nav').stop(true, true).fadeIn(150);
        }
    });

    $('.personal_nav').mouseleave(function () {
        if ($('.personal_nav').is(":visible")) {
            $('.personal_nav').stop(true, true).fadeOut(150);
        }
    });

    $('.search_icon').click(function () {
        var name = $.trim($('.nav_search_input').val());
        if (name === '') {
            window.location.href = '/mall_page.do';
        } else {
            window.location.href = '/findShopByName.do?name=' + encodeURIComponent(name);
        }
    });

    $('.nav_search_input').keypress(function (e) {
        if (e.which === 13) {
            $('.search_icon').click();
        }
    });
});

jQuery(document).ready(function ($) {
    var phone = $('#user_name_a').attr('value');
    if (phone && phone !== 'wsk') {
        var host = window.location.host;
        var me = new Date().getTime();
        var protocol = window.location.protocol === 'https:' ? 'wss://' : 'ws://';
        var websocket = new WebSocket(protocol + host + "/sockjs/webSocketIMServer");
        websocket.onopen = function () {
            console.log("websocket连接上");
            websocket.send(phone + "," + me + ",start");
        };
        websocket.onmessage = function (evnt) {
            var result = evnt.data;
            if (result == "error") {
                alert("该账号在其他地方登录了，请检查是否为本人操作，防止密码丢失！！！");
                window.location.href = '/logout.do';
                return;
            }
            setTimeout(function () {
                if (websocket.readyState === WebSocket.OPEN) {
                    messageHandle();
                }
            }, 2000);
        };
        websocket.onerror = function () {
            console.log("websocket错误");
        };
        websocket.onclose = function () {
            console.log("websocket关闭");
        };
        function messageHandle() {
            websocket.send(phone + "," + me + ",send");
        }
    }
});
