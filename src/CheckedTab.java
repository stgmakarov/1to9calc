import java.util.Hashtable;

public class CheckedTab {
    public static Hashtable<String,String> htab = new Hashtable<String,String>();

    public void addKey(String k, String v){
        this.htab.put(k,v);
    }

    public boolean checkExists(String k){
        return  this.htab.containsKey(k);
    }

    public long getSize(){
        return htab.size();
    }
}
