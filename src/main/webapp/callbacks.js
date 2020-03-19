$(main);

function main(){
    
    window.hideStoreTimeout = 7;
    window.fullscreenImage = null;
    window.posts = [];
    window.busy = false;
    window.plashques = [];
    if(window.thread == '-1')
        window.currentPage = 0;
    window.loadingNextPage = false;
    window.pendingJSONsCount = 0;
    window.pendingJSONs = [];
    window.isOP = navigator.userAgent.indexOf("Opera") != -1 && navigator.userAgent.indexOf("Presto") != -1;
    
    initPostsMap();
    
    var e = $(".reply_map").toArray();
    for(var a = 0; a < e.length; a++){
        processReplyMap(e[a]);
    }
    
    overallHeight = $(document).height();
    
    processPostReplyLinkContainer(document.body);
    
    processPostformLinks();
    
    processPostformSubmitButtons();
    
    synchronisePostforms();
    
    if(thread == '-1'){
    $(window).scroll(function(){
        if(loadingNextPage || scrollY + innerHeight != overallHeight || catalog.length < (currentPage + 1) * 10)
            return;
        loadingNextPage = true;
        currentPage ++;
        for(var a = currentPage * 10; a < catalog.length && a < (currentPage + 1) * 10; a++){
            var d = new XMLHttpRequest();
            d.open('GET', '/res/' + board + '/' + catalog[a] + '.json?prev=1');
            d.send();
            d.orderNum = a - currentPage * 10;
            d.onreadystatechange = function(){
                if(this.readyState != 4)
                    return;
                pendingJSONs[this.orderNum] = this.responseText;
                //alert(this.responseText);
                pendingJSONsCount ++;
                if(pendingJSONsCount == 10 || currentPage * 10 + pendingJSONsCount == catalog.length){
                    pendingJSONsCount = 0;
                    loadingNextPage = false;
                    var processed_posts;
                    var html = '';
                    for(var b = 0; b < pendingJSONs.length; b++){
                        try{processed_posts = JSON.parse(pendingJSONs[b]);}catch(e){showAlert("Ошибка парсинга ответа сервера"); return;}
                        for(var c = 0; c < processed_posts.posts.length; c++){
                            processed_posts.posts[c].html = generateFlyingPostHTML(processed_posts.posts[c], false);
                            posts.push(processed_posts.posts[c]);
                            if(c == 0)
                                html += '<div class="line threads_delimiter"></div><div class="thread-' + processed_posts.posts[0].postnum + '">'
                            html += '<div class="thread_post thread_post_' + processed_posts.posts[c].postnum + '" id="' + processed_posts.posts[c].postnum + '"><div class="post_' + (processed_posts.posts[c].oppost == 'true' ? 'oppost' : 'reply') + '">';
                            html += generateFlyingPostHTML(processed_posts.posts[c], true);
                            html += '</div></div>';
                            if(c == processed_posts.posts.length - 1){
                                html += '</div>';
                                //alert(html);
                            }
                            if(c == 0 && processed_posts.missed_posts != '0'){
                                var missed = parseInt(processed_posts.missed_posts);
                                var suffix = (missed == 1 && missed%100 != 11 ? '' : 'о');
                                var post_suffix = (missed%100 > 10 && missed%100 < 15 ? "ов" : missed%10 == 1 ? "" : missed%10 == 2 || missed%10 == 3 || missed%10 == 4 ? "а" : "ов");
                                html += '<div class="thread_missed">Пропущен' + suffix + ' ' + processed_posts.missed_posts
                                + " пост" + post_suffix + '. Нажмите <a href="/boards/' + board + '/' + processed_posts.posts[c].postnum + '">ответ</a>, чтобы посмотреть.</div>';
                            }
                        }
                    }
                    pendingJSONs = [];
                    var temp = document.createElement('div');
                    temp.innerHTML = html;
                    $(".threads_container")[0].appendChild(temp);
                    //var thread = $(".thread-" + processed_posts.posts[0].postnum)[0];
                    var e = temp.getElementsByClassName("postnum");
                    for(var d = 0; d < e.length; d++){
                        addPostnumClickCallback(e[d]);
                    }
                    processPostButtonsContainer(temp);
                    processPostReplyLinkContainer(temp);
                    processHiddenPosts(temp);
                    e = temp.getElementsByTagName("img");
                    $(temp).find(".postbtn_hide").click(function(){
                        hidePost(this.getAttribute('data-num'));
                    });
                    $(temp).find(".hidden_post").click(function(){
                        unhidePost(this);
                    });
                    for(var d = 0; d < e.length; d++)
                        processPreview(e[d]);
                    e = temp.getElementsByClassName("reply_map");
                    for(var d = 0; d < e.length; d++){
                        processReplyMap(e[d]);
                    }
                    $(temp.children[0]).unwrap();
                    overallHeight = $(document).height();
                }
            }
        }
    });
    }
    
    $(".postbtn_hide").click(function(){
        hidePost(this.getAttribute('data-num'));
    });
    
    var e = $(".postnum").toArray();
    for(var a = 0; a < e.length; a++){
        addPostnumClickCallback(e[a]);
    }
    
    $(".qr_submit_button").click(function(){
        if($(".qr_form_message").val() == ''){
            alert("Вы ничего не ввели в сообщении");
            $(".qr_form_message").css('border', '1px solid red');
            return;
        }
        var d = new XMLHttpRequest();
        var formData = new FormData($(".qr_contentform")[0]);
        formData.append('board', window.board);
        formData.append('thread', window.qrFormTarget);
        formData.append('trip', $(".qr_options_field").val());
        d.open('POST', '/takaba/posting');
        d.send(formData);
            d.onreadystatechange = function(){
                if(this.readyState == 4){
                    alert(this.responseText);
                    var resp = JSON.parse(this.responseText);
                    showAlert((resp.Status == 1 ? "Ошибка постинга: " : "") + resp.Message);
                }
            }
    });
    
    e = document.getElementsByClassName("qr_close_button")[0];
    e.qrForm = document.getElementsByClassName("qr_form_container")[0];
    e.addEventListener('click', function(){this.qrForm.style.display = 'none';});
    
    e = document.getElementsByClassName("qr_form_header")[0];
    window.qrForm = document.getElementsByClassName("qr_form_container")[0];
    window.qrForm.style.left = '0px';
    window.qrForm.style.top = '0px';
    window.qrFormDragged = false;
    window.qrForm.offsetFromMouseX = 0;
    window.qrForm.offsetFromMouseY = 0;
    e.addEventListener('mousedown', function(mouse){window.qrFormDragged = true;
    window.qrForm.offsetFromMouseX = mouse.clientX - window.qrForm.offsetLeft;
    window.qrForm.offsetFromMouseY = mouse.clientY - window.qrForm.offsetTop;
    })
    
    processHiddenPosts(document.body);
    
    $(".hidden_post").click(function(){
        unhidePost(this);
    });
    
    e = document.getElementsByClassName("attachment");
    for(var a = 0; a < e.length; a++){
        processPreview(e[a]);
    }
    
    window.imageDragged = false;
    window.biggestImageMove = 0;
    document.addEventListener('mousemove', function(mouse){if(!window.imageDragged && !window.qrFormDragged) return;
    mouse.preventDefault();
    if(window.imageDragged){
        var deltaX = mouse.clientX - (parseInt(window.fullscreenImage.style.left) + window.fullscreenImage.offsetFromMouseX);
        var deltaY = mouse.clientY - (parseInt(window.fullscreenImage.style.top) + window.fullscreenImage.offsetFromMouseY);
        if(Math.abs(deltaX) > window.biggestImageMove || Math.abs(deltaY) > window.biggestImageMove)
            window.biggestImageMove = Math.max(Math.abs(deltaX), Math.abs(deltaY));
        window.fullscreenImage.style.left = (parseInt(window.fullscreenImage.style.left) + deltaX).toString() + 'px';
        window.fullscreenImage.style.top = (parseInt(window.fullscreenImage.style.top) + deltaY).toString() + 'px';
    }else{
        var deltaX = mouse.clientX - (parseInt(window.qrForm.style.left) + window.qrForm.offsetFromMouseX);
        var deltaY = mouse.clientY - (parseInt(window.qrForm.style.top) + window.qrForm.offsetFromMouseY);
        var newX = parseInt(window.qrForm.style.left) + deltaX;
        var newY = parseInt(window.qrForm.style.top) + deltaY;
        if(newX >= 0){
            if(newX + window.qrForm.clientWidth >= window.innerWidth)
                window.qrForm.style.left = (window.innerWidth - window.qrForm.clientWidth) + 'px';
            else
                window.qrForm.style.left = newX + 'px';
        }
        else
            window.qrForm.style.left = '0px';
        if(newY >= 0){
            if(newY + window.qrForm.clientHeight >= window.innerHeight)
                window.qrForm.style.top = (window.innerHeight - window.qrForm.clientHeight) + 'px';
            else
                window.qrForm.style.top = newY + 'px';
        }
        else
            window.qrForm.style.top = '0px';
    }
    });
    document.addEventListener('mouseup', function(){
        if(window.imageDragged)
            window.imageDragged = false;
        if(window.qrFormDragged)
            window.qrFormDragged = false;
    });
    
    a = document.createElement('div');
    a.dataNum = '';
    a.setAttribute('class', 'select_list');
    e = document.createElement('a');
    e.innerHTML = "Ответить";
    e.setAttribute('onClick', '$(".postnum[data-num=" + select_list.dataNum.split("_")[0] + "]").click(); select_list.style.display = "none"; select_list_open = false;');
    a.appendChild(e);
    e = document.createElement('a');
    e.innerHTML = "Скрыть";
    window.list_hide_button = e;
    a.appendChild(e);
    e = document.createElement('a');
    e.innerHTML = "Пожаловаться";
    e.setAttribute('onClick', 'processReport();');
    window.list_report_button = e;
    a.appendChild(e);
    document.body.appendChild(a);
    a.parentDataNum = '';
    window.select_list = a;
    window.select_list_open = false;
    processPostButtonsContainer(document);
}

