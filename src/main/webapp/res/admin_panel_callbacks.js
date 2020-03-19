$(main);

function main(){
    window.BG = $(".transparent_bg")[0];
    $(".add_mod_link").click(function(e){
        e.preventDefault();
        if(this.isOpen){
            $(".mods_action_list").hide();
            this.isOpen = false;
        }else{
            $(".mods_action_list").show();
            this.isOpen = true;
        }
    })[0].isOpen = false;
    
    $(".add_moder_link").click(function(){
        $(".add_moder_form_container").show();
        $(window.BG).show();
    });
    
    $(".add_moder_form_close_button").click(function(){
        $(".add_moder_form_container").hide();
        $(window.BG).hide();
    });
    
    $(".add_board_form_close_button").click(function(){
        $(".add_board_form_container").hide();
        $(window.BG).hide();
    });
    
    e = document.getElementsByClassName("add_moder_form_button")[0];
    e.formData = document.getElementsByClassName("add_moder_form")[0];
    e.addEventListener('click', function(){
        var form_data = new FormData(this.formData);
        var al = parseInt(form_data.get("level"));
        if(isNaN(al) || al > 4 || al < 1){
            $(".add_moder_lvl_field").css('border', '1px solid red');
            alert("Неправильный уровень доступа");
            return;
        }
        d = new XMLHttpRequest();
        d.open('POST', '/takaba/add_moder');
        d.send(form_data);
            d.onreadystatechange = function(){
                if(this.readyState == 4)
                    alert(this.responseText);
            }
    });
    
    e = document.getElementsByClassName("edit_moder_link")[0];
    e.formContainer = document.getElementsByClassName("edit_moder_form_container")[0];
    e.isOpen = false;
    e.addEventListener('click', function(){
            this.formContainer.style.display = "block";
            window.BG.style.display = "block";
        });
    a = document.getElementsByClassName("edit_moder_form_close_button")[0];
    a.formContainer = e.formContainer;
    a.addEventListener('click', function(){
        this.formContainer.style.display = "none";
        window.BG.style.display = "none";
    });
    e = document.getElementsByClassName("edit_moder_form_button")[0];
    e.formData = document.getElementsByClassName("edit_moder_form")[0];
    e.addEventListener('click', function(){
        var form_data = new FormData(this.formData);
        var al = parseInt(form_data.get("level"));
        if(isNaN(al) || al > 4 || al < 1){
            $(".edit_moder_lvl_field").css('border', '1px solid red');
            alert("Неправильный уровень доступа");
            return;
        }
        d = new XMLHttpRequest();
        d.open('POST', '/takaba/edit_moder');
        d.send(form_data);
            d.onreadystatechange = function(){
                if(this.readyState == 4)
                    alert(this.responseText);
            }
    });
    
    
    e = document.getElementsByClassName("view_moders_link")[0];
    window.modTableContainer = document.getElementsByClassName("view_moders_form_container")[0];
    window.modTable = document.getElementsByClassName("moders_table")[0];
    e.addEventListener('click', function(){
        window.BG.style.display = 'block';
        d = new XMLHttpRequest();
        d.open('GET', '/takaba/mods_json');
        d.send();
            d.onreadystatechange = function(){
                if(this.readyState != 4)
                    return;
                var data = JSON.parse(this.responseText);
                for(var a = 0; a < data.length; a++){
                    var info_row = document.createElement('tr');
                    info_row.innerHTML = '<td class="mod_info_cell">' + data[a].name + '</td><td class="mod_info_cell">' + data[a].level + '</td><td class="mod_info_cell">' + data[a].boards + '</td><td class="mod_info_cell">' + data[a].key + '</td>';
                    window.modTable.appendChild(info_row);
                }
                $(window.modTableContainer).show();
                $(window.modTableContainer).css({top: window.innerHeight/2 + 'px', left: window.innerWidth/2 + 'px', transform: 'translate(-50%, -50%)'});
            }
    });
        a = document.getElementsByClassName("view_moders_form_close_button")[0];
    a.addEventListener('click', function(){
        window.modTableContainer.style.display = "none";
        window.BG.style.display = "none";
        while(window.modTable.children.length > 1)
            window.modTable.removeChild(window.modTable.children[window.modTable.children.length-1]);
    });
    
    $(".remove_moder_link").click(function(){
        $(window.BG).show();
        $(".delete_moder_form_container").show();
    });
    
    $(".delete_moder_form_button").click(function(){
        d = new XMLHttpRequest();
        d.open('POST', '/takaba/delete_moder');
        var form_data = new FormData($(".delete_moder_form")[0]);
        d.send(form_data);
            d.onreadystatechange = function(){
                if(this.readyState != 4)
                    return;
                alert(JSON.parse(this.responseText).Message);
            }
    });
    
    $(".delete_moder_form_close_button").click(function(){
        $(window.BG).hide();
        $(".delete_moder_form_container").hide();
    });
    
    $(".add_board_form_close_button").click(function(){
        $(window.BG).hide();
        $(".add_board_form_container").hide();
    });
}