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
    private String root_path = System.getProperty("catalina.base").replace("\\", "/") + "/webapps/ROOT/";
    @Inject private BoardsCache boards_cache;
    
    @RequestMapping(value = "posting", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String posting(StandardMultipartHttpServletRequest request){ // постинг
        try{
            String board_id = utfEncode(request.getParameter("board"));
            String text = utfEncode((String) request.getParameter("contents"));
            ArrayList<String> repliesTo = new ArrayList<>();
            String[] parts = text.split(">>");
            Board board = boards_cache.getBoard(board_id);
            if(board == null)
                return buildResponse("1", "Доски не существует");
            ArrayList<Ban> bans = boards_cache.bans_list.get(board_id);
            if(boards_cache.bans_list.get("global_bans") != null) // проверяем не забанен ли клиент
                bans.addAll(boards_cache.bans_list.get("global_bans"));
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
                    if((int)Math.floor(file.getSize() / 1024 / 1024) > board.max_file_size)
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
                    int final_index = 0;
                    while(final_index < parts[a].length() && "1234567890".indexOf(parts[a].charAt(final_index)) != -1)
                        final_index ++;
                    String link_to_replace = parts[a].substring(0, final_index);
                    repliesTo.add(link_to_replace);
                    board.getPost(link_to_replace).addReply(String.valueOf(board.getTotalPosts()+1L));
                    parts[a] = parts[a].replaceFirst(link_to_replace, "<a class=\"post-reply-link\" data-num=\"" + link_to_replace + "\" parent-post-num=\"" + String.valueOf(board.getTotalPosts()+1L) + "\">>>" + link_to_replace + "</a>");
                    text = text.concat(parts[a]);
                }
            }
            text = text.replace("\r\n", "<br/>");
            String thread = utfEncode(request.getParameter("thread"));
            String name = utfEncode(request.getParameter("name"));
            String tripcode = "";
            if(name.contains("#")){ // генерация трипкода
                String[] name_parts = name.split("#", 2);
                tripcode = name_parts[1];
                name = name_parts[0];
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
                        boolean width_greater = img.getWidth() > img.getHeight();
                        double ratio = (double)(img.getWidth()) / img.getHeight();
                        double reverse_ratio = (double)(img.getHeight()) / img.getWidth();
                        BufferedImage thumb = new BufferedImage(
                        width_greater ? 250 : (int)(250*ratio),
                        width_greater ? (int)(250*reverse_ratio) : 250,
                        BufferedImage.SCALE_DEFAULT);
                        Image tmp = img.getScaledInstance(thumb.getWidth(), thumb.getHeight(), Image.SCALE_SMOOTH);
                        Graphics2D g2d = thumb.createGraphics();
                        g2d.drawImage(tmp, 0, 0, null);
                        g2d.dispose();
                        img = thumb; // превью
                    }
                    String[] spl = file.getOriginalFilename().split("\\.");
                    String extension = spl[spl.length-1];
                    File folder = new File(root_path + "res/" + board_id + "/src/");
                    folder.mkdirs();
                    String full_path = root_path + "res/" + board_id + "/src/" + filename + "." + extension;
                    String thumb_path = root_path + "res/" + board_id + "/thumb/" + filename + "." + extension;
                    InputStream is = file.getInputStream();
                    ByteArrayOutputStream os = new ByteArrayOutputStream();
                    int res = is.read();
                    while(res != -1){
                       os.write(res);
                       res = is.read();
                    }
                    os.flush();
                    if(!board.delayedFlushingEnabled){ // сохраняем в кэш либо сразу на диск
                        File full_size = new File(full_path);
                        File thumb = new File(thumb_path);
                        FileOutputStream stream = new FileOutputStream(full_size);
                        stream.write(os.toByteArray());
                        stream.close();
                        stream = new FileOutputStream(thumb);
                        ImageIO.write(img, extension, stream);
                        stream.close();
                    }else{
                        CachedImage full = new CachedImage(filename, extension, os.toByteArray());
                        boards_cache.images_cache.put(full_path, full);
                        os = new ByteArrayOutputStream();
                        ImageIO.write(img, extension, os);
                        os.flush();
                        boards_cache.images_cache.put(thumb_path, new CachedImage(filename, extension, os.toByteArray()));
                        boards_cache.needsImagesFlushing = true;
                    }
                    files.add(Json.createObjectBuilder()
                    .add("name", filename + "." + extension)
                    .add("width", String.valueOf(width))
                    .add("height", String.valueOf(height))
                    .add("thumb_width", String.valueOf(img.getWidth()))
                    .add("thumb_height", String.valueOf(img.getHeight()))
                    .add("full_path", "/res/" + board_id + "/src/" + filename + "." + extension)
                    .add("thumb_path", "/res/" + board_id + "/thumb/" + filename + "." + extension)
                    .add("size", String.valueOf((int) Math.ceil(os.toByteArray().length / 1024L)))
                    .build());
                }
            }
            JsonWriter writer;
            JsonReader reader;
            JsonArrayBuilder builder;
            String board_folder_path = root_path + "res//" + board_id;
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
            File catalog_file = new File(board_folder_path + "/catalog.json");
            trich.Thread cached_thread = board.getThread(thread);
            if(cached_thread != null){ // обновление in-memory storage
                cached_thread.addPost
                (Long.toString(board.getTotalPosts()+1L), thread, cached_thread.getPostcount()+1, name, tripcode, date, subject, text, files, false, request.getRemoteAddr(), repliesTo, new ArrayList<>());
                board.bumpThread(cached_thread);
                board.setTotalPosts(board.getTotalPosts()+1L);
            }else{
                cached_thread = new trich.Thread(new Post(
                Long.toString(board.getTotalPosts()+1L),
                thread, 1, name, tripcode, date, subject,
                text,
                files,
                true,
                request.getRemoteAddr(), repliesTo, new ArrayList<>()), board_id);
                board.addThread(cached_thread, true);
                board.setTotalPosts(board.getTotalPosts()+1L);
            }
            if(board.delayedFlushingEnabled){
                board.needsCatalogFlushing = true;
                cached_thread.needsFlushing = true;
            }else{
                boards_cache.flushThread(cached_thread);
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
        StringWriter string_writer = new StringWriter();
        JsonWriter json_writer = Json.createWriter(string_writer);
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("BoardTitle", board.getTitle());
        builder.add("BoardInfo", board.getDesc());
        builder.add("DefaultName", board.getDefaultName());
        builder.add("MaxFileSize", board.max_file_size);
        builder.add("DelayedFlushing", String.valueOf(board.delayedFlushingEnabled));
        json_writer.write(builder.build());
        return string_writer.toString();
    }
    
    @RequestMapping(value = "config", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String changeConfig(HttpServletRequest request){ // отредактировать настройки доски
        String mod_id = boards_cache.checkModerator(request);
        if(mod_id == null)
            return buildResponse("1", "Нет доступа");
        if(boards_cache.active_moder_sessions.get(mod_id).getAccessLevel() < 4)
            return buildResponse("1", "Нет доступа");
        String title = utfEncode(request.getParameter("title"));
        String edited_board = request.getParameter("edited");
        String id = request.getParameter("id");
        String brief = utfEncode(request.getParameter("brief"));
        String name = utfEncode(request.getParameter("default_name"));
        String mfs = request.getParameter("mfs");
        if(title == null || edited_board == null || id == null || brief == null || name == null || mfs == null){
            return buildResponse("1", "Отсутствуют необходимые параметры");
        }
        if(edited_board.equals("+")){
            int mfs_;
            try{
                mfs_ = Integer.parseInt(mfs);
            }catch(Exception e){
                return buildResponse("1", "Неверный формат");
            }
            Board board = new Board(id, title, brief, name, mfs, boards_cache);
            board.delayedFlushingEnabled = request.getParameter("delayed_flushing") != null;
            boards_cache.getBoards().add(board);
            boards_cache.boards_list.put(id, board);
            board.addBanReasonsSet(boards_cache.general_ban_reasons);
            File board_folder = new File(root_path + "res/" + id);
            board_folder.mkdirs();
            board.needsSettingsFlushing = true;
            return buildResponse("0", "Доска создана");
        }
        Board board = boards_cache.getBoard(edited_board);
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
            boards_cache.boards_list.remove(board.id);
            boards_cache.boards_list.put(id, board);
        }
        board.id = id;
        board.title = title;
        board.default_name = name;
        board.max_file_size = mfs_;
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
        if(text == null || thread == null || board == null || posts == null || boards_cache.getBoard(board) == null){
            return buildResponse("1", "Не указаны необходимые параметры");
        }
        if(boards_cache.getBoard(board) == null)
            return buildResponse("1", "Доски не существует");
        ArrayList<String> posts_list = new ArrayList<>();
        for(int a = 0; a < posts.length; a++)
            posts_list.add(posts[a]);
        boards_cache.getBoard(board).addReport(new Report(board, posts_list, text, ip, String.valueOf(boards_cache.getReportsCounter()+1)));
        boards_cache.setReportsCounter(boards_cache.getReportsCounter()+1);
        File reports_file = new File(root_path + "//res//" + board + "//reports.json");
        JsonArrayBuilder builder = Json.createArrayBuilder();
        if(reports_file.length() != 0){
            JsonReader reader = Json.createReader(new BufferedReader(new InputStreamReader(new FileInputStream(root_path + "//res//" + board + "//reports.json"), "UTF-8")));
            ArrayList<JsonObject> reports = new ArrayList<>(reader.readArray().getValuesAs(JsonObject.class));
            reader.close();
            for(int a = 0; a < reports.size(); a++)
                builder.add(reports.get(a));
        }
        JsonArrayBuilder posts_builder = Json.createArrayBuilder();
        for(int a = 0; a < posts.length; a++)
            posts_builder.add(posts[a]);
        builder.add(Json.createObjectBuilder()
        .add("board", board)
        .add("posts", posts_builder.build())
        .add("text", text)
        .add("ip", ip)
        .add("id", String.valueOf(boards_cache.getReportsCounter()))
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
        if(boards_cache.checkModerator(request) == null)
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
            Board board = boards_cache.getBoard(boards[a]);
            if(board == null)
                continue;
            boards_cache.removeReport(board.getReportByID(ids[a]));
            JsonArrayBuilder builder = Json.createArrayBuilder();
            JsonReader reader = Json.createReader(new BufferedReader(new InputStreamReader(new FileInputStream(root_path + "//res//" + boards[a] + "//reports.json"), "UTF-8")));
            ArrayList<JsonObject> reports = new ArrayList<>(reader.readArray().getValuesAs(JsonObject.class));
            reader.close();
            for(int b = 0; b < reports.size(); b++){
                if(!reports.get(b).getJsonString("id").getString().equals(ids[a]))
                    builder.add(reports.get(b));
            }
            JsonWriter writer = Json.createWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(root_path + "//res//" + boards[a] + "//reports.json"), "UTF-8")));
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
        String mod_id = boards_cache.checkModerator(request);
        if(mod_id == null)
            return buildResponse("1", "Нет доступа");
        String[] posts_to_delete = request.getParameterValues("post");
        if(posts_to_delete == null){
            return buildResponse("1", "Нет постов для удаления");
        }
        String board_id = request.getParameter("board");
        boolean applicable = false;
        String[] boards = boards_cache.active_moder_sessions.get(mod_id).getBoards();
        for(int a = 0; a < boards.length; a++){
            if(boards[a].equals(board_id)){
                applicable = true;
                break;
            }
        }
        if(!applicable)
            return buildResponse("1", "Нет доступа");
        Board board = boards_cache.getBoard(board_id);
        Post post;
        for(int a = 0; a < posts_to_delete.length; a++){
            post = board.getPost(posts_to_delete[a]);
            if(post == null)
                continue;
            for(int b = 0; b < post.getRepliedPosts().size(); b++){
                try{Post replied_post = board.getPost(post.getRepliedPosts().get(b));
                replied_post.removeReply(post.getPostnum());}catch(Exception e){continue;}
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
        String board_id = request.getParameter("board");
        if(postnum == null || board_id == null){
            return buildResponse("1", "Недостаточно параметров");
        }
        Board board = boards_cache.getBoard(board_id);
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
        String mod_id = boards_cache.checkModerator(request);
        if(mod_id == null)
            return buildResponse("1", "Нет доступа");
        if(boards_cache.active_moder_sessions.get(mod_id).getAccessLevel() < 4)
            return buildResponse("1", "Нет доступа");
        String key = request.getParameter("key");
        String al = request.getParameter("level");
        String boards_raw = request.getParameter("boards");
        String name = request.getParameter("name");
        if(key == null){
            return buildResponse("1", "Не указан ID");
        }
        if(boards_cache.getModByID(key) == null)
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
        Mod mod = boards_cache.getModByID(key);
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
        String mod_id = boards_cache.checkModerator(request);
        if(mod_id == null)
            return buildResponse("1", "Нет доступа");
        if(boards_cache.active_moder_sessions.get(mod_id).getAccessLevel() < 4)
            return buildResponse("1", "Нет доступа");
        String[] moders_to_remove = request.getParameterValues("moder");
        File mods_file = new File(root_path + "WEB-INF/mods.json");
        JsonReader reader = Json.createReader(new BufferedReader(new InputStreamReader(new FileInputStream(mods_file), "UTF-8")));
        ArrayList<JsonObject> mods = new ArrayList<>(reader.readArray().getValuesAs(JsonObject.class));
        reader.close();
        JsonArrayBuilder builder = Json.createArrayBuilder();
        building_new_array:
        for(int a = 0; a < mods.size(); a++){
            JsonObject mod = mods.get(a);
            for(int b = 0; b < moders_to_remove.length; b++){
                if(mod.getString("key").equals(moders_to_remove[b]))
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
        String mod_id = boards_cache.checkModerator(request);
        if(mod_id == null)
            return new JstlView("/thread_404.html");
        if(boards_cache.active_moder_sessions.get(mod_id).getAccessLevel() < 4)
            return new JstlView("/thread_404.html");
        model.put("boards", boards_cache.getBoards());
        return new JstlView("/WEB-INF/boards_management.jsp");
    }
    
    @RequestMapping(value = "mods_json", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getMods(HttpServletRequest request){ // получить данные о модераторах в json
        String mod_id = boards_cache.checkModerator(request);
        if(mod_id == null)
            return buildResponse("1", "Нет доступа");
        if(boards_cache.active_moder_sessions.get(mod_id).getAccessLevel() < 4)
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
        if(boards_cache.getModByID(key) == null)
            return new RedirectView("/takaba/mod_panel?task=login"); // TODO wrong ID
        String session_id = UUID.randomUUID().toString();
        while(boards_cache.active_moder_sessions.containsKey(session_id)){
            session_id = "";
            session_id = UUID.randomUUID().toString();
        }
        Cookie cookie = new Cookie("mod_session_id", session_id.toString());
        cookie.setMaxAge(1800);
        cookie.setPath("/");
        response.addCookie(cookie);
        boards_cache.active_moder_sessions.put(session_id, boards_cache.getModByID(key));
        String session_lambda = new String(session_id);
        System.out.println("Adding cookie with session ID = " + session_id + " and " + cookie.getMaxAge() + " maxAge");
        boards_cache.thread_pool.schedule(() -> {boards_cache.active_moder_sessions.remove(session_lambda);}, 1800, java.util.concurrent.TimeUnit.SECONDS);
        return new RedirectView("/takaba/mod_panel");
        }catch(Exception e){
            e.printStackTrace(System.out);
            return new InternalResourceView("/mod_login_form.html");
        }
    }
    
    @RequestMapping(value = "ban", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String addBan(HttpServletRequest request){ // забанить
        String mod_id = boards_cache.checkModerator(request);
        if(mod_id == null)
            return buildResponse("1", "Нет доступа");
        if(boards_cache.active_moder_sessions.get(mod_id).getAccessLevel() < 2)
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
        Board requested_board = boards_cache.getBoard(board);
        String IP = boards_cache.getBoard(board).getPost(post).getIP();
        Date expires = null;
        if(permanent == null){
            expires = new Date(
            Integer.parseInt(year) - 1900,
            Integer.parseInt(month)-1,
            Integer.parseInt(day),
            Integer.parseInt(hour),
            Integer.parseInt(minute));
        }
        boards_cache.addBan(permanent == null ? expires.getTime() : 0L, IP, requested_board.ban_reasons.get(Integer.parseInt(reason)), board, permanent == null ? false : true, global == null ? false : true, boards_cache.getBansCounter()+1L);
        boards_cache.setBansCounter(boards_cache.getBansCounter()+1L);
        return buildResponse("0", "Выдан бан № " + boards_cache.getBansCounter());
    }
    
    @RequestMapping(value = "unban", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String unban(HttpServletRequest request){ // разбанить
        String mod_id = boards_cache.checkModerator(request);
        if(mod_id == null || boards_cache.active_moder_sessions.get(mod_id).getAccessLevel() < 3){
            return buildResponse("1", "Нет доступа");
        }
        String id = request.getParameter("id");
        if(id == null){
            return buildResponse("1", "Не указан номер бана");
        }
        Ban ban = boards_cache.getBanByID(id);
        if(ban == null){
            return buildResponse("1", "Бан с таким ID не найден");
        }
        ScheduledFuture ban_schedule = boards_cache.scheduled_unbans.get(id);
        if(ban_schedule != null)
            ban_schedule.cancel(false);
        boards_cache.removeBan(boards_cache.getBanByID(id));
        return buildResponse("0", "Бан удалён");
    }
    
    private void ensureModeratorPersistence(Mod mod) throws IOException{ // общий функционал для добавления и редактирования модератора
        try{
        File moders_file = new File(root_path + "/WEB-INF/mods.json");
        JsonArrayBuilder boards_list_builder = Json.createArrayBuilder();
        String[] mod_boards = mod.getBoards();
        for(int a = 0; a < mod_boards.length; a++){
            boards_list_builder.add(mod_boards[a]);
        }
        JsonWriter writer;
        if(!moders_file.exists()){
            writer = Json.createWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(moders_file), "UTF-8")));
            writer.writeArray(Json.createArrayBuilder()
                .add(Json.createObjectBuilder()
                    .add("key", mod.getID())
                    .add("level", String.valueOf(mod.getAccessLevel()))
                    .add("boards", boards_list_builder.build())
                    .add("name", mod.getName())
                    .build())
            .build());
            writer.close();
        }else{
            boolean is_present = false;
            JsonArrayBuilder builder = Json.createArrayBuilder();
            JsonReader reader = Json.createReader(new BufferedReader(new InputStreamReader(new FileInputStream(moders_file), "UTF-8")));
            JsonArray mods = reader.readArray();
            reader.close();
            ArrayList<JsonObject> mods_list = new ArrayList<>(mods.getValuesAs(JsonObject.class));
            for(int a = 0; a < mods_list.size(); a++){
                if(mods_list.get(a).getString("key").equals(mod.getID())){
                    builder.add(Json.createObjectBuilder()
                    .add("key", mod.getID())
                    .add("level", String.valueOf(mod.getAccessLevel()))
                    .add("boards", boards_list_builder.build())
                    .add("name", mod.getName())
                    .build());
                    is_present = true;
                }else{
                    builder.add(mods_list.get(a));
                }
            }
            if(!is_present){
                builder.add(Json.createObjectBuilder()
                    .add("key", mod.getID())
                    .add("level", String.valueOf(mod.getAccessLevel()))
                    .add("boards", boards_list_builder.build())
                    .add("name", mod.getName())
                .build());
            }
            writer = Json.createWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(moders_file), "UTF-8")));
            writer.writeArray(builder.build());
            writer.close();
        }
        boards_cache.addModerator(mod);
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
        String modID = boards_cache.checkModerator(request);
        String task = request.getParameter("task");
        if(task == null){
            if(modID != null){
                model.put("name", boards_cache.active_moder_sessions.get(modID).getName());
                model.put("access_level", boards_cache.active_moder_sessions.get(modID).getAccessLevel());
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
                if(boards_cache.active_moder_sessions.get(modID).getAccessLevel() < 4)
                    return new RedirectView("/takaba/mod_panel");
                return new JstlView("/WEB-INF/admin_panel.html");
            case "view_reports":
                ArrayList<Report> reports = new ArrayList<>();
                String[] boards = boards_cache.active_moder_sessions.get(modID).getBoards();
                for(int a = 0; a < boards.length; a++){
                    Board board = boards_cache.getBoard(boards[a]);
                    if(board == null)
                        continue;
                    reports.addAll(board.getReports());
                }
                model.put("reports", reports);
                model.put("cache", boards_cache);
                return new JstlView("/WEB-INF/view_reports.jsp");
            case "view_bans":
                ArrayList<Ban> bans_to_display = new ArrayList<>();
                String id = request.getParameter("id");
                if(id != null){
                    Ban ban = boards_cache.getBanByID(id);
                    if(ban != null)
                        bans_to_display.add(ban);
                    model.put("bans", bans_to_display);
                    model.put("cache", boards_cache);
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
                ArrayList<Ban> bans_catalog = boards_cache.getBansCatalog();
                for(int a = (page-1)*10; a < page*10 && a < bans_catalog.size(); a++){
                    bans_to_display.add(bans_catalog.get(a));
                }
                model.put("bans", bans_to_display);
                model.put("cache", boards_cache);
                return new JstlView("/WEB-INF/view_bans.jsp");
            case "logout":
                boards_cache.active_moder_sessions.remove(modID);
                Cookie mod_cookie = new Cookie("mod_session_id", "");
                mod_cookie.setMaxAge(0);
                response.addCookie(mod_cookie);
                return new RedirectView("/");
            default:
                return new JstlView("/res/mod_panel.jsp");
        }
        
    }
}