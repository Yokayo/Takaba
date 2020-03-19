package trich;

import java.util.*;
import javax.json.*;

public class HTMLGenerator{
    
    public String test = "test";
    
    public String generatePostHTML(Post post, String board, ArrayList<Post> postsDisplayed, boolean notForThread){
        String res = "";
        res += "<div class=\"thread_post thread_post_" + post.getPostnum() + "\" id='" + post.getPostnum() + "'>"
        + "<div class=\"post_" + (post.isOP() ? "oppost" : "reply") + "\">"
        + "<div class=\"post_details\">"
        + "<input type=\"checkbox\" name=\"delete\" class=\"turnmeoff\" value=\"" + post.getPostnum() + "\"/> " +
        (post.getSubject().equals("") ? "" : "<span class=\"post_subject\">" + post.getSubject() + "</span> ") + post.getName()
        + (!post.getTripcode().equals("") ? "<span class=\"post_trip\">!" + post.getTripcode() + "</span>" : "")
        + "<span class=\"post_date\">" + post.getDate() + "</span>"
        + "<a class='post_reflink' href='" + (notForThread ? "/boards/" + board + "/" + post.getThread() + "#" + post.getPostnum() : "#" + post.getPostnum())
        + "'>№</a><span class=\"postnum\" data-num=\"" + post.getPostnum() + "\">" + post.getPostnum() + "</span>"
        + (notForThread ? "" : "<span class=\"post_number\">#" + post.getNumInThread() + "</span>")
        + "<span class=\"post_buttons_container\">"
        + "<a class=\"postbtn_hide\" data-num=\"" + post.getPostnum() + "\"></a>"
        + "<a class=\"postbtn_report\" data-num=\"" + post.getPostnum() + "_" + post.getThread() + "\"></a>"
        + "<a class=\"postbtn_options\" data-num=\"" + post.getPostnum() + "_" + post.getThread() + "\"></a>"
        + (!notForThread ? "" : (post.getNumInThread() == 1 ? "<span class='oppost_reply_link'>[<a href=/boards/"
        + board + "/" + post.getPostnum() + ">Ответ</a>]</span>" : ""))
        + "</div>";
        ArrayList<JsonObject> pics = post.getPics();
        if(pics.size() == 1){
            JsonObject pic = pics.get(0);
            res += "<div class=\"file_attr single_file_attr\"><a href=\""
            + pic.getString("full_path")
            + "\" class=\"file_link\">"
            + pic.getString("name")
            + "</a></div>"
            + "<div class=\"file_attachment_sign single_file_attachment_sign\">(" + pic.getString("size") + "Кб, " + pic.getString("width") + "x" + pic.getString("height") + ")" + "</div>"
            + "<a href=\"" + pic.getString("full_path") + "\" class=\"file_attachment_single\">"
            + "<img src=\"" + pic.getString("thumb_path") + "\" data-src=\"" + pic.getString("full_path") + "\" src-width=\"" + pic.getString("width") + "\" src-height=\"" + pic.getString("height") + "\" class=\"attachment\"></img></a>";
        }
        if(pics.size() > 1){
            res += "<div class=\"multiple_files_container\">";
            for(int a = 0; a < pics.size(); a++){
                res += "<div class=\"multiple_files_single\">"
                + "<div class=\"file_attr multiple_file_attr\">"
                + "<a href=\"" + pics.get(a).getString("full_path") + "\" class=\"file_link\">" + pics.get(a).getString("name") + "</a>"
                + "</div>"
                + "<div class=\"file_attachment_sign\">(" + pics.get(a).getString("size") + "Кб, " + pics.get(a).getString("width") + "x" + pics.get(a).getString("height") + ")" + "</div>"
                + "<a href=\"" + pics.get(a).getString("full_path") + "\" class=\"file_attachment_multiple\">"
                + "<img src=\"" + pics.get(a).getString("thumb_path") + "\" data-src=\"" + pics.get(a).getString("full_path") + "\" src-width=\"" + pics.get(a).getString("width") + "\" src-height=\"" + pics.get(a).getString("height") + "\" class=\"attachment\"></img></a>"
                + "</div>";
            }
            res += "</div><br/>";
        }
        ArrayList<String> repliesToRender = new ArrayList<>();
        searchingRenderedReplies:
        for(int a = 0; a < post.getReplies().size(); a++){
            for(int b = 0; b < postsDisplayed.size(); b++){
                if(postsDisplayed.get(b).getPostnum().equals(post.getReplies().get(a))){
                    repliesToRender.add(post.getReplies().get(a));
                    continue searchingRenderedReplies;
                }
            }
        }
        res += "<article class=\"post_message\">"+ post.getMessage() + "</article>"
        + "<span class=\"reply_map reply_map_" + post.getPostnum() + "\"" + (repliesToRender.size() > 0 ? "" : "style=\"display: none;\"") + " data-num=\"" + post.getPostnum() + "\">Ответы: ";   // на главной этим займётся js
        for(int a = 0; a < repliesToRender.size(); a++){
             res += "<a class=\"reply_map_entry\" data-num=\"" + repliesToRender.get(a) + "\">>>" + repliesToRender.get(a) + "</a>" + (a == repliesToRender.size()-1 ? "" : ", ");
         }
        res += "</span>";
        res += "</div>"
        + "</div>";
        return res;
    }
    
    public String test(){
        return test;
    }
    
}