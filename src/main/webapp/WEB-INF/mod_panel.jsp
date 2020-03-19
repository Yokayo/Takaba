<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ page contentType="text/html; charset=UTF-8" %>
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="ru" lang="ru">
	<head>
		<title>Мод-панель — Три.ч</title>
		<!-- <meta http-equiv="Content-Type" content="text/html; charset=utf-8"> -->
        <link rel="stylesheet" type="text/css" href="/res/modpanel.css"/>
	</head>
    <body class="body_makaba">
        <div class="mod_panel_bar">
            <div class="mod_panel_nav">
                <div class="mod_panel_item"><a href="/takaba/mod_panel?task=admin_panel">Админ-панель</a></div>
                <div class="mod_panel_item"><a href="/takaba/mod_panel?task=view_bans">Баны</a></div>
                <div class="mod_panel_item"><a href="/takaba/mod_panel?task=view_reports">Жалобы</a></div>
                <div class="mod_panel_item"><a href="/takaba/mod_panel?task=logout">Выход</a></div>
            </div>
        </div>
        <div class="welcome_wrapper">Добро пожаловать, ${name}. Ваш уровень доступа: ${access_level}</div>
    </body>
</html>