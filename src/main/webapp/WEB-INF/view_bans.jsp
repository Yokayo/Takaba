<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ page contentType="text/html; charset=UTF-8" %>
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="ru" lang="ru">
	<head>
		<title>Баны — Три.ч</title>
		<!-- <meta http-equiv="Content-Type" content="text/html; charset=utf-8"> -->
        <link rel="stylesheet" type="text/css" href="/res/modpanel.css"/>
        <script src="/jQuery.js"></script>
        <script src="/res/view_bans_callbacks.js"></script>
	</head>
    <body class="body_makaba">
        <div class="mod_panel_bar">
            <div class="mod_panel_nav">
                <div class="mod_panel_item"><a href="/takaba/mod_panel?task=admin_panel">Админ-панель</a></div>
                <div class="mod_panel_item"><span>Баны</span></div>
                <div class="mod_panel_item"><a href="/takaba/mod_panel?task=view_reports">Жалобы</a></div>
                <div class="mod_panel_item"><a href="/takaba/mod_panel?task=logout">Выход</a></div>
            </div>
        </div>
        <div class="centered_wrapper title_wrapper">Баны</div>
        <div class="bans_table_container">
        <table class="bans_table" cellspacing="0">
                <thead class="bans_table_header">
                    <th class="bans_table_header_id">ID бана</th>
                    <th class="bans_table_header_ip">IP</th>
                    <th class="bans_table_header_reason">Причина</th>
                    <th class="bans_table_header_exp">Истекает</th>
                    <th class="bans_table_header_cb"></th>
                </thead>
            <c:forEach items="${bans}" var="ban">
            <tr class="bans_table_row bans_table_${ban.ID}">
            <td>${ban.ID}</td>
            <td>${ban.IP}</td>
            <td>${ban.reason}${(ban.isGlobal() ? "" : "//!"}${ban.isGlobal() ? "" : ban.board)}</td>
            <td>${ban.isPermanent() ? "" : ban.humanReadableExpirationDate}</td>
            <td><div class="delete_items_link unban_link" data-num="${ban.ID}">Разбанить</div></td>
            </tr></c:forEach>
            </table>
            <br/>
            <!--<div class="delete_items_link unban_link">Разбанить</div>-->
            <form action="/takaba/mod_panel" method="get">
                <input type="hidden" name="task" value="view_bans"></input>
                <input class="moder_panel_input" type="text" size="18" name="id" placeholder="номер бана"></input>
                <button class="find_ban_link" type="submit">Найти бан</button>
            </form>
        </div>
    </body>
</html>