/**
 * Category cascade & image preview for publish/require forms
 */
$(function () {
    var typeList = getTypeList();
    var curFirst = 0;
    var curSecond = 0;
    var initialSort = $('.choose_third_type').attr('data-current-sort');
    if (initialSort) {
        var found = false;
        for (var f = 0; f < typeList.length && !found; f++) {
            for (var s = 0; s < typeList[f].length && !found; s++) {
                var items = typeList[f][s].content;
                for (var t = 0; t < items.length; t++) {
                    if (String(items[t].id) === String(initialSort)) {
                        curFirst = f;
                        curSecond = s;
                        found = true;
                        break;
                    }
                }
            }
        }
        if (found) {
            $('.choose_first_type option').prop('selected', false);
            $('.choose_first_type option#type_' + (curFirst + 1)).prop('selected', true);
        }
    }
    updateSecondSelect();
    if (initialSort) {
        $('.choose_second_type option').prop('selected', false);
        $('.choose_second_type option#' + (10000 + curSecond)).prop('selected', true);
        $('.choose_third_type').val(initialSort);
    }

    $(".upload_img_input").change(function () {
        if (!this.files || !this.files[0]) return;
        var objUrl = getObjectURL(this.files[0]);
        $('.show_choose_img').attr("src", objUrl).css({display: 'block', opacity: 1});
    });

    function getObjectURL(file) {
        if (window.createObjectURL !== undefined) {
            return window.createObjectURL(file);
        } else if (window.URL !== undefined) {
            return window.URL.createObjectURL(file);
        } else if (window.webkitURL !== undefined) {
            return window.webkitURL.createObjectURL(file);
        }
        return null;
    }

    $('.choose_first_type').change(function () {
        var getSelect = $(this).children('option:selected').attr("id");
        curFirst = parseInt(getSelect.replace("type_", ""), 10) - 1;
        curSecond = 0;
        updateSecondSelect();
    });

    $('.choose_second_type').change(function () {
        var getSelect = $(this).children('option:selected').attr("id");
        curSecond = parseInt(getSelect, 10) - 10000;
        updateThirdSelect();
    });

    function updateThirdSelect() {
        var temp = typeList[curFirst];
        var thirdText = "";
        var res = temp[curSecond].content;
        for (var j = 0; j < res.length; j++) {
            var rel_type = res[j];
            thirdText += "<option id='" + rel_type.id + "' value='" + rel_type.id + "'" + (j === 0 ? " selected='selected'" : "") + ">" + rel_type.name + "</option>";
        }
        $('.choose_third_type').html(thirdText);
    }

    function updateSecondSelect() {
        var temp = typeList[curFirst];
        var secondText = "";
        var thirdText = "";
        for (var i = 0; i < temp.length; i++) {
            var which = temp[i];
            secondText += "<option id='" + (10000 + i) + "'>" + which.name + "</option>";
            if (i === curSecond) {
                var res = which.content;
                for (var j = 0; j < res.length; j++) {
                    var rel_type = res[j];
                    thirdText += "<option id='" + rel_type.id + "' value='" + rel_type.id + "'" + (j === 0 ? " selected='selected'" : "") + ">" + rel_type.name + "</option>";
                }
            }
        }
        $('.choose_second_type').html(secondText);
        $('.choose_third_type').html(thirdText);
    }

    var $tip = $('.show_tip');
    if ($tip.length > 0 && $.trim($tip.text()) !== '') {
        alert($.trim($tip.text()));
    }
});
