package com.gelakinetic.mtgJson2Familiar;

import org.apache.commons.text.StringEscapeUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("BooleanMethodIsAlwaysInverted")
public class JudgeDocConverter {

    /**
     * Get all the judge docs, convert them, and check for differences with the existing judge docs
     */
    public static boolean GetLatestDocs() {
        boolean ret = true;

        m2fLogger.log(m2fLogger.LogLevel.INFO, "Processing judge docs");

        // Make sure the downloads directory exists
        File downloadsDir = new File(Filenames.DOWNLOADS_DIR);
        if (!downloadsDir.exists()) {
            if (!downloadsDir.mkdir()) {
                m2fLogger.log(m2fLogger.LogLevel.INFO, "Couldn't create " + Filenames.DOWNLOADS_DIR + "dir");
                return false;
            }
        }

        if (!processWpnDocs()) {
            ret = false;
        }
        if (!processMagicGgDoc("Digital_IPG", Filenames.DIPG_FILE)) {
            ret = false;
        }
        if (!processMagicGgDoc("Digital_Tournament_Rules", Filenames.DTR_FILE)) {
            ret = false;
        }

        // Clean up downloads folder
        for (File toDelete : Objects.requireNonNull(new File(Filenames.DOWNLOADS_DIR).listFiles())) {
            if (!toDelete.delete()) {
                m2fLogger.log(m2fLogger.LogLevel.ERROR, "Couldn't delete " + toDelete.getName());
            }
        }
        m2fLogger.log(m2fLogger.LogLevel.INFO, "Done processing comprehensive rules");

        return ret;
    }

    /**
     * Download, convert, and process a document from Judge Academy
     * After the document is processed, if it differs from the one in the rules folder, copy it to the rules folder
     *
     * @param sourceName A part of the name of the source PDF to filter by
     * @param outFile    The name of the file to write
     * @return true if there were no errors, false if there were errors
     */
    private static boolean processMagicGgDoc(String sourceName, String outFile) {

        m2fLogger.log(m2fLogger.LogLevel.INFO, "Processing Judge Academy, " + outFile);

        // Connect to the root page
        Document docPage = NetUtils.ConnectWithRetries("https://magic.gg/mtg-arena?section=policy#mtg-arena");
        if (null == docPage) {
            m2fLogger.log(m2fLogger.LogLevel.ERROR, "Couldn't connect to https://magic.gg/mtg-arena?section=policy#mtg-arena");
            return false;
        }

        // Find all the scripts
        for (Element scripts : docPage.getElementsByTag("script")) {

            // Unescape the script
            String unescaped = StringEscapeUtils.unescapeJava(scripts.outerHtml());

            // Look at each line of the script for a PDF
            Pattern pattern = Pattern.compile("\"(.*" + sourceName + ".*\\.pdf)\"", Pattern.CASE_INSENSITIVE);
            for (String line : unescaped.split("[\\r\\n]+")) {
                Matcher matcher = pattern.matcher(line);

                // If the PDF was found
                if (matcher.find()) {
                    // Get the link and make sure it starts with https://
                    String linkStr = matcher.group(1);
                    if (!linkStr.toLowerCase().startsWith("http")) {
                        linkStr = "https:" + linkStr;
                    }

                    // Get the PDF name
                    String pdfFileName = linkStr.substring(linkStr.lastIndexOf('/') + 1);

                    // Create a file to download to
                    File downloadFolder = new File(Filenames.DOWNLOADS_DIR);
                    File dlFile = new File(downloadFolder, pdfFileName);

                    // Download and process the PDF
                    return downloadAndProcessPDF(linkStr, dlFile, outFile);
                }
            }
        }

        // Made it this far, so the PDF wasn't found and processed
        m2fLogger.log(m2fLogger.LogLevel.ERROR, "Document not found (" + outFile + ")");
        return false;
    }

