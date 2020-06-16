package ajaxControllerBeans;

import java.util.*;
import java.io.*;
import java.nio.charset.*;
import org.springframework.stereotype.Controller;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.servlet.view.JstlView;
import org.springframework.web.servlet.view.InternalResourceView;
import org.springframework.web.multipart.support.StandardMultipartHttpServletRequest;
import org.springframework.web.multipart.MultipartFile;
import org.apache.commons.codec.digest.Crypt;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSessionBindingListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.Part;
import javax.servlet.ServletInputStream;
import javax.annotation.PreDestroy;
import javax.annotation.PostConstruct;
import javax.json.*;
import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.*;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ScheduledFuture;
import trich.*;
import rootContextBeans.*;


@Controller(value = "ajax_controller")
public class AjaxController{ // основной функциональный контроллер, отвечает на ajax запросы и ответственнен за модерку
                             // почему ещё и за модерку? Потому что адрес подходящий. Хотя можно и поменять
    private String rootPath = System.getProperty("catalina.base").replace("\\", "/") + "/webapps/ROOT/";
    @Inject private BoardsCache boardsCache;
    
    @RequestMapping(value = "posting", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String posting(StandardMultipartHttpServletRequest request){ // постинг
        try{
            String boardId = utfEncode(request.getParameter("board"));
            String text = utfEncode((String) request.getParameter("contents"));
            ArrayList<String> repliesTo = new ArrayList<>();
            String[] parts = text.split(">>");
            Board board = boardsCache.getBoard(board_id);
            if(board == null)
                return buildResponse("1", "Доски не существует");
            ArrayList<Ban> bans = boardsСache.bansList.get(board_id);
            if(boardsСache.bansList.get("global_bans") != null) // проверяем не забанен ли клиент
                bans.addAll(boards_cache.bansList.get("global_bans"));
            if(bans != null){
                for(int a = 0; a < bans.size(); a++){
                    if(bans.get(a).getIP().equals(request.getRemoteAddr())){
                        Ban ban = bans.get(a);
                        return buildResponse("1", "Постинг запрещён. Бан №" + ban.getID() + ". Причина: " + (ban.isPermanent() ? "[P] " : "") + ban.getReason() + (ban.isGlobal() ? "(global)" : "/!!" + ban.getBoard() + (ban.isPermanent() ? "" : ". Истекает: " + ban.getHumanReadableExpirationDate())));
                    }
                }
            }
            ArrayList<MultipartFile> pics = new ArrayList<>(request.getFiles("pic")); // макс. кол-во картинок
            if(pics.size() > 4){
                return buildResponse("1", "Слишком много файлов. Макс. кол-во: 4.");
            }
            if(pics.size() > 0){ // проверка картинок на ограничения, чтоб зря не обрабатывать
                String extension;
                for(MultipartFile file: pics){
                    if(file.isEmpty())
                        continue;
                    if((int)Math.floor(file.getSize() / 1024 / 1024) > board.maxFileSize)
                        return buildResponse("1", "Файл слишком большой");
                    String[] spl = file.getOriginalFilename().split("\\.");
                    extension = spl[spl.length-1];
                    if(!extension.equals("png") && !extension.equals("jpg") && !extension.equals("jpeg")){
                        return buildResponse("1", "Неподдерживаемый тип файлов (" + extension + ")");
                    }
                }
            }
            if(parts.length > 1){ // создание ссылок на цитируемые посты
                text = parts[0];
                for(int a = 1; a < parts.length; a++){
                    int finalIndex = 0;
                    while(finalIndex < parts[a].length() && "1234567890".indexOf(parts[a].charAt(finalIndex)) != -1)
                        finalIndex ++;
                    String linkToReplace = parts[a].substring(0, finalIndex);
                    repliesTo.add(linkToReplace);
                    board.getPost(linkToReplace).addReply(String.valueOf(board.getTotalPosts()+1L));
                    parts[a] = parts[a].replaceFirst(linkToReplace, "<a class=\"post-reply-link\" data-num=\"" + linkToReplace + "\" parent-post-num=\"" + String.valueOf(board.getTotalPosts()+1L) + "\">>>" + link_to_replace + "</a>");
                    text = text.concat(parts[a]);
                }
            }
            text = text.replace("\r\n", "<br/>");
            String thread = utfEncode(request.getParameter("thread"));
            String name = utfEncode(request.getParameter("name"));
            String tripcode = "";
            if(name.contains("#")){ // генерация трипкода
                String[] nameParts = name.split("#", 2);
                tripcode = nameParts[1];
                name = nameParts[0];
                tripcode = generateTrip(tripcode);
            }
            String subject = utfEncode((String) request.getParameter("subject"));
            ArrayList<JsonObject> files = new ArrayList<>();
            if(pics.size() > 0){ // обработка картинок, генерация фулсайза и превью, сохранение данных в кэш
                for(MultipartFile file: pics){
                    if(file.isEmpty())
                        continue;
                    String filename = Long.toString(System.currentTimeMillis());
                    BufferedImage img = ImageIO.read(file.getInputStream()); // фулсайз
                    int width = img.getWidth();
                    int height = img.getHeight();
                    if(img.getWidth() > 250 || img.getHeight() > 250){
                        boolean widthGreater = img.getWidth() > img.getHeight();
                        double ratio = (double)(img.getWidth()) / img.getHeight();
                        double reverseRatio = (double)(img.getHeight()) / img.getWidth();
                        BufferedImage thumb = new BufferedImage(
                        widthGreater ? 250 : (int)(250*ratio),
                        widthGreater ? (int)(250*reverseRatio) : 250,
                        BufferedImage.SCALE_DEFAULT);
                        Image tmp = img.getScaledInstance(thumb.getWidth(), thumb.getHeight(), Image.SCALE_SMOOTH);
                        Graphics2D g2d = thumb.createGraphics();
                        g2d.drawImage(tmp, 0, 0, null);
                        g2d.dispose();
                        img = thumb; // превью
                    }
                    String[] spl = file.getOriginalFilename().split("\\.");
                    String extension = spl[spl.length-1];
                    File folder = new File(rootPath + "res/" + boardId + "/src/");
                    folder.mkdirs();
                    String fullPath = rootPath + "res/" + boardId + "/src/" + filename + "." + extension;
                    String thumbPath = rootPath + "res/" + boardId + "/thumb/" + filename + "." + extension;
                    InputStream is = file.getInputStream();
                    ByteArrayOutputStream os = new ByteArrayOutputStream();
                    int res = is.read();
                    while(res != -1){
                       os.write(res);
                       res = is.read();
                    }
                    os.flush();
                    if(!board.delayedFlushingEnabled){ // сохраняем в кэш либо сразу на диск
                        File fullSize = new File(fullPath);
                        File thumb = new File(thumbPath);
                        FileOutputStream stream = new FileOutputStream(fullSize);
                        stream.write(os.toByteArray());
                        stream.close();
                        stream = new FileOutputStream(thumb);
                        ImageIO.write(img, extension, stream);
                        stream.close();
                    }else{
                        CachedImage full = new CachedImage(filename, extension, os.toByteArray());
                        boardsCache.images_cache.put(fullPath, full);
                        os = new ByteArrayOutputStream();
                        ImageIO.write(img, extension, os);
                        os.flush();
                        boardsCache.images_cache.put(thumb_path, new CachedImage(filename, extension, os.toByteArray()));
                        boardsCache.needsImagesFlushing = true;
                    }
                    files.add(Json.createObjectBuilder()
                    .add("name", filename + "." + extension)
                    .add("width", String.valueOf(width))
                    .add("height", String.valueOf(height))
                    .add("thumb_width", String.valueOf(img.getWidth()))
                    .add("thumb_height", String.valueOf(img.getHeight()))
                    .add("full_path", "/res/" + boardId + "/src/" + filename + "." + extension)
                    .add("thumb_path", "/res/" + boardId + "/thumb/" + filename + "." + extension)
                    .add("size", String.valueOf((int) Math.ceil(os.toByteArray().length / 1024L)))
                    .build());
                }
            }
            JsonWriter writer;
            JsonReader reader;
            JsonArrayBuilder builder;
            String boardFolderPath = rootPath + "res//" + boardId;
            if(thread.equals("-1")) // новый тред
                thread = Long.toString(board.getTotalPosts()+1L);
            if(name.equals("")){
                name = board.getDefaultName();
            }
            ArrayList<JsonString> catalog;
            JsonArray posts;
            Date date_ = new Date();
            String dow = new String();
            switch(date_.getDay()){ // резолвинг дня недели
                case 1:
                    dow = "Пнд";
                    break;
                case 2:
                    dow = "Втр";
                    break;
                case 3:
                    dow = "Срд";
                    break;
                case 4:
                    dow = "Чтв";
                    break;
                case 5:
                    dow = "Птн";
                    break;
                case 6:
                    dow = "Суб";
                    break;
                case 0:
                    dow = "Вск";
                    break;
            }
            String month = "";
            switch(date_.getMonth()){ // резолвинг месяца
                case 0:
                    month = "Янв";
                    break;
                case 1:
                    month = "Фев";
                    break;
                case 2:
                    month = "Мар";
                    break;
                case 3:
                    month = "Апр";
                    break;
                case 4:
                    month = "Май";
                    break;
                case 5:
                    month = "Июн";
                    break;
                case 6:
                    month = "Июл";
                    break;
                case 7:
                    month = "Авг";
                    break;
                case 8:
                    month = "Сен";
                    break;
                case 9:
                    month = "Окт";
                    break;
                case 10:
                    month = "Ноя";
                    break;
                case 11:
                    month = "Дек";
                    break;
            }
            String date = dow // human-readable дата
            + " "
            + (date_.getDate() < 10 ? "0" + date_.getDate() : date_.getDate())
            + " "
            + month
            + " "
            + (date_.getYear() + 1900)
            + " "
            + (date_.getHours() < 10 ? "0" + date_.getHours() : date_.getHours())
            + ":"
            + (date_.getMinutes() < 10 ? "0" + date_.getMinutes() : date_.getMinutes())
            + ":"
            + (date_.getSeconds() < 10 ? "0" + date_.getSeconds() : date_.getSeconds());
            File catalogFile = new File(boardFolderPath + "/catalog.json");
            trich.Thread cachedThread = board.getThread(thread);
            if(cachedThread != null){ // обновление in-memory storage
                cachedThread.addPost
                (Long.toString(board.getTotalPosts()+1L), thread, cachedThread.getPostcount()+1, name, tripcode, date, subject, text, files, false, request.getRemoteAddr(), repliesTo, new ArrayList<>());
                board.bumpThread(cachedThread);
                board.setTotalPosts(board.getTotalPosts()+1L);
            }else{
                cachedThread = new trich.Thread(new Post(
                Long.toString(board.getTotalPosts()+1L),
                thread, 1, name, tripcode, date, subject,
                text,
                files,
                true,
                request.getRemoteAddr(), repliesTo, new ArrayList<>()), boardId);
                board.addThread(cachedThread, true);
                board.setTotalPosts(board.getTotalPosts()+1L);
            }
            if(board.delayedFlushingEnabled){
                board.needsCatalogFlushing = true;
                cached_thread.needsFlushing = true;
            }else{
                boards_cache.flushThread(cachedThread);
            }
            return buildResponse("0", "Сообщение отправлено");
        }catch(Exception e){
            e.printStackTrace(System.out);
            return buildResponse("1", e.toString());
        }
    }
    
    private String buildResponse(String code, String msg){
        return "{\"Status\":" + code + ",\"Message\":\"" + msg + "\"}";
    }
    
    private String utfEncode(String str){
        return new String(str.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
    }
    
    @RequestMapping(value = "posting", method = RequestMethod.GET)
    public View test(){
        return new RedirectView("/");
    }
    
    @RequestMapping(value = "config", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String config(HttpServletRequest request){ // получить настройки доски
        String id = request.getParameter("board");
        if(id == null){
            return buildResponse("1", "Не указана доска");
        }
        Board board = boards_cache.getBoard(id);
        if(board == null){
            return buildResponse("1", "Доски не существует");
        }
        StringWriter stringWriter = new StringWriter();
        JsonWriter jsonWriter = Json.createWriter(stringWriter);
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("BoardTitle", board.getTitle());
        builder.add("BoardInfo", board.getDesc());
        builder.add("DefaultName", board.getDefaultName());
        builder.add("MaxFileSize", board.max_file_size);
        builder.add("DelayedFlushing", String.valueOf(board.delayedFlushingEnabled));
        jsonWriter.write(builder.build());
        return stringWriter.toString();
    }
    
    @RequestMapping(value = "config", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String changeConfig(HttpServletRequest request){ // отредактировать настройки доски
        String modId = boards_cache.checkModerator(request);
        if(modId == null)
            return buildResponse("1", "Нет доступа");
        if(boardsCache.activeModerSessions.get(mod_id).getAccessLevel() < 4)
            return buildResponse("1", "Нет доступа");
        String title = utfEncode(request.getParameter("title"));
        String editedBoard = request.getParameter("edited");
        String id = request.getParameter("id");
        String brief = utfEncode(request.getParameter("brief"));
        String name = utfEncode(request.getParameter("default_name"));
        String mfs = request.getParameter("mfs");
        if(title == null || editedBoard == null || id == null || brief == null || name == null || mfs == null){
            return buildResponse("1", "Отсутствуют необходимые параметры");
        }
        if(editedBoard.equals("+")){
            int mfs_;
            try{
                mfs_ = Integer.parseInt(mfs);
            }catch(Exception e){
                return buildResponse("1", "Неверный формат");
            }
            Board board = new Board(id, title, brief, name, mfs, boardsCache);
            board.delayedFlushingEnabled = request.getParameter("delayed_flushing") != null;
            boardsCache.getBoards().add(board);
            boardsCache.boards_list.put(id, board);
            board.addBanReasonsSet(boardsCache.generalBanReasons);
            File boardFolder = new File(rootPath + "res/" + id);
            boardFolder.mkdirs();
            board.needsSettingsFlushing = true;
            return buildResponse("0", "Доска создана");
        }
        Board board = boardsCache.getBoard(editedBoard);
        if(board == null){
            return buildResponse("1", "Доски не существует");
        }
        int mfs_;
        try{
        mfs_ = Integer.parseInt(mfs);
        }catch(Exception e){
            return buildResponse("1", "Неверный формат");
        }
        if(!id.equals(board.id)){
            boardsCache.boardsList.remove(board.id);
            boardscache.boardsList.put(id, board);
        }
        board.id = id;
        board.title = title;
        board.defaultName = name;
        board.maxFileSize = mfs_;
        board.desc = brief;
        board.delayedFlushingEnabled = request.getParameter("delayed_flushing") != null;
        board.needsSettingsFlushing = true;
        return buildResponse("0", "Данные отредактированы");
    }
    
    @RequestMapping(value = "send_report", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String sendReport(HttpServletRequest request){ // обработка жалоб
        try{                                              // жалобы не попадают в авто-флашинг, а записываются сразу
        String text = utfEncode((String) request.getParameter("text"));
        String thread = utfEncode((String) request.getParameter("thread"));
        String board = utfEncode((String) request.getParameter("board"));
        String[] posts = request.getParameterValues("posts");
        String ip = request.getRemoteAddr();
        if(text == null || thread == null || board == null || posts == null || boardsCache.getBoard(board) == null){
            return buildResponse("1", "Не указаны необходимые параметры");
        }
        if(boardsCache.getBoard(board) == null)
            return buildResponse("1", "Доски не существует");
        ArrayList<String> postsList = new ArrayList<>();
        for(int a = 0; a < posts.length; a++)
            postsList.add(posts[a]);
        boardsCache.getBoard(board).addReport(new Report(board, postsList, text, ip, String.valueOf(boards_cache.getReportsCounter()+1)));
        boardsCache.setReportsCounter(boardsCache.getReportsCounter()+1);
        File reportsFile = new File(rootPath + "//res//" + board + "//reports.json");
        JsonArrayBuilder builder = Json.createArrayBuilder();
        if(reportsFile.length() != 0){
            JsonReader reader = Json.createReader(new BufferedReader(new InputStreamReader(new FileInputStream(rootPath + "//res//" + board + "//reports.json"), "UTF-8")));
            ArrayList<JsonObject> reports = new ArrayList<>(reader.readArray().getValuesAs(JsonObject.class));
            reader.close();
            for(int a = 0; a < reports.size(); a++)
                builder.add(reports.get(a));
        }
        JsonArrayBuilder postsBuilder = Json.createArrayBuilder();
        for(int a = 0; a < posts.length; a++)
            postsBuilder.add(posts[a]);
        builder.add(Json.createObjectBuilder()
        .add("board", board)
        .add("posts", postsBuilder.build())
        .add("text", text)
        .add("ip", ip)
        .add("id", String.valueOf(boardsCache.getReportsCounter()))
        .build());
        JsonWriter writer = Json.createWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(root_path + "//res//" + board + "//reports.json"), "UTF-8")));
        writer.writeArray(builder.build());
        writer.close();
        return buildResponse("0", "Накляузничано");
        }catch(Exception e){
            e.printStackTrace(System.out);
            return buildResponse("1", e.toString().replaceAll("\\", "\\\\"));
        }
    }
    
    @RequestMapping(value = "delete_reports", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String deleteReports(HttpServletRequest request){ // удалить жалобы
        try{
        if(boardsCache.checkModerator(request) == null)
            return buildResponse("1", "Нет доступа");
        String[] raw = request.getParameterValues("id");
        if(raw == null)
            return buildResponse("1", "Не найдено репортов для удаления");
        String[] ids = new String[raw.length];
        String[] boards = new String[raw.length];
        for(int a = 0; a < raw.length; a++){
            ids[a] = raw[a].split("_")[1];
            boards[a] = raw[a].split("_")[0];
        }
        for(int a = 0; a < raw.length; a++){
            Board board = boardsCache.getBoard(boards[a]);
            if(board == null)
                continue;
            boardsCache.removeReport(board.getReportByID(ids[a]));
            JsonArrayBuilder builder = Json.createArrayBuilder();
            JsonReader reader = Json.createReader(new BufferedReader(new InputStreamReader(new FileInputStream(rootPath + "//res//" + boards[a] + "//reports.json"), "UTF-8")));
            ArrayList<JsonObject> reports = new ArrayList<>(reader.readArray().getValuesAs(JsonObject.class));
            reader.close();
            for(int b = 0; b < reports.size(); b++){
                if(!reports.get(b).getJsonString("id").getString().equals(ids[a]))
                    builder.add(reports.get(b));
            }
            JsonWriter writer = Json.createWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(rootPath + "//res//" + boards[a] + "//reports.json"), "UTF-8")));
            writer.writeArray(builder.build());
            writer.close();
        }
        return buildResponse("0", "Жалобы удалены");
        }catch(Exception e){
            e.printStackTrace(System.out);
            return buildResponse("1", e.toString());
        }
    }
    
