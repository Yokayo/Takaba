package rootContextBeans;

import java.util.*;
import java.io.*;
import javax.json.*;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Scope;
import org.springframework.web.multipart.MultipartFile;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Cookie;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.*;
import trich.*;


@Component("boards_cache")
@Scope("singleton")
public class BoardsCache{ // центральная часть борды - in-memory storage
                          // хранит все доски, посты на них, IP, конфигурацию досок, модераторов, баны и т.д.
    File boards_folder = new File(System.getProperty("catalina.base").replace("\\", "/") + "/webapps/ROOT/res");
    private String root_path = System.getProperty("catalina.base").replace("\\", "/") + "/webapps/ROOT/";
    public HashMap<String, Board> boards_list = new HashMap<>(100); // карта досок
    public Map<String, Mod> mods_list = new HashMap<>(); // список модераторов
    private ArrayList<Board> boards_catalog = new ArrayList<>(); // каталог досок, нужен для вывода списком в админ-панели
    public HashMap<String, Mod> active_moder_sessions = new HashMap<>(); // карта активных сессий модераторов
    public HashMap<String, ArrayList<Ban>> bans_list = new HashMap<>(); // список банов
    public HashMap<String, ScheduledFuture> scheduled_unbans = new HashMap<>(); // список разбанов, нужен для ручного разбана
    private ArrayList<Ban> bans_catalog = new ArrayList<>(); // каталог банов, для вывода списком на вкладке "Баны" в модерке
    private int reports_counter = 0; // счётчик жалоб для присвоения ID
    private long bans_counter = 0; // счётчик банов для ID
    public boolean needsBansFlushing, needsImagesFlushing = false; // есть ли изменения в списке банов или картинок соответственно, для авто-флашинга
    public ArrayList<String> general_ban_reasons = new ArrayList<>(); // список причин бана
    public HashMap<String, CachedImage> images_cache = new HashMap<>(); // кэш загруженных картинок, периодически выгружается авто-флашингом
    public ScheduledThreadPoolExecutor thread_pool = new ScheduledThreadPoolExecutor(21); // потоки для авто-флашинга
    private ScheduledThreadPoolExecutor ban_tasks = new ScheduledThreadPoolExecutor(10); // потоки для разбанов
    