function showAlert(msg){
    ab = document.createElement('div');
    document.body.appendChild(ab);
    var y = '0px';
    if(plashques.length > 0){
        y = plashques.length * 40 + 'px';
    }
    ab.style.display = 'block';
    ab.style.background = '#DDDDDD';
    ab.style.position = 'fixed';
    ab.style.float = 'left';
    ab.style.right = '5px';
    ab.style.top = y;
    ab.style.marginTop = '5px';
    ab.style.height = '20px';
    ab.style.opacity = '0.9';
    ab.style.border = '1px solid #808080';
    ab.style.borderRadius = '2px';
    ab.style.paddingTop = '9px';
    ab.style.paddingBottom = '5px';
    ab.style.paddingLeft = '10px';
    ab.style.paddingRight = '10px';
    ab.style.font = 'normal normal 400 14px/normal Arial';
    ab.innerHTML = msg;
    plashques.push(ab);
    setTimeout(function(){hideAlert(this);}.bind(ab), 3000);
}

function hideAlert(al){
        var hideInterval = setInterval(function(){
            var padding = parseInt(al.style.paddingTop.split("p")[0]);
            if(al.clientHeight <= 4){
                plashques.shift();
                for(var a = 0; a < plashques.length; a++){
                    if(!plashques[a].moveInterval){
                    plashques[a].moveInterval = setInterval(function(){
                        if(parseInt(this.style.top) > (plashques.indexOf(this)) * 40){
                            $(this).css({top: (parseInt(this.style.top) - 4)});
                        }else{
                            clearInterval(this.moveInterval);
                            this.moveInterval = false;
                        }
                    }.bind(plashques[a]), 50);
                    }
                }
                al.parentNode.removeChild(al);
                clearInterval(hideInterval);
                clearInterval(al.moveInterval);
                return;
            }
            if(parseInt(al.style.height.split("p")[0]) < padding*2){
                al.style.paddingTop = (padding - 1).toString() + 'px';
                al.style.paddingBottom = (padding - 1).toString() + 'px';
                padding -= 1;
            }
            al.style.height = (al.clientHeight - padding*2 - 2).toString() + 'px';
            al.style.opacity = (parseFloat(al.style.opacity) - 0.1).toString();
            return;
        }, 35);
        
}

