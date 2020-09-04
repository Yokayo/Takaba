package trich;

import java.util.*;
import rootContextBeans.*;
import javax.persistence.*;

@Entity
@Table(name = "boards")
public class Board{
    
    @Id
    private String id;
    
    @Column(name = "name")
    private String title;
    
    @Column(name = "description")
    private String desc;
    
    @Column(name = "`defaultName`")
    private String defaultName;
    
    @ManyToMany(mappedBy = "boards")
    private List<Mod> moders;
    
    @Transient private long totalPosts = 0L;
    
    @OneToMany(fetch = FetchType.EAGER, orphanRemoval = true, cascade = CascadeType.REMOVE)
    @JoinColumn(name = "board")
    @OrderBy("num ASC")
    private List<trich.Thread> threads;
    
    @Transient private ArrayList<Report> reports;
    
    @ElementCollection(fetch = FetchType.LAZY, targetClass = String.class)
    @CollectionTable(
        name = "`banReasons`",
        joinColumns = @JoinColumn(name = "boardID")
    )
    private List<String> banReasons;
    
    @Column(name = "`maxFileSize`")
    private int maxFileSize;
    
    public Board(){}
    
    public Board(String id_, String title_, String desc_, String defaultName_, String mfs, BoardsCache bc){//, String[] catalog_cache_){
        title = title_;
        desc = desc_;
        defaultName = defaultName_;
        id = id_;
        maxFileSize = Integer.parseInt(mfs);
    }
    
    public void addThread(trich.Thread thread, boolean bump){
        if(threads.contains(thread)){
            return;
        }
        if(threads.size() == 50){
            for(int a = threads.size()-1; a >= 0; a--){
                if(a == 0){
                    threads.set(0, thread);
                    return;
                }
                threads.set(a, threads.get(a-1));
            }
        }else{
            threads.add(thread);
            if(bump) bumpThread(thread);
        }
    }
    
    public void removeThread(trich.Thread thread){
        if(!threads.contains(thread))
            return;
        threads.remove(thread);
    }
    
    public void bumpThread(trich.Thread thread){
        if(!threads.contains(thread))
            return;
        trich.Thread t = threads.get(0);
        if(t == thread)
            return;
        for(int a = threads.indexOf(thread); a > 0; a--){
            threads.set(a, threads.get(a-1));
        }
        threads.set(0, thread);
    }
    
    public void addBanReasonsSet(List<String> reasons){
        banReasons.addAll(reasons);
    }
    
    public void addReport(Report report){
        reports.add(report);
    }
    
    public void setTotalPosts(long p){
        totalPosts = p;
    }
    
    public long getTotalPosts(){
        return totalPosts;
    }
    
    public List<String> getBanReasons(){
        return Collections.unmodifiableList(banReasons);
    }
    
    public Post getPost(String num){
        Thread thread;
        Post post;
        List<Post> posts;
        for(int a = 0; a < threads.size(); a++){
            thread = threads.get(a);
            posts = thread.getPosts();
            for(int b = 0; b < posts.size(); b++){
                post = posts.get(b);
                if(post.getPostnum().equals(num))
                    return post;
            }
        }
        return null;
    }
    
    public Report getReportByID(String id){
        for(int a = 0; a < reports.size(); a++){
            Report report = reports.get(a);
            if(report.getID().equals(id))
                return report;
        }
        return null;
    }
    
    public trich.Thread getThread(String num){
        trich.Thread thread;
        for(int a = 0; a < threads.size(); a++){
            thread = threads.get(a);
            if(thread.getPost(0).getPostnum().equals(num))
                return thread;
        }
        return null;
    }
    
    public List<trich.Thread> getThreads(){
        return Collections.unmodifiableList(threads);
    }
    
    public ArrayList<trich.Thread> getFirstNThreads(int count){
        if(threads.size() <= count)
            return new ArrayList<>(threads);
        ArrayList<trich.Thread> list = new ArrayList<>();
        for(int a = 0; a < count && a < threads.size(); a++){
            list.add(threads.get(a));
        }
        return list;
    }
    
    public void removeReport(Report report){
        for(int a = 0; a < reports.size(); a++){
            if(String.valueOf(reports.get(a).getID()).equals(report.getID())){
                reports.remove(a);
                return;
            }
        }
    }
    
    public void setTitle(String title_){
        title = title_;
    }
    
    public void setId(String id_){
        id = id_;
    }
    
    public void setMaxFileSize(int mfs){
        maxFileSize = mfs;
    }
    
    public void setDesc(String desc_){
        desc = desc_;
    }
    
    public void setDefaultName(String name){
        defaultName = name;
    }
    
    public String getTitle(){
        return title;
    }
    
    public String getDesc(){
        return desc;
    }
    
    public String getID(){
        return id;
    }
    
    public String getDefaultName(){
        return defaultName;
    }
    
    public int getMaxFileSize(){
        return maxFileSize;
    }
    
    public List<Report> getReports(){
        return Collections.unmodifiableList(reports);
    }
    
}