    @PostConstruct
    public void buildCache(){try{ // инициализация кэша
            thread_pool.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
            ban_tasks.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
            thread_pool.scheduleWithFixedDelay(() -> { // авто-флашинг
                ArrayList<Board> boards = getBoards();
                for(int a = 0; a < boards.size(); a++){ // проход по доскам
                    Board board = boards.get(a);
                    if(board.needsCatalogFlushing){ // флашинг каталога тредов
                        try{
                            board.needsCatalogFlushing = false;
                            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(boards_folder + "/" + boards.get(a).getID() + "//catalog.json"), false), "UTF-8"));
                            StringWriter string_writer = new StringWriter();
                            JsonWriter json_writer = Json.createWriter(string_writer);
                            JsonArrayBuilder builder = Json.createArrayBuilder();
                            ArrayList<trich.Thread> threads = board.getThreads();
                            for(int b = 0; b < threads.size(); b++){
                                builder.add(threads.get(b).getPost(0).getPostnum());
                            }
                            json_writer.write(builder.build());
                            json_writer.close();
                            writer.write(string_writer.toString());
                            writer.close();
                        }catch(Exception e){e.printStackTrace(System.out);}
                    }
                    if(board.needsSettingsFlushing){ // флашинг настроек доски
                        try{
                            board.needsSettingsFlushing = false;
                            File config_file = new File(boards_folder + "/" + boards.get(a).getID() + "/board_config.json");
                            if(!config_file.exists())
                                config_file.createNewFile();
                            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(config_file, false), "UTF-8"));
                            JsonWriter json_writer = Json.createWriter(writer);
                            JsonObjectBuilder builder = Json.createObjectBuilder();
                            builder.add("BoardTitle", board.getTitle());
                            builder.add("BoardInfo", board.getDesc());
                            builder.add("DefaultName", board.getDefaultName());
                            builder.add("MaxFileSize", board.max_file_size);
                            builder.add("DelayedFlushing", board.delayedFlushingEnabled ? "true" : "false");
                            json_writer.write(builder.build());
                            writer.close();
                        }catch(Exception e){e.printStackTrace(System.out);}
                    }
                    ArrayList<trich.Thread> board_threads = boards.get(a).getThreads();
                    for(int b = 0; b < board_threads.size(); b++){ // флашинг постов в тредах
                        try{
                            trich.Thread thread = board_threads.get(b);
                            if(!thread.needsFlushing)
                                continue;
                            thread.needsFlushing = false;
                            flushThread(thread);
                            }catch(Exception e){e.printStackTrace(System.out); continue;}
                    }
                }
                if(needsBansFlushing){ // флашинг банов
                    try{
                        JsonArrayBuilder builder = Json.createArrayBuilder();
                        ArrayList<Ban> bans = getBansCatalog();
                        for(int a = 0; a < bans.size(); a++){
                            Ban ban = bans.get(a);
                            builder.add(Json.createObjectBuilder()
                            .add("ip", ban.getIP())
                            .add("reason", ban.getReason())
                            .add("board", ban.getBoard())
                            .add("expiration", String.valueOf(ban.getExpirationDate()))
                            .add("id", String.valueOf(ban.getID()))
                            .add("permanent", ban.isPermanent() ? "true" : "false")
                            .add("global", ban.isGlobal() ? "true" : "false")
                            .build());
                        }
                        JsonWriter writer = Json.createWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(root_path + "WEB-INF/bans.json"), false), "UTF-8")));
                        writer.writeArray(builder.build());
                        writer.close();
                        needsBansFlushing = false;
                    }catch(Exception e){e.printStackTrace(System.out);}
                }
                if(needsImagesFlushing){ // флашинг картинок
                    try{
                        CachedImage img;
                        Iterator i = images_cache.keySet().iterator();
                        while(i.hasNext()){
                            String path = (String) i.next();
                            img = images_cache.get(path);
                            File file = new File(path);
                            file.getParentFile().mkdirs();
                            file.createNewFile();
                            FileOutputStream output = new FileOutputStream(file);
                            output.write(img.data);
                            output.close();
                            i.remove();
                        }
                        needsImagesFlushing = false;
                    }catch(Exception e){e.printStackTrace(System.out);}
                }
            }, 30L, 30L, java.util.concurrent.TimeUnit.SECONDS);
        }catch(Exception e){e.printStackTrace(System.out);}
        ban_tasks.allowCoreThreadTimeOut(true);
        ban_tasks.setKeepAliveTime(2L, TimeUnit.HOURS);
        String[] boards = boards_folder.list( // загрузка досок
            (File folder, String name) -> {return new File(folder, name).isDirectory();}
        );
        JsonReader reader;
        BufferedReader thread_reader;
        String thread_json;
        
        try{ // загрузка причин бана
            File ban_reasons_file = new File(System.getProperty("catalina.base") + "/webapps/ROOT/WEB-INF/ban_reasons_general.txt");
            if(ban_reasons_file.exists()){
                BufferedReader reasons_reader = new BufferedReader(new InputStreamReader(new FileInputStream(ban_reasons_file), "UTF-8"));
                String result = "none";
                while(result != null && !result.equals("")){
                    result = reasons_reader.readLine();
                    general_ban_reasons.add(result);
                }
                reasons_reader.close();
            }else{
                general_ban_reasons.add("Качество контента");
                general_ban_reasons.add("Вайп");
                general_ban_reasons.add("Неймфаг, злоупотребление аватаркой/трипкодом");
                general_ban_reasons.add("Призывы к нанесению вреда Тричу");
            }
        }catch(Exception e){
            e.printStackTrace(System.out);
            general_ban_reasons.add("Качество контента");
            general_ban_reasons.add("Вайп");
            general_ban_reasons.add("Неймфаг, злоупотребление аватаркой/трипкодом");
            general_ban_reasons.add("Призывы к нанесению вреда Тричу");
        }
        
        for(int boards_counter = 0; boards_counter < boards.length; boards_counter++){ // загрузка каждой доски по порядку
            try{
                File board_config;
                Board board;
                board_config = new File(boards_folder + "/" + boards[boards_counter] + "/board_config.json");
                if(!board_config.exists()) // доска без настроек
                    continue;
                reader = Json.createReader(new BufferedReader(new InputStreamReader(new FileInputStream(board_config), "UTF-8")));
                JsonObject config = reader.readObject();
                reader.close();
                try{
                    board = new Board(boards[boards_counter], config.getString("BoardTitle"), config.getString("BoardInfo"), config.getString("DefaultName"), config.getString("MaxFileSize", "2"), this);
                }catch(Exception e){ // неверно отформатировано
                    continue;
                }
                try{
                    board.delayedFlushingEnabled = Boolean.parseBoolean(config.getString("DelayedFlushing"));
                }catch(Exception e){}
                File board_catalog = new File(boards_folder + "/" + boards[boards_counter] + "/catalog.json");
                board.addBanReasonsSet(general_ban_reasons);
                if(board_catalog.exists()){
                    reader = Json.createReader(new BufferedReader(new FileReader(board_catalog)));
                    ArrayList<JsonString> catalog = new ArrayList<>(reader.readArray().getValuesAs(JsonString.class));
                    reader.close();
                    long biggestPostnum = 0;
                    for(int threads_counter = 0; threads_counter < catalog.size(); threads_counter++){
                        trich.Thread thread;
                        JsonObject oppost, post;
                        String thread_num = catalog.get(threads_counter).getString();
                        thread_reader = new BufferedReader(new InputStreamReader(new FileInputStream(boards_folder + "//" + boards[boards_counter] + "//" + thread_num + ".json"), "UTF-8"));
                        thread_json = thread_reader.readLine();
                        thread_reader.close();
                        reader = Json.createReader(new BufferedReader(new StringReader(thread_json)));
                        ArrayList<JsonObject> posts = new ArrayList<>(reader.readObject().getJsonArray("posts").getValuesAs(JsonObject.class));
                        reader.close();
                        reader = Json.createReader(new BufferedReader(new FileReader(boards_folder + "/" + boards[boards_counter] + "/" + thread_num + "_ips.json")));
                        ArrayList<JsonString> thread_ips = new ArrayList<>(reader.readArray().getValuesAs(JsonString.class));
                        reader.close();
                        oppost = posts.get(0);
                        ArrayList<JsonString> repliesTo_raw = new ArrayList<>(oppost.getJsonArray("replies_to").getValuesAs(JsonString.class));
                        ArrayList<String> repliesTo_ready = new ArrayList<>();
                        for(int a = 0; a < repliesTo_raw.size(); a++)
                            repliesTo_ready.add(repliesTo_raw.get(a).getString());
                        ArrayList<JsonString> repliedBy_raw = new ArrayList<>(oppost.getJsonArray("replied_by").getValuesAs(JsonString.class));
                        ArrayList<String> repliedBy_ready = new ArrayList<>();
                        for(int a = 0; a < repliedBy_raw.size(); a++)
                            repliedBy_ready.add(repliedBy_raw.get(a).getString());
                        ArrayList<JsonObject> files_list = new ArrayList<>(oppost.getJsonArray("pics").getValuesAs(JsonObject.class));
                        thread = new trich.Thread(new Post(
                        oppost.getString("postnum"),
                        oppost.getString("parent"),
                        Integer.parseInt(oppost.getString("nit")),
                        oppost.getString("name"),
                        oppost.getString("trip"),
                        oppost.getString("date"),
                        oppost.getString("subject"),
                        oppost.getString("message"),
                        files_list,
                        true,
                        thread_ips.get(0).getString(),
                        repliesTo_ready,
                        repliedBy_ready), boards[boards_counter]);
                        if(Long.parseLong(oppost.getString("postnum")) > biggestPostnum)
                            biggestPostnum = Long.parseLong(oppost.getString("postnum"));
                        thread.needsFlushing = false;
                        for(int posts_counter = 1; posts_counter < posts.size(); posts_counter++){
                            post = posts.get(posts_counter);
                            files_list = new ArrayList<>(post.getJsonArray("pics").getValuesAs(JsonObject.class));
                            repliesTo_raw = new ArrayList<>(post.getJsonArray("replies_to").getValuesAs(JsonString.class));
                            repliesTo_ready = new ArrayList<>();
                            for(int a = 0; a < repliesTo_raw.size(); a++)
                                repliesTo_ready.add(repliesTo_raw.get(a).getString());
                            repliedBy_raw = new ArrayList<>(post.getJsonArray("replied_by").getValuesAs(JsonString.class));
                            repliedBy_ready = new ArrayList<>();
                            for(int a = 0; a < repliedBy_raw.size(); a++)
                                repliedBy_ready.add(repliedBy_raw.get(a).getString());
                            thread.addPost(
                            post.getString("postnum"),
                            post.getString("parent"),
                            Integer.parseInt(post.getString("nit")),
                            post.getString("name"),
                            post.getString("trip"),
                            post.getString("date"),
                            post.getString("subject"),
                            post.getString("message"),
                            files_list,
                            false,
                            thread_ips.get(posts_counter).getString(),
                            repliesTo_ready,
                            repliedBy_ready
                            );
                            if(Long.parseLong(post.getString("postnum")) > biggestPostnum)
                                biggestPostnum = Long.parseLong(post.getString("postnum"));
                        }
                        board.setTotalPosts(biggestPostnum);
                        board.addThread(thread, false);
                        }
                }
                File reports_catalog = new File(boards_folder + "/" + boards[boards_counter] + "/reports.json");
                if(reports_catalog.length() != 0){
                    reader = Json.createReader(new BufferedReader(new InputStreamReader(new FileInputStream(reports_catalog), "UTF-8")));
                    JsonArray reports_list = reader.readArray();
                    reader.close();
                    System.out.println("Found " + reports_list.size() + " reports.");
                    for(int a = 0; a < reports_list.size(); a++){
                        JsonObject report = reports_list.getJsonObject(a);
                        ArrayList<JsonString> report_posts_raw = new ArrayList<>(report.getJsonArray("posts").getValuesAs(JsonString.class));
                        ArrayList<String> report_posts = new ArrayList<>();
                        for(int b = 0; b < report_posts_raw.size(); b++)
                            report_posts.add(report_posts_raw.get(b).getString());
                        board.addReport(new Report(
                        boards[boards_counter],
                        report_posts,
                        report.getString("text"),
                        report.getString("ip"),
                        report.getString("id")));
                        reports_counter = Integer.parseInt(report.getString("id")) + 1;
                    }
                }
                boards_list.put(boards[boards_counter], board);
                boards_catalog.add(board);
            }catch(Exception e){
            e.printStackTrace(System.out);
            continue;
        }
        }
        try{ // загрузка модераторов
        File mods_file = new File(System.getProperty("catalina.base") + "/webapps/ROOT/WEB-INF/mods.json");
        reader = Json.createReader(new BufferedReader(new InputStreamReader(new FileInputStream(mods_file), "UTF-8")));
        ArrayList<JsonObject> mods = new ArrayList<>(reader.readArray().getValuesAs(JsonObject.class));
        reader.close();
        for(int a = 0; a < mods.size(); a++){
            JsonObject mod = mods.get(a);
            ArrayList<JsonString> boards_raw = new ArrayList<>(mod.getJsonArray("boards").getValuesAs(JsonString.class));
            String[] mod_boards = new String[boards_raw.size()];
            for(int b = 0; b < mod_boards.length; b++){
                mod_boards[b] = boards_raw.get(b).getString();
            }
            mods_list.put(mod.getString("key"), new Mod(mod.getString("key"), mod.getString("level"), mod_boards, mod.getString("name")));
        }
        }catch(Exception e){
            e.printStackTrace(System.out);
            return;
        }
        try{ // загрузка банов
            File bans_file = new File(System.getProperty("catalina.base") + "/webapps/ROOT/WEB-INF/bans.json");
            if(bans_file.exists()){
                reader = Json.createReader(new BufferedReader(new InputStreamReader(new FileInputStream(bans_file), "UTF-8")));
                ArrayList<JsonObject> bans = new ArrayList<>(reader.readArray().getValuesAs(JsonObject.class));
                reader.close();
                for(int a = 0; a < bans.size(); a++){
                    JsonObject ban = bans.get(a);
                    Ban new_ban = addBan(Long.parseLong(ban.getString("expiration")),
                    ban.getString("ip"),
                    ban.getString("reason"),
                    ban.getString("board"),
                    ban.getString("permanent").equals("false") ? false : true,
                    ban.getString("global").equals("false") ? false : true,
                    Long.parseLong(ban.getString("id")));
                    bans_counter = Long.parseLong(ban.getString("id"));
                    String lambda_board = new_ban.getBoard();
                    String lambda_id = String.valueOf(new_ban.getID());
                    if(!new_ban.isPermanent()){
                        scheduled_unbans.put(String.valueOf(new_ban.getID()), ban_tasks.schedule(() ->{
                            bans_list.get(lambda_board).remove(new_ban); bans_catalog.remove(new_ban); scheduled_unbans.remove(String.valueOf(lambda_id));
                        }, new_ban.getExpirationDate() - new Date().getTime(), TimeUnit.MILLISECONDS));
                    }
                }
            }
        }catch(Exception e){
            e.printStackTrace(System.out);
        }
    }
    
    public void flushThread(trich.Thread thread) throws IOException{
        File thread_file = new File(root_path + "res/" + thread.getBoard() + "/" + thread.getPost(0).getPostnum() + ".json");
        if(!thread_file.exists())
            thread_file.createNewFile();
        JsonWriter writer = Json.createWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(thread_file, false), "UTF-8")));
        JsonArrayBuilder thread_builder = Json.createArrayBuilder();
        JsonArrayBuilder ips_builder = Json.createArrayBuilder();
        for(int c = 0; c < thread.getPosts().size(); c++){
            Post post = thread.getPost(c);
            JsonArrayBuilder pics_builder = Json.createArrayBuilder();
            for(int d = 0; d < post.getPics().size(); d++)
                pics_builder.add(post.getPics().get(d));
            JsonArrayBuilder repliesTo_builder = Json.createArrayBuilder();
            for(int d = 0; d < post.getRepliedPosts().size(); d++)
                repliesTo_builder.add(post.getRepliedPosts().get(d));
            JsonArrayBuilder repliedBy_builder = Json.createArrayBuilder();
            for(int d = 0; d < post.getReplies().size(); d++)
                repliedBy_builder.add(post.getReplies().get(d));
            thread_builder.add(Json.createObjectBuilder()
            .add("postnum", post.getPostnum())
            .add("nit", String.valueOf(post.getNumInThread()))
            .add("parent", post.getThread())
            .add("name", post.getName())
            .add("subject", post.getSubject())
            .add("date", post.getDate())
            .add("trip", post.getTripcode())
            .add("message", post.getMessage())
            .add("pics", pics_builder.build())
            .add("oppost", post.isOP() ? "true" : "false")
            .add("replies_to", repliesTo_builder.build())
            .add("replied_by", repliedBy_builder.build())
            .build());
            ips_builder.add(post.getIP());
        }
        writer.write(Json.createObjectBuilder().add("posts", thread_builder.build()).build());
        writer.close();
        File ips_catalog = new File(root_path + "res/" + thread.getBoard() + "/" + thread.getPost(0).getPostnum() + "_ips.json");
        if(!ips_catalog.exists()){
            ips_catalog.createNewFile();
        }
        writer = Json.createWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(ips_catalog, false), "UTF-8")));
        writer.write(ips_builder.build());
        writer.close();
    }
    
    public void addReport(Report report){
        getBoard(report.getBoard()).addReport(report);
    }
    
    public void removeReport(Report report){
        getBoard(report.getBoard()).removeReport(report);
    }
    
    public Ban addBan(long date, String ip, String reason, String board, boolean permanent, boolean global, long id){
        if(global){
            if(bans_list.get("global_bans") == null)
                bans_list.put("global_bans", new ArrayList<>());
            board = "global_bans";
        }
        if(bans_list.get(board) == null)
            bans_list.put(board, new ArrayList<>());
        Ban ban = new Ban(date, ip, reason, board, permanent, global, bans_counter+1L);
        bans_list.get(board).add(ban);
        bans_catalog.add(ban);
        try{String lambda_board = board;
        if(!ban.isPermanent()){
            scheduled_unbans.put(String.valueOf(ban.getID()), ban_tasks.schedule(() ->{removeBan(ban);}, ban.getExpirationDate() - System.currentTimeMillis(), TimeUnit.MILLISECONDS));
        }
        }catch(Exception e){e.printStackTrace(System.out);} // если вдруг почему-то не получилось запланировать разбан
        needsBansFlushing = true;
        return ban;
    }
    
    public Ban getBanByID(String id){
        for(int a = 0; a < bans_catalog.size(); a++){
            if(String.valueOf(bans_catalog.get(a).getID()).equals(id))
                return bans_catalog.get(a);
        }
        return null;
    }
    
    public void removeBan(Ban ban){
        bans_list.get(ban.getBoard()).remove(ban);
        bans_catalog.remove(ban);
        scheduled_unbans.remove(String.valueOf(ban.getID()));
        needsBansFlushing = true;
        System.out.println(bans_catalog.contains(ban));
    }
    
    public void addModerator(Mod mod){
        try{
        mods_list.put(mod.getID(), mod);
        }catch(Exception e){e.printStackTrace(System.out);}
    }
    
    public void removeModerator(String ID){
        mods_list.remove(ID);
    }
    
    public Board getBoard(String board_id){
        return boards_list.get(board_id);
    }
    
    public int getReportsCounter(){
        return reports_counter;
    }
    
    public long getBansCounter(){
        return bans_counter;
    }
    
    public void setBansCounter(long counter){
        bans_counter = counter;
    }
    
    public ArrayList<Board> getBoards(){
        return boards_catalog;
    }
    
    public ArrayList<Ban> getBansCatalog(){
        return bans_catalog;
    }
    
    public Mod getModByID(String id){
        return mods_list.get(id);
    }
    
    public HashMap<String, ArrayList<Ban>> getBansList(){
        return bans_list;
    }
    
    public void setReportsCounter(int counter){
        reports_counter = counter;
    }
    
    public String checkModerator(HttpServletRequest request){ // проверяет по реквесту, является ли клиент модером
        Cookie[] cookies = request.getCookies();              // нужно для доступа к модераторским функциям
        boolean isMod = false;
        String modID = "";
        if(cookies == null)
            return null;
            for(int a = 0; a < cookies.length; a++){
                if(cookies[a].getName().equals("mod_session_id") && active_moder_sessions.containsKey(cookies[a].getValue())){
                    modID = cookies[a].getValue();
                    isMod = true;
                    break;
                }
            }
        if(isMod)
            return modID;
        else
            return null;
    }
    
    @PreDestroy
    public void finalizer(){
        thread_pool.shutdown();
        ban_tasks.shutdown();
    }
    
}