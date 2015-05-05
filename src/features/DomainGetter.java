package features;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private static final String FILE_NAME =
            "file://localhost/C:/WorkSpace/Twitter/data/urlMap.txt";

    private static final HashMap<String, String> URL_MAP =
            new HashMap<String, String>();
    static {
        init();
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
        String domain = ""; // Cannot find domain of url.
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
        String domain = "";
        try {
            String host = new URL(url).getHost();
            domain =
                    InternetDomainName.from(host).topPrivateDomain().toString()
                            .split("\\.")[0];
        } catch (MalformedURLException e) {
            domain = "";
        } catch (IllegalArgumentException e) {
            domain = ""; // Url like http://89.31.102.21/ourplanet/.
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

    public static void main (String[] args) throws Exception {
        rankUrls();
    }
}
