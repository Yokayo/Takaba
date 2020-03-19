<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ page contentType="text/html; charset=UTF-8" %>
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="ru" lang="ru">
	<head>
		<title>${board_title}</title>
		<!-- <meta http-equiv="Content-Type" content="text/html; charset=utf-8"> -->
        <link rel="stylesheet" type="text/css" href="/takaba.css"/>
	</head>
    <body class="makaba" style="background: #EEEEEE">
        <script src="/jQuery.js"></script>
        <script src="/callbacks.js"></script>
        <script>window.board = '${board_id}';
window.thread = '-1';
window.catalog = [<c:forEach items="${catalog}" var="thread">'${thread.getPost(0).getPostnum()}',</c:forEach>];</script>
        1111
        <div class="centered_wrapper postform_link_wrapper">
            [<a class="postform_link">Создать</a>]
        </div>
        <br/>
        <div class="centered_wrapper">
            <span class="board_desc">${board_desc}</span>
        </div>
        <%@ include file="/WEB-INF/postform.jsp" %>
        <div class="threads_container">
            <c:forEach items="${posts}" var="post">
                <c:if test="${post.numInThread == 1}">
                    <div class="line threads_delimiter"></div>
                    <div class="thread-${post.postnum}">
                </c:if>
                ${generator.generatePostHTML(post, board_id, posts, true)}
                <c:if test="${post.numInThread == 1 && missed_posts.get(post.postnum) != null}">
                    <div class="thread_missed">${missed_posts.get(post.postnum)} Нажмите <a href="/boards/${board_id}/${post.postnum}">ответ</a>, чтобы посмотреть.</div>
                </c:if>
                <c:if test="${post.numInThread == board.getThread(post.thread).postcount}">
                    </div>
                </c:if>
            </c:forEach>
            <%@ include file="/WEB-INF/qr_postform.jsp" %>
        </div><c:if test="${isModerator}"><script src="/res/appendModFunctions.js"></script>
        <script>window.banReasons = [<c:forEach items="${ban_reasons}" var="reason">'${reason}',</c:forEach>];</script></c:if>
    </body>
</html>