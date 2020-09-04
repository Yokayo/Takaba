package trich;

import java.util.*;
import javax.persistence.*;

@Entity
@Table(name = "moderators")
public class Mod{
    
    @Id
    private String id;
    
    @Column(name = "nick")
    private String name;
    
    @Column(name = "al")
    private int accessLevel;
    
    @ManyToMany
    @JoinTable(
        name = "mods_boards",
        joinColumns = @JoinColumn(name = "mod_id"),
        inverseJoinColumns = @JoinColumn(name = "board_id")
    )
    private List<Board> boards;
    
    public Mod(){}
    
    public Mod(String key_, int al_, List<Board> boards_, String name_){
        if(al_ > 4 || al_ < 1)
            al_ = 1;
        id = key_;
        accessLevel = al_;
        boards = new ArrayList<>(boards_);
        name = name_;
    }
    
    public String getID(){
        return id;
    }
    
    public int getAccessLevel(){
        return accessLevel;
    }
    
    public List<Board> getBoards(){
        return Collections.unmodifiableList(boards);
    }
    
    public String getName(){
        return name;
    }
    
    public void setAccessLevel(int al){
        if(al > 4 || al < 1)
            al = 1;
        accessLevel = al;
    }
    
    public void setName(String name_){
        name = name_;
    }
    
    public void setBoards(List<Board> boards_){
        boards = boards_;
    }
    
}