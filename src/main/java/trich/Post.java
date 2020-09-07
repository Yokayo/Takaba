package trich;

import java.util.*;
import javax.json.*;
import javax.persistence.*;

@Entity
@Table(name = "posts")
public class Post{
    
    @Column(name = "number")
    private String postnum;
    
    @Column(name = "name")
    private String name;
    
    @Column(name = "tripcode")
    private String trip;
    
    @Column(name = "date")
    private String date;
    
    @Column(name = "subject")
    private String subject;
    
    @Column(name = "message")
    private String message;
    
    @Column(name = "`IP`")
    private String IP;
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "posts_global_IDs_generator")
    @SequenceGenerator(name = "posts_global_IDs_generator", sequenceName = "`posts_globalID_seq`", allocationSize = 1)
    @Column(name = "`globalID`")
    private long id;
    
    @Column(name = "`numInThread`")
    private int nit;
    
    @ManyToOne
    @JoinColumn(name = "thread")
    private trich.Thread thread;
    
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "`postID`")
    private List<CachedImage> pics; // массив ID картинок
    
    @Transient private boolean isOppost;
    
    @ManyToMany(cascade = CascadeType.MERGE)
    @JoinTable(
        name = "`repliesMap`",
        joinColumns = @JoinColumn(name = "original_post"),
        inverseJoinColumns = @JoinColumn(name = "reply_post")
    )
    private List<Post> repliedBy;
    
    @ManyToMany(mappedBy = "repliedBy")
    private List<Post> isReplyTo;

    public Post(){}
    
    public Post(String postnum_, trich.Thread t, String name_, String trip_, String date_, String subject_, String message_, boolean op, String ip){
        postnum = postnum_;
        name = name_;
        trip = trip_;
        date = date_;
        subject = subject_;
        message = message_;
        isOppost = op;
        thread = t;
        IP = ip;
    }

    public Post(String postnum_, trich.Thread t, String name_, String trip_, String date_, String subject_, String message_, List<CachedImage> pics_, boolean op, String ip, List<Post> repliesTo_, List<Post> repliedBy_){
        postnum = postnum_;
        name = name_;
        trip = trip_;
        date = date_;
        subject = subject_;
        message = message_;
        isOppost = op;
        pics = pics_;
        thread = t;
        IP = ip;
        isReplyTo = repliesTo_;
        repliedBy = repliedBy_;
    }

    public trich.Thread getThread(){
        return thread;
    }
    
    public String getPostnum(){
        return postnum;
    }

    public String getName(){
        return name;
    }

    public String getTripcode(){
        return trip;
    }
    
    public String getDate(){
        return date;
    }

    public String getSubject(){
        return subject;
    }

    public String getMessage(){
        return message;
    }
    
    public int getNumInThread(){
        return nit;
    }
    
    public void setNumInThread(int nit_){
        nit = nit_;
    }
    
    public List<Post> getRepliedPosts(){
        return isReplyTo;
    }
    
    public List<Post> getReplies(){
        return repliedBy;
    }
    
    public void addReply(Post reply){
        if(reply == null)
            return;
        repliedBy.add(reply); // TODO contains check (?)
    }
    
    public void removeReply(Post post){
        repliedBy.remove(post);
    }
    
    public List<CachedImage> getPics(){
        return pics;
    }
    
    public boolean isOP(){
        return isOppost;
    }
    
    public String getIP(){
        return IP;
    }
    
    public void setThread(trich.Thread thread_){
        thread = thread_;
        nit = thread.getPosts().size()+1;
        isOppost = nit == 1;
    }

}