import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.Arrays;
import java.lang.reflect.Array;

//import java.util.Arrays;

/**
 * Created by koningrde on 24-3-2016.
 */


class EventProfiler {

    private long previousTimeStamp = -1;
    private boolean showLog = false;

    public EventProfiler(boolean showLog) {
        this.showLog = showLog;
    }

    public void logOff() {
        this.showLog = false;
    }

    public void logOn() {
        this.showLog = true;
    }

    public long start(){
        previousTimeStamp = System.nanoTime();
        return previousTimeStamp;
    }

    public long log(String label)
    {
        long thisTimeStamp = System.nanoTime();
        long duration = thisTimeStamp - previousTimeStamp;

        if (showLog)
            System.out.println(label + " takes "+ duration/1e6 + " ms");

        previousTimeStamp = thisTimeStamp;

        return previousTimeStamp;
    }
}

class Utils {

    // source: http://stackoverflow.com/questions/1519736/random-shuffling-of-an-array
    // Implementing Fisher�Yates shuffle
    public static void shuffleArray(int[] ar) {
        // If running on Java 6 or older, use `new Random()` on RHS here
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        for (int i = ar.length - 1; i > 0; i--) {
            int index = rnd.nextInt(i + 1);
            // Simple swap
            int a = ar[index];
            ar[index] = ar[i];
            ar[i] = a;
        }
    }

    public static void printArray(int[] anArray) {
        System.out.print("Array: ");
        for (int i=0; i< anArray.length; i++){
            System.out.print(anArray[i]+" ");
        }
        System.out.println();
    }


    public static int[] fillArray(int amount) {
        int[] result = new int[amount];
        for (int i=0; i<amount; i++){
            result[i] = i;
        }
        return result;
    }

    public static void addValue(int[] anArray, int value) {
        for (int i=0; i<anArray.length; i++){
            anArray[i] += value;
        }
    }




    //Modication 4, this is the bubble sort class
    //@SuppressWarnings({ "rawtypes", "unchecked" })
    public static void bubbleSort(int[] anArray, int fromIndex, int toIndex)
    {
        int d;
        for (int i = toIndex - 1; i > fromIndex; i--)
        {
            boolean isSorted = true;
            for (int j = fromIndex; j < i; j++){
                //If elements in wrong order then swap them
                if (((Comparable) anArray[j]).compareTo(anArray[j + 1]) > 0)
                {
                    isSorted = false;
                    d = anArray[j + 1];
                    anArray[j + 1] = anArray[j];
                    anArray[j] = d;
                }
            }
            //If no swapping then array is already sorted
            if (isSorted) {
                break;
            }
        }
    }
}

class MinimumInRangeRunner implements Runnable {

    private int[] anArray;
    private int fromIndex;
    private int tillIndex;


    public MinimumInRangeRunner(int[] anArray, int fromIndex, int tillIndex) {
        // store parameters for later use
        this.anArray = anArray;
        this.fromIndex = fromIndex;
        this.tillIndex = tillIndex;
    }

    @Override
    public void run() {
        int minimum = Integer.MAX_VALUE;
        int index = -1;

        minimum = Integer.MAX_VALUE;
        for (int i=fromIndex; i<tillIndex; i++) {
            if (anArray[i] < minimum) {
                index = i;
                minimum = anArray[index];
            }
        }
       // System.out.println("Local min " + localMin);
        ParallelMin.updateMin(minimum);
    }
}


class MinimumOnDeltaRunner implements Runnable {

    private int[] anArray;
    private int fromIndex;
    private int delta;

    public MinimumOnDeltaRunner(int[] anArray, int fromIndex, int delta) {
        this.fromIndex = fromIndex;
        this.delta = delta;
        this.anArray = anArray;
    }

    @Override
    public void run() {
        int minimum = Integer.MAX_VALUE;
        int index = -1;
        int tillIndex = anArray.length;

        for (int i=fromIndex; i<tillIndex; i+=delta) {
            if (anArray[i] < minimum) {
                index = i;
                minimum = anArray[index];
            }
        }
        System.out.println("Local min with " + fromIndex + " and delta " + delta + " is " + minimum);
        ParallelMin.updateMin(minimum);
    }
}




