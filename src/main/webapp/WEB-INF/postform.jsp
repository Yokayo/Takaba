<%@ page contentType="text/html; charset=UTF-8" %>
<head>
<meta charset="UTF-8">
</head>
<div class="centered_wrapper postform_wrapper">
    <form class="contentform">
        <div class="contentform_baseline">
            <input class="postform_input" id="subject" name="subject" placeholder="тема"></input>
            <button type="button" class="submit_button">Отправить</button>
        </div>
        <div class="contentform_baseline">
            <input class="postform_input" id="name" name="name" placeholder="имя"></input>
            <input class="postform_input" id="options" name="options" placeholder="опции"></input>
        </div>
        <textarea class="postform_text postform_input" name="contents" class="submit_form_text" rows="10" placeholder="Комментарий"></textarea>
        <input class="postform_input" type="file" name="pic" multiple="true"></input></form>
</div>