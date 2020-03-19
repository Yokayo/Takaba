document.addEventListener('DOMContentLoaded', main);

function main(){
    var e = document.getElementsByClassName("reports_checkall")[0];
    window.checkboxes = document.getElementsByClassName("reports_check");
    e.addEventListener('click', function(){
        for(a = 0; a < this.checkboxes.length; a++)
            this.checkboxes[a].checked = this.checked;
    });
    
    e = document.getElementsByClassName("delete_reports_link")[0];
    e.addEventListener('click', function(){
        if(window.checkboxes.length < 1)
            return;
        var data = new Array();
        var checkboxes = new Array();
        for(a = 0; a < window.checkboxes.length; a++){
            if(window.checkboxes[a].checked){
                data.push(window.checkboxes[a].getAttribute('data-num'));
                checkboxes.push(window.checkboxes[a]);
            }
        }
        if(data.length < 1)
            return;
        d = new XMLHttpRequest();
        d.open('POST', '/takaba/delete_reports');
        var form_data = new FormData();
        for(a = 0; a < data.length; a++)
        form_data.append('id', data[a]);
        d.send(form_data);
        d.onreadystatechange = function(){
            if(this.readyState != 4)
                return;
            alert(this.responseText);
            for(a = 0; a < checkboxes.length; a++){
                removeTableRow(document.getElementsByClassName('reports_table_' + checkboxes[a].getAttribute('data-num').split('_')[1])[0]);
            }
        }
    });
}

function removeTableRow(e){
    $(e).fadeOut(500, null);
}