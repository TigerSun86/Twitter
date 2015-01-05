package test;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import twitter4j.HashtagEntity;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.examples.timeline.GetUserTimeline;
import util.OReadWriter;

public class Test2 {
    private static final String PATH = "C:/Users/sunx2013/Desktop";
    public static void main(String[] args){
        final File folder = new File(OReadWriter.PATH);
        final File[] fileList = folder.listFiles();
        System.out.println("Old # of users: " + fileList.length);

        //final Twitter twitter = new TwitterFactory().getInstance();

        int sum = 0;
        Status latest = null;
        Status oldest = null;
        for (final File fileEntry : fileList) {
            // Open each file.
            final String fullPath = fileEntry.getAbsolutePath();
            final UserData ud = (UserData) OReadWriter.read(fullPath);

            for(Status t: ud.tweets){
                if(t.isRetweet()){
                    HashtagEntity[] h = t.getHashtagEntities();
                    if(h != null && h.length != 0){
                        System.out.println("helo");
                    }
                }
            }
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
    
    private static void split2EachUser(){
        for (int count = 1; count <= 63; count++) {
            // Open each file.
            final String fileName =
                    OReadWriter.FILE_NAME + count + OReadWriter.EXT;
            final String fullPath = OReadWriter.PATH + fileName;
            @SuppressWarnings("unchecked")
            final HashMap<Long, UserData> idToUser =
                    (HashMap<Long, UserData>) OReadWriter.read(fullPath);
            System.out.println("Begin: "+fileName);
            final Iterator<Entry<Long, UserData>> iter =
                    idToUser.entrySet().iterator();
            while (iter.hasNext()) {
                final UserData ud = iter.next().getValue();
                // Save updated data to file in new folder.
                final String fileName2 = ud.userProfile.getId()+ OReadWriter.EXT;
                final String fullPath2 = OReadWriter.PATH2 + fileName2;
                OReadWriter.write(ud, fullPath2);
                System.out.println("Finished: "+fileName2);
            }
        }
    }
}