function processPreview(preview){
    preview.fullSize = preview.getAttribute("data-src");
    preview.fullWidth = preview.getAttribute("src-width");
    preview.fullHeight = preview.getAttribute("src-height");
    preview.fullscreenContainer = null;
    preview.addEventListener('click', function(e){
        e.preventDefault();
        if(window.fullscreenImage != null){
            if(window.fullscreenImage == this.fullscreenContainer){
                document.body.removeChild(window.fullscreenImage);
                window.fullscreenImage = null;
                this.fullscreenContainer = null;
                return;
            }
            document.body.removeChild(window.fullscreenImage);
        }
        var displayW, displayH;
        displayW = this.fullWidth;
        displayH = this.fullHeight;
        multiplier = 1.0;
        while(displayW > window.innerWidth || displayH > window.innerHeight){
            if(multiplier <= 0.15)
                break;
            multiplier -= 0.15;
            displayW = this.fullWidth * multiplier;
            displayH = this.fullHeight * multiplier;
        }
        var full = document.createElement('div');
        full.fullWidth = this.fullWidth;
        full.fullHeight = this.fullHeight;
        full.parentThumbnail = this;
        full.offsetFromMouseX = 0;
        full.offsetFromMouseY = 0;
        full.multiplier = multiplier;
        full.setAttribute('style', 'width: '
        + displayW + 'px; height: '
        + displayH + 'px; position: fixed; outline: 8px solid #555555; top: '
        + window.innerHeight/2 + 'px; left: '
        + window.innerWidth/2 + 'px; background: rgb(85, 85, 85); z-index: 1001; transform: translate(-50%, -50%)');
        full.innerHTML = '<img style="width: 100%; height: 100%;" draggable="false" src=' + this.fullSize + '></img>';
        full.addEventListener('mousedown', function(mouse){mouse.preventDefault(); 
            biggestImageMove = 0;
            imageDragged = true;
            this.offsetFromMouseX = mouse.clientX - this.offsetLeft;
            this.offsetFromMouseY = mouse.clientY - this.offsetTop;});
        full.addEventListener('mouseup', function(){window.imageDragged = false; if(biggestImageMove < 5){fullscreenImage = null; this.parentThumbnail.fullscreenContainer = null; document.body.removeChild(this);}});
        full.addEventListener('mousewheel', function(scroll){scroll.preventDefault();
            var _multiplier = this.multiplier;
            if(scroll.wheelDelta > 0)
                this.multiplier += 0.15;
            else
                this.multiplier -= 0.15;
            if(this.multiplier > 3.0 || this.multiplier < 0.01)
                this.multiplier = _multiplier;
            this.style.height = (this.fullHeight * this.multiplier).toString() + "px";
            this.style.width = (this.fullWidth * this.multiplier).toString() + "px";
        });
        document.body.appendChild(full);
        window.fullscreenImage = full;
        this.fullscreenContainer = full;
    });
}

