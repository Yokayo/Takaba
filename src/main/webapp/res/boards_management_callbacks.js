$(main)

function main(){
    BG = $(".transparent_bg")[0];
    container = $(".board_info_container")[0];
    $(".boards_list_item").click(function(){
        var board = $(this).attr("id");
        if(board == "+"){
            $(BG).show();
            $(container).show();
            $("#edited_board").val(this.id);
            $(".board_name").html("Добавить доску");
            $("#id").val('');
            $("#title").val('');
            $("#default_name").val('');
            $("#brief").val('');
            $("#max_file_size").val('');
            return;
        }
        var d = new XMLHttpRequest();
        d.open('GET', "/takaba/config?board=" + board);
        d.send();
        d.id = board;
        d.onreadystatechange = function(){
            if(this.readyState != 4)
                return;
            var info = JSON.parse(this.responseText);
            if(info.Status){
                alert(info.Message);
                return;
            }
            $(BG).show();
            $(container).show();
            $("#id").val(this.id);
            $("#title").val(info.BoardTitle);
            $("#default_name").val(info.DefaultName);
            $("#brief").val(info.BoardInfo);
            $("#max_file_size").val(info.MaxFileSize);
            $("#edited_board").val(this.id);
            $(".board_name").html(info.BoardTitle);
            $("#delayed_flushing_cb")[0].checked = (info.DelayedFlushing == 'true');
        }
    });
    
    $(".edit_board_container_close_button").click(function(){
        $(BG).hide();
        $(container).hide();
    });
    
    $(".submit_button").click(function(){
        var formData = new FormData($("#edit_board_form")[0]);
        var d = new XMLHttpRequest();
        d.open('POST', '/takaba/config');
        d.send(formData);
        d.onreadystatechange = function(){
            alert(this.responseText);
        }
    });
}