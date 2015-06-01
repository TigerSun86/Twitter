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
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.google.common.net.InternetDomainName;
import common.DataReader;

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
    private static final String FILE_NAME2 =
            "file://localhost/C:/WorkSpace/Twitter/data/ShortUrlMap2.txt";
    private static final String FILE_NAME_LONGURL2FILEID =
            "file://localhost/C:/WorkSpace/Twitter/data/LongUrl2FileId.txt";
    private static final String FILE_PATH =
            "file://localhost/C:/WorkSpace/Twitter/data/WebPages/";
    private static final String EXT = ".txt";
    private static final String MAP_FILE_SEPA = " ";

    private static final String SHORTEN_URL = "ShortenUrl";
    private static final String NEW_URL = "NewUrl";
    private static final String TITLE = "Title";
    private static final String ITEM_SEPA = " ";
    private static final String ARTICLE_MARK = "****";
    private static final int INVALID_FILEID = -1;

    private static final String CONTENT_TYPE_TEXT = "text";

    private HashSet<String> visitedShortenUrls = null;
    private BufferedWriter shortenUrlMapFileWriter = null;
    private BufferedWriter longUrl2FileIdWriter = null;
    private HashMap<String, Integer> longUrl2FileIdMap = null;
    private int nextFileId = 0;

    private static final HashMap<String, String> URL_MAP =
            new HashMap<String, String>();
    static {
        // init();
    }

    private static void init () {
        final DataReader in = new DataReader(FILE_NAME);
        while (true) {
            String line = in.nextLine();
            if (line == null) {
                break;
            }
            String[] tmp = line.split(" ");
            String url = tmp[0];
            String domain = "";
            if (tmp.length == 3) {
                domain = tmp[2];
            }
            URL_MAP.put(url, domain);
        }
        in.close();
    }

    public static String getDomain (String url) {
        String domain = UNKNOWN_DOMAIN; // Cannot find domain of url.
        if (URL_MAP.containsKey(url)) {
            domain = URL_MAP.get(url);
        } else {
            String newUrl = getRedirectedUrl(url);
            if (newUrl.isEmpty()) {
                newUrl = getRedirectedUrlManually(url);
            }
            if (!newUrl.isEmpty()) {
                domain = urlToDomain(newUrl);
            }
            URL_MAP.put(url, domain);
            write2File(url, newUrl, domain);
        }
        return domain;
    }

    private static void write2File (String url, String newUrl, String domain) {
        try {
            File file = new File(new URL(FILE_NAME).getPath());
            assert file.exists();
            FileWriter fw = new FileWriter(file.getAbsoluteFile(), true);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(url + " " + newUrl + " " + domain);
            bw.close();
        } catch (IOException e) {
            System.err.println("Cannot find file " + FILE_NAME);
        }
    }

    private static final String L = Pattern.quote("<long-url><![CDATA[");
    private static final String R = Pattern.quote("]]></long-url>");

    private static String getRedirectedUrl (String url) {
        // HTTP GET request
        String redirectedUrl = "";
        try {
            String encoded = URLEncoder.encode(url, "UTF-8");
            String request = "http://api.longurl.org/v2/expand?url=" + encoded;
            HttpURLConnection con =
                    (HttpURLConnection) new URL(request).openConnection();
            // optional default is GET
            con.setRequestMethod("GET");
            // add request header
            con.setRequestProperty("User-Agent", "Mozilla");
            int responseCode = con.getResponseCode();
            if (responseCode == 200) {
                BufferedReader in =
                        new BufferedReader(new InputStreamReader(
                                con.getInputStream()));
                String inputLine;
                StringBuffer response = new StringBuffer();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                Pattern datePatt = Pattern.compile(L + "(.*)" + R);

                Matcher m = datePatt.matcher(response.toString());
                if (m.find()) {
                    redirectedUrl = m.group(1);
                }
            }
        } catch (Exception e) {
            redirectedUrl = "";
        }
        return redirectedUrl;
    }

    private static String getRedirectedUrlManually (String url) {
        String redirectedUrl = "";
        try {
            HttpURLConnection conn =
                    (HttpURLConnection) new URL(url).openConnection();
            conn.setReadTimeout(10000);
            conn.addRequestProperty("Accept-Language", "en-US,en;q=0.8");
            conn.addRequestProperty("User-Agent", "Mozilla");
            conn.addRequestProperty("Referer", "google.com");

            boolean redirect = false;
            // normally, 3xx is redirect
            int status = conn.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) {
                if (status == HttpURLConnection.HTTP_MOVED_TEMP
                        || status == HttpURLConnection.HTTP_MOVED_PERM
                        || status == HttpURLConnection.HTTP_SEE_OTHER)
                    redirect = true;
            }
            if (redirect) {
                // get redirect url from "location" header field
                redirectedUrl = conn.getHeaderField("Location");
            }
        } catch (Exception e) {
            redirectedUrl = "";
        }
        return redirectedUrl;
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

    // For test
    private static void rankUrls () throws IOException {
        BufferedReader in =
                new BufferedReader(new FileReader(new File(
                        new URL(FILE_NAME).getPath())));
        HashMap<String, Integer> count = new HashMap<String, Integer>();
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            String[] urls = inputLine.split(" ");
            if (urls.length == 3 && !urls[2].isEmpty()) {
                String domain = urls[2];
                if (count.containsKey(domain)) {
                    count.put(domain, count.get(domain) + 1);
                } else {
                    count.put(domain, 1);
                }
            }
        }
        in.close();
        ArrayList<RankUrl> rank = new ArrayList<RankUrl>();
        for (Entry<String, Integer> entry : count.entrySet()) {
            rank.add(new RankUrl(entry.getKey(), entry.getValue()));
        }
        Collections.sort(rank);
        for (RankUrl r : rank) {
            System.out.println(r.toString());
        }
    }

    private static class RankUrl implements Comparable<RankUrl> {
        String domain;
        int count;

        public RankUrl(String domain, int count) {
            this.domain = domain;
            this.count = count;
        }

        @Override
        public int compareTo (RankUrl o) {
            if (this.count != o.count) {
                return o.count - this.count;
            } else {
                return this.domain.compareTo(o.domain);
            }
        }

        @Override
        public String toString () {
            return domain + " " + count;
        }
    }

    public static List<String> getWebPages () {
        List<String> pages = new ArrayList<String>();
        File folder = null;
        try {
            folder = new File(new URL(FILE_PATH).getPath());
        } catch (MalformedURLException e1) {
            e1.printStackTrace();
        }
        for (final File fileEntry : folder.listFiles()) {
            try {
                BufferedReader in =
                        new BufferedReader(new FileReader(fileEntry));
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
                pages.add(sb.toString());
            } catch (IOException e) { // If this file has problem, just skip it.
            }
        }
        return pages;
    }

    public static void main2 (String[] args) throws Exception {
        rankUrls();
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
                    System.out.printf(
                            "%s and %d others store in the same file %s%n",
                            shortUrl, line.split(ITEM_SEPA).length - 2,
                            fileName);
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

    private void storeFile (String shortUrl) throws IOException {
        String longUrl = UNKNOWN_URL;
        String domain = UNKNOWN_DOMAIN;
        String fileName = UNKNOWN_FILE;
        boolean allClear = false;
        try {
            HttpURLConnection conn = getUrlConnection(shortUrl);
            longUrl = conn.getURL().toString();
            domain = urlToDomain(longUrl);
            if (conn.getResponseCode() < 400
                    && (conn.getContentType() != null && conn.getContentType()
                            .startsWith(CONTENT_TYPE_TEXT))) {
                // Can find Page, and it's not a url of video.

                if (!longUrl2FileIdMap.containsKey(longUrl)) {
                    // A page hasn't been downloaded.
                    Document doc = Jsoup.parse(downloadWebPage(conn));
                    String title = doc.title().toString();
                    fileName = nextFileId + EXT;
                    writeWebPage2File(shortUrl, longUrl, fileName, title, doc
                            .text().toString());

                    longUrl2FileIdWriter.write(longUrl + MAP_FILE_SEPA
                            + nextFileId + System.lineSeparator());
                    longUrl2FileIdWriter.flush();

                    longUrl2FileIdMap.put(longUrl, nextFileId);
                    nextFileId++;
                } else { // Url has already downloaded.
                    int fileId = longUrl2FileIdMap.get(longUrl);
                    if (fileId != INVALID_FILEID) {
                        fileName = fileId + EXT;
                        // Update the file with shortUrl.
                        writeWebPage2File(shortUrl, null, fileName, null, null);
                    }
                }
                // Got here means page was downloaded well without any
                // exception.
                allClear = true;
            }
        } catch (IOException e) {
            // Read time out.
        }
        if (!allClear && !longUrl.equals(UNKNOWN_URL)) {
            // Expanded url but cannot download it (not found, time out, or it's
            // a video)
            // Mark the file id as invalid.
            longUrl2FileIdWriter.write(longUrl + MAP_FILE_SEPA + INVALID_FILEID
                    + System.lineSeparator());
            longUrl2FileIdWriter.flush();

            longUrl2FileIdMap.put(longUrl, INVALID_FILEID);
        }

        shortenUrlMapFileWriter.write(shortUrl + MAP_FILE_SEPA + longUrl
                + MAP_FILE_SEPA + domain + MAP_FILE_SEPA + fileName
                + System.lineSeparator());
        shortenUrlMapFileWriter.flush();
    }

    private void initVisitedShortenUrls () throws IOException {
        BufferedReader newMapFile =
                new BufferedReader(
                        new FileReader(new URL(FILE_NAME2).getPath()));
        HashSet<String> set = new HashSet<String>();
        String line;
        while ((line = newMapFile.readLine()) != null) {
            String[] urls = line.split(MAP_FILE_SEPA);
            if (urls.length > 0) {
                set.add(urls[0]);
            }
        }
        newMapFile.close();
        visitedShortenUrls = set;
    }

    private void initLongUrl2FileIdMap () throws IOException {
        HashMap<String, Integer> map = new HashMap<String, Integer>();
        BufferedReader in =
                new BufferedReader(new FileReader(new URL(
                        FILE_NAME_LONGURL2FILEID).getPath()));
        String inputLine;
        int maxFileId = -1;
        while ((inputLine = in.readLine()) != null) {
            String[] s = inputLine.split(MAP_FILE_SEPA);
            if (s.length == 2) {
                int fileId = Integer.parseInt(s[1]);
                map.put(s[0], fileId);
                if (maxFileId < fileId) {
                    maxFileId = fileId;
                }
            }
        }
        in.close();
        longUrl2FileIdMap = map;
        // If there's no Id, the initial one will be 0.
        nextFileId = maxFileId + 1;
    }

    private void repair () throws IOException {
        CookieHandler.setDefault(new CookieManager()); // For nytimes.com.

        initVisitedShortenUrls();
        shortenUrlMapFileWriter =
                new BufferedWriter(new FileWriter(
                        new URL(FILE_NAME2).getPath(), true));

        initLongUrl2FileIdMap();
        longUrl2FileIdWriter =
                new BufferedWriter(new FileWriter(new URL(
                        FILE_NAME_LONGURL2FILEID).getPath(), true));

        BufferedReader in =
                new BufferedReader(new FileReader(new URL(FILE_NAME).getPath()));
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            String[] urls = inputLine.split(MAP_FILE_SEPA);
            if (urls.length > 0 && !visitedShortenUrls.contains(urls[0])) {
                String url = urls[0];
                visitedShortenUrls.add(url);
                String longUrl = urls[1];
                if (longUrl.equals(UNKNOWN_URL)) {
                    storeFile(url);
                } else {
                    shortenUrlMapFileWriter.write(inputLine
                            + System.lineSeparator());
                    shortenUrlMapFileWriter.flush();
                }
            }
        }

        in.close();
        shortenUrlMapFileWriter.close();
        longUrl2FileIdWriter.close();
    }

    private void test () throws IOException {
        CookieHandler.setDefault(new CookieManager()); // For nytimes.com.

        initVisitedShortenUrls();
        shortenUrlMapFileWriter =
                new BufferedWriter(new FileWriter(
                        new URL(FILE_NAME2).getPath(), true));

        initLongUrl2FileIdMap();
        longUrl2FileIdWriter =
                new BufferedWriter(new FileWriter(new URL(
                        FILE_NAME_LONGURL2FILEID).getPath(), true));

        BufferedReader in =
                new BufferedReader(new FileReader(new URL(FILE_NAME).getPath()));
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            String[] urls = inputLine.split(MAP_FILE_SEPA);
            if (urls.length > 0 && !visitedShortenUrls.contains(urls[0])) {
                String url = urls[0];
                visitedShortenUrls.add(url);
                storeFile(url);
            }
        }

        in.close();
        shortenUrlMapFileWriter.close();
        longUrl2FileIdWriter.close();
    }

    public static void main (String[] args) throws Exception {
        new DomainGetter().repair();
    }
}
