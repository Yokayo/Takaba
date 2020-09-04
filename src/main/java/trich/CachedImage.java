package trich;

import javax.persistence.*;

@Entity
@Table(name = "images")
public class CachedImage{
    
    @Column(name = "name")
    private String name;
    
    @Column(name = "metadata")
    private String metadata;
    
    @Column(name = "`fullPath`")
    private String path;
    
    @Column(name = "`thumbPath`")
    private String thumbPath;
    
    @Transient private String width, height;
    
    @Id
    private long id;

    public CachedImage(){
    }

    public String getName(){
        return name;
    }

    public void setName(String name_){
        name = name_;
    }

    public String getMetadata(){
        return metadata;
    }

    public void setMetadata(String metadata_){
        metadata = metadata_;
        String dimensions = metadata.split(" ")[1];
        width = dimensions.split("x")[0];
        height = dimensions.split("x")[1];
    }
    
    public String getWidth(){
        return width;
    }
    
    public String getHeight(){
        return height;
    }
    
    public String getPath(){
        return path;
    }
    
    public void setPath(String path_){
        path = path_;
    }
    
    public String getThumbPath(){
        return thumbPath;
    }
    
    public void setThumbPath(String thumbPath_){
        thumbPath = thumbPath_;
    }
    
    public void setId(long id_){
        id = id_;
    }
    
    public long getId(){
        return id;
    }

}