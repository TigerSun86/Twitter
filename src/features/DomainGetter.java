package features;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.google.common.net.InternetDomainName;

/**
 * FileName: DomainGetter.java
 * @Description:
 *
 * @author Xunhu(Tiger) Sun
 *         email: sunx2013@my.fit.edu
 * @date May 1, 2015 1:43:57 AM
 */
public class DomainGetter {
    public static final String UNKNOWN_URL = "UNKNOWN_URL";
    public static final String UNKNOWN_DOMAIN = "UNKNOWN_DOMAIN";
    public static final String UNKNOWN_FILE = "UNKNOWN_FILE";

    private static final String FILE_NAME =
            "file://localhost/C:/WorkSpace/Twitter/data/ShortUrlMap.txt";
    private static final String FILE_PATH =
            "file://localhost/C:/WorkSpace/Twitter/data/WebPages/";
    private static final String EXT = ".txt";
    private static final String MAP_FILE_SEPA = ",TIGER_SEPA,";

    private static final String SHORTEN_URL = "ShortenUrl";
    private static final String NEW_URL = "NewUrl";
    private static final String TITLE = "Title";
    private static final String ITEM_SEPA = " ";
    private static final String ARTICLE_MARK = "****";
    private static final int INVALID_FILEID = -1;

    private static final String CONTENT_TYPE_TEXT = "text";

    private BufferedWriter shortenUrlMapFileWriter = null;
    private HashMap<String, String> shortUrl2FileName = null;
    private HashMap<String, String> shortUrl2Domain = null;
    private HashMap<String, Integer> longUrl2FileId = null;
    private int nextFileId = 0;

    private static DomainGetter domainGetter = null;

    public static DomainGetter getInstance () {
        if (domainGetter == null) {
            domainGetter = new DomainGetter();
        }
        return domainGetter;
    }

