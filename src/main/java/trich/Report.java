package trich;

import java.util.*;

public class Report{
    
    private String board, text, ip, id;
    private ArrayList<String> posts;
    
    public Report(String board_, ArrayList<String> posts_, String text_, String ip_, String id_){
        board = board_;
        posts = posts_;
        text = text_;
        ip = ip_;
        id = id_;
    }
    
    public String getBoard(){
        return board;
    }
    
    public ArrayList<String> getPosts(){
        return posts;
    }
    
    public String getText(){
        return text;
    }
    
    public String getIP(){
        return ip;
    }
    
    public String getID(){
        return id;
    }
    
}