/**
 * Mall sidebar category filter, pagination, and add-to-cart
 */
$(function () {
    insertShopCar();
    bindDetailClick();
    var type_list = getTypeList();

    $('.my_type_div ul li').hover(function () {
        var temp_class = $(this).attr("class");
        var idx = parseInt(temp_class.replace('type_', ''), 10) - 1;
        if (isNaN(idx) || !type_list[idx]) return;
        addList(idx);
        var rect = $('.my_type_div')[0].getBoundingClientRect();
        $('.particular_type_div').css({
            top: Math.max(120, rect.top) + 'px',
            left: (rect.right + 12) + 'px'
        }).show(0);
    });

    function addList(id) {
        var which = type_list[id];
        var my_string = "";
        for (var j = 0; j < which.length; j++) {
            var type_i = which[j];
            var arr = type_i.content;
            var a_list = "";
            for (var i = 0; i < arr.length; i++) {
                a_list += "<a id='" + arr[i].id + "' class='shop_sort'>" + arr[i].name + "</a>";
            }
            my_string += "<div class='one_part'><div class='type_title_div'>" +
                "<span class='type_border_span'>1</span><h3>" + type_i.name + "</h3></div><div " +
                "class='type_goods_list'>" + a_list + "</div></div>";
        }
        $('.particular_type_div').html(my_string);
        $('.type_goods_list a.shop_sort').click(function () {
            var sortId = $(this).attr('id');
            var sortName = $(this).text();
            hideParticular();
            $.ajax({
                url: '/selectBySort.do',
                type: 'post',
                dataType: 'JSON',
                data: {sort: sortId},
                success: function (data) {
                    renderProducts(data, '分类“' + sortName + '”下暂无商品');
                }
            });
        });
    }

    function renderProducts(data, emptyMsg) {
        var $container = $('.all_product .product_content_div');
        $container.html('');
        if (!data || data.length === 0) {
            $container.html("<div style='grid-column:1/-1;background:#fff;border:1px solid #e5e7eb;border-radius:12px;padding:48px;text-align:center;color:#64748b;'>" + (emptyMsg || '暂时没有该分类的商品') + "</div>");
            return;
        }
        for (var i = 0; i < data.length; i++) {
            var item = data[i];
            $container.append(
                "<div class='detail_product'>" +
                "<input type='hidden' value='" + item.id + "'/>" +
                "<div class='product_img_div'><a href='/selectById.do?id=" + item.id + "'>" +
                "<img class='show_img' src='" + item.image + "' title='" + item.name + "' onerror=\"this.onerror=null;this.src='/img/home/feature_prodects/cont2.jpg'\"/>" +
                "</a></div>" +
                "<span class='detail_product_name' value='" + item.id + "' title='" + item.name + "'>" + item.name + "</span>" +
                "<p class='show_tip' title='" + (item.remark || '') + "'>" + (item.remark || '') + "</p>" +
                "<div class='card_bottom'>" +
                "<span class='detail_product_cost'>￥" + item.price + "</span>" +
                "<span class='detail_buy product_1' value='" + item.id + "'>加入购物车</span>" +
                "</div>" +
                "</div>"
            );
        }
        bindDetailClick();
        insertShopCar();
    }

    $('header').click(function () {
        hideParticular();
    });

    bindClick();

    function bindClick() {
        $('.pagination_div ul li').off('click').on('click', function () {
            var cur = $(".pagination_div ul li.current_page").children("a").html();
            $(".pagination_div ul li.current_page").removeClass("current_page");
            $(this).addClass("current_page");
            var which_click = $(this).children("a").html();
            if (cur !== which_click) {
                selectByCounts(which_click);
            }
        });
    }

    $('.pagination_lt').click(function () {
        var current = $('.pagination_div ul li.current_page');
        var temp = parseInt(current.children("a").html(), 10);
        if (temp <= 1) return false;
        updateCurrent(current, 1, temp);
        selectByCounts($('.pagination_div ul li.current_page').children("a").html());
    });

    $('.pagination_gt').click(function () {
        var current = $('.pagination_div ul li.current_page');
        var temp = parseInt(current.children("a").html(), 10);
        var maxPage = parseInt($('.pagination_div').attr('data-total-pages'), 10) || 99;
        if (temp >= maxPage) return false;
        updateCurrent(current, 2, temp);
        selectByCounts($('.pagination_div ul li.current_page').children("a").html());
    });

    function updateCurrent(current, to, temp) {
        var num = current.nextAll().length;
        if (to === 1) {
            if (num === 4) {
                current.siblings(":last").remove();
                current.before("<li><a>" + (temp - 1) + "</a></li>");
            }
            if (num === 3 && temp - 2 >= 1) {
                current.siblings(":last").remove();
                current.siblings(":first").before("<li><a>" + (temp - 2) + "</a></li>");
            }
            current.removeClass("current_page");
            current.prev().addClass("current_page");
        } else {
            if (num === 0) {
                current.siblings(":first").remove();
                current.after("<li><a>" + (temp + 1) + "</a></li>");
            }
            if (num === 1) {
                current.siblings(":first").remove();
                current.siblings(":last").after("<li><a>" + (temp + 2) + "</a></li>");
            }
            current.removeClass("current_page");
            current.next().addClass("current_page");
        }
        bindClick();
    }

    function selectByCounts(currentCounts) {
        $.ajax({
            url: '/selectByCounts.do',
            type: 'post',
            dataType: 'JSON',
            data: {counts: currentCounts},
            success: function (data) {
                renderProducts(data, '当前页暂无商品');
            }
        });
    }

    function bindDetailClick() {
        $('.detail_product_name').off('click').on('click', function () {
            var id = $(this).attr('value');
            window.location.href = '/selectById.do?id=' + id;
        });
    }

    function insertShopCar() {
        $('.detail_buy').off('click').on('click', function () {
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
    }
});

function hideParticular() {
    if ($('.particular_type_div').is(":visible")) {
        $('.particular_type_div').hide(0);
    }
}
