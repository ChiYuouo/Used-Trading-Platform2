/**
 * Created by wsk1103 on 2017/5/21.
 */
$(function () {
    $('.send_comment_button').click(function () {
        var value = $.trim($('.comment_textarea').val());
        var token = $('.token').val();
        var $comment = $('.comment_content');
        var id = $('.id').val();
        if (!value) {
            alert('请输入留言内容');
            return;
        }
        $.ajax({
            url: '/insertShopContext.do',
            type: 'post',
            dataType: 'JSON',
            data: {id: id, context: value, token: token},
            success: function (data) {
                var result = data.result;
                if (result == '2') {
                    alert('请先登录！！！');
                    window.location.href = '/login.do';
                } else if (result == '1') {
                    var name = data.username;
                    var time = data.time;
                    var context = data.context;
                    var cc = "<div class='one_comment'><span class='username'>用户：" + name + "</span><span class='time'>发表于：" + time + "</span><p class='content'>" + context + "</p></div>";
                    $comment.prepend(cc);
                    $('.comment_textarea').val('');
                } else {
                    alert(data.msg || "发表留言失败，请先检查格式");
                }
            },
            error: function () {
                alert('网络错误，请稍后重试');
            }
        });
    });

    $('.buy_button').click(function () {
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

    $('.collect_button').click(function () {
        var $btn = $(this);
        var sid = $btn.attr('data-sid');
        var collected = $btn.attr('data-collected') === 'true';
        if (!collected) {
            $.ajax({
                url: '/addUserCollection.do',
                dataType: 'JSON',
                type: 'post',
                data: {sid: sid},
                success: function (data) {
                    if (data.result == '2') {
                        alert('您还未登录，请先登录！！！');
                        window.location.href = '/login.do';
                    } else if (data.result == '1') {
                        $btn.attr('data-collected', 'true').text('已收藏');
                        if (data.data) {
                            $btn.attr('data-ucid', data.data);
                        }
                        alert(data.msg || '收藏成功');
                    } else {
                        alert(data.msg || '收藏失败');
                    }
                }
            });
        } else {
            var ucid = $btn.attr('data-ucid') || '';
            $.ajax({
                url: '/deleteUserCollection.do',
                dataType: 'JSON',
                type: 'post',
                data: {ucid: ucid, sid: sid},
                success: function (data) {
                    if (data.result == '2') {
                        alert('您还未登录，请先登录！！！');
                        window.location.href = '/login.do';
                    } else if (data.result == '1') {
                        $btn.attr('data-collected', 'false').attr('data-ucid', '').text('收藏商品');
                        alert(data.msg || '已取消收藏');
                    } else {
                        alert(data.msg || '取消收藏失败');
                    }
                }
            });
        }
    });

    $('.delete_comment_btn').click(function () {
        if (!confirm('确定删除这条留言吗？')) {
            return;
        }
        var $btn = $(this);
        var cid = $btn.attr('data-id');
        $.ajax({
            url: '/deleteShopContext.do',
            type: 'post',
            dataType: 'JSON',
            data: {id: cid},
            success: function (data) {
                if (data.result == '1') {
                    $btn.closest('.one_comment').remove();
                } else {
                    alert(data.msg || '删除失败');
                }
            }
        });
    });
});
