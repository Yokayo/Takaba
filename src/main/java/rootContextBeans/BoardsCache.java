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
import javax.persistence.*;
import org.hibernate.*;
import org.hibernate.cfg.Configuration;
import org.hibernate.query.Query;
import trich.*;


@Component("boardsCache")
@Scope("singleton")
public class BoardsCache{ // центральная часть борды - in-memory storage
                          // хранит все доски, посты на них, IP, конфигурацию досок, модераторов, баны и т.д.
    private HashMap<String, Board> boardsList = new HashMap<>(100); // карта досок
    
    private Map<String, Mod> modsList = new HashMap<>(); // список модераторов
    
    private ArrayList<Board> boardsCatalog = new ArrayList<>(); // каталог досок, нужен для вывода списком в админ-панели
    
    private HashMap<String, Mod> activeModerSessions = new HashMap<>(); // карта активных сессий модераторов
    
    private HashMap<String, ArrayList<Ban>> bansList = new HashMap<>(); // список банов
    
    private ArrayList<Ban> bansCatalog = new ArrayList<>(); // каталог банов, для вывода списком на вкладке "Баны" в модерке
    
    private HashMap<String, ScheduledFuture> scheduledUnbans = new HashMap<>(); // список разбанов, нужен для ручного разбана
    
    private ArrayList<String> generalBanReasons = new ArrayList<>(); // список причин бана
    
    private ArrayList<String> allowedFileExtensions = new ArrayList<>(); // поддерживаемые форматы файлов
    
    private ScheduledThreadPoolExecutor banTasks = new ScheduledThreadPoolExecutor(10); // потоки для разбанов
    private ScheduledThreadPoolExecutor threadPool = new ScheduledThreadPoolExecutor(20); // потоки для других задач
    private long reportsCounter = 0; // счётчик жалоб для присвоения ID
    private long bansCounter = 0; // счётчик банов для ID
    SessionFactory sessionFactory;
    
    
    @PostConstruct
    public void buildCache(){
        allowedFileExtensions.add("jpg");
        allowedFileExtensions.add("jpeg");
        allowedFileExtensions.add("png");
        sessionFactory = new Configuration().configure().buildSessionFactory();
        try{
            Session session = sessionFactory.openSession();
            org.hibernate.query.Query query = session.createQuery("FROM Board");
            List<Board> boards = query.list();
            for(int a = 0; a < boards.size(); a++){
                Board board = boards.get(a);
                session.update(board);
                boardsList.put(board.getID(), board);
            }
            query = session.createQuery("FROM Mod");
            List<Mod> mods = query.getResultList();
            for(int a = 0; a < mods.size(); a++){
                Mod moder = mods.get(a);
                session.update(moder);
                modsList.put(moder.getID(), moder);
            }
            session.close();
        }catch(HibernateException e){}
    }
    
    public void addReport(Report report){
        getBoard(report.getBoard()).addReport(report);
    }
    
    public void removeReport(Report report){
        getBoard(report.getBoard()).removeReport(report);
    }
    
    public void addBan(Ban ban){
        if(getBoard(ban.getBoard()) == null && !ban.getBoard().equals("global_bans")){
            printError("Controller tried to save ban on non-existent board. No operation.");
            return;
        }
        if(bansList.get(ban.getBoard()) == null)
            bansList.put(ban.getBoard(), new ArrayList<>());
        persistObject(ban);
        bansCatalog.add(ban);
        try{
            if(!ban.isPermanent())
                scheduledUnbans.put(String.valueOf(ban.getID()), banTasks.schedule(() -> {removeBan(ban);}, ban.getExpirationDate() - System.currentTimeMillis(), TimeUnit.MILLISECONDS));
        }catch(Exception e){}
        return;
    }
    
    public Ban getBan(String id){
        for(int a = 0; a < bansCatalog.size(); a++){
            if(String.valueOf(bansCatalog.get(a).getID()).equals(id))
                return bansCatalog.get(a);
        }
        return null;
    }
    
    public void removeBan(Ban ban){
        ScheduledFuture banSchedule = scheduledUnbans.get(ban.getID());
        if(banSchedule != null)
            banSchedule.cancel(false);
        bansCatalog.remove(ban);
        scheduledUnbans.remove(String.valueOf(ban.getID()));
        removeObject(ban);
    }
    
    public void addModerator(Mod mod){
        modsList.put(mod.getID(), mod);
        persistObject(mod);
    }
    
    public void removeModerator(String ID){
        Mod mod = modsList.get(ID);
        if(mod == null)
            return;
        modsList.remove(ID);
        removeObject(mod);
    }
    
    public Mod getModerator(String id){
        return modsList.get(id);
    }
    
    public void addPost(Board board, Post post){
        if(board == null || post == null)
            return;
        trich.Thread thread = post.getThread();
        if(thread == null){
            System.out.println("Saving post with num " + post.getPostnum() + ", its thread is null.");
            thread = new trich.Thread();
            thread.setBoard(board);
            post.setThread(thread);
            thread.setNum(post.getPostnum());
            thread.addPost(post);
            board.addThread(thread, true);
            persistObject(thread);
        }else{
            post.setThread(thread);
            thread.addPost(post);
            mergeObject(thread);
        }
        board.setTotalPosts(board.getTotalPosts()+1L);
        mergeObject(board);
    }
    
