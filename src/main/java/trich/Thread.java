package trich;

import java.util.*;
import java.io.*;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;
import org.springframework.beans.factory.annotation.Autowired;
import javax.json.*;
import trich.*;
import rootContextBeans.BoardsCache;
//import rootContextBeans.CachedJSON;

public class Thread{
    
    private ArrayList<Post> posts = new ArrayList<>();
    private boolean bumplimit;
    private int postcount = 0;
    private String title, board;
    //private CachedJSON json;
    //private String ips_json;
    private String root_path = System.getProperty("catalina.base").replace("\\", "/") + "/webapps/ROOT/";
    public String json;
    public String prev_json;
    public boolean needsFlushing = false;
    @Autowired private BoardsCache boards_cache;
    
    public Thread(Post oppost, String board_id){
        try{
        //json = new CachedJSON("null");
        //ips_json = "null";
        SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(this);
        System.out.println("boards_cache injected is " + boards_cache == null);
        addPost(oppost.getPostnum(), oppost.getThread(), oppost.getNumInThread(), oppost.getName(), oppost.getTripcode(), oppost.getDate(), oppost.getSubject(), oppost.getMessage(), oppost.getPics(), true, oppost.getIP(), oppost.getRepliedPosts(), oppost.getReplies());
        String msg = oppost.getMessage();
        if(oppost.getSubject().equals("")){
            title = msg.substring(0, 50 > msg.length() ? msg.length() : 50);
        }else{
            title = oppost.getSubject();
        }
        board = board_id;
        //json = new CachedJSON(json_);
        }catch(Exception e){e.printStackTrace(System.out);}
    }
    
    public String getTitle(){
        return title;
    }
    
    public String getBoard(){
        return board;
    }
    
    public void addPost(String postnum_, String t, int nit, String name_, String trip_, String date_, String subject_, String message_, ArrayList<JsonObject> pics_, boolean op, String ip, ArrayList<String> repliesTo, ArrayList<String> repliedBy){
        Post post_to_add = new Post(postnum_, t, nit, name_, trip_, date_, subject_, message_, pics_, op, ip, repliesTo, repliedBy);
        posts.add(post_to_add);
        postcount ++;
        JsonArrayBuilder builder = Json.createArrayBuilder();
        for(int a = 0; a < posts.size(); a++){
            Post post = posts.get(a);
            builder.add(buildPostIgnoreEncoding(post.getPostnum(), post.getThread(), post.getNumInThread(), post.getName(), post.getDate(), post.getSubject(), post.getTripcode(), post.getMessage(), post.getPics(), post.isOP(), post.getRepliedPosts(), post.getReplies()));
        }
        StringWriter string_writer = new StringWriter();
        JsonWriter writer = Json.createWriter(string_writer);
        writer.writeObject(Json.createObjectBuilder().add("posts", builder.build()).build());
        writer.close();
        json = string_writer.toString();
        builder = Json.createArrayBuilder();
        Post oppost = posts.get(0);
        builder.add(buildPostIgnoreEncoding(oppost.getPostnum(), oppost.getPostnum(), oppost.getNumInThread(), oppost.getName(), oppost.getDate(), oppost.getSubject(), oppost.getTripcode(), oppost.getMessage(), oppost.getPics(), true, oppost.getRepliedPosts(), oppost.getReplies()));
            for(int a = postcount > 4 ? posts.size()-3 : 1; a < posts.size(); a++){
                Post json_post = posts.get(a);
                builder.add(buildPostIgnoreEncoding(json_post.getPostnum(), json_post.getThread(), json_post.getNumInThread(), json_post.getName(), json_post.getDate(), json_post.getSubject(), json_post.getTripcode(), json_post.getMessage(), json_post.getPics(), false, json_post.getRepliedPosts(), json_post.getReplies()));
            }
        string_writer = new StringWriter();
        writer = Json.createWriter(string_writer);
        writer.write(Json.createObjectBuilder().add("posts", builder.build()).add("missed_posts", Math.max(postcount-4, 0)).build());
        writer.close();
        prev_json = string_writer.toString();
    }
    
