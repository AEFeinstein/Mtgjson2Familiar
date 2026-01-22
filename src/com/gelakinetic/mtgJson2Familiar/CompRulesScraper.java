package com.gelakinetic.mtgJson2Familiar;

import org.jsoup.nodes.Document;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;

public class CompRulesScraper {

    /**
     * @param time The time to format
     * @return A String containing the year, month, and date for this Calendar object
     */
    public static String GetEmbeddedDate(Calendar time) {
        return String.format("%04d-%02d-%02d", time.get(Calendar.YEAR), time.get(Calendar.MONTH), time.get(Calendar.DAY_OF_MONTH));
    }

    /**
     * Check if there are newer comprehensive rules than what we have.
     * If there are, download them
     *
     * @return true if the rules were updated, or didn't need updating, false if there was an error
     */
    boolean GetLatestRules() {
        m2fLogger.log(m2fLogger.LogLevel.INFO, "Processing comprehensive rules");
        try {
            //  One big line to get the webpage, then the element, then the attribute for the comprehensive rules url
            Document rulePage = NetUtils.ConnectWithRetries("https://magic.wizards.com/en/game-info/gameplay/rules-and-formats/rules");
            if (null == rulePage) {
                m2fLogger.log(m2fLogger.LogLevel.ERROR, "Couldn't connect to rule page");
                return false;
            }
            String url = rulePage.getElementsByAttributeValueContaining("href", "txt").get(0).attr("href");

            //  Pick the date out of the link
            String dateSubStr = url.substring(url.length() - 12, url.length() - 4);
            if ("02109224".equals(dateSubStr)) {
                dateSubStr = "20210924";
            }
            Calendar cal = Calendar.getInstance();
            cal.clear();
            //noinspection MagicConstant
            cal.set(Integer.parseInt(dateSubStr.substring(0, 4)), Integer.parseInt(dateSubStr.substring(4, 6)) - 1, Integer.parseInt(dateSubStr.substring(6, 8)));
            String date = GetEmbeddedDate(cal);

            // Check the date from the URL and the date in the last parsed rules
            try (BufferedReader br = new BufferedReader(new FileReader(new File(Filenames.RULES_DIR, Filenames.COMP_RULES), StandardCharsets.UTF_8))) {
                String lastKnownDate = br.readLine();
                if (lastKnownDate.equals(date)) {
                    // Dates match, so don't update anything
                    m2fLogger.log(m2fLogger.LogLevel.INFO, "No new comprehensive rules");
                    return true;
                }
            } catch (IOException e) {
                m2fLogger.log(m2fLogger.LogLevel.ERROR, "Couldn't read date from older rules. Downloading new rules.");
                m2fLogger.logStackTrace(m2fLogger.LogLevel.ERROR, e);
            }

            // Download the rules
            boolean ruleStatus = downloadRules(url, cal);
            m2fLogger.log(m2fLogger.LogLevel.INFO, "Done processing comprehensive rules (" + ruleStatus + ")");
            return ruleStatus;

        } catch (Exception e) {
            m2fLogger.log(m2fLogger.LogLevel.ERROR, "Error scraping rules");
            m2fLogger.logStackTrace(m2fLogger.LogLevel.ERROR, e);
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
        StringBuilder bestCompRules = new StringBuilder();
        StringBuilder bestProblematicLines = new StringBuilder();
        int minProblematicLines = Integer.MAX_VALUE;

        // Try downloading with each of these charsets because WotC can't make up their mind
        String[] charsets = {
                "UTF-8",
                "Cp1252",
                "UTF-16BE",
                "UTF-16LE",
                "UTF-16",
                "US-ASCII",
                "ISO-8859-1"};

        // For each charset
        for (String charset : charsets) {
            try {
                m2fLogger.log(m2fLogger.LogLevel.DEBUG, "Downloading rules with charset " + charset);
                // Try downloading with the given charset
                // Use these to build strings
                StringBuilder compRules = new StringBuilder();
                StringBuilder problematicLines = new StringBuilder();
                downloadRulesCharset(rulesUrl, compRules, problematicLines, charset);

                if (compRules.length() > 0 && problematicLines.length() < minProblematicLines) {
                    minProblematicLines = problematicLines.length();
                    bestCompRules = new StringBuilder(compRules);
                    bestProblematicLines = new StringBuilder(problematicLines);
                }

                // If there are problematic lines, or no lines
                if (problematicLines.length() > 0 || compRules.length() == 0) {
                    m2fLogger.log(m2fLogger.LogLevel.DEBUG, "charset " + charset + " had " + problematicLines.length() + " problematic lines and " + compRules.length() + " total lines");
                    // Set up to try again with the next charset
                    compRules.setLength(0);
                    problematicLines.setLength(0);
                } else if (problematicLines.length() == 0 && compRules.length() > 0) {
                    m2fLogger.log(m2fLogger.LogLevel.DEBUG, "charset " + charset + " had no problematic lines and " + compRules.length() + " total lines");
                    // All good, break out of the for loop
                    break;
                }
            } catch (IOException e) {
                m2fLogger.log(m2fLogger.LogLevel.ERROR, "Error parsing rules");
                m2fLogger.logStackTrace(m2fLogger.LogLevel.ERROR, e);
                return false;
            }
        }

        // If there are any problematic lines, print them and return
        if (!bestProblematicLines.toString().isEmpty()) {
            m2fLogger.log(m2fLogger.LogLevel.ERROR, "Non-ASCII lines in comp rules:\n" + bestProblematicLines);
            // Carry on with the best rules
        }

        // If there were no errors or problematic lines
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(Filenames.RULES_DIR, Filenames.COMP_RULES)), StandardCharsets.UTF_8))) {
            // Write the rules to the file
            bw.write(GetEmbeddedDate(date).replace("\r", ""));
            bw.write("\n\n");
            bw.write(bestCompRules.toString().replace("\r", ""));
        } catch (IOException e) {
            m2fLogger.log(m2fLogger.LogLevel.ERROR, "Error writing rules");
            m2fLogger.logStackTrace(m2fLogger.LogLevel.ERROR, e);
            return false;
        }
        return true;
    }

    /**
     * @param rulesUrl         The URL for the rules to download
     * @param compRules        The StringBuilder to write rules to
     * @param problematicLines The StringBuilder to write problematic lines to
     * @param charset          The charset to interpret the file with
     */
    private void downloadRulesCharset(String rulesUrl, StringBuilder compRules, StringBuilder problematicLines, String charset) throws IOException {
        // Download the rules, one line at a time
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new URL(NetUtils.escapeUrl(rulesUrl)).openStream(), charset))) {
            // Don't initially add lines
            boolean addLines = false;
            //  Read the file, one line at a time
            String line;
            while ((line = br.readLine()) != null) {

                //  Clean the line
                line = NetUtils.removeNonAscii(line).trim();

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
        }
    }
}