    /**
     * Download, convert, and process all documents from Wizards Play Network.
     * After the document is processed, if it differs from the one in the rules folder, copy it to the rules folder
     * <p>
     * https://wpn.wizards.com/en/rules-documents only loads a subset of documents. Rather than clicking the
     * "load more" button this function will only scan the first loaded links. These are the most recent documents.
     * This function may not process all three judge docs if the links aren't on the front page.
     *
     * @return true if there were no errors, false if there were errors
     */
    private static boolean processWpnDocs() {
        m2fLogger.log(m2fLogger.LogLevel.INFO, "Processing Wizards Play Network docs");

        HashMap<String, String> filesToProcess = new HashMap<>();

        // Example URLs
        // filesToProcess.put(Filenames.MTR_FILE, "https://media.wpn.wizards.com/attachements/mtg_mtr_2022mar7_en.pdf");
        // filesToProcess.put(Filenames.IPG_FILE, "https://media.wpn.wizards.com/attachements/mtg_ipg_5feb21_en_0.pdf");
        // filesToProcess.put(Filenames.JAR_FILE, "https://media.wpn.wizards.com/attachements/mtg_jar_25sep20_en.pdf");

        // Just get links from the first page, which is sorted by date updated
        Document wpnPage = NetUtils.ConnectWithRetries("https://wpn.wizards.com/en/rules-documents");
        if (null != wpnPage) {
            for (Element link : wpnPage.getElementsByTag("a")) {
                String linkStr = link.attr("href");
                // If this is a link to a PDF file
                if (linkStr.toLowerCase().endsWith(".pdf")) {
                    // Check the type
                    if (linkStr.toLowerCase().contains("_mtr_")) {
                        // Don't allow duplicates
                        if (!filesToProcess.containsKey(Filenames.MTR_FILE)) {
                            filesToProcess.put(Filenames.MTR_FILE, linkStr);
                        }
                    } else if (linkStr.toLowerCase().contains("_ipg_")) {
                        // Don't allow duplicates
                        if (!filesToProcess.containsKey(Filenames.IPG_FILE)) {
                            filesToProcess.put(Filenames.IPG_FILE, linkStr);
                        }
                    } else if (linkStr.toLowerCase().contains("_jar_")) {
                        // Don't allow duplicates
                        if (!filesToProcess.containsKey(Filenames.JAR_FILE)) {
                            filesToProcess.put(Filenames.JAR_FILE, linkStr);
                        }
                    }
                }
            }
        }

        boolean ret = true;
        // For each document on the main page to process
        for (String outFile : filesToProcess.keySet()) {
            // Get the link from the hashmap
            String linkStr = filesToProcess.get(outFile);

            // Get the PDF name from the link
            String pdfFileName = linkStr.substring(linkStr.lastIndexOf('/') + 1);

            // Create a file to download to
            File downloadFolder = new File(Filenames.DOWNLOADS_DIR);
            File dlFile = new File(downloadFolder, pdfFileName);

            // Download and process the PDF
            if (!downloadAndProcessPDF(linkStr, dlFile, outFile)) {
                ret = false;
            }
        }
        return ret;
    }