function calculateGlobalXOffset(el){
    if(isOP)
        return el.getBoundingClientRect().left;
    var a = 0;
    while(el){
        a += el.offsetLeft;
        a -= (el.horizontallyInverted ? el.offsetWidth : 0);
        el = el.offsetParent;
    }
    return a;
}

function calculateGlobalYOffset(el){
    var a = 0;
    if(isOP)
        return scrollY + el.getBoundingClientRect().top;
    while(el){
       // alert(el.offsetTop + ' ' + el.getAttribute('class'));
        a += el.offsetTop;
        a -= (el.verticallyInverted ? el.offsetHeight : 0);
        el = el.offsetParent;
    }
    return a;
}

function getTimeInDays() {
    return Math.ceil((+new Date)/1000/60/60/24);
}

function unhidePost(e){
    var b = window.localStorage.getItem('board.' + window.board + '.hidden');
    var num = e.getAttribute('post');
    var index = b.indexOf('_' + num + '|');
    if(index == -1)
        return;
    var end_index = index + 2 + num.length;
    while(b.charAt(index) != '|' && index != 0)
        index --;
    if(b.charAt(index) == '|')
        index ++;
    b = b.replace(b.substring(index, end_index), '');
    window.localStorage.setItem('board.' + window.board + '.hidden', b);
    var c = document.getElementsByClassName('thread-' + num)[0];
    if(c == null){
        c = document.getElementsByClassName('thread_post_' + num)[0];
        if(c == null)
            return;
    }
    c.style.display = 'block';
    e.parentNode.parentNode.removeChild(e.parentNode);
}

function hidePost(num){
    var b = window.localStorage.getItem('board.' + window.board + '.hidden');
    if(!b || b == null || b.constructor.name != 'String'){
        b = '';
        window.localStorage.setItem('board.' + window.board + '.hidden', b);
    }
    if(b.indexOf('_' + num + '|') != -1 || b.indexOf(num + '|') == 0){
        return;
    }
    b = b.concat(getTimeInDays().toString() + '_' + num + '|');
    window.localStorage.setItem('board.' + window.board + '.hidden', b);
    e = document.getElementsByClassName("thread-" + num);
    if(e.length == 0){
        e = document.getElementsByClassName("thread_post_" + num);
        if(e.length == 0)
            return;
        e = e[0];
        e.style.display = 'none';
        e.insertAdjacentHTML('afterend',
        '<div style="display: block; margin-top: 8px; margin-bottom: 8px;"><div onClick="unhidePost(this);"style="display: inline-block; height: 20px; background: #DDDDDD; box-shadow: 0px 0px 0px 2px #7F7F7F; cursor: pointer; font: normal normal 400 15px/19.5px "Trebuchet MS";" class="hidden_post" post="'
        + num + '">Скрытый пост <span style="font-weight: bold">№'
        + num
        + '</span></div></div>');
    }else{
        e = e[0];
        e.style.display = 'none';
        e.insertAdjacentHTML('afterend',
        '<div style="display: block; margin-top: 8px; margin-bottom: 8px;"><div onClick="unhidePost(this);"style="display: inline-block; height: 20px; background: #DDDDDD; box-shadow: 0px 0px 0px 2px #7F7F7F; cursor: pointer; font: normal normal 400 15px/19.5px "Trebuchet MS";" class="hidden_post hidden_thread" post="'
        + num + '">Скрытый тред <span style="font-weight: bold">№'
        + num
        + '</span></div></div>');
    }
    if(window.select_list_open){
        window.select_list.style.display = "none";
        window.select_list_open = false;
    }
}

