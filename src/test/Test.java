package test;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;

/**
 * FileName:     Test.java
 * @Description: 
 *
 * @author Xunhu(Tiger) Sun
 *         email: TigerSun86@gmail.com
 * @date Oct 15, 2014 4:21:16 PM
 */
public class Test {
	private static final String KEYWORD = "#ClimateChange";
	
	private static final int K = 5;
	private static Date AFTER_DATA = null;
	static {
	    try {
            AFTER_DATA =  new SimpleDateFormat("MM/dd/yyyy").parse("09/01/2014");
        } catch (ParseException e) {
            e.printStackTrace();
        }
	} 
	
	private static class TopAuthor {
        final long authorId;
        int count;
        final String screenName;
        public TopAuthor(long authorId, int count,String screenName) {
            this.authorId = authorId;
            this.count = count;
            this.screenName = screenName;
        }
        @Override
        public String toString(){
            return "[a:"+authorId+",c:"+count+",n:"+screenName+"]";
        }
	}
	
    private static ArrayList<Status> allTweets;
    private static HashMap<Long,Integer> tweetsCounter;
    private static LinkedList<TopAuthor> topKAuthors;
    public static void main(String[] args) throws InterruptedException{
        allTweets = new ArrayList<Status>();
        tweetsCounter = new  HashMap<Long,Integer> ();
        topKAuthors = new LinkedList<TopAuthor>();
        
        Query query = new Query("Giants");
        query.setMaxId(522959740175785984L);
        query.setCount(100);
        
        Date date = null;
        
        Twitter twitter = new TwitterFactory().getInstance();
        boolean isRunning = true;
        while(isRunning){
            try {                
                QueryResult result;

                do {
                    result = twitter.search(query);
                    List<Status> tweets = result.getTweets();
                    for (Status tweet : tweets) {
                        recordTweet(tweet);
                        date = tweet.getCreatedAt();
                    }
                    if (date != null){
                        System.out.println(date.toString());
                    }
                    System.out.println(topKAuthors.toString());
                } while ((query = result.nextQuery()) != null);
                
                if (query == null || (date != null && date.after(AFTER_DATA))){
                    // No more or too old.
                    System.out.println("No more or too old, exit");

                    storeTweets();
                    isRunning = false;
                }
            } catch (TwitterException te) {
                System.out.println("Failed to search tweets: " + te.getMessage());

                storeTweets();
                allTweets = new ArrayList<Status>();
                if (te.exceededRateLimitation()){ // Got limitation.
                    final int seconds = te.getRateLimitStatus().getSecondsUntilReset();
                    System.out.println("Retry in " + seconds);
                    if(seconds>0){
                        Thread.sleep((seconds+1)*1000);
                    }
                } else {
                    isRunning = false;
                }
            }
        }
    }
    
    private static int fileCount = 1;
    private static final String PATH = "C:/WorkSpace/Twitter/data";
    private static void storeTweets(){
        ObjectOutputStream out= null;
        try
        {
           System.out.println("Serialized data is saving in /tweets"+fileCount+".ser, count is "+allTweets.size());
           FileOutputStream fileOut =
           new FileOutputStream(PATH+"/tweets"+fileCount+".ser");
           BufferedOutputStream bOut = new BufferedOutputStream(fileOut);
           out = new ObjectOutputStream(bOut);
           out.writeObject(allTweets);
           System.out.println("Saving finished");
           fileCount++;
        }catch(IOException i)
        {
            i.printStackTrace();
        }finally{
            if (out != null){
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
    }
    
    private static ArrayList<Status> recoverTweets(String fileName){
        ArrayList<Status> tweets = null;
        ObjectInputStream in= null;
        try
        {
           System.out.printf("Reading tweets from " +fileName);
           FileInputStream fileIn =
           new FileInputStream(fileName);
           BufferedInputStream bIn = new BufferedInputStream(fileIn);
           in = new ObjectInputStream(bIn);
           tweets = (ArrayList<Status>)in.readObject();
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
    
    private static void recordTweet(Status tweet){
        allTweets.add(tweet);
        if (tweet.isRetweet()){
         // Count only when this is an original tweet of the user.
            return;
        }
        
        // Increase the counter for the author.
        final Long authorId = new Long(tweet.getUser().getId());
        Integer count = tweetsCounter.get(authorId);
        if (count == null){
            count = new Integer(1);
        } else {
            count = new Integer(count.intValue() + 1);
        }
        tweetsCounter.put(authorId, count);
        
        // Update the top k author's id.
        // Delete it before update.
        for(int i = 0; i < topKAuthors.size(); i++){
            if(topKAuthors.get(i).authorId == authorId.longValue()){
                topKAuthors.remove(i);
                break;
            }
        }
        
        boolean isInserted = false;
        for (int i = 0; i < topKAuthors.size(); i++){
            if (topKAuthors.get(i).count < count.intValue()){
                // Insert.
                topKAuthors.add(i, new TopAuthor(authorId, count,tweet.getUser().getScreenName()));
                if (topKAuthors.size() > K){
                    topKAuthors.removeLast();
                }
                isInserted = true;
                break;
            }
        }
        if(!isInserted && (topKAuthors.size() < K)){ // Not full.
            // Insert.
            topKAuthors.add(new TopAuthor(authorId, count,tweet.getUser().getScreenName()));
        }
        
        // System.out.println("@" + tweet.getUser().getScreenName() + " - " + tweet.getText());
    }
}
