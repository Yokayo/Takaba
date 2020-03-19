<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ page contentType="text/html; charset=UTF-8" %>
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="ru" lang="ru">
	<head>
		<title>Жалобы — Три.ч</title>
		<!-- <meta http-equiv="Content-Type" content="text/html; charset=utf-8"> -->
        <link rel="stylesheet" type="text/css" href="/res/modpanel.css"/>
        <script src="/jQuery.js"></script>
        <script src="/res/view_reports_callbacks.js"></script>
	</head>
    <body class="body_makaba">
        <div class="mod_panel_bar">
            <div class="mod_panel_nav">
                <div class="mod_panel_item"><a href="/takaba/mod_panel?task=admin_panel">Админ-панель</a></div>
                <div class="mod_panel_item"><a href="/takaba/mod_panel?task=view_bans">Баны</a></div>
                <div class="mod_panel_item"><span>Жалобы</span></div>
                <div class="mod_panel_item"><a href="/takaba/mod_panel?task=logout">Выход</a></div>
            </div>
        </div>
        <div class="centered_wrapper title_wrapper">Жалобы</div>
        <div class="reports_info_wrapper">
            <table class="reports_table" cellspacing="0">
                <tr class="reports_table_header">
                    <th class="reports_table_header_check left_rtc"><input type="checkbox" class="reports_checkall"></input></th>
                    <th class="reports_table_header_board">Доска</th>
                    <th class="reports_table_header_posts">Посты</th>
                    <th class="reports_table_header_text">Текст жалобы</th>
                    <th class="reports_table_header_ip right_rtc">IP</th>
                </tr>
                <c:forEach items="${reports}" var="report" varStatus="count">
                    <tr class="reports_table_${report.getID()}"><td class="report_cell_check">
                    <input type="checkbox" class="reports_check" data-num="${report.board}_${report.getID()}"></input></td>
                    <td class="reports_table_cell regular_link"><a href="boards/${report.board}" class="regular_link">${report.board}</a></td>
                    <td class="reports_table_cell">
                        <c:forEach items="${report.posts}" var="post" varStatus="posts_count">
                        <a href="/boards/${report.board}/${cache.getBoard(report.getBoard()).getPost(post).getThread()}" class="regular_link">${post}</a>
                        </c:forEach>
                    </td>
                    <td class="reports_table_cell">${report.text}</td><td class="reports_table_cell" style="border-radius:">${report.getIP()}</td></tr>
                </c:forEach>
            </table><br/>
            <div class="delete_items_link delete_reports_link">Удалить выделенные жалобы</a><br/>
        </div>
    </body>
</html>