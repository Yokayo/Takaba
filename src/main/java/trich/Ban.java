package trich;

import java.util.*;
import java.util.concurrent.*;
import javax.persistence.*;
import trich.TrichDict;

@Entity
@Table(name = "bans")
public class Ban{
    
    @Column(name = "expires")
    private long expires;
    
    @Column(name = "reason")
    private String reason;
    
    @Column(name = "board")
    private String board;
    
    @Column(name = "permanent")
    private boolean isPermanent;
    
    @Column(name = "global")
    private boolean isGlobal;
    
    @Column(name = "IP")
    private String IP;
    
    private String humanReadableExpirationDate = "";
    
    @Transient private ScheduledFuture expiration;
    
    @Id
    @Column(name = "id")
    private long ID;
    
    public Ban(long expires_, String ip, String reason_, String board_, boolean isPermanent_, boolean isGlobal_, long id){
        expires = expires_;
        IP = ip;
        reason = reason_;
        board = board_;
        isPermanent = isPermanent_;
        isGlobal = isGlobal_;
        ID = id;
        this.updateHumanReadableExpirationDate();
    }
    
    public long getExpirationDate(){
        return expires;
    }
    
    public void setExpirationDate(long expires_){
        expires = expires_;
        this.updateHumanReadableExpirationDate();
    }
    
    public String getHumanReadableExpirationDate(){
        return humanReadableExpirationDate;
    }
    
    public void updateHumanReadableExpirationDate(){
        Date exp = new Date(expires);
        humanReadableExpirationDate = TrichDict.weekDays.get(exp.getDay())
                                    + " "
                                    + exp.getDate()
                                    + " "
                                    + TrichDict.months.get(exp.getMonth())
                                    + " "
                                    + (exp.getYear() + 1900)
                                    + " "
                                    + exp.getHours()
                                    + ":"
                                    + (exp.getMinutes() < 10 ? "0" + exp.getMinutes() : exp.getMinutes());
    }
    
    public String getReason(){
        return reason;
    }
    
    public String getBoard(){
        return board;
    }
    
    public String getIP(){
        return IP;
    }
    
    public boolean isGlobal(){
        return isGlobal;
    }
    
    public boolean isPermanent(){
        return isPermanent;
    }
    
    public long getID(){
        return ID;
    }
    
}