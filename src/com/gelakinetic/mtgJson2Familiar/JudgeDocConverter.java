package com.gelakinetic.mtgJson2Familiar;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JudgeDocConverter {

    /**
     * Get all the judge docs, convert them, and check for differences with the existing judge docs
     */
    public static boolean GetLatestDocs() {
        boolean ret = true;

        //noinspection RedundantIfStatement
        if (null == processWpnDoc("https://wpn.wizards.com/en/document/magic-gathering-tournament-rules", Filenames.MTR_FILE)) {
            ret = false;
        }
        if (null == processWpnDoc("https://wpn.wizards.com/en/document/magic-gathering-judging-regular-rel", Filenames.JAR_FILE)) {
            ret = false;
        }
        if (null == processWpnDoc("https://wpn.wizards.com/en/document/magic-infraction-procedure-guide", Filenames.IPG_FILE)) {
            ret = false;
        }
        if (null == processJudgeAcademyDoc("https://judgeacademy.com/policy-documents/", Filenames.DIPG_FILE)) {
            ret = false;
        }
        if (null == processJudgeAcademyDoc("https://judgeacademy.com/policy-documents/", Filenames.DTR_FILE)) {
            ret = false;
        }

        return ret;
    }

    /**
     * TODO doc
     *
     * @param pageUrl
     * @param outFile
     * @return
     */
    private static File processJudgeAcademyDoc(String pageUrl, String outFile) {

        // Connect to the root page
        Document docPage = NetUtils.ConnectWithRetries(pageUrl);
        if (null == docPage) {
            m2fLogger.log(m2fLogger.LogLevel.ERROR, "Couldn't connect to " + pageUrl);
            return null;
        }

        // Find all links in the page
        for (Element link : docPage.getElementsByTag("a")) {
            String linkStr = link.attr("href");
            // If this is a link to a PDF file
            if (linkStr.toLowerCase().endsWith(".pdf")) {

                // Get the PDF name
                String pdfFileName = linkStr.substring(linkStr.lastIndexOf('/') + 1);

                // If this is the PDF we're looking for
                if (pdfFileName.toLowerCase().contains(outFile.substring(1, outFile.lastIndexOf('.')))) {

                    // Create a file to download to
                    File downloadFolder = new File(Filenames.DOWNLOADS_DIR);
                    File dlFile = new File(downloadFolder, pdfFileName);

                    // Download and process the PDF
                    downloadAndProcessPDF(linkStr, dlFile, outFile);
                }
            }
        }
        return null;
    }

    /**
     * TODO doc
     *
     * @param pageUrl
     * @param outFile
     * @return
     */
    private static File processWpnDoc(String pageUrl, String outFile) {

        // Connect to the root page
        Document docPage = NetUtils.ConnectWithRetries(pageUrl);
        if (null == docPage) {
            m2fLogger.log(m2fLogger.LogLevel.ERROR, "Couldn't connect to " + pageUrl);
            return null;
        }

        // Find all links in the page
        for (Element link : docPage.getElementsByTag("a")) {
            String linkStr = link.attr("href");
            // If this is a link to a PDF file
            if (linkStr.toLowerCase().endsWith(".pdf")) {

                // Get the PDF name
                String pdfFileName = linkStr.substring(linkStr.lastIndexOf('/') + 1);

                // Create a file to download to
                File downloadFolder = new File(Filenames.DOWNLOADS_DIR);
                File dlFile = new File(downloadFolder, pdfFileName);

                // Download and process the PDF
                downloadAndProcessPDF(linkStr, dlFile, outFile);
            }
        }
        return null;
    }

    /**
     * TODO
     *
     * @param linkStr
     * @param dlFile
     * @param outFileName
     */
    private static void downloadAndProcessPDF(String linkStr, File dlFile, String outFileName) {
        try {
            // Download the new file
            InputStream in = new URL(linkStr).openStream();
            Files.copy(in, dlFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            // Process the PDF
            // Once downloaded, convert it
            File htmlFile = convertPDFtoHTML(dlFile);
            if (null == htmlFile) {
                m2fLogger.log(m2fLogger.LogLevel.ERROR, "Couldn't convert " + dlFile.getName());
                return;
            }

            // Once converted, merge images into single file
            File finalFile = embedBase64Images(htmlFile, outFileName);
            if (null == finalFile || !finalFile.exists()) {
                m2fLogger.log(m2fLogger.LogLevel.ERROR, finalFile.getName() + " doesn't exist");
            }

            // Compare the new doc with the existing one, copy it over if necessary
            compareJudgeDoc(outFileName);
        } catch (IOException e) {
            m2fLogger.log(m2fLogger.LogLevel.ERROR, "Couldn't download " + linkStr);
        }
    }

    /**
     * TODO doc
     *
     * @param pdfFile
     * @return
     */
    private static File convertPDFtoHTML(File pdfFile) {
        ProcessBuilder builder = new ProcessBuilder();
        builder.command("pdftohtml", "-s", "-c", pdfFile.getName());
        builder.directory(pdfFile.getParentFile());
        try {
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
            m2fLogger.log(m2fLogger.LogLevel.ERROR, "Couldn't execute " + builder.toString());
            return null;
        }
    }

    /**
     * TODO doc
     *
     * @param toEmbedIn
     * @param outName
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

                bw.write(line + "\n");
            }
            return mergedFile;
        } catch (IOException e) {
            m2fLogger.log(m2fLogger.LogLevel.ERROR, "Couldn't embed base64 images " + e.getMessage());
            return null;
        }
    }

    /**
     * TODO doc
     *
     * @param f
     * @return
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
     * TODO
     *
     * @param f
     * @param extraCharsToTrim
     * @return
     */
    private static String getFilenameNoExtension(File f, int extraCharsToTrim) {
        String noExtension = "";

        int i = f.getName().lastIndexOf('.');
        if (i > 0) {
            noExtension = f.getName().substring(0, i - extraCharsToTrim);
        }
        return noExtension;
    }

    /**
     * TODO doc
     *
     * @param outFileName
     */
    private static void compareJudgeDoc(String outFileName) {
        boolean filesEqual = true;

        try {
            // Read both files
            List<String> dlLines = Files.readAllLines(new File(Filenames.DOWNLOADS_DIR, outFileName).toPath());
            List<String> ruLines = Files.readAllLines(new File(Filenames.RULES_DIR, outFileName).toPath());

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
                    bw.write(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
                // TODO error
            }
        }
    }
}
