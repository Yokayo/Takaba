<%@ page contentType="text/html; charset=UTF-8" %>
<div class="qr_form_container">
    <div class="qr_header qr_form_header">Ответить<span class="qr_close_button">X</span></div>
    <form class="qr_contentform">
        <div class="contentform_baseline">
            <input class="qr_postform_input" id="subject" name="subject" placeholder="тема"></input>
            <button type="button" class="qr_submit_button">Отправить</button>
        </div>
        <div class="contentform_baseline">
            <input class="qr_postform_input" id="name" name="name" placeholder="имя"></input>
            <input class="qr_postform_input qr_options_field" id="options" name="options" placeholder="опции"></input>
        </div>
        <textarea class="qr_form_message" name="contents" rows="10" placeholder="Комментарий"></textarea>
        <input class="postform_input" type="file" name="pic" class="qr_files" multiple="true"></input></form>
</div>