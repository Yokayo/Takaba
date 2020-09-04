package rootContextBeans;

import java.util.*;
import java.io.*;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletConfig;
import javax.inject.Inject;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;
import trich.*;

public class StaticServlet extends HttpServlet{ // сервлет для доступа к статичным ресурсам
                                                
    @Inject private BoardsCache boardsCache;
    private String rootPath = System.getProperty("catalina.base").replace("\\", "/") + "/webapps/ROOT/res";
    
    public void init(ServletConfig config){
        try{
            super.init(config);
            SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(this);
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response){
        try{
            String path = request.getPathInfo();
            String parts[] = path.split("/");
            if(parts == null || parts.length < 2 || (parts.length == 2 && parts[1].equals(""))){
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            if(parts.length == 2){
                if(checkModerator(request) == null){
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                getServletContext().getNamedDispatcher("default").forward(request, response);
                return;
            }
            Board board = boardsCache.getBoard(parts[1]);
            if(board == null){
                System.err.println("No board found with id = " + parts[1] + ". Full path = " + path);
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            if(parts.length == 3){ // "/test/***.json"
                if(parts[2].contains("_ips") || !parts[2].contains(".")){
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                String threadNum = parts[2].split("\\.")[0];
                trich.Thread thread = board.getThread(threadNum);
                if(thread == null){
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                if(request.getParameter("prev") == null){
                    response.setContentType("application/json; charset=UTF-8");
                    response.setCharacterEncoding("UTF-8");
                    PrintWriter writer = response.getWriter();
                    writer.write(board.getThread(threadNum).getJSON());
                    writer.close();
                    return;
                }else{
                    response.setContentType("application/json; charset=UTF-8");
                    response.setCharacterEncoding("UTF-8");
                    PrintWriter writer = response.getWriter();
                    writer.write(thread.getPrevJSON());
                    writer.close();
                    return;
                }
            }
            getServletContext().getNamedDispatcher("default").forward(request, response);
            return;
        }catch(Exception e){
            e.printStackTrace();
            return;
        }
    }
    
    public String checkModerator(HttpServletRequest request){
        Cookie[] cookies = request.getCookies();
        boolean isMod = false;
        String modID = "";
        if(cookies == null)
            return null;
            for(int a = 0; a < cookies.length; a++){
                if(cookies[a].getName().equals("mod_session_id") && boardsCache.getActiveModerSessions().containsKey(cookies[a].getValue())){
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
    
}