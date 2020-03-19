package trich;

import java.util.*;

public class Mod{

    private String id, name;
    private int access_level;
    private String[] boards;
    
    public Mod(String id_, String al, String[] boards_, String name_) throws NumberFormatException{
        id = id_;
        try{
            access_level = Integer.parseInt(al);
        }catch(NumberFormatException e){
            throw e;
        }
        boards = boards_;
        name = name_;
    }
    
    public String getID(){
        return id;
    }
    
    public int getAccessLevel(){
        return access_level;
    }
    
    public String[] getBoards(){
        return boards;
    }
    
    public String getName(){
        return name;
    }
    
    public void setAccessLevel(int al){
        access_level = al;
    }
    
    public void setName(String name_){
        name = name_;
    }
    
    public void setBoards(String[] boards_){
        boards = boards_;
    }
    
}