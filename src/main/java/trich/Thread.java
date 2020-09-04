package trich;

import java.util.*;
import java.io.*;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;
import org.springframework.beans.factory.annotation.Autowired;
import javax.json.*;
import javax.persistence.*;
import trich.*;
import rootContextBeans.BoardsCache;

@Entity
@Table(name = "threads")
public class Thread{
    
    @OneToMany(fetch = FetchType.LAZY, orphanRemoval = true, cascade = CascadeType.ALL)
    @JoinColumn(name = "thread")
    private List<Post> posts = new ArrayList<>();
    
    @Column(name = "num")
    private String num;
    
    @Column(name = "postcount")
    private int postcount;
    
    @ManyToOne
    @JoinColumn(name = "board")
    private Board board;
    
    @Column(name = "`isSticky`")
    private boolean isSticky;
    
    @Column(name = "`isClosed`")
    private boolean isClosed;
    
    @Transient private String json;
    
    @Transient private String prev_json;
    
    @Column(name = "title")
    private String title;
    
    @Transient @Autowired private BoardsCache boardsCache;
    
    @Id
    @Column(name = "`globalID`")
    private long id;
    
    public Thread(){postcount = 0;}
    
    public void addPost(Post postToAdd){
        postToAdd.setNumInThread(postcount+1);
        if(postcount == 0){
            num = postToAdd.getPostnum();
            String message = postToAdd.getMessage();
            if(message.length() > 50)
                title = message.substring(0, 50).trim() + "...";
            else
                title = message;
        }
        posts.add(postToAdd);
        postcount = postcount + 1;
        System.out.println("Post added. Thread postcount = " + postcount);
        this.rebuildJSON();
        board.bumpThread(this);
    }
    
    public void removePost(Post postToRemove){
        if(postToRemove == posts.get(0))
            return;
        posts.remove(postToRemove); // TODO remove pics files on post deletion
        postcount --;
        this.rebuildJSON();
        board.setTotalPosts(board.getTotalPosts()-1L);
    }
    
    public int getPostcount(){
        return postcount;
    }
    
    public Post getPost(int num){
        return posts.get(num);
    }
    
    public List<Post> getPosts(){
        return Collections.unmodifiableList(posts);
    }
    
    public List<Post> getPostsToDisplay(){
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
    
    public String getNum(){
        return num;
    }
    
    public void setNum(String num_){
        num = num_;
    }
    
    public Board getBoard(){
        return board;
    }
    
    public void setBoard(Board board_){
        board = board_;
    }
    
    public String getTitle(){
        return title;
    }
    
    private void rebuildJSON(){
        JsonArrayBuilder builder = Json.createArrayBuilder();
        for(int a = 0; a < posts.size(); a++){
            Post post = posts.get(a);
            builder.add(buildPostIgnoreEncoding(post.getPostnum(),
            num,
            post.getName(),
            post.getDate(),
            post.getSubject(),
            post.getTripcode(),
            post.getMessage(),
            post.getPics(),
            post.isOP(),
            post.getRepliedPosts(),
            post.getReplies()));
        }
        json = Json.createObjectBuilder().add("posts", builder.build()).build().toString();
        builder = Json.createArrayBuilder();
        Post oppost = posts.get(0);
        builder.add(buildPostIgnoreEncoding(oppost.getPostnum(), oppost.getPostnum(), oppost.getName(), oppost.getDate(), oppost.getSubject(), oppost.getTripcode(), oppost.getMessage(), oppost.getPics(), true, oppost.getRepliedPosts(), oppost.getReplies()));
            for(int a = postcount > 4 ? posts.size()-3 : 1; a < posts.size(); a++){
                Post json_post = posts.get(a);
                builder.add(buildPostIgnoreEncoding(json_post.getPostnum(), json_post.getThread().getNum(), json_post.getName(), json_post.getDate(), json_post.getSubject(), json_post.getTripcode(), json_post.getMessage(), json_post.getPics(), false, json_post.getRepliedPosts(), json_post.getReplies()));
            }
        prev_json = Json.createObjectBuilder().add("posts", builder.build()).build().toString();
    }
    
    private JsonObject buildPostIgnoreEncoding(String num,
                                               String parent,
                                               String name,
                                               String date,
                                               String subject,
                                               String trip,
                                               String message,
                                               List<CachedImage> pics,
                                               boolean op,
                                               List<Post> repliesTo,
                                               List<Post> repliedBy){
        JsonArrayBuilder pics_array = Json.createArrayBuilder();
        for(int a = 0; a < pics.size(); a++)
            pics_array.add(pics.get(a).getPath());
        JsonArrayBuilder repliesTo_array = Json.createArrayBuilder();
        for(int a = 0; a < repliesTo.size(); a++)
            repliesTo_array.add(repliesTo.get(a).getPostnum());
        JsonArrayBuilder repliedBy_array = Json.createArrayBuilder();
        for(int a = 0; a < repliedBy.size(); a++)
            repliedBy_array.add(repliedBy.get(a).getPostnum());
        return Json.createObjectBuilder()
            .add("postnum", num)
            .add("parent", parent)
            .add("name", name)
            .add("subject", subject)
            .add("date", date)
            .add("trip", trip)
            .add("message", message)
            .add("pics", pics_array.build())
            .add("oppost", String.valueOf(op))
            .add("replies_to", repliesTo_array.build())
            .add("replied_by", repliedBy_array.build())
            .build();
    }
    
    public String getJSON(){
        if(json != null && !json.equals(""))
            return json;
        this.rebuildJSON();
        return json;
    }
    
    public String getPrevJSON(){
        if(json != null && !prev_json.equals(""))
            return prev_json;
        this.rebuildJSON();
        return prev_json;
    }
    
}