public class ParallelMin {


    private int count = 0;

    public static int globalMin;

    public synchronized  static void updateMin(int value)
    {
        if (value < globalMin)
            globalMin = value;
    }


    static <T> T[] joinArrayGeneric(T[]... arrays) {
        int length = 0;
        for (T[] array : arrays) {
            length += array.length;
        }

        //T[] result = new T[length];
        final T[] result = (T[]) Array.newInstance(arrays[0].getClass().getComponentType(), length);

        int offset = 0;
        for (T[] array : arrays) {
            System.arraycopy(array, 0, result, offset, array.length);
            offset += array.length;
        }

        return result;
    }

    static int[] joinArray(int[]... arrays) {
        int length = 0;
        for (int[] array : arrays) {
            length += array.length;
        }

        final int[] result = new int[length];

        int offset = 0;
        for (int[] array : arrays) {
            System.arraycopy(array, 0, result, offset, array.length);
            offset += array.length;
        }

        return result;
    }

    public static void main(String[] args) throws InterruptedException {

        // int[] anArraySorted;

        // commandline parameter settings -Xms8096m -Xint




        EventProfiler profiler = new EventProfiler(true);

        profiler.start();

        int[] anArray = Utils.fillArray((int) 1e3);
        //anArray = Utils.printArray();
        profiler.log("Filling array");

        //Utils.addValue(anArray, -10);
        profiler.log("Adding value -10");

        Utils.shuffleArray(anArray);

        //1st modification, printed the filled array here.
        //Utils.printArray(anArray);


        profiler.log("Shuffle array");

        profiler.start();
        ParallelMin.globalMin = Integer.MAX_VALUE;
        new MinimumInRangeRunner(anArray, 0, anArray.length).run();
        profiler.log("Finding minimum serial "+ ParallelMin.globalMin);

        List<Runnable> runners = new ArrayList<Runnable>();

        int processors = Runtime.getRuntime().availableProcessors();
        System.out.println("Processors " + processors);

        for (int cuts = 1; cuts <= processors; cuts++) {

            runners.clear();

            profiler.start();
            // problem decomposition, create tasks
            for (int i = 0; i < cuts; i++) {
                // bad memory locality
                runners.add(new MinimumOnDeltaRunner(anArray, i, cuts));
            }
            profiler.log("task creation ");

            globalMin = Integer.MAX_VALUE;
            // start threads
            ExecutorService executor = Executors.newCachedThreadPool();

            runners.forEach(executor::submit);

            executor.shutdown();
            if (executor.awaitTermination(1, TimeUnit.DAYS))
                profiler.log(cuts + " threads done " + globalMin);
            System.out.println();



            //Here we retrieve the size of the anArray
            int n = anArray.length/4;
            System.out.println("divisor size :" +n);
            int l = anArray.length;
            System.out.println("Array size :" +l);


            // Now test arrays , 4 arrays each starting at 0 and going towards n
            int[] array11 = Arrays.copyOfRange(anArray, 0, n);
            int[] array22 = Arrays.copyOfRange(anArray, n, n + n);
            int[] array33 = Arrays.copyOfRange(anArray, n + n, n * 3);
            int[] array44 = Arrays.copyOfRange(anArray, n * 3, n * 4);




            if (cuts == 1) {


                //This is the original array
                System.out.println("--------This is the original unsorted, undivided array which everything starts with-----");
                Utils.printArray(anArray);
                System.out.println("--------This is the original unsorted, undivided array which everything starts with-----");


                System.out.println(" These are arrays we think of sorting in the next loop, these were divided from the original array above");
                System.out.println(" Array #1:");
                Utils.printArray(array11);
                System.out.println(" Array #2:");
                Utils.printArray(array22);
                System.out.println(" Array #3:");
                Utils.printArray(array33);
                System.out.println(" Array #4:");
                Utils.printArray(array44);

                System.out.println("------------------------------------------------This is a delimiter with no purpose ------------------------------------------------------------------");
                //bubble sorts the array 1 that was divided from the primary array
                Utils.bubbleSort(array11,0,array11.length);
                System.out.println("This is the bubble sorted array 1:"); Utils.printArray(array11);
            } else if (cuts == 2) {

                //bubble sorts the array 2 that was divided from the primary array
                Utils.bubbleSort(array22,0,array22.length);
                System.out.println("This is the bubble sorted array 2:"); Utils.printArray(array22);

            } else if (cuts == 3) {

                //bubble sorts the array 3 that was divided from the primary array
                Utils.bubbleSort(array33,0,array33.length);
                System.out.println("This is the bubble sorted array 3:"); Utils.printArray(array33);

            } else if (cuts == 4) {

                //bubble sorts the array 2 that was divided from the primary array
                Utils.bubbleSort(array44,0,array44.length);
                System.out.println("This is the bubble sorted array 4:"); Utils.printArray(array44);


                // Merge arrays into one array

                int[] ArraysMerged = joinArray(array11, array22,array33,array44);
                System.out.println( "The joined array" +  Arrays.toString(ArraysMerged));


                // Now bubblesorting the joined array
                Utils.bubbleSort(ArraysMerged,0,ArraysMerged.length);
                System.out.println("This is the joined array got bubble sorted[End Result] :");
                Utils.printArray(ArraysMerged);


            }






            /*
            for (int y=0; y<array11.length;y++){
                System.out.println(array11[y]);
            }

            System.out.println("This is my delimiter_________________________________");

            for (int o=0; o<array22.length;o++){
                System.out.println(array22[o]);
            }

            System.out.println("This is my delimiter_________________________________");

            for (int k=0; k<array33.length;k++){
                System.out.println(array33[k]);
            }

            System.out.println("This is my delimiter__________________________________");

            for (int e=0; e<array44.length;e++){
                System.out.println(array44[e]);
            }

            System.out.println("This is my delimiter___________________________________"); */






















            /*
            // 2nd modification, here I call the bubblesort on array anArray
            // 1st try was that
            // Utils.bubbleSort(anArray, 0, anArray.length);
            // 2nd trial of 2nd modification
            //Utils.bubbleSort(anArray,0,anArray.length);
            Utils.bubbleSort(anArray,0,anArray.length);

            Utils.printArray(anArray); */


            /*

            // Here it divided the array into its first part
            int[] array1 = Arrays.copyOfRange(anArray, 0, 10);
            // Here it divided the array into its second part
            int[] array2 = Arrays.copyOfRange(anArray, 11, 21);

            int[] array3 = Arrays.copyOfRange(anArray, 22, 32);
            int[] array4 = Arrays.copyOfRange(anArray, 32, 43);
            int[] array5 = Arrays.copyOfRange(anArray, 44, 54);
            int[] array6 = Arrays.copyOfRange(anArray, 55, 65);
            int[] array7 = Arrays.copyOfRange(anArray, 66, 76);
            int[] array8 = Arrays.copyOfRange(anArray, 77, 87);
            int[] array9 = Arrays.copyOfRange(anArray, 88, 98);
            int[] array10 = Arrays.copyOfRange(anArray, 99, 109);



            System.out.println("This is the input of the unsorted array anArray[]: " + array1[0] + " " +array1[1] + " "+array1[2] + " "+array1[3] + " " +array1[4] + " " + array1[5] + " " +array1[6] + " "+array1[7] + " "+array1[8] + " " +array1[9]);

            System.out.println("This is the input of the unsorted array anArray[] 2: " + array2[0] + " " + array2[1] + " "+ array2[2] + " " + array2[3] + " " + array2[4] + " " + array2[5] + " " +array2[6] + " "+array2[7] + " "+array2[8] + " " +array2[9] );

            System.out.println("This is the input of the unsorted array anArray[] 3: " + array3[0] + " " + array3[1] + " "+ array3[2] + " " + array3[3] + " " + array3[4] + " " + array3[5] + " " +array3[6] + " "+array3[7] + " "+array3[8] + " " +array3[9] );

            System.out.println("This is the input of the unsorted array anArray[] 4: " + array4[0] + " " + array4[1] + " "+ array4[2] + " " + array4[3] + " " + array4[4] + " " + array4[5] + " " +array4[6] + " "+array4[7] + " "+array4[8] + " " +array4[9] );

            System.out.println("This is the input of the unsorted array anArray[] 5: " + array5[0] + " " + array5[1] + " "+ array5[2] + " " + array5[3] + " " + array5[4] + " " + array5[5] + " " +array5[6] + " "+array5[7] + " "+array5[8] + " " +array5[9] );

            System.out.println("This is the input of the unsorted array anArray[] 6: " + array6[0] + " " + array6[1] + " "+ array6[2] + " " + array6[3] + " " + array6[4] + " " + array6[5] + " " +array6[6] + " "+array6[7] + " "+array6[8] + " " +array6[9] );

            System.out.println("This is the input of the unsorted array anArray[] 7: " + array7[0] + " " + array7[1] + " "+ array7[2] + " " + array7[3] + " " + array7[4] + " " + array7[5] + " " +array7[6] + " "+array7[7] + " "+array7[8] + " " +array7[9] );

            System.out.println("This is the input of the unsorted array anArray[] 8: " + array8[0] + " " + array8[1] + " "+ array8[2] + " " + array8[3] + " " + array8[4] + " " + array8[5] + " " +array8[6] + " "+array8[7] + " "+array8[8] + " " +array8[9] );

            System.out.println("This is the input of the unsorted array anArray[] 9: " + array9[0] + " " + array9[1] + " "+ array9[2] + " " + array9[3] + " " + array9[4] + " " + array9[5] + " " +array9[6] + " "+array9[7] + " "+array9[8] + " " +array9[9] );

            System.out.println("This is the input of the unsorted array anArray[] 10: " + array10[0] + " " + array10[1] + " "+ array10[2] + " " + array10[3] + " " + array10[4] + " " + array10[5] + " " +array10[6] + " "+array10[7] + " "+array10[8] + " " +array10[9] );



            //bubble sorts the array 2 that was divided from the primary array
            Utils.bubbleSort(array5,0,array5.length);
            System.out.println("This is the bubble sorted array 5:"); Utils.printArray(array5);

            //bubble sorts the array 2 that was divided from the primary array
            Utils.bubbleSort(array6,0,array6.length);
            System.out.println("This is the bubble sorted array 6:"); Utils.printArray(array6);


            //bubble sorts the array 3 that was divided from the primary array
            Utils.bubbleSort(array7,0,array7.length);
            System.out.println("This is the bubble sorted array 7:"); Utils.printArray(array7);


            //bubble sorts the array 2 that was divided from the primary array
            Utils.bubbleSort(array8,0,array8.length);
            System.out.println("This is the bubble sorted array 8:"); Utils.printArray(array8);


            //bubble sorts the array 2 that was divided from the primary array
            Utils.bubbleSort(array9,0,array9.length);
            System.out.println("This is the bubble sorted array 9:"); Utils.printArray(array9);

            //bubble sorts the array 2 that was divided from the primary array
            Utils.bubbleSort(array10,0,array10.length);
            System.out.println("This is the bubble sorted array 10:"); Utils.printArray(array10);

             */




/*
            public void doWork() {
                Thread t1 = new Thread(new Runnable() {
                    public void run() {


                        Utils.bubbleSort(anArray,0,anArray.length);
                        System.out.println("This is the array to start with 1: " + anArray.toString(anArray));

                    }
                });

                Thread t2 = new Thread(new Runnable() {
                    public void run() {


                        Utils.bubbleSort(anArray,0,anArray.length);

                        System.out.println("This is the array to start with 5: " + anArray.toString(anArray));

                    }
                });

                Thread t3 = new Thread(new Runnable() {
                    public void run() {



                        Utils.bubbleSort(anArray,0,anArray.length);

                        System.out.println("This is the array to start with 9: " + anArray.toString(anArray));
                    }
                });


                Thread t4 = new Thread(new Runnable() {
                    public void run() {
                        Utils.bubbleSort(anArray,0,anArray.length);

                        System.out.println("This is the array to start with 13: " + anArray.toString(anArray));
                    }
                });

                t1.start();
                t2.start();
                t3.start();
                t4.start();


                try{
                    t1.join();
                    t2.join();
                    t3.join();
                    t4.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }



                //  System.out.println("Count is: " + count);
            }


*/

        }






    }





}
