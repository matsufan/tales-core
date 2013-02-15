package tales.utils;




public class Average{


	
	
    public int numFields;
    private float[] numbers;


    
    
    public Average(int numFields){
        this.numFields = numFields;
        numbers = new float[numFields];
    }

    
    

    public void add(float num){

        // REMOVE THE LAST
        float temp[] = new float[numbers.length];
        for(int i = 1; i < numbers.length; i++){
            temp[i-1] = numbers[i];
        }

        numbers = temp;
        numbers[numbers.length - 1] = num;
    }

    
    

    public float getAverage(){
        float aver = 0;
        for(int i = 0; i < numbers.length; i++){
            aver += numbers[i];
        }

        return aver/numbers.length;
    }
    
}