function processReport(){
    if(window.select_list_open){
        window.select_list.style.display = 'none';
        window.select_list_open = false;
    }
    //alert(window.list_report_button.getAttribute('data-num'));
            var posts = [];
            var cb = document.getElementsByClassName('turnmeoff');
            for(a = 0; a < cb.length; a++){
                if(cb[a].checked)
                    posts.push(cb[a].getAttribute('value'));
            }
            var target_post = window.list_report_button.getAttribute("data-num").split("_")[0];
            if(!posts.includes(target_post))
                posts.push(target_post);
            var comment = prompt('Введите причину');
            if(comment == '' || comment == null)
                return;
            d = new XMLHttpRequest();
            var form_data = new FormData();
            form_data.append('board', window.board);
            form_data.append('thread', window.list_report_button.getAttribute("data-num").split("_")[1]);
            for(a = 0; a < posts.length; a++)
                form_data.append('posts', posts[a]);
            form_data.append('text', comment);
            d.open('POST', '/takaba/send_report');
            //d.setRequestHeader("Content-Type","multipart/form-data; charset=utf-8");
            d.send(form_data);
            d.onreadystatechange = function(){
                if(this.readyState == 4){
                    showAlert(JSON.parse(this.responseText).Message);
                }
            };
}

function getPost(postnum){
    for(var a = 0; a < posts.length; a++){
        if(posts[a].postnum == postnum)
            return posts[a];
    }
    return null;
}

function processReplyMap(map){
    for(var a = 0; a < map.children.length; a++){
        var entry = map.children[a];
        $(entry).mouseover(function(){
            showPostPreview(this, $(map).attr("data-num"));
        });//.prop('hasChildHover', false);
    }
}

function processPostReplyLinkContainer(link){
        var e = link.getElementsByClassName("post-reply-link");
        if(!e)
            return;
        for(var a = 0; a < e.length; a++){
            if($(e[a]).attr('parent-post-num') == null)
                continue;
            $(e[a]).mouseover(function(){
                showPostPreview(this, $(this).attr("parent-post-num"));
            });//.prop('hasChildHover', false);
        }
}

function appendToPostMap(parsedJSON){
    for(var a = 0; a < parsedJSON.posts.length; a++){
        parsedJSON.posts[a].html = generateFlyingPostHTML(parsedJSON.posts[a]);
        posts.push(parsedJSON.posts[a]);
    }
}

