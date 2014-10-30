package test;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import twitter4j.Status;
import twitter4j.examples.timeline.GetUserTimeline;

public class Test2 {
    private static final String PATH = "C:/Users/sunx2013/Desktop";
    public static void main(String[] args){
        HashMap<Long, UserData> idToUser =  recoverTweets("D:/Twitter/userdata/"+"ud16.ser");
        for (Entry<Long, UserData> en: idToUser.entrySet()){
            for (Status t: en.getValue().tweets){
                if (t.isRetweet()){
                    System.out.println(t.getText());
                    System.out.println(t.getSource());
                    final Status ret = t.getRetweetedStatus();
                    System.out.println(ret.getText());
                    System.out.println(t.getSource());
                }

                
            }
        }
        //GetUserTimeline.main(new String[] {"USATODAY"});
        //ArrayList<Status> tweets = recoverTweets("/tweets1.ser");
        try
        {
           ArrayList<String> allTweets = new ArrayList<String>();
           allTweets.add("aaa");
           allTweets.add("bbb");
           FileOutputStream fileOut =
           new FileOutputStream(PATH+"/aaa.ser");
           ObjectOutputStream out = new ObjectOutputStream(fileOut);
           out.writeObject(allTweets);
           out.close();
           fileOut.close();
           System.out.printf("Serialized data is saved in /tweets.ser, count is "+allTweets.size());
        }catch(IOException i)
        {
            i.printStackTrace();
            System.exit(-1);
        }
    }
    private static HashMap<Long, UserData> recoverTweets(String fileName){
        HashMap<Long, UserData> tweets = null;
        ObjectInputStream in= null;
        try
        {
           System.out.printf("Reading tweets from " +fileName);
           FileInputStream fileIn =
           new FileInputStream(fileName);
           BufferedInputStream bIn = new BufferedInputStream(fileIn);
           in = new ObjectInputStream(bIn);
           tweets = (HashMap<Long, UserData>)in.readObject();
           System.out.println("Reading finished");
        }catch(IOException | ClassNotFoundException i)
        {
            i.printStackTrace();
        }finally{
            if (in != null){
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
        return tweets;
    }
}
