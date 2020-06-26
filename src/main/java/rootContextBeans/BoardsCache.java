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


@Component("boardsCache")
@Scope("singleton")
public class BoardsCache{ // центральная часть борды - in-memory storage
                          // хранит все доски, посты на них, IP, конфигурацию досок, модераторов, баны и т.д.
    File boardsFolder = new File(System.getProperty("catalina.base").replace("\\", "/") + "/webapps/ROOT/res");
    private String rootPath = System.getProperty("catalina.base").replace("\\", "/") + "/webapps/ROOT/";
    public HashMap<String, Board> boardsList = new HashMap<>(100); // карта досок
    public Map<String, Mod> modsList = new HashMap<>(); // список модераторов
    private ArrayList<Board> boardsCatalog = new ArrayList<>(); // каталог досок, нужен для вывода списком в админ-панели
    public HashMap<String, Mod> activeModerSessions = new HashMap<>(); // карта активных сессий модераторов
    public HashMap<String, ArrayList<Ban>> bansList = new HashMap<>(); // список банов
    public HashMap<String, ScheduledFuture> scheduledUnbans = new HashMap<>(); // список разбанов, нужен для ручного разбана
    private ArrayList<Ban> bansCatalog = new ArrayList<>(); // каталог банов, для вывода списком на вкладке "Баны" в модерке
    private int reportsCounter = 0; // счётчик жалоб для присвоения ID
    private long bansCounter = 0; // счётчик банов для ID
    public boolean needsBansFlushing, needsImagesFlushing = false; // есть ли изменения в списке банов или картинок соответственно, для авто-флашинга
    public ArrayList<String> generalBanReasons = new ArrayList<>(); // список причин бана
    public HashMap<String, CachedImage> imagesCache = new HashMap<>(); // кэш загруженных картинок, периодически выгружается авто-флашингом
    public ScheduledThreadPoolExecutor threadPool = new ScheduledThreadPoolExecutor(21); // потоки для авто-флашинга
    private ScheduledThreadPoolExecutor banTasks = new ScheduledThreadPoolExecutor(10); // потоки для разбанов
    