function showPostPreview(link, parentPostNum){
            if($(".post_hover_" + $(link).attr("data-num")).length)
                return;
            var postnum = $(link).attr("data-num");
            var post = getPost(postnum);
            if(post == null){
                var d = new XMLHttpRequest();
                d.open('GET', '/takaba/json_on_demand?num=' + postnum + '&board=' + board);
                d.send();
                d.onreadystatechange = function(){
                    if(this.readyState != 4)
                        return;
                    var msg = JSON.parse(this.responseText);
                    if(msg.Status){
                        showAlert(msg.Message);
                        return;
                    }
                    appendToPostMap(msg);
                    showPostPreview(link, parentPostNum);
                }
                return;
            }
            var e = document.createElement('div');
            $(e).addClass('post_hover post_hover_' + $(link).attr('data-num'));
            e.innerHTML = post.html;
            document.body.appendChild(e);
            var rect = link.getBoundingClientRect();
            var highest_picture = 0;
            var offset = $(e).outerHeight(true); // пипка для вычисления размеров поста до рендера
            var xOffset = 0;
            if(post.pics.length > 1){ // для постов с несколькими картинками
                for(var a = 0; a < post.pics.length; a++){
                    //xOffset += 17*2 + post.pics.thumb_width;
                    if(post.pics[a].thumb_height > highest_picture){
                        highest_picture = post.pics[a].thumb_height;
                        offset = parseInt(highest_picture);
                    }
                }
                xOffset = $(e.getElementsByClassName("post_details")).outerWidth(true);
                xOffset += 4; // padding
                offset = $(e.getElementsByClassName("post_details")).outerHeight(true)
                + $(e.getElementsByClassName("file_attr")).outerHeight(true)
                + $(e.getElementsByClassName("multiple_files_container")).outerHeight(true)
                + $(e.getElementsByClassName("file_attachment_sign")).outerHeight(true)
                + $(e.getElementsByClassName("post_message")).outerHeight(true)
                + $(e.getElementsByClassName("reply_map")).outerHeight(true);
            }
            if(post.pics.length == 1){ // с одной
                highest_picture = parseInt(post.pics[0].thumb_height);
                xOffset = $(e.getElementsByClassName("file_attachment_sign")).outerWidth(true); // margin
                offset = $(e.getElementsByClassName("post_details")).outerHeight(true)
                + $(e.getElementsByClassName("file_attr")).outerHeight(true)
                //+ 5 // какое-то число, которое постоянно теряет jquery
                + $(e.getElementsByClassName("file_attachment_sign")).outerHeight(true)
                + Math.max(highest_picture, $(e.getElementsByClassName("post_message")).outerHeight() + $(e.getElementsByClassName("reply_map")).outerHeight());
            }
            if(post.pics.length == 0){ // и без картинок
                offset = parseInt(e.clientHeight);
                xOffset = e.offsetWidth;
            }
            offset += parseInt(link.offsetHeight);
            var rect = link.getBoundingClientRect();
            var displayOnTop = rect.bottom + offset > innerHeight && rect.bottom - offset >= 0;
            var displayToLeft = rect.left+10 > innerWidth/2;
            var transformX = '0%';
            var transformY = '0%';
            var x = calculateGlobalXOffset(link) + 10;
            //alert(x);
            var y = calculateGlobalYOffset(link) + parseInt(link.offsetHeight);
            var max_width = innerWidth;
            if(displayOnTop){
                if(post.pics.length < 2){
                    y -= offset;
                    if(post.pics.length == 1)
                        y -= parseInt(link.offsetHeight)*0.8;
                }else{
                    y -= parseInt(link.offsetHeight);
                    transformY = '-100%';
                    e.verticallyInverted = true;
                }
            }
            if(displayToLeft){
                transformX = '-100%';
                e.horizontallyInverted = true;
                if(x <= 0){
                    max_width = calculateGlobalXOffset(link) + parseInt(link.offsetWidth)*0.2;
                }
            }
            if(post.pics.length > 0 && isOP)
                y -= 7;
            //alert(displayToLeft + ', ontop = ' + displayOnTop + ', x = ' + x);
            $(e).css({position: 'absolute',
            transform: 'translate(' + transformX + ', ' + transformY + ')',
            top: y,
            left: x,
            'max-width': max_width,
            'min-width': (post.pics.length > 1 ? '600px' : '')
            });
            addPostnumClickCallback(e.getElementsByClassName("postnum_" + postnum));
            processPostButtonsContainer(e);
            processPostReplyLinkContainer(e);
            a = e.getElementsByTagName("img");
            for(var b = 0; b < a.length; b++)
                processPreview(a[b]);
            a = e.getElementsByClassName("reply_map");
            for(var b = 0; b < a.length; b++){
                processReplyMap(a[b]);
            }
            e.disperseTimeout = false;
            e.parentPost = $(".post_hover_" + parentPostNum)[0];
            $(e).mouseout(function(){
                var this_ = this;
                clearTimeout(this.disperseTimeout);
                this.disperseTimeout = setTimeout(function(){
                    if(!this_)
                        return;
                    this_.parentNode.removeChild(this_);
                }, 800);
                var fp = this;
                while(fp.hasOwnProperty('parentPost')){ // удаление родительских постов
                    fp = fp.parentPost;
                    if(!fp || !fp.hasOwnProperty('disperseTimeout')) // значит добрались до первого поста
                        break;
                    clearTimeout(fp.disperseTimeout);
                    fp.disperseTimeout = setTimeout(function(){this.parentNode.removeChild(this);}.bind(fp), 800);
                }
            });
            $(e).mouseover(function(){
                clearTimeout(this.disperseTimeout);
                var fp = this;
                while(fp.hasOwnProperty('parentPost') && fp.parentPost != null){ // очистка колбэков родительских постов
                    fp = fp.parentPost;
                    if(!fp.hasOwnProperty('disperseTimeout')) // значит добрались до первого поста
                        break;
                    clearTimeout(fp.disperseTimeout);
                }
            });
        
}

function initPostsMap(){
    for(var a = 0; a < Math.min(10, window.catalog.length); a++){
        d = new XMLHttpRequest();
        d.open('GET', '/res/' + window.board + '/' + window.catalog[a] + '.json' + (window.thread == '-1' ? '?prev=true' : ''));
        d.send();
        d.onreadystatechange = function(){
            if(this.readyState != 4)
                return;
            try{var parsed = JSON.parse(this.responseText);}catch(e){alert(this.responseText);showAlert("Ошибка парсинга ответа сервера"); return;}
            for(var b = 0; b < parsed.posts.length; b++){
                parsed.posts[b].html = generateFlyingPostHTML(parsed.posts[b], thread == '-1');
                posts.push(parsed.posts[b]);
            }
        };
    }
}

function processPostformLinks(){
    var e = document.getElementsByClassName("postform_link");
    for(var a = 0; a < e.length; a++){
        e[a].isOpen = false;
        e[a].postform = document.getElementsByClassName("postform_wrapper")[a];
        e[a].addEventListener("mouseup", function(){
            this.postform.style.display = this.isOpen ? "none" : "block";
            this.isOpen = this.isOpen ? false : true;
            this.innerHTML = this.isOpen ? "Закрыть форму постинга" : thread == '-1' ? "Создать" : "Ответить в тред";
        });
    }
}

