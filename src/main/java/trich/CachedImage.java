package trich;

public class CachedImage{

public String name, extension;
public byte[] data;

public CachedImage(String name_, String ex, byte[] data_){
    data = data_;
    name = name_;
    extension = ex;
}

}