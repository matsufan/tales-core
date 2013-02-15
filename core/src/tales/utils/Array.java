package tales.utils;




import java.util.ArrayList;
import java.util.HashSet;




public final class Array {




    @SuppressWarnings({ "unchecked", "rawtypes" })
	public static ArrayList removeDuplicates(ArrayList array){
        HashSet hashSet = new HashSet(array);
        return new ArrayList(hashSet);
    }
    
}
