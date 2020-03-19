package rootContextBeans;

import java.util.*;
import java.io.*;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletConfig;
import javax.inject.Inject;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;
import trich.*;

public class StaticServlet extends HttpServlet{ // сервлет для доступа к статичным ресурсам
                                                // нужен для кэша картинок и для регулирования доступа к мод-ресурсам
    @Autowired private BoardsCache boards_cache;
    private String root_path = System.getProperty("catalina.base").replace("\\", "/") + "/webapps/ROOT/res";
    
    public void init(ServletConfig config){
        try{
        super.init(config);
        SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(this);
        }catch(Exception e){e.printStackTrace(System.out);}
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
            if(boards_cache.checkModerator(request) == null){
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            getServletContext().getNamedDispatcher("default").forward(request, response);
            return;
        }
        Board board = boards_cache.getBoard(parts[1]);
        if(board == null){
            System.out.println("No board found with id " + parts[1] + ". Full path = " + path);
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if(parts.length == 3){ // "/test/***.json"
            if(parts[2].contains("_ips") || !parts[2].contains(".")){
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            String thread_num = parts[2].split("\\.")[0];
            trich.Thread thread = board.getThread(thread_num);
            if(thread == null){
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            if(request.getParameter("prev") == null){
                response.setContentType("application/json; charset=UTF-8");
                response.setCharacterEncoding("UTF-8");
                PrintWriter writer = response.getWriter();
                writer.write(board.getThread(thread_num).json);
                writer.close();
                return;
            }else{
                response.setContentType("application/json; charset=UTF-8");
                response.setCharacterEncoding("UTF-8");
                PrintWriter writer = response.getWriter();
                writer.write(thread.prev_json);
                writer.close();
                return;
            }
        }
        CachedImage img = boards_cache.images_cache.get(root_path + path);
        if(img != null){
            ServletOutputStream output = response.getOutputStream();
            output.write(img.data);
            output.close();
            return;
        }else{
            getServletContext().getNamedDispatcher("default").forward(request, response);
            return;
        }
        }catch(Exception e){e.printStackTrace(System.out); return;}
    }
    
}