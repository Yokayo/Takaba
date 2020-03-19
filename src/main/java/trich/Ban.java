package trich;

import java.util.*;
import java.util.concurrent.*;

public class Ban{
    
    private long expires;
    private String reason;
    private String board;
    private boolean isPermanent, isGlobal;
    private String IP;
    private String humanReadableExpirationDate = "";
    private ScheduledFuture expiration;
    private long ID;
    
    public Ban(long expires_, String ip, String reason_, String board_, boolean isPermanent_, boolean isGlobal_, long id){
        expires = expires_;
        IP = ip;
        reason = reason_;
        board = board_;
        isPermanent = isPermanent_;
        isGlobal = isGlobal_;
        ID = id;
        Date exp = new Date(expires);
        String dow = new String();
            switch(exp.getDay()){
                case 1:
                    dow = "Пнд";
                    break;
                case 2:
                    dow = "Втр";
                    break;
                case 3:
                    dow = "Срд";
                    break;
                case 4:
                    dow = "Чтв";
                    break;
                case 5:
                    dow = "Птн";
                    break;
                case 6:
                    dow = "Суб";
                    break;
                case 0:
                    dow = "Вск";
                    break;
            }
            String month = "";
            switch(exp.getMonth()){
                case 0:
                    month = "Янв";
                    break;
                case 1:
                    month = "Фев";
                    break;
                case 2:
                    month = "Мар";
                    break;
                case 3:
                    month = "Апр";
                    break;
                case 4:
                    month = "Май";
                    break;
                case 5:
                    month = "Июн";
                    break;
                case 6:
                    month = "Июл";
                    break;
                case 7:
                    month = "Авг";
                    break;
                case 8:
                    month = "Сен";
                    break;
                case 9:
                    month = "Окт";
                    break;
                case 10:
                    month = "Ноя";
                    break;
                case 11:
                    month = "Дек";
                    break;
            }
            this.humanReadableExpirationDate += dow + " " + exp.getDate() + " " + month + " " + (exp.getYear() + 1900) + " " + exp.getHours() + ":" + (exp.getMinutes() < 10 ? "0" + exp.getMinutes() : exp.getMinutes());
    }
    
    public long getExpirationDate(){
        return expires;
    }
    
    public String getHumanReadableExpirationDate(){
        return humanReadableExpirationDate;
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
    
    public ScheduledFuture getExpiration(){
        return expiration;
    }
    
    public void setExpiration(ScheduledFuture exp){
        expiration = exp;
    }
    
    public void setID(long id){
        ID = id;
    }
    
    public long getID(){
        return ID;
    }
    
}