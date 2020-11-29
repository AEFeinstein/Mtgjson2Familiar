package com.gelakinetic.mtgJson2Familiar;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class CompRulesScraper {
    void GetLatestRules() throws NullPointerException {
        /* One big line to get the webpage, then the element, then the attribute for the comprehensive rules url */
        String url = NetUtils.ConnectWithRetries("https://magic.wizards.com/en/game-info/gameplay/rules-and-formats/rules")
                .getElementsByAttributeValueContaining("href", "txt").get(0).attr("href");

        /* Pick the date out of the link */
        String dateSubStr = url.substring(url.length() - 12, url.length() - 4);
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(Integer.parseInt(dateSubStr.substring(0, 4)),
                Integer.parseInt(dateSubStr.substring(4, 6)) - 1,
                Integer.parseInt(dateSubStr.substring(6, 8)));
        String dateStr = GetRfc822Date(cal.getTime());

        downloadRules(url, cal);
    }

    void downloadRules(String rulesUrl, Calendar date) {
        StringBuilder compRules = new StringBuilder();
        StringBuilder problematicLines = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new URL(rulesUrl).openStream()))) {
            /* Read the file, one line at a time */
            String line;
            boolean addLines = false;
            while ((line = br.readLine()) != null) {
                /* Clean and write the line */
                line = NetUtils.removeNonAscii(line);

                if (line.equals("Credits")) {
                    if (!addLines) {
                        compRules.append("RULES_VERYLONGSTRINGOFLETTERSUNLIKELYTOBEFOUNDINTHEACTUALRULES");
                        addLines = true;
                    } else {
                        compRules.append("EOF_VERYLONGSTRINGOFLETTERSUNLIKELYTOBEFOUNDINTHEACTUALRULES\n");
                        addLines = false;
                    }
                } else if ("Glossary".equals(line)) {
                    if (addLines) {
                        compRules.append("GLOSSARY_VERYLONGSTRINGOFLETTERSUNLIKELYTOBEFOUNDINTHEACTUALRULES\n");
                    }
                } else if (addLines) {
                    compRules.append(line).append("\n");
                    /* If the line still has any non-ascii chars, note it */
                    if (line.matches(".*[^\\x00-\\x7F].*")) {
                        problematicLines.append(line).append("\n");
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("rules/MagicCompRules.txt"), StandardCharsets.UTF_8))) {
            bw.write(GetEmbeddedDate(date));
            bw.write("\n\n");
            bw.write(compRules.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.exit(0);
    }

    public static String GetRfc822Date(Date time) {
        return new SimpleDateFormat("EEE', 'dd' 'MMM' 'yyyy' 'HH:mm:ss' 'Z", Locale.US).format(time);
    }

    public static String GetEmbeddedDate(Calendar time) {

        return String.format("%04d-%02d-%02d", time.get(Calendar.YEAR), time.get(Calendar.MONTH), time.get(Calendar.DAY_OF_MONTH));
    }
}
