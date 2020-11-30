package com.gelakinetic.mtgJson2Familiar;

import org.jsoup.nodes.Document;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;

public class CompRulesScraper {

    /**
     * Check if there are newer comprehensive rules than what we have.
     * If there are, download them
     *
     * @return true if the rules were updated, or didn't need updating, false if there was an error
     */
    boolean GetLatestRules() {
        try {
            //  One big line to get the webpage, then the element, then the attribute for the comprehensive rules url
            Document rulePage = NetUtils.ConnectWithRetries("https://magic.wizards.com/en/game-info/gameplay/rules-and-formats/rules");
            if (null == rulePage) {
                System.err.println("Couldn't connect to rule page");
                return false;
            }
            String url = rulePage.getElementsByAttributeValueContaining("href", "txt").get(0).attr("href");

            //  Pick the date out of the link 
            String dateSubStr = url.substring(url.length() - 12, url.length() - 4);
            Calendar cal = Calendar.getInstance();
            cal.clear();
            //noinspection MagicConstant
            cal.set(Integer.parseInt(dateSubStr.substring(0, 4)),
                    Integer.parseInt(dateSubStr.substring(4, 6)) - 1,
                    Integer.parseInt(dateSubStr.substring(6, 8)));
            String date = GetEmbeddedDate(cal);

            // Check the date from the URL and the date in the last parsed rules
            try (BufferedReader br = new BufferedReader(new FileReader(new File(Filenames.RULES_DIR, Filenames.COMP_RULES), StandardCharsets.UTF_8))) {
                String lastKnownDate = br.readLine();
                if (lastKnownDate.equals(date)) {
                    // Dates match, so don't update anything
                    System.out.println("No new comprehensive rules");
                    return true;
                }
            } catch (IOException e) {
                System.err.println("Couldn't read date from older rules. Downloading new rules.");
                e.printStackTrace();
            }

            // Download the rules
            return downloadRules(url, cal);

        } catch (Exception e) {
            System.err.println("Error scraping rules");
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Download, parse, clean, and write the comprehensive rules
     *
     * @param rulesUrl The URL for the rules to download
     * @param date     The date these rules are valid from
     * @return true if the rules were downloaded and written, false otherwise
     */
    boolean downloadRules(String rulesUrl, Calendar date) {
        // Use these to build strings
        StringBuilder compRules = new StringBuilder();
        StringBuilder problematicLines = new StringBuilder();

        // Download the rules, one line at a time
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new URL(rulesUrl).openStream()))) {
            // Don't initially add lines
            boolean addLines = false;
            //  Read the file, one line at a time 
            String line;
            while ((line = br.readLine()) != null) {
                //  Clean the line
                line = NetUtils.removeNonAscii(line);

                // See what to do with the line
                if ("Credits".equals(line)) {
                    if (!addLines) {
                        // When "Credits" is seen the first time, append this marker and start adding lines
                        compRules.append("RULES_VERYLONGSTRINGOFLETTERSUNLIKELYTOBEFOUNDINTHEACTUALRULES");
                        addLines = true;
                    } else {
                        // When "Credits" is seen the second time, append this marker and stop adding lines
                        compRules.append("EOF_VERYLONGSTRINGOFLETTERSUNLIKELYTOBEFOUNDINTHEACTUALRULES\n");
                        addLines = false;
                    }
                } else if ("Glossary".equals(line)) {
                    if (addLines) {
                        // When "Glossary" is seen when adding lines, replace it with this marker
                        compRules.append("GLOSSARY_VERYLONGSTRINGOFLETTERSUNLIKELYTOBEFOUNDINTHEACTUALRULES\n");
                    }
                } else if (addLines) {
                    // Add this line to the output
                    compRules.append(line).append("\n");
                    // If the line still has any non-ascii chars, note it
                    if (line.matches(".*[^\\x00-\\x7F].*")) {
                        problematicLines.append(line).append("\n");
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error parsing rules");
            e.printStackTrace();
            return false;
        }

        // If there are any problematic lines, print them and return
        if (!problematicLines.toString().isEmpty()) {
            System.err.println("Problematic lines in comp rules:\n" + problematicLines.toString());
            return false;
        }

        // If there were no errors or problematic lines
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(Filenames.RULES_DIR, Filenames.COMP_RULES)), StandardCharsets.UTF_8))) {
            // Write the rules to the file
            bw.write(GetEmbeddedDate(date));
            bw.write("\n\n");
            bw.write(compRules.toString());
        } catch (IOException e) {
            System.err.println("Error writing rules");
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * @param time The time to format
     * @return A String containing the year, month, and date for this Calendar object
     */
    public static String GetEmbeddedDate(Calendar time) {
        return String.format("%04d-%02d-%02d", time.get(Calendar.YEAR), time.get(Calendar.MONTH), time.get(Calendar.DAY_OF_MONTH));
    }
}
