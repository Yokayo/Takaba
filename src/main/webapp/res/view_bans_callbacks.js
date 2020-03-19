$(main);

function main(){
    $(".unban_link").click(function(){
        var d = new XMLHttpRequest();
        d.open('POST', '/takaba/unban?id=' + $(this).attr('data-num'));
        var this_ = this;
        d.send();
        d.onreadystatechange = function(){
            if(this.readyState != 4)
                return;
            alert(this.responseText);
            if(JSON.parse(this.responseText).Status == "0"){
                $(".bans_table_" + $(this_).attr('data-num')).fadeOut(500, function(){});
            }
        }
    });
}