    private DomainGetter() {
        try {
            initUrlMaps();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getWebPage (String shortUrl) {
        if (!shortUrl2FileName.containsKey(shortUrl)) {
            crawlOneUrl(shortUrl);
        }
        String fileName = shortUrl2FileName.get(shortUrl);
        if (fileName.equals(UNKNOWN_FILE)) {
            return "";
        } else {
            return readWebPageFile(FILE_PATH + fileName);
        }

    }

    public String getDomain (String shortUrl) {
        if (!shortUrl2Domain.containsKey(shortUrl)) {
            return UNKNOWN_DOMAIN;
        }
        return shortUrl2Domain.get(shortUrl);
    }

    public static List<String> getAllWebPages () {
        List<String> pages = new ArrayList<String>();
        File folder = null;
        try {
            folder = new File(new URL(FILE_PATH).getPath());
        } catch (MalformedURLException e1) {
            e1.printStackTrace();
        }
        for (final File fileEntry : folder.listFiles()) {
            try {
                String str =
                        readWebPageFile(fileEntry.toURI().toURL().toString());
                if (!str.isEmpty()) {
                    pages.add(str);
                }
            } catch (MalformedURLException e1) {
            }
        }
        return pages;
    }

    private static String urlToDomain (String url) {
        String domain = UNKNOWN_DOMAIN;
        try {
            String host = new URL(url).getHost();
            domain =
                    InternetDomainName.from(host).topPrivateDomain().toString();
        } catch (MalformedURLException e) {
            domain = UNKNOWN_DOMAIN;
        } catch (IllegalArgumentException e) {
            domain = UNKNOWN_DOMAIN; // Url like http://89.31.102.21/ourplanet/.
        }
        return domain;
    }

    private static String readWebPageFile (String fileName) {
        String result = "";
        try {
            BufferedReader in =
                    new BufferedReader(new FileReader(
                            new URL(fileName).getPath()));
            StringBuilder sb = new StringBuilder();
            boolean pageContent = false;
            String line;
            while ((line = in.readLine()) != null) {
                if (pageContent) {
                    sb.append(line + System.lineSeparator());
                } else {
                    if (line.equals(ARTICLE_MARK)) {
                        pageContent = true;
                    }
                }
            }
            in.close();
            result = sb.toString();
        } catch (IOException e) { // If this file has problem, just skip it.
        }
        return result;
    }

    private HttpURLConnection getUrlConnection (String url) throws IOException {
        URL obj = new URL(url);
        HttpURLConnection conn = (HttpURLConnection) obj.openConnection();
        // default is GET
        conn.setRequestMethod("GET");
        conn.setUseCaches(false);
        conn.setReadTimeout(5000);
        // act like a browser
        conn.setRequestProperty("User-Agent", "Mozilla/5.0");
        conn.setRequestProperty("Accept",
                "text/*,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        conn.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

        // System.out.println("Request URL ... " + url);

        boolean redirect = false;
        // normally, 3xx is redirect
        int status = conn.getResponseCode();
        if (status != HttpURLConnection.HTTP_OK) {
            if (status == HttpURLConnection.HTTP_MOVED_TEMP
                    || status == HttpURLConnection.HTTP_MOVED_PERM
                    || status == HttpURLConnection.HTTP_SEE_OTHER)
                redirect = true;
        }

        // System.out.println("Response Code ... " + status);
        // System.out.println("New URL ... " + conn.getURL());
        if (redirect) {
            // get redirect url from "location" header field
            String newUrl = conn.getHeaderField("Location");

            // get the cookie if need, for login
            String cookies = conn.getHeaderField("Set-Cookie");
            // System.out.println(cookies);

            // open the new connnection again
            conn = (HttpURLConnection) new URL(newUrl).openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Cookie", cookies);
            conn.setUseCaches(false);
            conn.setReadTimeout(5000);
            // act like a browser
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setRequestProperty("Accept",
                    "text/*,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            conn.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
            // System.out.println("Redirect to URL : " + newUrl);
        }
        return conn;
    }

    private void writeWebPage2File (String shortUrl, String newUrl,
            String fileName, String title, String text) throws IOException {
        StringBuilder sb = new StringBuilder();
        File file = new File(new URL(FILE_PATH + fileName).getPath());
        if (!file.exists()) { // Create a new file.
            file.createNewFile();
            sb.append(SHORTEN_URL + ITEM_SEPA + shortUrl
                    + System.lineSeparator());
            sb.append(NEW_URL + ITEM_SEPA + newUrl + System.lineSeparator());
            sb.append(TITLE + ITEM_SEPA + title + System.lineSeparator());
            sb.append(ARTICLE_MARK + System.lineSeparator());
            sb.append(text + System.lineSeparator());
        } else { // Update the file.
            // Read the file and update information, then rewrite the file
            // later.
            assert newUrl == null;
            assert title == null;
            assert text == null;
            boolean hasShort = false;
            BufferedReader in = new BufferedReader(new FileReader(file));
            String line;
            while ((line = in.readLine()) != null) {
                if (!hasShort && line.startsWith(SHORTEN_URL)) {
                    hasShort = true;
                    line += ITEM_SEPA + shortUrl;
                    // System.out.printf(
                    // "%s and %d others store in the same file %s%n",
                    // shortUrl, line.split(ITEM_SEPA).length - 2,
                    // fileName);
                }
                sb.append(line + System.lineSeparator());
            }
            in.close();
        }
        FileWriter fw = new FileWriter(file.getAbsoluteFile(), false);
        BufferedWriter bw = new BufferedWriter(fw);
        bw.write(sb.toString());
        bw.close();
    }

    private String downloadWebPage (HttpURLConnection conn) throws IOException {
        BufferedReader in =
                new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String inputLine;
        StringBuffer html = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            html.append(inputLine);
        }
        in.close();

        return html.toString();
    }

    private void crawlAndStoreInFile (String shortUrl) throws IOException {
        String longUrl = UNKNOWN_URL;
        String domain = UNKNOWN_DOMAIN;
        String fileName = UNKNOWN_FILE;
        try {
            HttpURLConnection conn = getUrlConnection(shortUrl);
            longUrl = conn.getURL().toString();
            domain = urlToDomain(longUrl);
            if (conn.getResponseCode() < 400
                    && (conn.getContentType() != null && conn.getContentType()
                            .startsWith(CONTENT_TYPE_TEXT))) {
                // Can find Page, and it's not a url of video.

                if (!longUrl2FileId.containsKey(longUrl)) {
                    // A page hasn't been downloaded.
                    Document doc = Jsoup.parse(downloadWebPage(conn));
                    String title = doc.title().toString();
                    fileName = nextFileId + EXT;
                    writeWebPage2File(shortUrl, longUrl, fileName, title, doc
                            .text().toString());
                } else { // Url has already downloaded.
                    int fileId = longUrl2FileId.get(longUrl);
                    if (fileId != INVALID_FILEID) {
                        fileName = fileId + EXT;
                        // Update the file with shortUrl.
                        writeWebPage2File(shortUrl, null, fileName, null, null);
                    }
                }
            }
        } catch (IOException e) {
            // Read time out.
        }

        if (!longUrl.equals(UNKNOWN_URL)) {// Update longUrl2FileId.
            if (fileName.equals(UNKNOWN_FILE)) {
                // Expanded url but cannot download it (not found, time out, or
                // it's a video)
                // Mark the file id as invalid.
                longUrl2FileId.put(longUrl, INVALID_FILEID);
            } else if (fileName.equals(nextFileId + EXT)) {
                // it's a new long url.
                longUrl2FileId.put(longUrl, nextFileId);
                nextFileId++;
            } // else written into old file.
        }

        shortUrl2Domain.put(shortUrl, domain);
        shortUrl2FileName.put(shortUrl, fileName);

        shortenUrlMapFileWriter.write(shortUrl + MAP_FILE_SEPA + longUrl
                + MAP_FILE_SEPA + domain + MAP_FILE_SEPA + fileName
                + System.lineSeparator());
        shortenUrlMapFileWriter.flush();
    }

    private void crawlOneUrl (String shortUrl) {
        try {
            CookieHandler.setDefault(new CookieManager()); // For nytimes.com.
            shortenUrlMapFileWriter =
                    new BufferedWriter(new FileWriter(
                            new URL(FILE_NAME).getPath(), true));
            crawlAndStoreInFile(shortUrl);
            shortenUrlMapFileWriter.close();
        } catch (IOException e) {
            e.printStackTrace(); // File exception.
        }
    }

    private void initUrlMaps () throws NumberFormatException, IOException {
        HashMap<String, String> domainMap = new HashMap<String, String>();
        HashMap<String, String> shortMap = new HashMap<String, String>();
        HashMap<String, Integer> longIdMap = new HashMap<String, Integer>();
        BufferedReader in =
                new BufferedReader(new FileReader(new URL(FILE_NAME).getPath()));
        int maxFileId = -1;
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            String[] s = inputLine.split(MAP_FILE_SEPA);
            if (s.length == 4) {
                String su = s[0];
                String lu = s[1];
                String domain = s[2];
                String fname = s[3];

                domainMap.put(su, domain);
                shortMap.put(su, fname);

                if (!lu.equals(UNKNOWN_URL)) {
                    int fileId;
                    if (fname.equals(UNKNOWN_FILE)) {
                        fileId = INVALID_FILEID;
                    } else {
                        fileId =
                                Integer.parseInt(fname.substring(0,
                                        fname.length() - EXT.length()));
                    }
                    longIdMap.put(lu, fileId);
                    if (maxFileId < fileId) {
                        maxFileId = fileId;
                    }
                }
            }
        }
        in.close();

        shortUrl2Domain = domainMap;
        shortUrl2FileName = shortMap;
        longUrl2FileId = longIdMap;
        // If there's no Id, the initial one will be 0.
        nextFileId = maxFileId + 1;
    }
}