    @RequestMapping(value = "delete_posts", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String deletePosts(HttpServletRequest request){ // удалить посты
        String modId = boardsCache.checkModerator(request);
        if(modId == null)
            return buildResponse("1", "Нет доступа");
        String[] postsToDelete = request.getParameterValues("post");
        if(postsToDelete == null){
            return buildResponse("1", "Нет постов для удаления");
        }
        String boardId = request.getParameter("board");
        boolean applicable = false;
        String[] boards = boards_cache.activeModerSessions.get(modId).getBoards();
        for(int a = 0; a < boards.length; a++){
            if(boards[a].equals(boardId)){
                applicable = true;
                break;
            }
        }
        if(!applicable)
            return buildResponse("1", "Нет доступа");
        Board board = boardsCache.getBoard(boardId);
        Post post;
        for(int a = 0; a < postsToDelete.length; a++){
            post = board.getPost(postsToDelete[a]);
            if(post == null)
                continue;
            for(int b = 0; b < post.getRepliedPosts().size(); b++){
                try{
                    Post repliedPost = board.getPost(post.getRepliedPosts().get(b));
                    repliedPost.removeReply(post.getPostnum());
                }catch(Exception e){continue;}
            }
            board.getThread(post.getThread()).removePost(post);
        }
        return buildResponse("0", "Сообщения удалены");
    }
    
    @RequestMapping(value = "add_moder", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String addModer(HttpServletRequest request){ // добавить модератора
        try{
        if(boards_cache.checkModerator(request) == null)
            return buildResponse("1", "Нет доступа");
        String al = request.getParameter("level");
        String boards_raw = request.getParameter("boards");
        String name = request.getParameter("name");
        String[] boards;
        if(al == null)
            return buildResponse("1", "Не указаны необходимые параметры");
        if(boards_raw.contains(",")){
            boards = boards_raw.split(",");
        }else{
            boards = new String[1];
            boards[0] = boards_raw;
        }
        for(int a = 0; a < boards.length; a++){
            boards[a] = boards[a].replaceAll(" ", "");
        }
        String key = ""; // can be replaced with other alg if necessary
        for(int a = 0; a < 5; a++){
            key = key.concat(generateTrip(String.valueOf((double)(1000000.0 * Math.random()))));
        }
        ensureModeratorPersistence(new Mod(key, al, boards, name));
        return buildResponse("0", "Модератор добавлен. Ключ: " + key);
        }catch(Exception e){
            e.printStackTrace(System.out);
            return buildResponse("1", e.toString());
        }
    }
    
    @RequestMapping(value = "json_on_demand", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String randomThreadJSON(HttpServletRequest request){ // получить json треда по номеру поста
        String postnum = request.getParameter("num");           // нужно для подгрузки ответов
        String boardId = request.getParameter("board");
        if(postnum == null || boardId == null){
            return buildResponse("1", "Недостаточно параметров");
        }
        Board board = boardsCache.getBoard(board_id);
        if(board == null){
            return buildResponse("1", "Доски не существует");
        }
        Post post = board.getPost(postnum);
        if(post == null){
            return buildResponse("1", "Пост не найден");
        }
        return board.getThread(post.getThread()).json;
    }
    
    @RequestMapping(value = "edit_moder", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String editModer(HttpServletRequest request){ // изменить данные модератора
        try{
        String modId = boards_cache.checkModerator(request);
        if(modId == null)
            return buildResponse("1", "Нет доступа");
        if(boardsCache.activeModerSessions.get(modId).getAccessLevel() < 4)
            return buildResponse("1", "Нет доступа");
        String key = request.getParameter("key");
        String al = request.getParameter("level");
        String boards_raw = request.getParameter("boards");
        String name = request.getParameter("name");
        if(key == null){
            return buildResponse("1", "Не указан ID");
        }
        if(boardsCache.getModByID(key) == null)
            return buildResponse("1", "Модератор не найден");
        String[] boards;
        if(al == null)
            return buildResponse("1", "Не указаны необходимые параметры");
        if(boards_raw.contains(",")){
            boards = boards_raw.split(",");
        }else{
            boards = new String[1];
            boards[0] = boards_raw;
        }
        for(int a = 0; a < boards.length; a++){
            boards[a] = boards[a].replaceAll(" ", "");
        }
        ensureModeratorPersistence(new Mod(key, al, boards, name));
        Mod mod = boardsCache.getModByID(key);
        mod.setAccessLevel(Integer.parseInt(al));
        mod.setName(name);
        mod.setBoards(boards);
        return buildResponse("0", "Данные модератора отредактированы");
        }catch(Exception e){
            e.printStackTrace(System.out);
            return buildResponse("1", e.toString());
        }
    }
    
    @RequestMapping(value = "delete_moder", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String deleteModer(HttpServletRequest request){ // удалить модератора
        try{
        String modId = boards_cache.checkModerator(request);
        if(modId == null)
            return buildResponse("1", "Нет доступа");
        if(boardsCache.activeModerSessions.get(modId).getAccessLevel() < 4)
            return buildResponse("1", "Нет доступа");
        String[] modersToRemove = request.getParameterValues("moder");
        File modsFile = new File(rootPath + "WEB-INF/mods.json");
        JsonReader reader = Json.createReader(new BufferedReader(new InputStreamReader(new FileInputStream(modsFile), "UTF-8")));
        ArrayList<JsonObject> mods = new ArrayList<>(reader.readArray().getValuesAs(JsonObject.class));
        reader.close();
        JsonArrayBuilder builder = Json.createArrayBuilder();
        building_new_array:
        for(int a = 0; a < mods.size(); a++){
            JsonObject mod = mods.get(a);
            for(int b = 0; b < modersToRemove.length; b++){
                if(mod.getString("key").equals(modersToRemove[b]))
                    continue building_new_array;
            }
            builder.add(mod);
        }
        JsonWriter writer = Json.createWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(root_path + "WEB-INF/mods.json"), "UTF-8")));
        writer.writeArray(builder.build());
        writer.close();
        return buildResponse("0", "Модераторы удалены");
        }catch(Exception e){
            e.printStackTrace(System.out);
            return buildResponse("1", e.toString());
        }
    }
    
    @RequestMapping(value = "board_management", method = RequestMethod.GET) // страница управления досками
    public View boardManagement(HttpServletRequest request, HashMap<String, Object> model){
        String modId = boardsCache.checkModerator(request);
        if(modId == null)
            return new JstlView("/thread_404.html");
        if(boardsCache.activeModerSessions.get(modId).getAccessLevel() < 4)
            return new JstlView("/thread_404.html");
        model.put("boards", boardsCache.getBoards());
        return new JstlView("/WEB-INF/boards_management.jsp");
    }
    
    @RequestMapping(value = "mods_json", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getMods(HttpServletRequest request){ // получить данные о модераторах в json
        String modId = boardsCache.checkModerator(request);
        if(modId == null)
            return buildResponse("1", "Нет доступа");
        if(boardsCache.activeModerSessions.get(modId).getAccessLevel() < 4)
            return buildResponse("1", "Нет доступа");
        JsonArrayBuilder builder = Json.createArrayBuilder();
        ArrayList<Mod> mods = new ArrayList<>(boards_cache.mods_list.values());
        for(int a = 0; a < mods.size(); a++){
            Mod mod = mods.get(a);
            String boards = "";
            String[] boards_raw = mod.getBoards();
            for(int b = 0; b < boards_raw.length; b++){
                boards += boards_raw[b];
                if(b != boards_raw.length-1)
                    boards += ", ";
            }
            builder.add(Json.createObjectBuilder()
                .add("key", mod.getID())
                .add("level", mod.getAccessLevel())
                .add("boards", boards)
                .add("name", mod.getName()));
        }
        StringWriter res = new StringWriter();
        JsonWriter writer = Json.createWriter(res);
        writer.write(builder.build());
        return res.toString();
    }
    
    @RequestMapping(value = "login", method = RequestMethod.POST)
    public View login(HttpServletRequest request, HttpServletResponse response){ // вход в систему для модераторов
        try{
        String key = request.getParameter("key");
        if(key == null){
            return new RedirectView("/takaba/mod_panel?task=login");
        }
        if(boardsCache.getModByID(key) == null)
            return new RedirectView("/takaba/mod_panel?task=login"); // TODO wrong ID
        String sessionId = UUID.randomUUID().toString();
        while(boardsCache.active_moder_sessions.containsKey(sessionId)){
            sessionId = "";
            sessionId = UUID.randomUUID().toString();
        }
        Cookie cookie = new Cookie("mod_session_id", sessionId.toString());
        cookie.setMaxAge(1800);
        cookie.setPath("/");
        response.addCookie(cookie);
        boardsCache.activeModerSessions.put(sessionId, boardsCache.getModByID(key));
        String sessionLambda = new String(sessionId);
        boardsCache.threadPool.schedule(() -> {boardsCache.activeModerSessions.remove(sessionLambda);}, 3600, java.util.concurrent.TimeUnit.SECONDS);
        return new RedirectView("/takaba/mod_panel");
        }catch(Exception e){
            e.printStackTrace(System.out);
            return new InternalResourceView("/mod_login_form.html");
        }
    }
    
    @RequestMapping(value = "ban", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String addBan(HttpServletRequest request){ // забанить
        String modId = boardsCache.checkModerator(request);
        if(modId == null)
            return buildResponse("1", "Нет доступа");
        if(boardsCache.activeModerSessions.get(modId).getAccessLevel() < 2)
            return buildResponse("1", "Нет доступа");
        String board = request.getParameter("board");
        String post = request.getParameter("post");
        String reason = request.getParameter("reason");
        String year = request.getParameter("year");
        String month = request.getParameter("month");
        String day = request.getParameter("day");
        String hour = request.getParameter("hour");
        String minute = request.getParameter("min");
        String permanent = request.getParameter("permanent");
        String global = request.getParameter("global");
        if(reason == null || post == null || (permanent == null && board == null))
            return buildResponse("1", "Отсутствуют необходимые параметры");
        if(permanent == null && (year == null || month == null || day == null || hour == null || minute == null))
            return buildResponse("1", "Отсутствуют необходимые параметры");
        Board requestedBoard = boardsCache.getBoard(board);
        String IP = boardsCache.getBoard(board).getPost(post).getIP();
        Date expires = null;
        if(permanent == null){
            expires = new Date(
            Integer.parseInt(year) - 1900,
            Integer.parseInt(month)-1,
            Integer.parseInt(day),
            Integer.parseInt(hour),
            Integer.parseInt(minute));
        }
        boardsCache.addBan(permanent == null ? expires.getTime() : 0L, IP, requestedBoard.banReasons.get(Integer.parseInt(reason)), board, permanent == null ? false : true, global == null ? false : true, boardsCache.getBansCounter()+1L);
        boardsCache.setBansCounter(boards_cache.getBansCounter()+1L);
        return buildResponse("0", "Выдан бан № " + boardsCache.getBansCounter());
    }
    
    @RequestMapping(value = "unban", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String unban(HttpServletRequest request){ // разбанить
        String modId = boardsCache.checkModerator(request);
        if(modId == null || boardsCache.activeModerSessions.get(modId).getAccessLevel() < 3){
            return buildResponse("1", "Нет доступа");
        }
        String id = request.getParameter("id");
        if(id == null){
            return buildResponse("1", "Не указан номер бана");
        }
        Ban ban = boardsCache.getBanByID(id);
        if(ban == null){
            return buildResponse("1", "Бан с таким ID не найден");
        }
        ScheduledFuture banSchedule = boardsCache.scheduledUnbans.get(id);
        if(banSchedule != null)
            banSchedule.cancel(false);
        boardsCache.removeBan(boardsCache.getBanByID(id));
        return buildResponse("0", "Бан удалён");
    }
    
    private void ensureModeratorPersistence(Mod mod) throws IOException{ // общий функционал для добавления и редактирования модератора
        try{
        File modersFile = new File(rootPath + "/WEB-INF/mods.json");
        JsonArrayBuilder boardsListBuilder = Json.createArrayBuilder();
        String[] modBoards = mod.getBoards();
        for(int a = 0; a < modBoards.length; a++){
            boardsListBuilder.add(modBoards[a]);
        }
        JsonWriter writer;
        if(!modersFile.exists()){
            writer = Json.createWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(modersFile), "UTF-8")));
            writer.writeArray(Json.createArrayBuilder()
                .add(Json.createObjectBuilder()
                    .add("key", mod.getID())
                    .add("level", String.valueOf(mod.getAccessLevel()))
                    .add("boards", boardsListBuilder.build())
                    .add("name", mod.getName())
                    .build())
            .build());
            writer.close();
        }else{
            boolean isPresent = false;
            JsonArrayBuilder builder = Json.createArrayBuilder();
            JsonReader reader = Json.createReader(new BufferedReader(new InputStreamReader(new FileInputStream(modersFile), "UTF-8")));
            JsonArray mods = reader.readArray();
            reader.close();
            ArrayList<JsonObject> modsList = new ArrayList<>(mods.getValuesAs(JsonObject.class));
            for(int a = 0; a < modsList.size(); a++){
                if(modsList.get(a).getString("key").equals(mod.getID())){
                    builder.add(Json.createObjectBuilder()
                    .add("key", mod.getID())
                    .add("level", String.valueOf(mod.getAccessLevel()))
                    .add("boards", boardsListBuilder.build())
                    .add("name", mod.getName())
                    .build());
                    is_present = true;
                }else{
                    builder.add(modsList.get(a));
                }
            }
            if(!isPresent){
                builder.add(Json.createObjectBuilder()
                    .add("key", mod.getID())
                    .add("level", String.valueOf(mod.getAccessLevel()))
                    .add("boards", boardsListBuilder.build())
                    .add("name", mod.getName())
                .build());
            }
            writer = Json.createWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(modersFile), "UTF-8")));
            writer.writeArray(builder.build());
            writer.close();
        }
        boardsCache.addModerator(mod);
        }catch(IOException e){
            throw e;
        }
    }
    
    private String generateTrip(String input){ // получить трипкод для ключа
                input = Crypt.crypt(input,
                input.concat("H..")
                .substring(1, 3)
                .replaceAll("^[A-Za-z]|", ".")
                .replaceAll(":", "A")
                .replaceAll(";", "B")
                .replaceAll("<", "C")
                .replaceAll("=", "D")
                .replaceAll(">", "E")
                .replaceAll("\\?", "F")
                .replaceAll("@", "G")
                .replaceAll("\\[", "a")
                .replaceAll("\\\\", "b")
                .replaceAll("\\]", "c")
                .replaceAll("\\^", "d")
                .replaceAll("_", "e")
                .replaceAll("`", "f"));
                input = input.substring(input.length()-10, input.length());
                return input;
    }
    
    @RequestMapping(value = "mod_panel", method = RequestMethod.GET) // доступ к мод-панели
    public View access_mod_panel(HttpServletRequest request, HttpServletResponse response, Map<String, Object> model){
        String modID = boardsCache.checkModerator(request);
        String task = request.getParameter("task");
        if(task == null){
            if(modID != null){
                model.put("name", boardsCache.activeModerSessions.get(modID).getName());
                model.put("access_level", boardsCache.activeModerSessions.get(modID).getAccessLevel());
                return new JstlView("/WEB-INF/mod_panel.jsp");
            }else
                return new InternalResourceView("/insufficient_previleges.html");
        }
        if(modID == null && !task.equals("login")){
            return new InternalResourceView("/insufficient_previleges.html");
        }
        if(modID != null && task.equals("login"))
            return new RedirectView("/takaba/mod_panel");
        switch(task){
            case "login":
                return new InternalResourceView("/mod_login_form.html");
            case "admin_panel":
                if(boardsCache.activeModerSessions.get(modID).getAccessLevel() < 4)
                    return new RedirectView("/takaba/mod_panel");
                return new JstlView("/WEB-INF/admin_panel.html");
            case "view_reports":
                ArrayList<Report> reports = new ArrayList<>();
                String[] boards = boardsCache.activeModerSessions.get(modID).getBoards();
                for(int a = 0; a < boards.length; a++){
                    Board board = boardsCache.getBoard(boards[a]);
                    if(board == null)
                        continue;
                    reports.addAll(board.getReports());
                }
                model.put("reports", reports);
                model.put("cache", boardsCache);
                return new JstlView("/WEB-INF/view_reports.jsp");
            case "view_bans":
                ArrayList<Ban> bansToDisplay = new ArrayList<>();
                String id = request.getParameter("id");
                if(id != null){
                    Ban ban = boardsCache.getBanByID(id);
                    if(ban != null)
                        bansToDisplay.add(ban);
                    model.put("bans", bansToDisplay);
                    model.put("cache", boardsCache);
                    return new JstlView("/WEB-INF/view_bans.jsp");
                }
                int page;
                try{
                    page = Integer.parseInt(request.getParameter("page"));
                    if(page < 1)
                        page = 1;
                }catch(Exception e){
                    page = 1;
                }
                ArrayList<Ban> bansCatalog = boardsCache.getBansCatalog();
                for(int a = (page-1)*10; a < page*10 && a < bansCatalog.size(); a++){
                    bansToDisplay.add(bansCatalog.get(a));
                }
                model.put("bans", bansToDisplay);
                model.put("cache", boards_cache);
                return new JstlView("/WEB-INF/view_bans.jsp");
            case "logout":
                boardsCache.activeModerSessions.remove(modID);
                Cookie modCookie = new Cookie("mod_session_id", "");
                modCookie.setMaxAge(0);
                response.addCookie(modCookie);
                return new RedirectView("/");
            default:
                return new JstlView("/res/mod_panel.jsp");
        }
        
    }
}