function processPostformSubmitButtons(){
    e = document.getElementsByClassName("submit_button");
    for(var a = 0; a < e.length; a++){
        e[a].postForm = document.getElementsByClassName("contentform")[a];
        e[a].postForm.thread = window.thread;
        e[a].postFormText = document.getElementsByClassName("postform_text")[a];
        e[a].optionsForm = e[a].postForm.children[1].children[1];
        e[a].addEventListener('click', function(){
            if(busy)
                return;
            if(this.postFormText.value == ''){
                this.postFormText.style.border = '1px solid red';
                showAlert('Вы ничего не ввели в сообщении');
                return;
            }
            busy = true;
            d = new XMLHttpRequest();
            this.postFormText.value = this.postFormText.value.replace(/</g, '&lt;');
            var form_data = new FormData(this.postForm);
            $(this.postFormText).val('');
            form_data.append("board", window.board);
            form_data.append("thread", this.postForm.thread);
            form_data.append("trip", this.optionsForm.value);
            d.open('POST', '/takaba/posting');
            d.send(form_data);
            d.onreadystatechange = function(){
                if(this.readyState == 4){
                    var resp = JSON.parse(this.responseText);
                    if(resp.Status != 0){
                        e = document.getElementsByClassName('postform_input');
                        for(var a = 0; a < e.length; a++){
                            e[a].value = '';
                        }
                    }
                    showAlert((resp.Status == 1 ? "Ошибка постинга: " : "") + resp.Message);
                    busy = false;
                }
            }

        });
    }
}
    
    function getThread(postnum){
        for(var a = 0; a < window.posts.length; a++){
            if(window.posts[a].postnum == postnum)
                return window.posts[a].parent;
        }
        return null;
    }
    
function generateFlyingPostHTML(post, notForThread){
        res = "<div class=\"post_details\">"
        + "<input type=\"checkbox\" name=\"delete\" class=\"turnmeoff\" value=\"" + post.postnum + "\"/> " +
        (post.subject == "" ? "" : "<span class=\"post_subject\">" + post.subject + "</span> ") + post.name
        + (!post.trip == "" ? "<span class=\"post_trip\">!" + post.trip + "</span>" : "")
        + "<span class=\"post_date\">" + post.date + "</span>"
        + "<a class='post_reflink' href='#" + post.postnum
        + "'>№</a><span class=\"postnum postnum_" + post.postnum + "\" data-num=\"" + post.postnum + "\">" + post.postnum + "</span>"
        + (notForThread ? "" : "<span class=\"post_number\">#" + post.nit + "</span>")
        + "<span class=\"post_buttons_container button_container_" + post.postnum + "\">"
        + "<a class=\"postbtn_hide\" data-num=\"" + post.postnum + "\"></a>"
        + "<a class=\"postbtn_report\" data-num=\"" + post.postnum + "_" + post.parent + "\"></a>"
        + "<a class=\"postbtn_options\" data-num=\"" + post.postnum + "_" + post.parent + "\"></a>"
        + (notForThread && post.oppost == 'true' ? '<span class="oppost_reply_link">[<a href="/boards/' + board + '/' + post.postnum + '">Ответ</a>]</span>' : '')
        + "</div>";
        if(post.pics.length == 1){
            var a = 0;
            res += "<div class=\"file_attr single_file_attr\"><a href=\""
            + post.pics[a].full_path
            + "\" class=\"file_link\">"
            + post.pics[a].name
            + "</a></div>"
            + "<div class=\"file_attachment_sign single_file_attachment_sign\">(" + post.pics[a].size + "Кб, " + post.pics[a].width + "x" + post.pics[a].height + ")" + "</div>"
            + "<a href=\"" + post.pics[a].full_path + "\" class=\"file_attachment_single\">"
            + "<img src=\"" + post.pics[a].thumb_path + "\" data-src=\"" + post.pics[a].full_path + "\" src-width=\"" + post.pics[a].width + "\" src-height=\"" + post.pics[a].height + "\" class=\"attachment\"></img></a>";
        }
        if(post.pics.length > 1){
            res += "<div class=\"multiple_files_container\">";
            for(a = 0; a < post.pics.length; a++){
                res += "<div class=\"multiple_files_single\">"
                + "<div class=\"file_attr multiple_file_attr\">"
                + "<a href=\"" + post.pics[a].full_path + "\" class=\"file_link\">" + post.pics[a].name + "</a>"
                + "</div>"
                + "<div class=\"file_attachment_sign\">(" + post.pics[a].size + "Кб, " + post.pics[a].width + "x" + post.pics[a].height + ")" + "</div>"
                + "<a href=\"" + post.pics[a].full_path + "\" class=\"file_attachment_multiple\">"
                + "<img src=\"" + post.pics[a].thumb_path + "\" data-src=\"" + post.pics[a].full_path + "\" src-width=\"" + post.pics[a].width + "\" src-height=\"" + post.pics[a].height + "\" class=\"attachment\"></img></a>"
                + "</div>";
            }
            res += "</div><br/>";
        }
        res += "<article class=\"post_message\">" + post.message + "</article>"
        + "<span class=\"reply_map reply_map_" + post.postnum + "\"" + (post.replied_by.length > 0 ? "" : "style=\"display: none;\"") + " data-num=\"" + post.postnum + "\">Ответы: ";
        for(a = 0; a < post.replied_by.length; a++){
             res += "<a class=\"reply_map_entry\" data-num=\"" + post.replied_by[a] + "\">>>" + post.replied_by[a] + "</a>" + (a == post.replied_by.length-1 ? "" : ", ");
         }
        res += "</span>";
    return res;
}

