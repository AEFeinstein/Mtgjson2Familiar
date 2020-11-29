package com.gelakinetic.mtgJson2Familiar;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class NetUtils {
    /**
     * A little wrapper function to overcome any network hiccups
     *
     * @param urlStr The URL to get a Document from
     * @return A Document, or null
     */
    public static Document ConnectWithRetries(String urlStr) {
        int retries = 0;
        while (retries < Integer.MAX_VALUE - 1) {
            try {
                // Note to self. If this stops working, wireshark a regular request from chrome and copy the cookie (and other fields)
                return Jsoup
                        .connect(urlStr)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/61.0.3163.100 Safari/537.36")
                        .header("Accept-Encoding", "gzip, deflate")
                        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8")
                        .header("Pragma", "no-cache")
                        .header("Cache-Control", "no-cache")
                        .header("Upgrade-Insecure-Requests", "1")
                        .header("DNT", "1")
                        .header("Accept-Language", "en-US,en;q=0.8")
                        .header("Cookie", "f5_cspm=1234; f5_cspm=1234; BIGipServerWWWNetPool02=4111468810.20480.0000; CardDatabaseSettings=1=en-US; _ga=GA1.2.1294897467.1509075187; _gid=GA1.2.838335687.1510109719; ASP.NET_SessionId=; __utmt=1; __utma=28542179.1294897467.1509075187.1510152850.1510184901.4; __utmb=28542179.1.10.1510184901; __utmc=28542179; __utmz=28542179.1510109911.1.1.utmcsr=(direct)|utmccn=(direct)|utmcmd=(none); f5avr1559183795aaaaaaaaaaaaaaaa=CHILKMHBENHPFFICIBHJKDGFPJAMDMHJJPPNJCEEANLNJMLMJNBKKFELMNEKNKFDHDICANOFDFDHNLJHINLABDKABADNIKGENJNFPFEMGGJPCENBGKLPAFOIBCDONJFM")
                        .timeout(0)
                        .get();
            } catch (Exception e) {
                retries++;
                try {
                    Thread.sleep(1000 * retries);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
        }
        return null;
    }

    /**
     * Replaces known non-ascii chars in a string with ascii equivalents
     *
     * @param line The string to clean up
     * @return The cleaned up string
     */
    static String removeNonAscii(String line) {
        String[][] replacements =
                {{"’", "'"},
                        {"®", "(R)"},
                        {"™", "(TM)"},
                        {"“", "\""},
                        {"”", "\""},
                        {"—", "-"},
                        {"–", "-"},
                        {"‘", "'"},
                        {"â", "a"},
                        {"á", "a"},
                        {"ƒ", "a"},
                        {" ", "a"},
                        {"ú", "u"},
                        {"û", "u"},
                        {"£", "u"},
                        {"Æ", "Ae"},
                        {"æ", "ae"},
                        {"©", "(C)"},
                        {"•", "*"},
                        {"…", "..."},
                        {"ò", "o"}};
        /* Loop through all the known replacements and perform them */
        for (String[] replaceSet : replacements) {
            line = line.replaceAll(replaceSet[0], replaceSet[1]);
        }
        return line;
    }
}