    /**
     * Download, convert, and process a PDF document.
     * It starts as a PDF, then is converted using pdftohtml to a webpage.
     * The webpage has all external images embedded as base64 data and internal links fixed.
     * After the document is processed, if it differs from the one in the rules folder, copy it to the rules folder
     *
     * @param linkStr     A link to the PDF to download and process
     * @param dlFile      The file to download the PDF to
     * @param outFileName The name for the output processed HTML file
     * @return true if there were no errors, false if there were errors
     */
    private static boolean downloadAndProcessPDF(String linkStr, File dlFile, String outFileName) {
        try {
            // Download the new file
            InputStream in = new URL(NetUtils.escapeUrl(linkStr)).openStream();
            Files.copy(in, dlFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            // Once downloaded, convert it to HTML
            File htmlFile = convertPDFtoHTML(dlFile);
            if (null == htmlFile) {
                m2fLogger.log(m2fLogger.LogLevel.ERROR, "Couldn't convert " + dlFile.getName());
                return false;
            }

            // Once converted, merge images into single file and fix links
            File finalFile = embedBase64Images(htmlFile, outFileName);
            if (null == finalFile || !finalFile.exists()) {
                assert finalFile != null;
                m2fLogger.log(m2fLogger.LogLevel.ERROR, finalFile.getName() + " doesn't exist");
                return false;
            }

            // Compare the new doc with the existing one, copy it over if necessary
            return compareJudgeDoc(outFileName);
        } catch (IOException e) {
            m2fLogger.log(m2fLogger.LogLevel.ERROR, "Couldn't download " + linkStr);
            return false;
        }
    }

    /**
     * Convert a PDF to HTML using pdftohtml.
     * pdftohtml MUST be installed on the host system for this function to work
     *
     * @param pdfFile The PDF file to convert
     * @return The resulting HTML file or null if there was a failure
     */
    private static File convertPDFtoHTML(File pdfFile) {

        // Set up the system call to pdftohtml
        ProcessBuilder builder = new ProcessBuilder();
        builder.command("pdftohtml", "-s", "-c", pdfFile.getName());
        builder.directory(pdfFile.getParentFile());

        try {
            // Call pdftohtml
            Process process = builder.start();
            if (0 == process.waitFor()) {
                // This is how pdftohtml renames files
                File htmlFile = new File(pdfFile.getParentFile(), getFilenameNoExtension(pdfFile, 0) + "-html.html");
                if (htmlFile.exists()) {
                    return htmlFile;
                } else {
                    m2fLogger.log(m2fLogger.LogLevel.ERROR, htmlFile.getName() + "doesn't exist after conversion");
                    return null;
                }
            } else {
                m2fLogger.log(m2fLogger.LogLevel.ERROR, "pdftohtml exited with non-zero code");
                return null;
            }
        } catch (IOException | InterruptedException e) {
            m2fLogger.log(m2fLogger.LogLevel.ERROR, "Couldn't execute " + builder);
            return null;
        }
    }

    /**
     * Take the HTML output of pdftohtml, embed all external images as base64 data, and fix internal links
     *
     * @param toEmbedIn The HTML file to merge images into and fix links in
     * @param outName   The processed HTML file, or null if there is an error
     */
    static File embedBase64Images(File toEmbedIn, String outName) {
        File directory = toEmbedIn.getParentFile();

        Pattern imgPattern = Pattern.compile("<img.*src=\"(\\S+)\"");
        Pattern hrefPattern = Pattern.compile("<a.*href=\"(" + getFilenameNoExtension(toEmbedIn, 4) + "(\\d+)\\.html)\"");

        Base64.Encoder encoder = Base64.getEncoder();

        File mergedFile = new File(directory, outName);

        try (BufferedReader br = new BufferedReader(new FileReader(toEmbedIn));
             BufferedWriter bw = new BufferedWriter(new FileWriter(mergedFile))) {

            String line;
            while (null != (line = br.readLine())) {
                // Embed images
                Matcher imgMatcher = imgPattern.matcher(line);
                if (imgMatcher.find()) {
                    File imgToEncode = new File(directory, imgMatcher.group(1));
                    String b64str = encoder.encodeToString(Files.readAllBytes(imgToEncode.toPath()));
                    line = line.replace(imgMatcher.group(1), "data:image/" + getFileExtension(imgToEncode) + ";base64, " + b64str);
                }

                // Fix links
                Matcher hrefMatcher = hrefPattern.matcher(line);
                if (hrefMatcher.find()) {
                    line = line.replace(hrefMatcher.group(1), "#page" + hrefMatcher.group(2) + "-div");
                }

                // Don't use special codes
                line = line.replace("&#160;", " ").replace("&#34;", "\"");

                bw.write(line + "\n");
            }
            return mergedFile;
        } catch (IOException e) {
            m2fLogger.log(m2fLogger.LogLevel.ERROR, "Couldn't embed base64 images " + e.getMessage());
            return null;
        }
    }

    /**
     * Compare a document in the downloads and rules folder.
     * If they're different, copy it from downloads to rules
     *
     * @param outFileName The filename to compare
     * @return true if the operation was successful, false if there was an error
     */
    private static boolean compareJudgeDoc(String outFileName) {
        boolean filesEqual = true;

        try {
            // Read both files
            List<String> dlLines = Files.readAllLines(new File(Filenames.DOWNLOADS_DIR, outFileName).toPath());
            List<String> ruLines = Files.readAllLines(new File(Filenames.RULES_DIR, outFileName).toPath());

            if (ruLines.isEmpty()) {
                filesEqual = false;
            } else {
                // Eat the date line
                ruLines.remove(0);

                if (dlLines.size() != ruLines.size()) {
                    // Different line numbers, definitely not equal
                    filesEqual = false;
                } else {
                    for (int i = 0; i < dlLines.size(); i++) {
                        if (!dlLines.get(i).equals(ruLines.get(i))) {
                            // Lines not equal
                            filesEqual = false;
                            break;
                        }
                    }
                }
            }
        } catch (IOException e) {
            // Some IO error, gotta copy!
            filesEqual = false;
        }

        // If the files are unequal, copy from download to rules
        if (!filesEqual) {
            try (BufferedReader br = new BufferedReader(new FileReader(new File(Filenames.DOWNLOADS_DIR, outFileName)));
                 BufferedWriter bw = new BufferedWriter(new FileWriter(new File(Filenames.RULES_DIR, outFileName)))) {

                // Write the current date
                LocalDateTime now = LocalDateTime.now();
                bw.write(String.format("%d-%02d-%02d\n", now.getYear(), now.getMonthValue() - 1, now.getDayOfMonth()));

                // Copy line by line
                String line;
                while (null != (line = br.readLine())) {
                    bw.write(line + '\n');
                }
            } catch (IOException e) {
                m2fLogger.log(m2fLogger.LogLevel.ERROR, "Couldn't embed base64 images " + e.getMessage());
                return false;
            }
        }
        // All done
        return true;
    }

    /**
     * Utility function to return a file extension
     *
     * @param f The file object to get an extension from
     * @return The file extension in String form
     */
    private static String getFileExtension(File f) {
        String extension = "";

        int i = f.getName().lastIndexOf('.');
        if (i > 0) {
            extension = f.getName().substring(i + 1);
        }
        return extension;
    }

    /**
     * Utility function to return a filename without the extension
     *
     * @param f                The file object to get a name from
     * @param extraCharsToTrim The number of characters to trim before the extension
     * @return The file extension in String form
     */
    private static String getFilenameNoExtension(File f, int extraCharsToTrim) {
        String noExtension = "";

        int i = f.getName().lastIndexOf('.');
        if (i > 0) {
            noExtension = f.getName().substring(0, i - extraCharsToTrim);
        }
        return noExtension;
    }
}