function addPostnumClickCallback(link){
    $(link).click(function(){
            $(".qr_form_container").show().attr("post-num", $(this).attr('data-num'));
            var $form = $(".qr_form_message").focus();
            $form[0].value += '>>' + $(this).attr('data-num') + '\n';
            $(".postform_text").val($form.val());
            window.qrFormTarget = getThread($(this).attr("data-num"));
        });
}

function processPostButtonsContainer(container){
    
    e = container.getElementsByClassName("postbtn_options");
    for(a = 0; a < e.length; a++){
        e[a].addEventListener('mouseup', function(){
            if(window.select_list_open){
                if(window.select_list.dataNum != this.getAttribute('data-num')){
                    var c = window.select_list;
                    c.style.left = (calculateGlobalXOffset(this) + this.clientWidth).toString() + "px";
                    c.style.top = calculateGlobalYOffset(this).toString() + "px";
                    c.dataNum = this.getAttribute('data-num');
                    window.list_hide_button.setAttribute('onClick', 'hidePost(' + c.dataNum.split('_')[0] + ');');
                    window.list_report_button.setAttribute('data-num', c.dataNum);
                    return;
                }
                window.select_list.style.display = 'none';
                window.select_list_open = false;
                return;
            }
            var c = window.select_list;
            c.style.display = 'inline-block';
            c.style.left = (calculateGlobalXOffset(this) + this.clientWidth).toString() + "px";
            c.style.top = calculateGlobalYOffset(this).toString() + "px";
            c.dataNum = this.getAttribute('data-num');
            window.list_hide_button.setAttribute('onClick', 'hidePost(' + c.dataNum.split('_')[0] + ');');
            window.list_report_button.setAttribute('data-num', c.dataNum);
            window.select_list_open = true;
        });
    }
    
    e = container.getElementsByClassName("postbtn_report");
    for(a = 0; a < e.length; a++){
        e[a].addEventListener('click', function(){
            window.list_report_button.setAttribute('data-num', this.getAttribute('data-num'));
            processReport();
        });
    }
}

function synchronisePostforms(){
    $(".qr_form_message").on('input', function(){
        $(".postform_text").val($(this).val());
    });
    $(".postform_text").on('input', function(){
        $(".qr_form_message").val($(this).val());
    });
}

function processHiddenPosts(container){
    var hidden = window.localStorage.getItem('board.' + window.board + '.hidden');
    var hidden_raw = hidden;
    if(hidden != null && hidden.length > 0){
        hidden = hidden.split('|');
        for(var a = 0; a < hidden.length; a++){
            var num = hidden[a].split('_')[1];
            var date = hidden[a].split('_')[0];
            if(getTimeInDays() - parseInt(date) > window.hideStoreTimeout){
                hidden_raw = hidden_raw.replace(hidden[a] + '|', '');
                window.localStorage.setItem('board.' + window.board + '.hidden', hidden_raw);
                continue;
            }
            e = container.getElementsByClassName("thread-" + num);
            if(e.length == 0){
                e = container.getElementsByClassName("thread_post_" + num);
                if(e.length == 0)
                    continue;
                e = e[0];
                e.style.display = 'none';
                e.insertAdjacentHTML('afterend',
                '<div style="display: block; margin-top: 8px; margin-bottom: 8px;"><div class="hidden_post" post="'
                + num + '">Скрытый пост <span style="font-weight: bold">№'
                + num
                + '</span></div></div>');
            }else{
                e = e[0];
                e.style.display = 'none';
                e.insertAdjacentHTML('afterend',
                '<div style="display: block; margin-top: 8px; margin-bottom: 8px;"><div class="hidden_post hidden_thread" post="'
                + num + '">Скрытый тред <span style="font-weight: bold">№'
                + num
                + '</span></div></div>');
            }
        }
    }
}

function generateThreadHTML(thread_preview){
    
}