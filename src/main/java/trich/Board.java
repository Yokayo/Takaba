package trich;

import java.util.*;
import rootContextBeans.*;

public class Board{
    public String id, title, desc, default_name;
    private long total_posts = 0L;
    private ArrayList<trich.Thread> threads = new ArrayList<>();
    private ArrayList<Report> reports = new ArrayList<>();
    public boolean needsCatalogFlushing, needsSettingsFlushing = false;
    public ArrayList<String> ban_reasons = new ArrayList<>();
    private BoardsCache boards_cache;
    public int max_file_size = 2;
    public boolean delayedFlushingEnabled = false;
    
    public Board(String id_, String title_, String desc_, String default_name_, String mfs, BoardsCache bc){//, String[] catalog_cache_){
        title = title_;
        desc = desc_;
        default_name = default_name_;
        id = id_;
        boards_cache = bc;
        max_file_size = Integer.parseInt(mfs);
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
    
    public void addBanReasonsSet(ArrayList<String> reasons){
        ban_reasons.addAll(reasons);
    }
    
    public void addReport(Report report){
        reports.add(report);
    }
    
    public void setTotalPosts(long p){
        total_posts = p;
    }
    
    public long getTotalPosts(){
        return total_posts;
    }
    
    public Post getPost(String num){
        Thread thread;
        Post post;
        ArrayList<Post> posts;
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
    
    public void removePost(String num){
        Post post = getPost(num);
        if(post == null)
            return;
        Thread thread = getThread(post.getThread());
        
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
    
    public ArrayList<trich.Thread> getThreads(){
        return threads;
    }
    
    public ArrayList<trich.Thread> getFirstNThreads(int count){
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
    
    public void setDesc(String desc_){
        desc = desc_;
    }
    
    public void setDefaultName(String name){
        default_name = name;
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
        return default_name;
    }
    
    public ArrayList<Report> getReports(){
        return reports;
    }
    
}