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
        window.thread = '${thread.num}';
        window.catalog = [];
        window.catalog.push('${thread.num}');</script>
        1111
        <div class="centered_wrapper postform_link_wrapper">[<a class="postform_link">Ответить в тред</a>]</div>
        <br/>
        <%@ include file="/WEB-INF/postform.jsp" %>
        <br/>
        <div class="line threads_delimiter"></div>
        <div class="posts_container">
            <div class="thread-${thread.getPost(0).postnum}">
                <c:forEach items="${thread.posts}" var="post">
                    <div class="thread_post thread_post_${post.numInThread}" id="${post.postnum}">
                        <div class=
                                <c:if test="${post.numInThread == 1}">"post_oppost"</c:if>
                                <c:if test="${post.numInThread > 1}">"post_reply"</c:if>
                        >
                        <div class="post_details">
                            <input type="checkbox" name="delete" class="turnmeoff" value="${post.postnum}">
                            <span class="post_name">${post.name}</span>
                            <c:if test="${!post.subject.equals('')}"><span class="post_subject">${post.subject}</span></c:if>
                            <c:if test="${!post.tripcode.equals('')}"><span class="post_trip">!${post.subject}</span></c:if>
                            <span class="post_date">${post.date}</span>
                            <a class="post_reflink" href="${post.thread.num}#${post.postnum}">№</a><span class="postnum" data-num="${post.postnum}">${post.postnum}</span>
                            <span class="post_number">${post.numInThread}</span>
                            <span class="post_buttons_container"><a class="postbtn_hide" data-num="${post.postnum}"></a><a class="postbtn_report" data-num="${post.postnum}"></a><a class="postbtn_options" data-num="${post.postnum}"></a></span>
                            <c:if test="${post.numInThread == 1}"><span class="oppost_reply_link">[<a href="${post.postnum}">Ответ</a>]</span></c:if>
                        </div>
                        <c:if test="${post.pics.size() > 0}">
                            <c:if test="${post.pics.size() > 1}"><div class="multiple_files_container"></c:if>
                            <c:forEach items="${post.pics}" var="pic">
                                <c:if test="${post.pics.size() > 1}"><div class="multiple_files_single"></c:if>
                                <div class="file_attr ${post.pics.size() > 1 ? "multiple_file_attr" : "single_file_attr"}"><a href="/${pic.path}" class="file_link">${pic.name}</a></div>
                                <div class="file_attachment_sign${post.pics.size() > 1 ? "" : " single_file_attachment_sign"}">${pic.metadata}</div>
                                <a href="/${pic.path}" class="${post.pics.size() > 1 ? "file_attachment_multiple" : "file_attachment_single"}"><img src="/${pic.thumbPath}" data-src="/${pic.path}" src-width="${pic.width}" src-height="${pic.height}" thumb-width="${pic.thumbWidth}" thumb-height="${pic.thumbHeight}" class="attachment"></img></a>
                                <c:if test="${post.pics.size() > 1}"></div></c:if>
                            </c:forEach>
                        </c:if>
                        <article class="post_message">${post.message}</article>
                        <span class="reply_map reply_map_${post.postnum}"${post.replies.size() > 0 ? "" : " style=\"display: none\""} data-num="${post.postnum}">Ответы: 
                        <c:forEach items="${post.replies}" var="reply" varStatus="replyNumber">${replyNumber.index == 0 ? "" : ", "}<a class="reply_map_entry" data-num="${reply.postnum}">>>${reply.postnum}</a></c:forEach>
                        </span></div>
                </div>
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