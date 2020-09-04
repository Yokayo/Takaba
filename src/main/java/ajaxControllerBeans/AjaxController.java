package ajaxControllerBeans;

import java.util.*;
import java.io.*;
import java.nio.charset.*;
import org.springframework.stereotype.Controller;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MissingRequestCookieException;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ExceptionHandler;
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
import org.hibernate.HibernateException;
import trich.*;
import rootContextBeans.*;


@Controller(value = "ajax_controller")
public class AjaxController{ // основной функциональный контроллер, отвечает на ajax запросы (в том числе модерские)
    
    private String rootPath = System.getProperty("catalina.base").replace("\\", "/") + "/webapps/ROOT/";
    
    @Inject private BoardsCache boardsCache;
    
    @RequestMapping(value = "posting", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String posting(@RequestParam(name = "contents", defaultValue = "") String text,
                          @RequestParam("board") String boardId,
                          @RequestParam String name,
                          @RequestParam String thread,
                          @RequestParam String subject,
                          @RequestParam("pic") ArrayList<MultipartFile> pics,
                          StandardMultipartHttpServletRequest request){ // постинг
            Board board = boardsCache.getBoard(boardId);
            if(board == null)
                return buildResponse("1", "Доски не существует");
            ArrayList<Ban> bans = boardsCache.getBansList().get(boardId); // проверяем не забанен ли клиент
            if(boardsCache.getBansList().get("global_bans") != null)
                bans.addAll(boardsCache.getBansList().get("global_bans"));
            if(bans != null){
                for(int a = 0; a < bans.size(); a++){
                    if(bans.get(a).getIP().equals(request.getRemoteAddr())){
                        Ban ban = bans.get(a);
                        return buildResponse("1", "Постинг запрещён. Бан №" + ban.getID() + ". Причина: " + (ban.isPermanent() ? "[P] " : "") + ban.getReason() + (ban.isGlobal() ? "(global)" : "/!!" + ban.getBoard() + (ban.isPermanent() ? "" : ". Истекает: " + ban.getHumanReadableExpirationDate())));
                    }
                }
            }
            if(pics.size() > 4){
                return buildResponse("1", "Слишком много файлов. Макс. кол-во: 4.");
            }
            if(pics.size() > 0){ // проверка картинок на ограничения, чтоб зря не обрабатывать
                String extension;
                for(MultipartFile file: pics){
                    if(file.isEmpty())
                        continue;
                    if((int)Math.ceil(file.getSize() / 1024 / 1024) > board.getMaxFileSize())
                        return buildResponse("1", "Файл слишком большой");
                    String[] spl = file.getOriginalFilename().split("\\.");
                    extension = spl[spl.length-1];
                    if(!boardsCache.getAllowedFileExtensions().contains(extension)){
                        return buildResponse("1", "Неподдерживаемый тип файлов (" + extension + ")");
                    }
                }
            }
            text = utfEncode(text)
                   .replace("<", "&lt;");
            ArrayList<Post> repliesTo = new ArrayList<>();
            String[] parts = text.split(">>");
            if(parts.length > 1){ // создание ссылок на цитируемые посты
                text = parts[0];
                for(int a = 1; a < parts.length; a++){
                    int finalIndex = 0;
                    while(finalIndex < parts[a].length() && "1234567890".indexOf(parts[a].charAt(finalIndex)) != -1)
                        finalIndex ++;
                    String linkToReplace = parts[a].substring(0, finalIndex);
                    repliesTo.add(board.getPost(linkToReplace)); // TODO no post found
                    board.getPost(linkToReplace).addReply(String.valueOf(board.getTotalPosts()+1L));
                    parts[a] = parts[a].replaceFirst(linkToReplace, "<a class=\"post-reply-link\" data-num=\"" + linkToReplace + "\" parent-post-num=\"" + String.valueOf(board.getTotalPosts()+1L) + "\">>>" + linkToReplace + "</a>");
                    text = text.concat(parts[a]);
                }
            }
            text = text.replace("\r\n", "<br/>");
            subject = utfEncode(subject);
            String tripcode = "";
            if(name.contains("#")){ // генерация трипкода
                String[] nameParts = name.split("#", 2);
                tripcode = nameParts[1];
                name = nameParts[0];
                tripcode = generateTrip(tripcode);
            }
            if(thread.equals("-1")) // новый тред
                thread = Long.toString(board.getTotalPosts()+1L);
            if(name.equals("")){
                name = board.getDefaultName();
            }
            ArrayList<CachedImage> files = new ArrayList<>();
            if(pics.size() > 0){ // обработка картинок, генерация фулсайза и превью, сохранение данных
                for(MultipartFile file: pics){
                    if(file.isEmpty())
                        continue;
                    String filename = Long.toString(System.currentTimeMillis()); // имя файла
                    BufferedImage img = null;
                    try{
                        img = ImageIO.read(file.getInputStream()); // фулсайз
                    }catch(IOException e){
                        boardsCache.printError("Couldn't read contents of file upload. Stack trace:");
                        e.printStackTrace();
                    }
                    int width = img.getWidth();
                    int height = img.getHeight();
                    if(img.getWidth() > 250 || img.getHeight() > 250){ // генерация превью
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
                    try{ // сохраняем в файлы
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
                        byte[] rawFullSizeData = os.toByteArray();
                        FileOutputStream stream = new FileOutputStream(new File(fullPath));
                        stream.write(rawFullSizeData); // сохранение фулсайза в файл
                        stream.close();
                        stream = new FileOutputStream(new File(thumbPath));
                        ImageIO.write(img, extension, stream); // сохранение превью
                        stream.close();
                        CachedImage fullImageMetadata = new CachedImage(); // 
                        fullImageMetadata.setName(filename + "." + extension);
                        fullImageMetadata.setMetadata("(" + Math.ceil(rawFullSizeData.length/8/1024) + "Кб, " + width + "x" + height + ")");
                        fullImageMetadata.setPath(fullPath);
                        fullImageMetadata.setThumbPath(thumbPath);
                        // saving image done
                        files.add(fullImageMetadata);
                    }catch(Exception e){
                        e.printStackTrace();
                        return buildResponse("1", e.toString());
                    }
                }
            }
            Date now = new Date();
            String dow = TrichDict.weekDays.get(now.getDay());
            String month = TrichDict.months.get(now.getMonth());
            String date = new StringBuilder(28).append(dow) // human-readable дата
            .append(" ")
            .append((now.getDate() < 10 ? "0" : "") + now.getDate())
            .append(" ")
            .append(month)
            .append(" ")
            .append((now.getYear() + 1900))
            .append(" ")
            .append((now.getHours() < 10 ? "0" : "") + now.getHours())
            .append(":")
            .append((now.getMinutes() < 10 ? "0" : "") + now.getMinutes())
            .append(":")
            .append((now.getSeconds() < 10 ? "0" : "") + now.getSeconds())
            .toString();
            trich.Thread cachedThread = board.getThread(thread);
            Post post = new Post(Long.toString(board.getTotalPosts()+1L),
                                 cachedThread, name, tripcode, date, subject,
                                 text, files, false, request.getRemoteAddr(), repliesTo,
                                 new ArrayList<>()); // обновление кэша
            boardsCache.addPost(board, post);
            return buildResponse("0", "Сообщение отправлено");
    }
    
    @RequestMapping(value = "posting", method = RequestMethod.GET)
    public View postGet(){
        return new RedirectView("/");
    }
    
    @RequestMapping(value = "config", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String config(@RequestParam("board") String id){ // получить настройки доски
        Board board = boardsCache.getBoard(id);
        if(board == null){
            return buildResponse("1", "Доски не существует");
        }
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("BoardTitle", board.getTitle());
        builder.add("BoardInfo", board.getDesc());
        builder.add("DefaultName", board.getDefaultName());
        builder.add("MaxFileSize", board.getMaxFileSize());
        return builder.build().toString();
    }
    
    @RequestMapping(value = "config", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String changeConfig(@CookieValue("mod_session_id") String modSessionID,
                               @RequestParam String title,
                               @RequestParam("edited") String editedBoard,
                               @RequestParam String id,
                               @RequestParam String brief,
                               @RequestParam("default_name") String name,
                               @RequestParam String mfs){ // отредактировать настройки доски
        if(boardsCache.checkModerator(modSessionID, 4) == null)
            return buildResponse("1", "Нет доступа");
        if(editedBoard.equals("+")){
            int mfs_;
            try{
                mfs_ = Integer.parseInt(mfs);
            }catch(NumberFormatException e){
                boardsCache.printWarning("Tried to set wrong maxFileSize for board. Resetting to 2 MBs.");
                mfs_ = 2;
            }
            Board board = new Board(id, title, brief, name, mfs, boardsCache);
            boardsCache.addBoard(board);
            board.addBanReasonsSet(boardsCache.getGeneralBanReasons());
            File boardFolder = new File(rootPath + "res/" + id);
            boardFolder.mkdirs();
            return buildResponse("0", "Доска создана");
        }
        Board board = boardsCache.getBoard(editedBoard);
        if(board == null){
            return buildResponse("1", "Доски не существует");
        }
        int mfs_;
        try{
            mfs_ = Integer.parseInt(mfs);
        }catch(NumberFormatException e){
            boardsCache.printWarning("Tried to set wrong maxFileSize for board. Resetting to 2 MBs.");
            mfs_ = 2;
        }
        if(!id.equals(board.getID())){
            boardsCache.removeBoard(board.getID());
            boardsCache.addBoard(board);
        }
        board.setId(id);
        board.setTitle(title);
        board.setDefaultName(name);
        board.setMaxFileSize(mfs_);
        board.setDesc(brief);
        boardsCache.mergeObject(board);
        return buildResponse("0", "Данные отредактированы");
    }
    
    @RequestMapping(value = "send_report", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String sendReport(@RequestParam String text,
                             @RequestParam String thread,
                             @RequestParam String board,
                             @RequestParam ArrayList<String> posts,
                             HttpServletRequest request){ // обработка жалоб
        text = utfEncode(text);
        String ip = request.getRemoteAddr();
        if(boardsCache.getBoard(board) == null)
            return buildResponse("1", "Доски не существует");
        ArrayList<String> postsList = new ArrayList<>();
        for(int a = 0; a < posts.size(); a++)
            postsList.add(posts.get(a));
        boardsCache.addReport(new Report(board, postsList, text, ip, String.valueOf(boardsCache.getReportsCounter()+1)));
        boardsCache.setReportsCounter(boardsCache.getReportsCounter()+1);
        return buildResponse("0", "Накляузничано");
    }
    
    @RequestMapping(value = "delete_reports", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String deleteReports(@CookieValue("mod_session_id") String modSessionID,
                                @RequestParam("id") ArrayList<String> raw){ // удалить жалобы
        if(boardsCache.checkModerator(modSessionID, 1) == null)
            return buildResponse("1", "Нет доступа");
        String[] ids = new String[raw.size()];
        String[] boards = new String[raw.size()];
        for(int a = 0; a < raw.size(); a++){
            String currentRawID = raw.get(a);
            ids[a] = currentRawID.split("_")[1];
            boards[a] = currentRawID.split("_")[0];
        }
        for(int a = 0; a < raw.size(); a++){
            Board board = boardsCache.getBoard(boards[a]);
            if(board == null)
                continue;
            boardsCache.removeReport(board.getReportByID(ids[a]));
        }
        return buildResponse("0", "Жалобы удалены");
    }
    
    @RequestMapping(value = "delete_posts", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String deletePosts(@CookieValue("mod_session_id") String modSessionID,
                              @RequestParam("post") ArrayList<String> postsToDelete,
                              @RequestParam("board") String boardId){ // удалить посты
        if(boardsCache.checkModerator(modSessionID, 1, boardId) == null)
            return buildResponse("1", "Нет доступа");
        Board board = boardsCache.getBoard(boardId);
        Post post;
        for(int a = 0; a < postsToDelete.size(); a++){
            boardsCache.removePost(board, postsToDelete.get(a));
        }
        return buildResponse("0", "Сообщения удалены");
    }
    
    @RequestMapping(value = "add_moder", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String addModer(@CookieValue("mod_session_id") String modSessionID,
                           @RequestParam(defaultValue = "1") String level,
                           @RequestParam(defaultValue = "") String boards,
                           @RequestParam String name){ // добавить модератора
        if(boardsCache.checkModerator(modSessionID, 4) == null)
            return buildResponse("1", "Нет доступа");
        int al;
        try{
            al = Integer.parseInt(level);
        }catch(Exception e){
            boardsCache.printWarning("Tried to add moderator with wrong access level. Resetting to 1.");
            al = 1;
        }
        if(al > 4 || al < 1){
            al = 1;
        }
        String[] boardsRaw = boards.split(",");
        ArrayList<Board> boardsToInsert = new ArrayList<>();
        for(int a = 0; a < boardsRaw.length; a++){
            if(boardsRaw[a].equals("") || boardsCache.getBoard(boardsRaw[a]) == null)
                continue;
            boardsToInsert.add(boardsCache.getBoard(boardsRaw[a].trim()));
        }
        String key = "";
        for(int a = 0; a < 5; a++){
            key = key.concat(generateTrip(String.valueOf((double)(1000000.0 * Math.random()))));
        }
        Mod moder = new Mod(key, al, boardsToInsert, name);
        boardsCache.addModerator(moder);
        return buildResponse("0", "Модератор добавлен. Ключ: " + key);
    }
    
    @RequestMapping(value = "json_on_demand", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String randomThreadJSON(@RequestParam String num,
                                   @RequestParam("board") String boardId){ // получить json треда по номеру
        Board board = boardsCache.getBoard(boardId);                       // нужно для подгрузки ответов
        if(board == null){
            return buildResponse("1", "Доски не существует");
        }
        trich.Thread thread = board.getThread(num);
        if(thread == null){
            return buildResponse("1", "Тред не найден");
        }
        return thread.getJSON();
    }
    
    @RequestMapping(value = "edit_moder", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String editModer(@CookieValue("mod_session_id") String modSessionID,
                            @RequestParam String key,
                            @RequestParam(defaultValue = "1") String level,
                            @RequestParam(defaultValue = "") String boards,
                            @RequestParam(required = false) String name){ // изменить данные модератора
        if(boardsCache.checkModerator(modSessionID, 4) == null)
            return buildResponse("1", "Нет доступа");
        if(boardsCache.getModerator(key) == null)
            return buildResponse("1", "Модератор не найден");
        int al;
        try{
            al = Integer.parseInt(level);
            if(al > 4 || al < 1)
                al = 1;
        }catch(NumberFormatException e){
            boardsCache.printWarning("Tried to save moderator with wrong access level. Resetting to 1.");
            al = 1;
        }
        String[] boardsRaw = boards.split(",");
        ArrayList<Board> boardsToInsert = new ArrayList<>();
        for(int a = 0; a < boardsRaw.length; a++){
            if(boardsRaw[a].equals("") || boardsCache.getBoard(boardsRaw[a]) == null)
                continue;
            boardsToInsert.add(boardsCache.getBoard(boardsRaw[a].trim()));
        }
        Mod mod = boardsCache.getModerator(key);
        mod.setAccessLevel(al);
        if(name != null)
            mod.setName(name);
        mod.setBoards(boardsToInsert);
        boardsCache.mergeObject(mod);
        return buildResponse("0", "Данные модератора отредактированы");
    }
    
    @RequestMapping(value = "delete_moder", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String deleteModer(@CookieValue("mod_session_id") String modSessionID,
                              @RequestParam("moder") ArrayList<String> modersToRemove){ // удалить модератора
        if(boardsCache.checkModerator(modSessionID, 4) == null)
            return buildResponse("1", "Нет доступа");
        for(int a = 0; a < modersToRemove.size(); a++){
            boardsCache.removeModerator(modersToRemove.get(a));
        }
        return buildResponse("0", "Модераторы удалены");
    }
    
    @RequestMapping(value = "mods_json", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getMods(@CookieValue("mod_session_id") String modSessionID){ // получить данные о модераторах в json
        if(boardsCache.checkModerator(modSessionID, 4) == null)
            return buildResponse("1", "Нет доступа");
        JsonArrayBuilder builder = Json.createArrayBuilder();
        ArrayList<Mod> mods = new ArrayList<>(boardsCache.getModsList().values());
        for(int a = 0; a < mods.size(); a++){
            Mod mod = mods.get(a);
            JsonArrayBuilder boardsBuilder = Json.createArrayBuilder();
            mod.getBoards().stream().forEach((Board board) -> {boardsBuilder.add(board.getID());});
            builder.add(Json.createObjectBuilder()
                .add("key", mod.getID())
                .add("level", mod.getAccessLevel())
                .add("boards", boardsBuilder.build())
                .add("name", mod.getName())
                .build());
        }
        return builder.build().toString();
    }
    
    @RequestMapping(value = "login", method = RequestMethod.POST)
    public View login(@RequestParam(required = false) String key, HttpServletResponse response){ // вход в систему для модераторов
        if(key == null){
            return new RedirectView("/moder_login_form.html");
        }
        if(boardsCache.getModerator(key) == null)
            return new RedirectView("/moder_login_form.html"); // TODO wrong ID
        String sessionId = UUID.randomUUID().toString();
        while(boardsCache.getActiveModerSessions().containsKey(sessionId))
            sessionId = UUID.randomUUID().toString();
        Cookie cookie = new Cookie("mod_session_id", sessionId.toString());
        cookie.setMaxAge(3600);
        cookie.setPath("/");
        response.addCookie(cookie);
        boardsCache.getActiveModerSessions().put(sessionId, boardsCache.getModerator(key));
        String sessionLambda = new String(sessionId);
        boardsCache.scheduleTask(() -> {boardsCache.getActiveModerSessions().remove(sessionLambda);}, 3600, java.util.concurrent.TimeUnit.SECONDS);
        return new RedirectView("/takaba/mod_panel");
    }
    
    @RequestMapping(value = "ban", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String addBan(@CookieValue("mod_session_id") String modSessionID,
                         @RequestParam String board,
                         @RequestParam String post,
                         @RequestParam String reason,
                         @RequestParam(required = false) String year,
                         @RequestParam(required = false) String month,
                         @RequestParam(required = false) String day,
                         @RequestParam(required = false) String hour,
                         @RequestParam(required = false) String minute,
                         @RequestParam(required = false) String permanent,
                         @RequestParam(required = false) String global){ // забанить
        Mod moderator = boardsCache.checkModerator(modSessionID, 2, board);
        if(moderator == null)
            return buildResponse("1", "Нет доступа");
        if(permanent == null && (year == null || month == null || day == null || hour == null || minute == null))
            return buildResponse("1", "Отсутствуют необходимые параметры");
        Board requestedBoard = boardsCache.getBoard(board);
        if(requestedBoard == null)
            return buildResponse("1", "Доски не существует");
        String IP;
        try{
            IP = requestedBoard.getPost(post).getIP();
        }catch(NullPointerException e){
            return buildResponse("1", "Пост не существует");
        }
        Date expires = null;
        if(permanent == null){
            expires = new Date(
            Integer.parseInt(year) - 1900,
            Integer.parseInt(month) - 1,
            Integer.parseInt(day),
            Integer.parseInt(hour),
            Integer.parseInt(minute));
        }
        boardsCache.addBan(new Ban(permanent == null ? expires.getTime() : 0L, IP, requestedBoard.getBanReasons().get(Integer.parseInt(reason)), global == null ? board : "global_bans", permanent == null ? false : true, global == null ? false : true, boardsCache.getBansCounter()+1L));
        boardsCache.setBansCounter(boardsCache.getBansCounter()+1L);
        return buildResponse("0", "Выдан бан №" + boardsCache.getBansCounter());
    }
    
    @RequestMapping(value = "unban", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String unban(@CookieValue("mod_session_id") String modSessionID,
                        @RequestParam String id){ // разбанить
        Ban ban = boardsCache.getBan(id);
        if(ban == null){
            return buildResponse("1", "Бан с таким ID не найден");
        }
        Mod moderator = boardsCache.checkModerator(modSessionID, 3, ban.getBoard());
        if(moderator == null)
            return buildResponse("1", "Нет доступа");
        boardsCache.removeBan(ban);
        return buildResponse("0", "Бан удалён");
    }
    
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseBody
    public String handleMissingParameters(){ // хэндлер для отсутствующих параметров запроса (любого)
        return buildResponse("1", "Отсутствуют необходимые параметры");
    }
    
    @ExceptionHandler(MissingRequestCookieException.class)
    @ResponseBody
    public String handleMissingModeratorSessionCookie(){ // хэндлер для отсутствующих Cookie (для модераторов)
        return buildResponse("1", "Нет доступа");
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
    
    private String buildResponse(String code, String msg){
        return Json.createObjectBuilder().add("Status", code)
                                         .add("Message", msg)
                                         .build().toString();
    }
    
    private String utfEncode(String str){
        return new String(str.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
    }
}