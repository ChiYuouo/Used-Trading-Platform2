$(function () {
    $("#accordion").accordion({heightStyle: "content"});
    $('.update_button').click(function () {
        var token = $('.token').val();
        var firstInfo = $(this).siblings(".first_info");
        var type = firstInfo.children("input").attr("type");
        var which_update = firstInfo.children("input").attr("class");
        var my_this = $(this);
        var value;
        if (type === "radio") {
            value = firstInfo.find("input[type='radio']:checked").val();
            if (!value) {
                alert('请选择性别');
                return;
            }
        } else {
            value = $.trim(firstInfo.children("input").val());
            if (!value) {
                firstInfo.children(".reqiure_enter").show(0);
                return;
            }
        }
        var postData = {token: token};
        postData[which_update] = value;
        $.ajax({
            url: '/certification.do',
            type: 'post',
            dataType: 'JSON',
            data: postData,
            success: function (data) {
                var result = data.result;
                if (result === 1) {
                    alert(data.msg || '更新成功');
                    my_this.parent().prev().children().text(value);
                    if (which_update === 'userName') {
                        $('.user_name_a').text(value);
                    }
                } else {
                    alert(data.msg || '更新失败，请检查信息格式');
                }
            },
            error: function () {
                alert('请求失败，请检查网络');
            }
        });
    });

    $('.update_password_button').click(function () {
        var token = $('.token').val();
        var oldPassword = $('.oldPassword').val();
        var newPassword = $('.newPassword').val();
        var confirmNewPassword = $('.confirmNewPassword').val();
        if (!oldPassword || !newPassword || !confirmNewPassword) {
            alert('请填写完整密码信息');
            return;
        }
        if (newPassword !== confirmNewPassword) {
            alert('两次输入的新密码不一致');
            return;
        }
        if (newPassword.length < 6 || newPassword.length > 32) {
            alert('新密码长度需在6到32位之间');
            return;
        }
        $.ajax({
            url: '/changePassword.do',
            type: 'post',
            dataType: 'JSON',
            data: {oldPassword: oldPassword, newPassword: newPassword, token: token},
            success: function (data) {
                if (data.result === 1) {
                    alert(data.msg || '密码修改成功');
                    $('.oldPassword, .newPassword, .confirmNewPassword').val('');
                } else {
                    alert(data.msg || '密码修改失败');
                }
            },
            error: function () {
                alert('请求失败，请检查网络');
            }
        });
    });

    $('.first_info input').bind("input propertychange change", function () {
        var val = $(this).val();
        if (val !== undefined && val !== '') {
            $(this).siblings(".reqiure_enter").hide(0);
        }
    });

    var $tip = $('.show_tip');
    if ($tip.length > 0 && $.trim($tip.text()) !== '') {
        alert($.trim($tip.text()));
    }
});
