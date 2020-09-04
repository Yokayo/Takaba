<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ page contentType="text/html; charset=UTF-8" %>
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="ru" lang="ru">
	<head>
		<title>/${board_id}/ - ${board_title}</title>
		<!-- <meta http-equiv="Content-Type" content="text/html; charset=utf-8"> -->
        <link rel="stylesheet" type="text/css" href="/takaba.css"/>
        <script src="/jQuery.js"></script>
        <script src="/callbacks.js"></script>
        <script>window.board = '${board_id}';
        window.thread = '-1';
        window.catalog = [<c:if test="${catalog.size() > 0}">
            <c:forEach items="${catalog}" var="thread" varStatus="count">
                ${count.index == 0 ? "" : ", "}'${thread.num}'
            </c:forEach>
        </c:if>];</script>
	</head>
    <body class="makaba" style="background: #EEEEEE">
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
                <div class="thread_post thread_post_${post.numInThread}" id="${post.postnum}">
                    <div class='post_${post.numInThread == 1 ? "oppost" : "reply"}'>
                        <div class="post_details">
                            <input type="checkbox" name="delete" class="turnmeoff" value="${post.postnum}">
                            <span class="post_name">${post.name}</span>
                            <c:if test="${!post.subject.equals('')}"><span class="post_subject">${post.subject}</span></c:if>
                            <c:if test="${!post.tripcode.equals('')}"><span class="post_trip">!${post.trip}</span></c:if>
                            <span class="post_date">${post.date}</span>
                            <a class="post_reflink" href="boards/${board_id}/${post.thread.num}#${post.postnum}">№</a><span class="postnum" data-num="${post.postnum}">${post.postnum}</span>
                            <span class="post_buttons_container"><a class="postbtn_hide" data-num="${post.postnum}"></a><a class="postbtn_report" data-num="${post.postnum}"></a><a class="postbtn_options" data-num="${post.postnum}"></a></span>
                            <c:if test="${post.numInThread == 1}"><span class="oppost_reply_link">[<a href="/boards/${board_id}/${post.postnum}">Ответ</a>]</span></c:if>
                       </div>
                    <c:if test="${post.pics.size() > 0}">
                        <c:if test="${post.pics.size() > 1}"><div class="multiple_files_container"></c:if>
                        <c:forEach items="${post.pics}" var="pic">
                            <c:if test="${post.pics.size() > 1}"><div class="multiple_files_single"></c:if>
                            <div class="file_attr ${post.pics.size() > 1 ? "multiple_file_attr" : "single_file_attr"}"><a href="${pic.path}" class="file_link">${pic.name}</a></div>
                            <div class="file_attachment_sign${post.pics.size() > 1 ? "" : " single_file_attachment_sign"}">${pic.metadata}</div>
                            <a href="${pic.path}" class="${post.pics.size() > 1 ? "file_attachment_multiple" : "file_attachment_single"}"><img src="${pic.thumbPath}" data-src="${pic.path}" src-width="${pic.width}" src-height="${pic.height}" class="attachment"></img></a>
                            <c:if test="${post.pics.size() > 1}"></div></c:if>
                        </c:forEach>
                        </div></br>
                    </c:if>
                    <article class="post_message">${post.message}</article>
                    <span class="reply_map reply_map_${post.postnum}"${post.replies.size() > 0 ? "" : " style=\"display: none\""} data-num="${post.postnum}">Ответы:
                    <c:forEach items="${post.replies}" var="reply" varStatus="replyNumber">
                        ${replyNumber.index == 0 ? "" : ", "}<a class="reply_map_entry" data-num="${reply.postnum}">>>${reply.postnum}</a>
                    </c:forEach>
                    </span>
                </div>
            </div>
            <c:if test="${missed_posts.get(post.postnum) != null}">
                <div class="thread_missed">${missed_posts.get(post.postnum)} Нажмите <a href="/boards/${board_id}/${post.postnum}">ответ</a>, чтобы посмотреть.</div>
            </c:if>
            <c:if test="${post.numInThread == post.thread.posts.size()}">
                </div>
            </c:if>
            </c:forEach>
            <%@ include file="/WEB-INF/qr_postform.jsp" %>
        </div><c:if test="${isModerator}"><script src="/res/appendModFunctions.js"></script>
        <script>window.banReasons = [<c:forEach items="${ban_reasons}" var="reason">'${reason}',</c:forEach>];</script></c:if>
    </body>
</html>