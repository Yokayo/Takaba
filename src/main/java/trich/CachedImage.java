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
    
    @Column(name = "width")
    private String width;

    @Column(name = "height")
    private String height;
    
    @Column(name = "`thumbWidth`")
    private String thumbWidth;

    @Column(name = "`thumbHeight`")
    private String thumbHeight;
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "`images_IDs_generator`")
    @SequenceGenerator(name = "`images_IDs_generator`", sequenceName = "images_id_seq", allocationSize = 1)
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
        height = dimensions.split("x")[1].replaceFirst("\\)", "");
    }
    
    public void setThumbDimensions(int w, int h){
        thumbWidth = String.valueOf(w);
        thumbHeight = String.valueOf(h);
    }
    
    public String getWidth(){
        return width;
    }
    
    public String getHeight(){
        return height;
    }
    
    public String getThumbWidth(){
        return thumbWidth;
    }
    
    public String getThumbHeight(){
        return thumbHeight;
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