    public void removePost(Board board, String postnum){
        if(board == null || postnum == null)
            return;
        Post post = board.getPost(postnum);
        if(post == null)
            return;
        post.getRepliedPosts().stream().forEach((Post replied) -> {replied.removeReply(post);});
        post.getPics().stream().forEach((CachedImage image) -> {
            File fullImage = new File(image.getPath());
            if(fullImage.exists())
                fullImage.delete();
            File thumbImage = new File(image.getThumbPath());
            if(thumbImage.exists())
                thumbImage.delete();
        });
        post.getThread().removePost(post);
    }
    
    public long getReportsCounter(){
        return reportsCounter;
    }
    
    public Map<String, Board> getBoardsList(){
        return Collections.unmodifiableMap(boardsList);
    }
    
    public List<String> getGeneralBanReasons(){
        return Collections.unmodifiableList(generalBanReasons);
    }
    
    public List<String> getAllowedFileExtensions(){
        return Collections.unmodifiableList(allowedFileExtensions);
    }
    
    public Map<String, ScheduledFuture> getScheduledUnbans(){
        return Collections.unmodifiableMap(scheduledUnbans);
    }
    
    public void addBoard(Board board){
        boardsList.put(board.getID(), board);
        boardsCatalog.add(board);
        persistObject(board);
    }
    
    public void removeBoard(String id){
        Board board = boardsList.get(id);
        if(board == null)
            return;
        boardsCatalog.remove(boardsList.get(id));
        boardsList.remove(id);
        removeObject(board);
    }
    
    public Board getBoard(String boardId){
        return boardsList.get(boardId);
    }
    
    public void removeThread(trich.Thread thread){
        thread.getBoard().removeThread(thread);
        removeObject(thread);
    }
    
    public void removeModerSession(String modID){
        activeModerSessions.remove(modID);
    }
    
    public Map<String, Mod> getActiveModerSessions(){
        return Collections.unmodifiableMap(activeModerSessions);
    }
    
    public long getBansCounter(){
        return bansCounter;
    }
    
    public void setBansCounter(long counter){
        bansCounter = counter;
    }
    
    public List<Board> getBoards(){
        return Collections.unmodifiableList(boardsCatalog);
    }
    
    public List<Ban> getBansCatalog(){
        return Collections.unmodifiableList(bansCatalog);
    }
    
    public Map<String, ArrayList<Ban>> getBansList(){
        return Collections.unmodifiableMap(bansList);
    }
    
    public Map<String, Mod> getModsList(){
        return Collections.unmodifiableMap(modsList);
    }
    
    public void setReportsCounter(long counter){
        reportsCounter = counter;
    }
    
    public void scheduleTask(Runnable task, long time, java.util.concurrent.TimeUnit timeUnits){
        threadPool.schedule(task, time, timeUnits);
    }
    
    public void persistObject(Object object){
        try{
            Session session = sessionFactory.openSession();
            session.persist(object);
            Transaction transaction = session.getTransaction();
            transaction.begin();
            session.flush();
            transaction.commit();
        }catch(HibernateException e){} // сервер всё равно сам заносит всё в логи
    }
    
    public void mergeObject(Object object){
        try{
            Session session = sessionFactory.openSession();
            session.merge(object);
            Transaction transaction = session.getTransaction();
            transaction.begin();
            session.flush();
            transaction.commit();
        }catch(HibernateException e){}
    }
    
    public void removeObject(Object object){
        try{
            Session session = sessionFactory.openSession();
            session.delete(object);
            Transaction transaction = session.getTransaction();
            transaction.begin();
            session.flush();
            transaction.commit();
        }catch(HibernateException e){}
    }
    
    @PreDestroy
    public void finalizer(){
        threadPool.shutdown();
        banTasks.shutdown();
    }
    
    public void printError(String errorText){
        System.err.println("[" + new Date() + "] ERROR: " + errorText);
    }
    
    public void printWarning(String warningText){
        System.err.println("[" + new Date() + "] WARNING: " + warningText);
    }
    
    
    
    public Mod checkModerator(String key, int minLvl){ // проверяет по реквесту, является ли клиент модером
        Mod mod = getActiveModerSessions().get(key);
        if(mod == null || mod.getAccessLevel() < minLvl)
            return null;
        return mod;
    }
    
    public Mod checkModerator(String key, int minLvl, String requiredBoard){ // то же самое, но с конкретной доской
        Mod mod = getActiveModerSessions().get(key);
        if(mod == null || mod.getAccessLevel() < minLvl)
            return null;
        java.util.List<Board> boards = mod.getBoards();
        for(int a = 0; a < boards.size(); a++){
            if(boards.get(a).getID().equals(requiredBoard))
                return mod;
        }
        return null;
    }
    
}