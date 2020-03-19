<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ page contentType="text/html; charset=UTF-8" %>
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="ru" lang="ru">
	<head>
		<title>/${board_id}/ - ${thread.title}</title>
		<!-- <meta http-equiv="Content-Type" content="text/html; charset=utf-8"> -->
        <link rel="stylesheet" type="text/css" href="/takaba.css"/>
	</head>
    <body class="makaba" style="background: #EEEEEE">
        <script src="/jQuery.js"></script>
        <script src="/callbacks.js"></script>
        <script>window.board = '${board_id}';
        window.thread = '${thread.getPost(0).getPostnum()}';
        window.catalog = [];
        window.catalog.push('${thread.getPost(0).getPostnum()}');</script>
        1111
        <div class="centered_wrapper postform_link_wrapper">[<a class="postform_link">Ответить в тред</a>]</div>
        <br/>
        <%@ include file="/WEB-INF/postform.jsp" %>
        <br/>
        <div class="line threads_delimiter"></div>
        <div class="posts_container">
            <div class="thread-${thread.getPost(0).postnum}">
                <c:forEach items="${thread.getPosts()}" var="post">
                    ${generator.generatePostHTML(post, board_id, thread.getPosts(), false)}
                </c:forEach>
            </div>
        </div>
        <br/>
        <div class="line threads_delimiter"></div>
        <div class="centered_wrapper postform_link_wrapper">[<a class="postform_link">Ответить в тред</a>]</div>
        <br/>
        <%@ include file="/WEB-INF/postform.jsp" %><c:if test="${isModerator}"><script src="/res/appendModFunctions.js"></script>
        <script>window.banReasons = [<c:forEach items="${ban_reasons}" var="reason">'${reason}',</c:forEach>];</script></c:if>
        <%@ include file="/WEB-INF/qr_postform.jsp" %>
    </body>
</html>