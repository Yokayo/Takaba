$(main);

function main(){ // добавляем модераторские функции к выпадающему списку
    var e = window.select_list;
    a = document.createElement('a');
    a.setAttribute('class', 'mod_list_delete_post mod_list_top_el');
    a.innerHTML = 'Удалить пост';
    $(a).click(function(){ // функция удаления поста
        window.select_list.style.display = "none";
        window.select_list_open = false;
        var d = new XMLHttpRequest();
        var formData = new FormData();
        formData.append('board', window.board);
        var $cb = $(".turnmeoff");
        formData.append("post", window.select_list.dataNum.split("_")[0]);
        for(a = 0; a < $cb.length; a++){
            var cb = $cb.get(a);
            if(cb.checked && !data.includes(cb.getAttribute("data-num")))
                formData.append('post', cb.getAttribute("data-num"));
        }
        d.open('POST', '/takaba/delete_posts');
        d.send(formData);
        d.onreadystatechange = function(){
            if(this.readyState == 4){
                showAlert(JSON.parse(this.responseText).Message);
            }
        };
    });
    e.appendChild(a);
    //----------------------------------------------------------------------
    a = document.createElement('a');
    a.setAttribute('class', 'mod_list_ban_post');
    a.innerHTML = 'Забанить';
    $(a).click(function(){ // функция бана
        window.select_list.style.display = "none";
        window.select_list_open = false;
        var container = document.createElement('div');
        $(container).css({background: 'rgb(221, 221, 221)', // inline стили, чтоб не заводить отдельный css для меню бана
        position: 'fixed',
        font: 'Verdana, Sans-Serif',
        color: 'rgb(21, 21, 21)',
        'box-shadow': '5px 5px 5px gray',
        border: '1px solid gray',
        top: window.innerHeight/10 + 'px',
        left: window.innerWidth/3 + 'px'})
        .addClass("ban_menu_container");
        a = document.createElement('div');
        a.innerHTML = 'Меню бана<span class="ban_menu_close_button" style="float: right; cursor: pointer">X</span>';
        $(a).css({background: '#ADADAD', display: 'block', margin: '2px', padding: '2px', 'text-align': 'center'});
        container.appendChild(a);
        var reason_select_container = document.createElement('div');
        reason_select_container.innerHTML = 'Причина:';
        $(reason_select_container).css({margin: '2px'});
        container.appendChild(reason_select_container);
        a = document.createElement('select');
        $(a).css({margin: '4px',
        'overflow-x': 'hidden',
        'overflow-y': 'scroll',
        'vertical-align': 'top'})
        .addClass("ban_reason_selector");
        $(a).attr('size', 10);
        for(var c = 0; c < window.banReasons.length; c++){
            if(window.banReasons[c] == '')
                continue;
            a.innerHTML += '<option class="ban_reason_option" value="' + c + '">' + window.banReasons[c] + '</option>';
        }
        reason_select_container.appendChild(a);
        a = document.createElement('div');
        $(a).css({
            margin: 2
        });
        a.innerHTML = 'До: <input type="date" class="unban_date_picker"></input> <input type="time" class="unban_time_picker"></input>'
        + ' <label for="ban_global_cb">На всех досках</label><input type="checkbox" class="ban_global_cb" id="ban_global_cb"></input>'
        + ' <label for="ban_permanent_cb">Перманентный</label><input type="checkbox" class="ban_permanent_cb" id="ban_permanent_cb"></input>';
        container.appendChild(a);
        a = document.createElement('div');
        $(a).css({'text-align': 'center', 'margin-top': 5, 'margin-bottom': 5});
        a.innerHTML = '<button style="padding: 2px 10px 2px 10px; background-attachment: scroll; background-repeat: repeat;background-image: none;background-position: 0% 0%; background-size: autobackground-origin: padding-box;background-clip: border-box;background-color: rgb(234, 234, 234);border: 1px solid rgb(202, 202, 202); cursor: pointer;" class="ban_button">Забанить</button>';
        container.appendChild(a);
        document.body.appendChild(container);
        var today = new Date();
        var month = today.getMonth()+1;
        $(".unban_time_picker").val("20:00");
        $(".unban_date_picker").val(today.getFullYear() + "-" + (month < 10 ? "0" + month : month) + "-" + today.getDate());
        $(".ban_menu_close_button").click(function(){$(".ban_menu_container").remove();});
        $(".ban_permanent_cb").change(function(){if(this.checked){$(".unban_date_picker, .unban_time_picker").prop('disabled', true);}else{$(".unban_date_picker, .unban_time_picker").prop('disabled', false);}});
        $(".ban_button").click(function(){
            var time_validation = $(".unban_time_picker").val(); // валидация времени
            if((time_validation == null || time_validation.length < 5 || time_validation.split(":").length < 2) && !$(".ban_permanent_cb")[0].checked){
                showAlert("Некорректное время.");
                return;
            }
            time_validation = $(".unban_date_picker").val(); // валидация даты
            if((time_validation == null || time_validation.length < 10 || time_validation.split("-").length < 3) && !$(".ban_permanent_cb")[0].checked){
                showAlert("Некорректная дата.");
                return;
            }
            if(!$(".ban_reason_option:selected").length){
                showAlert("Укажите причину бана.");
                return;
            }
            var formData = new FormData(); // запрос
            formData.append("board", board);
            formData.append("post", select_list.dataNum.split("_")[0]);
            formData.append("reason", $(".ban_reason_option:selected").val());
            if($(".ban_permanent_cb")[0].checked)
                formData.append("permanent", "true");
            else{
                formData.append("year", $(".unban_date_picker").val().split("-")[0]);
                formData.append("month", $(".unban_date_picker").val().split("-")[1]);
                formData.append("day", $(".unban_date_picker").val().split("-")[2]);
                formData.append("hour", $(".unban_time_picker").val().split(":")[0]);
                formData.append("min", $(".unban_time_picker").val().split(":")[1]);
            }
            if($(".ban_global_cb")[0].checked)
                formData.append("global", "true");
            var d = new XMLHttpRequest();
            d.open('POST', "/takaba/ban");
            d.send(formData);
            d.onreadystatechange = function(){
                if(this.readyState != 4)
                    return;
                alert(JSON.parse(this.responseText).Message);
                $(".ban_menu_container").remove();
            }
        });
    });
    e.appendChild(a);
}