    public void removePost(Post post_to_remove){
        /*if(json.json.equals("null"))
            return;
        JsonArrayBuilder builder = Json.createArrayBuilder();
        JsonReader reader = Json.createReader(new StringReader(json.json));
        ArrayList<JsonObject> thread_posts = new ArrayList<>(reader.readObject().getJsonArray("posts").getValuesAs(JsonObject.class));
        reader.close();
        for(int a = 0; a < thread_posts.size(); a++){
            JsonObject array_post = thread_posts.get(a);
            if(!array_post.getString("postnum").equals(post_to_remove.getPostnum()))
                builder.add(buildPostIgnoreEncoding(array_post.getString("postnum"), array_post.getString("parent"), 1, array_post.getString("name"), array_post.getString("date"), array_post.getString("subject"), array_post.getString("trip"), array_post.getString("message"), new ArrayList<JsonObject>(array_post.getJsonArray("pics").getValuesAs(JsonObject.class)), array_post.getString("oppost").equals("true") ? true : false));
        }
        StringWriter string_writer = new StringWriter();
        JsonWriter writer = Json.createWriter(string_writer);
        writer.writeObject(Json.createObjectBuilder().add("posts", builder.build()).build());
        writer.close();
        json.json = string_writer.toString();
        builder = Json.createArrayBuilder();
        reader = Json.createReader(new StringReader(ips_json));
        ArrayList<JsonString> ips = new ArrayList<>(reader.readArray().getValuesAs(JsonString.class));
        reader.close();
        for(int a = 0; a < ips.size(); a++){
            JsonString ip = ips.get(a);
            if(!ip.getString().equals(post_to_remove.getIP()))
                builder.add(ip.getString());
        }
        string_writer = new StringWriter();
        writer = Json.createWriter(string_writer);
        writer.writeArray(builder.build());
        writer.close();
        ips_json = string_writer.toString();*/
        needsFlushing = true;
        posts.remove(post_to_remove);
        ArrayList<JsonObject> paths = post_to_remove.getPics();
            for(int b = 0; b < paths.size(); b++){
            String path = root_path.substring(0, root_path.length()-1) + paths.get(b).getString("full_path");
            //System.out.println("Seeking file " + path);
            if(boards_cache.images_cache.containsKey(path)){
                boards_cache.images_cache.remove(path);
            }else{
                File file = new File(path);
                if(file.exists()){
                    file.delete();
                }
            }
            path = root_path.substring(0, root_path.length()-1) + paths.get(b).getString("thumb_path");
            //System.out.println("Seeking file " + path);
            if(boards_cache.images_cache.containsKey(path)){
                boards_cache.images_cache.remove(path);
            }else{
                File file = new File(path);
                if(file.exists()){
                    file.delete();
                }
            }
        }
        JsonArrayBuilder builder = Json.createArrayBuilder();
        for(int a = 0; a < posts.size(); a++){
            Post post = posts.get(a);
            builder.add(buildPostIgnoreEncoding(post.getPostnum(), post.getThread(), post.getNumInThread(), post.getName(), post.getDate(), post.getSubject(), post.getTripcode(), post.getMessage(), post.getPics(), post.isOP(), post.getRepliedPosts(), post.getReplies()));
        }
        StringWriter string_writer = new StringWriter();
        JsonWriter writer = Json.createWriter(string_writer);
        writer.writeObject(Json.createObjectBuilder().add("posts", builder.build()).build());
        writer.close();
        json = string_writer.toString();
        for(int a = 0; a < posts.size(); a++){
            posts.get(a).setNumInThread(a+1);
        }
        postcount --;
        builder = Json.createArrayBuilder();
        Post oppost = posts.get(0);
        builder.add(buildPostIgnoreEncoding(oppost.getPostnum(), oppost.getPostnum(), oppost.getNumInThread(), oppost.getName(), oppost.getDate(), oppost.getSubject(), oppost.getTripcode(), oppost.getMessage(), oppost.getPics(), true, oppost.getRepliedPosts(), oppost.getReplies()));
            for(int a = postcount > 4 ? posts.size()-3 : 1; a < posts.size(); a++){
                Post json_post = posts.get(a);
                builder.add(buildPostIgnoreEncoding(json_post.getPostnum(), json_post.getThread(), json_post.getNumInThread(), json_post.getName(), json_post.getDate(), json_post.getSubject(), json_post.getTripcode(), json_post.getMessage(), json_post.getPics(), false, json_post.getRepliedPosts(), json_post.getReplies()));
            }
        string_writer = new StringWriter();
        writer = Json.createWriter(string_writer);
        writer.write(Json.createObjectBuilder().add("posts", builder.build()).build());
        writer.close();
        prev_json = string_writer.toString();
    }
    
    public int getPostcount(){
        return postcount;
    }
    
    public Post getPost(int num){
        return posts.get(num);
    }
    
    public ArrayList<Post> getPosts(){
        return posts;
    }
    
    public ArrayList<Post> getPostsToDisplay(){
        if(postcount < 5){
            return posts;
        }
        ArrayList<Post> ret = new ArrayList<>();
        ret.add(posts.get(0));
        ret.add(posts.get(postcount-3));
        ret.add(posts.get(postcount-2));
        ret.add(posts.get(postcount-1));
        return ret;
    }
    
    public boolean isBumplimit(){
        return bumplimit;
    }
    
    private JsonObject buildPostIgnoreEncoding(
    String num, String parent, int nit, String name, String date, String subject, String trip, String message, ArrayList<JsonObject> pics, boolean op, ArrayList<String> repliesTo, ArrayList<String> repliedBy
    ){
        JsonArrayBuilder pics_array = Json.createArrayBuilder();
        for(int a = 0; a < pics.size(); a++)
            pics_array.add(pics.get(a));
        JsonArrayBuilder repliesTo_array = Json.createArrayBuilder();
        for(int a = 0; a < repliesTo.size(); a++)
            repliesTo_array.add(repliesTo.get(a));
        JsonArrayBuilder repliedBy_array = Json.createArrayBuilder();
        for(int a = 0; a < repliedBy.size(); a++)
            repliedBy_array.add(repliedBy.get(a));
        return Json.createObjectBuilder()
            .add("postnum", num)
            .add("nit", String.valueOf(nit))
            .add("parent", parent)
            .add("name", name)
            .add("subject", subject)
            .add("date", date)
            .add("trip", trip)
            .add("message", message)
            .add("pics", pics_array.build())
            .add("oppost", op ? "true" : "false")
            .add("replies_to", repliesTo_array.build())
            .add("replied_by", repliedBy_array.build())
            .build();
    }
}