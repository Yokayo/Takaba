package viewControllerBeans;

import java.util.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.JstlView;
import org.springframework.web.servlet.view.InternalResourceView;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Cookie;
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
    @Inject private BoardsCache boardsCache;
    private StringBuilder missedMessageBuilder = new StringBuilder(22);
    
    {
        missedMessageBuilder.append("Пропущен");
    }
    
    @GetMapping(value = "")
    public View mainPage(){
        return new RedirectView("/");
    }
    
	@GetMapping(value = "{board}")
	public View displayBoard(Map<String, Object> model,
                             @PathVariable("board") String boardId,
                             @CookieValue(defaultValue = "") String modSessionID,
                             HttpServletRequest request) {
        if(boardId.equals("boards")){
            return new RedirectView("/");
        }
        Board board = boardsCache.getBoard(boardId);
        if(board == null){
            return new RedirectView("/");
        }
        ArrayList<Post> postsToDisplay = new ArrayList<>();
        ArrayList<trich.Thread> threadsToDisplay = board.getFirstNThreads(10);
        trich.Thread thread;
        List<Post> array;
        HashMap<String, String> postsMissed = new HashMap<>();
        for(int a = 0; a < threadsToDisplay.size(); a++){
            array = threadsToDisplay.get(a).getPostsToDisplay();
            postsToDisplay.addAll(array);
            if(threadsToDisplay.get(a).getPostcount() <= 4)
                continue;
            int missed = threadsToDisplay.get(a).getPostcount() - array.size();
            int modulo = missed%10;
            postsMissed.put(threadsToDisplay.get(a).getPost(0).getPostnum(),
            missedMessageBuilder.append(modulo == 1 ? "" : "о")
            .append(" ")
            .append(missed)
            .append(modulo == 1 ? " пост." : modulo > 1 && modulo < 5 ? " поста." : " постов.")
            .toString());
            missedMessageBuilder.delete(8, missedMessageBuilder.capacity());
        }
        model.put("ban_reasons", board.getBanReasons());
        model.put("board", board);
        model.put("board_id", boardId);
        model.put("board_desc", board.getDesc());
        model.put("board_title", board.getTitle());
        model.put("posts", postsToDisplay);
        model.put("missed_posts", postsMissed);
        //model.put("generator", generator);
        model.put("isModerator", boardsCache.getActiveModerSessions().get(modSessionID) != null);
        model.put("catalog", board.getThreads());
		return new JstlView("/WEB-INF/board_page.jsp");
	}
    
	@GetMapping(value = "{board}/{thread}")
	public View displayThread(Map<String, Object> model,
                              @PathVariable("board") String boardId,
                              @PathVariable("thread") String num,
                              @CookieValue(defaultValue = "") String modSessionID,
                              HttpServletRequest request) {
        Board board = boardsCache.getBoard(boardId);
        trich.Thread thread = board.getThread(num);
        if(thread == null)
            return new InternalResourceView("/thread_404.html");
        model.put("board_id", boardId);
        model.put("thread", thread);
        // model.put("generator", generator);
        model.put("isModerator", boardsCache.getActiveModerSessions().get(modSessionID) != null);
        model.put("ban_reasons", board.getBanReasons());
		return new JstlView("/WEB-INF/thread_page.jsp");
	}
    
    @RequestMapping(value = "mod_panel", method = RequestMethod.GET) // доступ к мод-панели
    public View accessModPanel(@CookieValue("mod_session_id") String modSessionID,
                               @RequestParam String task,
                               @RequestParam(required = false) String id,
                               @RequestParam(required = false) String page,
                               HttpServletResponse response,
                               Map<String, Object> model){
        Mod moderator = boardsCache.checkModerator(modSessionID, 1);
        if(moderator == null)
            return new InternalResourceView("/insufficient_previleges.html");
        switch(task){
            case "admin_panel":
                if(moderator.getAccessLevel() < 4)
                    return new RedirectView("/takaba/mod_panel");
                return new JstlView("/WEB-INF/admin_panel.html");
            case "view_reports":
                ArrayList<Report> reports = new ArrayList<>();
                java.util.List<Board> boards = moderator.getBoards();
                for(int a = 0; a < boards.size(); a++){
                    Board board = boards.get(a);
                    if(board == null)
                        continue;
                    reports.addAll(board.getReports());
                }
                model.put("reports", reports);
                model.put("cache", boardsCache);
                return new JstlView("/WEB-INF/view_reports.jsp");
            case "view_bans":
                ArrayList<Ban> bansToDisplay = new ArrayList<>();
                if(id != null){
                    Ban ban = boardsCache.getBan(id);
                    if(ban != null)
                        bansToDisplay.add(ban);
                    model.put("bans", bansToDisplay);
                    model.put("cache", boardsCache);
                    return new JstlView("/WEB-INF/view_bans.jsp");
                }
                int pageNumber;
                try{
                    pageNumber = Integer.parseInt(page);
                    if(pageNumber < 1)
                        pageNumber = 1;
                }catch(Exception e){
                    pageNumber = 1;
                }
                java.util.List<Ban> bansCatalog = boardsCache.getBansCatalog();
                for(int a = (pageNumber-1)*10; a < pageNumber*10 && a < bansCatalog.size(); a++){
                    bansToDisplay.add(bansCatalog.get(a));
                }
                model.put("bans", bansToDisplay);
                model.put("cache", boardsCache);
                return new JstlView("/WEB-INF/view_bans.jsp");
            case "logout":
                boardsCache.removeModerSession(modSessionID);
                Cookie modCookie = new Cookie("mod_session_id", "");
                modCookie.setMaxAge(0);
                response.addCookie(modCookie);
                return new RedirectView("/");
            default:
                return new JstlView("/res/mod_panel.jsp");
        }
    }
    
    @RequestMapping(value = "board_management", method = RequestMethod.GET) // страница управления досками
    public View boardManagement(@CookieValue("mod_session_id") String modSessionID,
                                HashMap<String, Object> model){
        if(boardsCache.checkModerator(modSessionID, 4) == null)
            return new JstlView("/thread_404.html");
        model.put("boards", boardsCache.getBoards());
        return new JstlView("/WEB-INF/boards_management.jsp");
    }
    
}