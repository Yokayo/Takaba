package trich;

import java.util.*;
import javax.json.*;

public class Post{

    private String postnum, name, trip, date, subject, message, thread, IP;
    private ArrayList<JsonObject> pics;
    private boolean isOppost;
    private int numInThread;
    private ArrayList<String> repliedBy, isReplyTo;

    public Post(String postnum_, String t, int nit, String name_, String trip_, String date_, String subject_, String message_, boolean op, String ip){
        postnum = postnum_;
        name = name_;
        trip = trip_;
        date = date_;
        subject = subject_;
        message = message_;
        isOppost = op;
        pics = new ArrayList<>();
        numInThread = nit;
        thread = t;
        IP = ip;
    }

    public Post(String postnum_, String t, int nit, String name_, String trip_, String date_, String subject_, String message_, ArrayList<JsonObject> pics_, boolean op, String ip, ArrayList<String> repliesTo_, ArrayList<String> repliedBy_){
        postnum = postnum_;
        name = name_;
        trip = trip_;
        date = date_;
        subject = subject_;
        message = message_;
        isOppost = op;
        pics = pics_;
        numInThread = nit;
        thread = t;
        IP = ip;
        isReplyTo = repliesTo_;
        repliedBy = repliedBy_;
    }

    public String getThread(){
        return thread;
    }
    
    public String getPostnum(){
        return postnum;
    }

    public String getName(){
        return name;
    }

    public String getTripcode(){
        return trip;
    }
    
    public String getDate(){
        return date;
    }

    public String getSubject(){
        return subject;
    }

    public String getMessage(){
        return message;
    }
    
    public ArrayList<String> getRepliedPosts(){
        return isReplyTo;
    }
    
    public ArrayList<String> getReplies(){
        return repliedBy;
    }
    
    public void addReply(String postnum){
        if(!repliedBy.contains(postnum))
            repliedBy.add(postnum);
    }
    
    public void removeReply(String postnum){
        repliedBy.remove(postnum);
    }
    
    public int getNumInThread(){
        return numInThread;
    }
    
    public void setNumInThread(int num_){
        numInThread = num_;
    }
    
    public ArrayList<JsonObject> getPics(){
        return pics;
    }
    
    public boolean isOP(){
        return isOppost;
    }
    
    public String getIP(){
        return IP;
    }

}