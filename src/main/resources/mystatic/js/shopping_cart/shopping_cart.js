/**
 * Created by alone on 2017/5/18.
 */
$(function () {
    calSum();

    function syncQuantity($row, newQty, oldQty) {
        var cartId = $row.attr('data-cart-id');
        $.ajax({
            url: '/updateGoodsCar.do',
            dataType: 'JSON',
            type: 'post',
            data: {id: cartId, quantity: newQty},
            success: function (data) {
                if (data.result == 1) {
                    $row.find('.number').html(newQty);
                    var cost = parseFloat($row.find('.cost span').html());
                    $row.find('.per_sum span').html(returnFloat(cost * newQty));
                    calSum();
                } else {
                    alert(data.msg || '数量更新失败');
                    $row.find('.number').html(oldQty);
                    calSum();
                }
            },
            error: function () {
                alert('网络错误，数量更新失败');
                $row.find('.number').html(oldQty);
            }
        });
    }

    // -号，不能低于 1
    $('span.minus').click(function () {
        var $row = $(this).closest('tr.table_content');
        var num = parseInt($(this).siblings(".number").html(), 10);
        if (num <= 1) {
            return;
        }
        syncQuantity($row, num - 1, num);
    });

    // +号，不超过库存
    $('span.add').click(function () {
        var $row = $(this).closest('tr.table_content');
        var num = parseInt($(this).siblings(".number").html(), 10);
        var stock = parseInt($row.attr('data-stock'), 10);
        if (!isNaN(stock) && num >= stock) {
            alert('已达到该商品最大库存（' + stock + '）');
            return;
        }
        syncQuantity($row, num + 1, num);
    });

    $('.table_content td.input_checkbox input').change(function () {
        calSum();
    });

    $('#all').change(function () {
        var checked = $(this).is(":checked");
        $("input[name='checkbox']").each(function () {
            this.checked = checked;
        });
        calSum();
    });

    // 保留两位小数
    function returnFloat(value) {
        var v = Math.round(parseFloat(value) * 100) / 100;
        if (isNaN(v)) {
            return "0.00";
        }
        var xsd = v.toString().split(".");
        if (xsd.length === 1) {
            return v.toString() + ".00";
        }
        if (xsd[1].length < 2) {
            return v.toString() + "0";
        }
        return v.toString();
    }

    // 计算总金额
    function calSum() {
        var sum = 0;
        $('.cart_content table tr.table_content').each(function () {
            var isCheck = $(this).children("td.input_checkbox").children("input").is(":checked");
            if (isCheck) {
                var per = parseFloat($(this).children(".per_sum").children("span").html());
                if (!isNaN(per)) {
                    sum += per;
                }
            }
        });
        $('.end_pay').children(".all_sum").children("span").html(returnFloat(sum));
    }

    $('.deleteShopCar').click(function () {
        var r = confirm('确定从购物车删除该商品吗？');
        if (r === true) {
            var id = $(this).attr('value');
            var sid = $(this).parent().siblings(".show_img").children().attr("value");
            $.ajax({
                url: '/deleteShopCar.do',
                dataType: 'JSON',
                type: 'post',
                data: {id: id, sid: sid},
                success: function (data) {
                    var result = data.result;
                    if (result == 2) {
                        alert('您还没有登录，请先登录');
                        window.location.href = '/login.do';
                    } else if (result == 1) {
                        alert("删除成功");
                        window.location.href = '/shopping_cart.do';
                    } else {
                        alert(data.msg || '删除失败，请检查网络');
                    }
                }
            });
        }
    });

    // 结算下单
    $('.pay_button').click(function () {
        var cartIds = [];
        $('.cart_content table tr.table_content').each(function () {
            var isCheck = $(this).children("td.input_checkbox").children("input").is(":checked");
            if (isCheck) {
                cartIds.push($(this).attr('data-cart-id'));
            }
        });
        if (cartIds.length === 0) {
            alert('请至少勾选一件需要结算的商品');
            return;
        }
        var address = $.trim($('#order_address').val());
        var context = $.trim($('#order_context').val());
        if (!address) {
            alert('请填写收货地址');
            $('#order_address').focus();
            return;
        }
        var total = $('.end_pay .all_sum span').html();
        if (!confirm('确认提交订单并结算？总金额：￥' + total + '元')) {
            return;
        }
        $.ajax({
            url: '/submitOrder.do',
            type: 'post',
            dataType: 'JSON',
            data: {cartIds: cartIds.join(','), address: address, context: context},
            success: function (data) {
                if (data.result == 2) {
                    alert('您还没有登录，请先登录');
                    window.location.href = '/login.do';
                } else if (data.result == 1) {
                    alert('下单成功！订单号：' + data.data);
                    window.location.href = '/my_orders.do';
                } else {
                    alert(data.msg || '下单失败，请稍后重试');
                }
            },
            error: function () {
                alert('网络错误，提交订单失败');
            }
        });
    });
});
