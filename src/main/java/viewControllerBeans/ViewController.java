package viewControllerBeans;

import java.util.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.JstlView;
import org.springframework.web.servlet.view.InternalResourceView;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletContext;
import java.io.*;
import javax.json.*;
import trich.*;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import rootContextBeans.*;

@Controller(value = "view_controller")
public class ViewController{
    
    @Inject private ServletContext ctx;
    @Inject private BoardsCache boards_cache;
    private HTMLGenerator generator = new HTMLGenerator();
    private String root_path = System.getProperty("catalina.base") + "//webapps//ROOT//";
    
    @GetMapping(value = "")
    public View mainPage(){
        return new RedirectView("/");
    }
    
	@GetMapping(value = "{board}")
	public View simple1(Map<String, Object> model, @PathVariable("board") String board_id, HttpServletRequest request) {
        if(board_id.equals("boards")){
            return new RedirectView("/");
        }
        Board board = boards_cache.getBoard(board_id);
        ArrayList<Post> posts_to_display = new ArrayList<>();
        ArrayList<trich.Thread> threads_to_display = board.getFirstNThreads(10);
        trich.Thread thread;
        ArrayList<Post> array;
        HashMap<String, String> posts_missed = new HashMap<>();
        for(int a = 0; a < threads_to_display.size(); a++){
            array = threads_to_display.get(a).getPostsToDisplay();
            posts_to_display.addAll(array);
            if(threads_to_display.get(a).getPostcount() > 4){
                int missed = threads_to_display.get(a).getPostcount() - array.size();
                int modulo = missed%10;
                posts_missed.put(threads_to_display.get(a).getPost(0).getPostnum(), "Пропущен"
                + (modulo == 1 ? "" : "о")
                + " "
                + missed
                + (modulo == 1 ? " пост." : modulo > 1 && modulo < 5 ? " поста." : " постов."));
            }else{
                posts_missed.put(threads_to_display.get(a).getPost(0).getPostnum(), null);
            }
        }
        model.put("ban_reasons", board.ban_reasons);
        model.put("board", board);
        model.put("board_id", board_id);
        model.put("board_desc", board == null ? "No desc" : board.getDesc());
        model.put("board_title", board == null ? board_id : board.getTitle());
        model.put("posts", posts_to_display);
        model.put("missed_posts", posts_missed);
        model.put("generator", generator);
        model.put("isModerator", boards_cache.checkModerator(request) != null);
        model.put("catalog", board.getThreads());
		return new JstlView("/WEB-INF/board_page.jsp");
	}
    
	@GetMapping(value = "{board}/{thread}")
	public View vt(Map<String, Object> model, @PathVariable("board") String board_id, @PathVariable("thread") String num, HttpServletRequest request) {
        Board board = boards_cache.getBoard(board_id);
        trich.Thread thread = board.getThread(num);
        if(thread == null)
            return new InternalResourceView("/thread_404.html");
        model.put("board_id", board_id);
        model.put("thread", thread);
        model.put("generator", generator);
        model.put("isModerator", boards_cache.checkModerator(request) != null);
        model.put("ban_reasons", board.ban_reasons);
		return new JstlView("/WEB-INF/thread_page.jsp");
	}
    
}