    @PostConstruct
    public void buildCache(){try{ // инициализация кэша
            threadPool.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
            banTasks.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
            threadPool.scheduleWithFixedDelay(() -> { // авто-флашинг
                ArrayList<Board> boards = getBoards();
                for(int a = 0; a < boards.size(); a++){ // проход по доскам
                    Board board = boards.get(a);
                    if(board.needsCatalogFlushing){ // флашинг каталога тредов
                        try{
                            board.needsCatalogFlushing = false;
                            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(boardsFolder + "/" + boards.get(a).getID() + "//catalog.json"), false), "UTF-8"));
                            StringWriter stringWriter = new StringWriter();
                            JsonWriter jsonWriter = Json.createWriter(stringWriter);
                            JsonArrayBuilder builder = Json.createArrayBuilder();
                            ArrayList<trich.Thread> threads = board.getThreads();
                            for(int b = 0; b < threads.size(); b++){
                                builder.add(threads.get(b).getPost(0).getPostnum());
                            }
                            jsonWriter.write(builder.build());
                            jsonWriter.close();
                            writer.write(stringWriter.toString());
                            writer.close();
                        }catch(Exception e){e.printStackTrace(System.out);}
                    }
                    if(board.needsSettingsFlushing){ // флашинг настроек доски
                        try{
                            board.needsSettingsFlushing = false;
                            File configFile = new File(boardsFolder + "/" + boards.get(a).getID() + "/board_config.json");
                            if(!configFile.exists())
                                configFile.createNewFile();
                            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(configFile, false), "UTF-8"));
                            JsonWriter jsonWriter = Json.createWriter(writer);
                            JsonObjectBuilder builder = Json.createObjectBuilder();
                            builder.add("BoardTitle", board.getTitle());
                            builder.add("BoardInfo", board.getDesc());
                            builder.add("DefaultName", board.getDefaultName());
                            builder.add("MaxFileSize", board.maxFileSize);
                            builder.add("DelayedFlushing", board.delayedFlushingEnabled ? "true" : "false");
                            jsonWriter.write(builder.build());
                            writer.close();
                        }catch(Exception e){e.printStackTrace(System.out);}
                    }
                    ArrayList<trich.Thread> boardThreads = boards.get(a).getThreads();
                    for(int b = 0; b < boardThreads.size(); b++){ // флашинг постов в тредах
                        try{
                            trich.Thread thread = boardThreads.get(b);
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
                        JsonWriter writer = Json.createWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(rootPath + "WEB-INF/bans.json"), false), "UTF-8")));
                        writer.writeArray(builder.build());
                        writer.close();
                        needsBansFlushing = false;
                    }catch(Exception e){e.printStackTrace(System.out);}
                }
                if(needsImagesFlushing){ // флашинг картинок
                    try{
                        CachedImage img;
                        Iterator i = imagesCache.keySet().iterator();
                        while(i.hasNext()){
                            String path = (String) i.next();
                            img = imagesCache.get(path);
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
        banTasks.allowCoreThreadTimeOut(true);
        banTasks.setKeepAliveTime(2L, TimeUnit.HOURS);
        String[] boards = boardsFolder.list( // загрузка досок
            (File folder, String name) -> {return new File(folder, name).isDirectory();}
        );
        JsonReader reader;
        BufferedReader threadReader;
        String threadJson;
        
        try(File banReasonsFile = new File(System.getProperty("catalina.base") + "/webapps/ROOT/WEB-INF/ban_reasons_general.txt")){ // загрузка причин бана
            if(banReasonsFile.exists()){
                BufferedReader reasonsReader = new BufferedReader(new InputStreamReader(new FileInputStream(banReasonsFile), "UTF-8"));
                String result = "none";
                while(result != null && !result.equals("")){
                    result = reasonsReader.readLine();
                    generalBanReasons.add(result);
                }
                reasonsReader.close();
            }
        }catch(Exception e){
            e.printStackTrace(System.out);
        }finally{
            if(generalBanReasons.isEmpty()){
                generalBanReasons.add("Качество контента");
                generalBanReasons.add("Вайп");
                generalBanReasons.add("Неймфаг, злоупотребление аватаркой/трипкодом");
                generalBanReasons.add("Призывы к нанесению вреда Тричу");
            }
        }
        
        for(int boardsCounter = 0; boardsCounter < boards.length; boardsCounter++){ // загрузка каждой доски по порядку
            try{
                File boardConfig;
                Board board;
                board_config = new File(boardsFolder + "/" + boards[boardsCounter] + "/board_config.json");
                if(!boardConfig.exists()) // доска без настроек
                    continue;
                reader = Json.createReader(new BufferedReader(new InputStreamReader(new FileInputStream(boardConfig), "UTF-8")));
                JsonObject config = reader.readObject();
                reader.close();
                try{
                    board = new Board(boards[boardsCounter], config.getString("BoardTitle"), config.getString("BoardInfo"), config.getString("DefaultName"), config.getString("MaxFileSize", "2"), this);
                }catch(Exception e){ // неверно отформатировано
                    continue;
                }
                try{
                    board.delayedFlushingEnabled = Boolean.parseBoolean(config.getString("DelayedFlushing"));
                }catch(Exception e){}
                File boardCatalog = new File(boardsFolder + "/" + boards[boardsCounter] + "/catalog.json");
                board.addBanReasonsSet(generalBanReasons);
                if(boardCatalog.exists()){
                    reader = Json.createReader(new BufferedReader(new FileReader(boardCatalog)));
                    ArrayList<JsonString> catalog = new ArrayList<>(reader.readArray().getValuesAs(JsonString.class));
                    reader.close();
                    long biggestPostnum = 0;
                    for(int threadsCounter = 0; threadsCounter < catalog.size(); threadsCounter++){
                        trich.Thread thread;
                        JsonObject oppost, post;
                        String threadNum = catalog.get(threadsCounter).getString();
                        threadReader = new BufferedReader(new InputStreamReader(new FileInputStream(boardsFolder + "//" + boards[boardsCounter] + "//" + threadNum + ".json"), "UTF-8"));
                        threadJson = threadReader.readLine();
                        threadReader.close();
                        reader = Json.createReader(new BufferedReader(new StringReader(threadJson)));
                        ArrayList<JsonObject> posts = new ArrayList<>(reader.readObject().getJsonArray("posts").getValuesAs(JsonObject.class));
                        reader.close();
                        reader = Json.createReader(new BufferedReader(new FileReader(boardsFolder + "/" + boards[boardsCounter] + "/" + threadNum + "_ips.json")));
                        ArrayList<JsonString> threadIps = new ArrayList<>(reader.readArray().getValuesAs(JsonString.class));
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
                        ArrayList<JsonObject> filesList = new ArrayList<>(oppost.getJsonArray("pics").getValuesAs(JsonObject.class));
                        thread = new trich.Thread(new Post(
                        oppost.getString("postnum"),
                        oppost.getString("parent"),
                        Integer.parseInt(oppost.getString("nit")),
                        oppost.getString("name"),
                        oppost.getString("trip"),
                        oppost.getString("date"),
                        oppost.getString("subject"),
                        oppost.getString("message"),
                        filesList,
                        true,
                        threadIps.get(0).getString(),
                        repliesTo_ready,
                        repliedBy_ready), boards[boardsCounter]);
                        if(Long.parseLong(oppost.getString("postnum")) > biggestPostnum)
                            biggestPostnum = Long.parseLong(oppost.getString("postnum"));
                        thread.needsFlushing = false;
                        for(int postsCounter = 1; postsCounter < posts.size(); postsCounter++){
                            post = posts.get(postsCounter);
                            filesList = new ArrayList<>(post.getJsonArray("pics").getValuesAs(JsonObject.class));
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
                            filesList,
                            false,
                            threadIps.get(postsCounter).getString(),
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
                File reportsCatalog = new File(boardsFolder + "/" + boards[boardsCounter] + "/reports.json");
                if(reportsCatalog.length() != 0){
                    reader = Json.createReader(new BufferedReader(new InputStreamReader(new FileInputStream(reportsCatalog), "UTF-8")));
                    JsonArray reportsList = reader.readArray();
                    reader.close();
                    System.out.println("Found " + reportsList.size() + " reports.");
                    for(int a = 0; a < reportsList.size(); a++){
                        JsonObject report = reportsList.getJsonObject(a);
                        ArrayList<JsonString> reportPosts_raw = new ArrayList<>(report.getJsonArray("posts").getValuesAs(JsonString.class));
                        ArrayList<String> reportPosts = new ArrayList<>();
                        for(int b = 0; b < reportPosts_raw.size(); b++)
                            reportPosts.add(reportPosts_raw.get(b).getString());
                        board.addReport(new Report(
                        boards[boardsCounter],
                        reportPosts,
                        report.getString("text"),
                        report.getString("ip"),
                        report.getString("id")));
                        reportsCounter = Integer.parseInt(report.getString("id")) + 1;
                    }
                }
                boardsList.put(boards[boardsCounter], board);
                boardsCatalog.add(board);
            }catch(Exception e){
            e.printStackTrace(System.out);
            continue;
        }
        }
        try{ // загрузка модераторов
        File modsFile = new File(System.getProperty("catalina.base") + "/webapps/ROOT/WEB-INF/mods.json");
        reader = Json.createReader(new BufferedReader(new InputStreamReader(new FileInputStream(modsFile), "UTF-8")));
        ArrayList<JsonObject> mods = new ArrayList<>(reader.readArray().getValuesAs(JsonObject.class));
        reader.close();
        for(int a = 0; a < mods.size(); a++){
            JsonObject mod = mods.get(a);
            ArrayList<JsonString> boards_raw = new ArrayList<>(mod.getJsonArray("boards").getValuesAs(JsonString.class));
            String[] modBoards = new String[boards_raw.size()];
            for(int b = 0; b < modBoards.length; b++){
                modBoards[b] = boards_raw.get(b).getString();
            }
            modsList.put(mod.getString("key"), new Mod(mod.getString("key"), mod.getString("level"), modBoards, mod.getString("name")));
        }
        }catch(Exception e){
            e.printStackTrace(System.out);
            return;
        }
        try{ // загрузка банов
            File bansFile = new File(System.getProperty("catalina.base") + "/webapps/ROOT/WEB-INF/bans.json");
            if(bansFile.exists()){
                reader = Json.createReader(new BufferedReader(new InputStreamReader(new FileInputStream(bansFile), "UTF-8")));
                ArrayList<JsonObject> bans = new ArrayList<>(reader.readArray().getValuesAs(JsonObject.class));
                reader.close();
                for(int a = 0; a < bans.size(); a++){
                    JsonObject ban = bans.get(a);
                    Ban newBan = addBan(Long.parseLong(ban.getString("expiration")),
                    ban.getString("ip"),
                    ban.getString("reason"),
                    ban.getString("board"),
                    ban.getString("permanent").equals("false") ? false : true,
                    ban.getString("global").equals("false") ? false : true,
                    Long.parseLong(ban.getString("id")));
                    bansCounter = Long.parseLong(ban.getString("id"));
                    String lambdaBoard = newBan.getBoard();
                    String lambdaId = String.valueOf(newBan.getID());
                    if(!newBan.isPermanent()){
                        scheduled_unbans.put(String.valueOf(newBan.getID()), banTasks.schedule(() ->{
                            bansList.get(lambdaBoard).remove(newBan); bans_catalog.remove(newBan); scheduledUnbans.remove(String.valueOf(lambdaId));
                        }, newBan.getExpirationDate() - new Date().getTime(), TimeUnit.MILLISECONDS));
                    }
                }
            }
        }catch(Exception e){
            e.printStackTrace(System.out);
        }
    }
    
    public void flushThread(trich.Thread thread) throws IOException{
        File threadFile = new File(rootPath + "res/" + thread.getBoard() + "/" + thread.getPost(0).getPostnum() + ".json");
        if(!threadFile.exists())
            threadFile.createNewFile();
        JsonWriter writer = Json.createWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(threadFile, false), "UTF-8")));
        JsonArrayBuilder threadBuilder = Json.createArrayBuilder();
        JsonArrayBuilder ipsBuilder = Json.createArrayBuilder();
        for(int c = 0; c < thread.getPosts().size(); c++){
            Post post = thread.getPost(c);
            JsonArrayBuilder picsBuilder = Json.createArrayBuilder();
            for(int d = 0; d < post.getPics().size(); d++)
                picsBuilder.add(post.getPics().get(d));
            JsonArrayBuilder repliesTo_builder = Json.createArrayBuilder();
            for(int d = 0; d < post.getRepliedPosts().size(); d++)
                repliesTo_builder.add(post.getRepliedPosts().get(d));
            JsonArrayBuilder repliedBy_builder = Json.createArrayBuilder();
            for(int d = 0; d < post.getReplies().size(); d++)
                repliedBy_builder.add(post.getReplies().get(d));
            threadBuilder.add(Json.createObjectBuilder()
            .add("postnum", post.getPostnum())
            .add("nit", String.valueOf(post.getNumInThread()))
            .add("parent", post.getThread())
            .add("name", post.getName())
            .add("subject", post.getSubject())
            .add("date", post.getDate())
            .add("trip", post.getTripcode())
            .add("message", post.getMessage())
            .add("pics", picsBuilder.build())
            .add("oppost", post.isOP() ? "true" : "false")
            .add("replies_to", repliesTo_builder.build())
            .add("replied_by", repliedBy_builder.build())
            .build());
            ipsBuilder.add(post.getIP());
        }
        writer.write(Json.createObjectBuilder().add("posts", threadBuilder.build()).build());
        writer.close();
        File ipsCatalog = new File(rootPath + "res/" + thread.getBoard() + "/" + thread.getPost(0).getPostnum() + "_ips.json");
        if(!ipsCatalog.exists()){
            ipsCatalog.createNewFile();
        }
        writer = Json.createWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(ipsCatalog, false), "UTF-8")));
        writer.write(ipsBuilder.build());
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
            if(bansList.get("global_bans") == null)
                bansList.put("global_bans", new ArrayList<>());
            board = "global_bans";
        }
        if(bansList.get(board) == null)
            bansList.put(board, new ArrayList<>());
        Ban ban = new Ban(date, ip, reason, board, permanent, global, bans_counter+1L);
        bansList.get(board).add(ban);
        bansCatalog.add(ban);
        try{String lambdaBoard = board;
        if(!ban.isPermanent()){
            scheduledUnbans.put(String.valueOf(ban.getID()), banTasks.schedule(() ->{removeBan(ban);}, ban.getExpirationDate() - System.currentTimeMillis(), TimeUnit.MILLISECONDS));
        }
        }catch(Exception e){e.printStackTrace(System.out);} // если вдруг почему-то не получилось запланировать разбан
        needsBansFlushing = true;
        return ban;
    }
    
    public Ban getBanByID(String id){
        for(int a = 0; a < bansCatalog.size(); a++){
            if(String.valueOf(bansCatalog.get(a).getID()).equals(id))
                return bans_catalog.get(a);
        }
        return null;
    }
    
    public void removeBan(Ban ban){
        bansList.get(ban.getBoard()).remove(ban);
        bansCatalog.remove(ban);
        scheduledUnbans.remove(String.valueOf(ban.getID()));
        needsBansFlushing = true;
    }
    
    public void addModerator(Mod mod){
        try{
            modsList.put(mod.getID(), mod);
        }catch(Exception e){
            e.printStackTrace(System.out);
        }
    }
    
    public void removeModerator(String ID){
        modsList.remove(ID);
    }
    
    public Board getBoard(String boardId){
        return boardsList.get(boardId);
    }
    
    public int getReportsCounter(){
        return reportsCounter;
    }
    
    public long getBansCounter(){
        return bansCounter;
    }
    
    public void setBansCounter(long counter){
        bansCounter = counter;
    }
    
    public ArrayList<Board> getBoards(){
        return boardsCatalog;
    }
    
    public ArrayList<Ban> getBansCatalog(){
        return bansCatalog;
    }
    
    public Mod getModByID(String id){
        return modsList.get(id);
    }
    
    public HashMap<String, ArrayList<Ban>> getBansList(){
        return bansList;
    }
    
    public void setReportsCounter(int counter){
        reportsCounter = counter;
    }
    
    public String checkModerator(HttpServletRequest request){ // проверяет по реквесту, является ли клиент модером
        Cookie[] cookies = request.getCookies();              // нужно для доступа к модераторским функциям
        boolean isMod = false;
        String modID = "";
        if(cookies == null)
            return null;
            for(int a = 0; a < cookies.length; a++){
                if(cookies[a].getName().equals("mod_session_id") && activeModerSessions.containsKey(cookies[a].getValue())){
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
        threadPool.shutdown();
        banTasks.shutdown();
    }
    
}
