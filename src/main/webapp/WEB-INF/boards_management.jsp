<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ page contentType="text/html; charset=UTF-8" %>
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="ru" lang="ru">
	<head>
		<title>Доски — Три.ч</title>
		<!-- <meta http-equiv="Content-Type" content="text/html; charset=utf-8"> -->
        <link rel="stylesheet" type="text/css" href="/res/modpanel.css"/>
        <style>
            .board_info_container{
                background: white;
                display: inline-block;
                display: none;
                position: fixed;
                z-index: 1001;
                margin-left: -183px;
                left: 50%;
                top: 25%;
            }
            .board_info_header{
                display: block;
                background: black;
                color: white;
                padding: 5px 5px 5px 5px;
            }
            .board_info_section{
                padding: 2px;
            }
            .edit_board_input{
                margin: 5px;
            }
            button.delete_board_button{
                font-size:12px;
                background: rgb(205, 0, 0);
                border: none;
                color: white;
                border-radius: 5px;
                font: normal normal 400 13.5px/normal "Trebuchet MS";
                cursor: pointer;
            }
            .edit_board_form_container{
                display: flex;
                flex-direction: row;
            }
            .edit_board_container_close_button{
                float: right;
                font-weight: bold;
                cursor: pointer;
                font: normal normal 700 14px/18.2px "Trebuchet MS";
                margin-right: 2px;
            }
            .transparent_bg{
                position: fixed;
                top: 0px;
                left: 0px;
                background: rgba(0, 0, 0, 0.5);
                width: 100%;
                height: 2000px;
                display: none;
                z-index: 1000;
            }
            .submit_button{
                font-size:12px;
                background: rgb(0, 155, 0);
                border: none;
                color: white;
                border-radius: 5px;
                font: normal normal 400 13.5px/normal "Trebuchet MS";
                cursor: pointer;
                display: inline-block;
                padding: 2px 12px 2px 12px;
                margin: 25px 20px 10px 20px;
            }
            .add_board_item{
                font-weight: bold;
            }
        </style>
	</head>
    <body class="body_makaba" style="background: #EEEEEE">
        <script src="/jQuery.js"></script>
        <script src="/res/boards_management_callbacks.js"></script>
        <div class="transparent_bg"></div>
        <div class="board_info_container"><div class="board_info_header"><span class="board_name"></span><span class="edit_board_container_close_button">X</span></div>
        <div class="board_info_section"><form id="edit_board_form">
        <div class="edit_board_form_container">
            <input type="hidden" id="edited_board" name="edited"></input>
            <input class="edit_board_input" id="id" name="id" placeholder="ID"></input>
            <input class="edit_board_input" id="title" name="title" placeholder="полное название"></input>
            </div>
        <div class="edit_board_form_container">
            <input class="edit_board_input" id="default_name" name="default_name" placeholder="дефолтное имя постера"></input>
            <input class="edit_board_input" id="brief" name="brief" placeholder="описание"></input>
            </div>
        <div class="edit_board_form_container">
            <input class="edit_board_input" id="max_file_size" name="mfs" placeholder="макс. размер файла"></input>
            <div class="edit_board_input"><label for="delayed_flushing_cb">Delayed Flushing</label><input type="checkbox" name="delayed_flushing" id="delayed_flushing_cb"></input></div>
            </div>
            <div style="text-align: center"><div class="submit_button">Сохранить</div></div>
        </form></div></div>
        <div class="mod_panel_bar">
            <div class="mod_panel_nav">
                <!--<div class="mod_panel_item"><a href="/makaba/mod_panel?task=boards">Доски</a></div>-->
                <div class="mod_panel_item"><span>Админ-панель</span></div>
                <div class="mod_panel_item"><a href="/makaba/mod_panel?task=view_bans">Баны</a></div>
                <div class="mod_panel_item"><a href="/makaba/mod_panel?task=view_reports">Жалобы</a></div>
                <div class="mod_panel_item"><a href="/makaba/mod_panel?task=logout">Выход</a></div>
            </div>
        </div>
        <div class="centered_wrapper title_wrapper">Управление досками</div>
        <div class="boards_list_container">
        <div class="boards_list_item add_board_item" id="+">+</div>
        <c:forEach items="${boards}" var="board"><div class="boards_list_item" id="${board.getID()}">${board.getTitle()}</div></c:forEach>
        </div>
    </body>
</html>