package com.gelakinetic.mtgJson2Familiar;

import org.apache.commons.io.IOUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.DocumentType;
import org.jsoup.nodes.Element;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JudgeDocScraper {

    /**
     * @return true if everything was scraped, false if there was an error
     */
    public boolean ScrapeAll() {

        m2fLogger.log(m2fLogger.LogLevel.INFO, "Processing judge documents");

        boolean status = true;
        if (!ScrapeDocument("mtr", Filenames.MTR_FILE, true)) {
            status = false;
        }
        if (!ScrapeDocument("ipg", Filenames.IPG_FILE, true)) {
            status = false;
        }
        if (!ScrapeDocument("jar", Filenames.JAR_FILE, false)) {
            status = false;
        }
        if (!ScrapeDocument("dipg", Filenames.DIPG_FILE, false)) {
            status = false;
        }
        if (!ScrapeDocument("dtr", Filenames.DTR_FILE, false)) {
            status = false;
        }

        m2fLogger.log(m2fLogger.LogLevel.INFO, "Done processing judge docs");
        return status;
    }

    /**
     * Scrape a judge document from blogs.magicjudges.org
     *
     * @param docType     The type of document to scrape
     * @param outputName  The name of the outputted file
     * @param removeLinks true to remove links, false to keep them
     * @return true if the document was scraped, false if there was an error
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean ScrapeDocument(String docType, String outputName, boolean removeLinks) {

        m2fLogger.log(m2fLogger.LogLevel.DEBUG, "Processing " + docType);

        HashSet<String> pagesToScrape = new HashSet<>();
        Document mainPage = NetUtils.ConnectWithRetries("https://blogs.magicjudges.org/rules/" + docType + "/");

        if (null == mainPage) {
            m2fLogger.log(m2fLogger.LogLevel.ERROR, "Couldn't connect to https://blogs.magicjudges.org/rules/" + docType + "/");
            return false;
        }
        for (Element link : mainPage.getElementsByAttributeValue("class", "entry-content").first().getElementsByTag("a")) {
            String linkHref = link.attr("href");
            if (linkHref.contains("/" + docType)) {
                if (!linkHref.endsWith("/")) {
                    linkHref += '/';
                }
                linkHref = linkHref.replaceAll("https", "http");

                // Fix some links
                linkHref = linkHref.replace("/dipg3/", "/dipg/3-0/");
                linkHref = linkHref.replace("/dipg3-1/", "/dipg/3-1/");

                pagesToScrape.add(linkHref);
            }
        }

        ArrayList<String> pagesAl = new ArrayList<>(pagesToScrape.size());
        pagesAl.addAll(pagesToScrape);
        Pattern pattern = Pattern.compile("http://blogs\\.magicjudges\\.org/rules/" + docType + "([0-9]+)-*([0-9]*)/");
        pagesAl.sort((str, oth) -> {

            Matcher strMatcher = pattern.matcher(str);
            Matcher otherMatcher = pattern.matcher(oth);
            if (strMatcher.matches() && otherMatcher.matches()) {

                int strSection = Integer.parseInt(strMatcher.group(1));
                int strSubsection;
                try {
                    strSubsection = Integer.parseInt(strMatcher.group(2));
                } catch (NumberFormatException | NullPointerException e) {
                    strSubsection = 0;
                }

                int othSection = Integer.parseInt(otherMatcher.group(1));
                int othSubsection;
                try {
                    othSubsection = Integer.parseInt(otherMatcher.group(2));
                } catch (NumberFormatException | NullPointerException e) {
                    othSubsection = 0;
                }

                if (strSection == othSection) {
                    return Integer.compare(strSubsection, othSubsection);
                } else {
                    return Integer.compare(strSection, othSection);
                }
            } else if (strMatcher.matches() && !otherMatcher.matches()) {
                return -1;
            } else if (!strMatcher.matches() && otherMatcher.matches()) {
                return 1;
            } else {
                return str.compareTo(oth);
            }
        });

        pagesAl.add(0, "https://blogs.magicjudges.org/rules/" + docType + "/");

        ArrayList<String> linkIds = new ArrayList<>();

        Document doc = new Document(docType + ".html");

        doc.appendChild(new DocumentType("html", "", ""));
        Element html = new Element("html");
        doc.appendChild(html);
        html.appendChild(new Element("head"));
        doc.head().appendElement("meta").attr("http-equiv", "Content-Type").attr("content", "text/html; charset=utf-8");

        for (String page : pagesAl) {
            addPageToFile(page, html, linkIds);
        }

        // Now that all sections have been written and all link IDs are known,
        // replace links with internal ones
        for (Element link : html.getElementsByTag("a")) {
            try {
                String linkDestination = link.attr("href");
                if (linkIds.contains(getLastPathSegment(linkDestination))) {
                    link.attr("href", "#" + getLastPathSegment(linkDestination));
                } else if (linkDestination.contains("cardfinder")) {
                    for (NameValuePair param : URLEncodedUtils.parse(new URI(linkDestination),
                            StandardCharsets.UTF_8)) {
                        if (param.getName().equals("find")) {
                            link.attr("href", "http://gatherer.wizards.com/Pages/Card/Details.aspx?name=" +
                                    URLEncoder.encode(NetUtils.removeNonAscii(StringEscapeUtils.unescapeHtml4(param.getValue())), StandardCharsets.UTF_8));
                        }
                    }
                } else if (removeLinks && linkDestination.contains("magicjudges")) {
                    m2fLogger.log(m2fLogger.LogLevel.DEBUG, "Link removed: " + linkDestination);
                    link.unwrap();
                }
            } catch (URISyntaxException e) {
                m2fLogger.log(m2fLogger.LogLevel.ERROR, "Failure when writing internal links");
                m2fLogger.logStackTrace(e);
                return false;
            }
        }

        // Clean up non-ascii chars
        String parsedDoc = NetUtils.removeNonAscii(doc.toString());

        // Read the prior document into RAM to check for differences
        try (BufferedReader br = new BufferedReader(new FileReader(new File(Filenames.RULES_DIR, outputName), StandardCharsets.UTF_8))) {
            // Eat the date line
            br.readLine();
            // Read everything
            StringBuilder priorDoc = new StringBuilder();
            String line;
            while (null != (line = br.readLine())) {
                priorDoc.append(line).append('\n');
            }
            // Trim whitespace
            priorDoc = new StringBuilder(priorDoc.toString().trim());
            // If the prior document and the parsed document are the same
            if (priorDoc.toString().equals(parsedDoc)) {
                m2fLogger.log(m2fLogger.LogLevel.DEBUG, "No change in " + outputName);
                return true;
            }
        } catch (IOException e) {
            m2fLogger.log(m2fLogger.LogLevel.ERROR, "Error reading prior document, continuing to write document");
            m2fLogger.logStackTrace(e);
        }

        // Write the HTML file
        try (BufferedWriter bw = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(new File(Filenames.RULES_DIR, outputName)), StandardCharsets.UTF_8))) {
            // Write the current date
            LocalDateTime now = LocalDateTime.now();
            bw.write(String.format("%d-%02d-%02d\n", now.getYear(), now.getMonthValue() - 1, now.getDayOfMonth()));
            // Write the HTML
            bw.write(parsedDoc.replace("\r", ""));
        } catch (IOException e) {
            m2fLogger.log(m2fLogger.LogLevel.DEBUG, "EXCEPTION!!! " + e.getMessage());
            m2fLogger.logStackTrace(e);
            return false;
        }
        return true;
    }

    /**
     * Scrape an individual page of a judge document
     *
     * @param page        The page to scrape
     * @param rootElement The root element to add the page to
     * @param linkIds     A list to add link IDs from this page to
     */
    private void addPageToFile(String page, Element rootElement, ArrayList<String> linkIds) {

        // Not a real page
        if (page.contains("/dtr/1-00/")) {
            return;
        }

        m2fLogger.log(m2fLogger.LogLevel.DEBUG, "Processing " + page);

        // Download the page
        Document mainPage = NetUtils.ConnectWithRetries(page);
        if (null == mainPage) {
            return;
        }

        // Get the main element
        Element content = mainPage.body();

        // Get the header content for this page
        Element entry_header = content.getElementsByAttributeValue("class", "entry-header").first();
        // Add a link ID, and save the link ID
        entry_header.attr("id", getLastPathSegment(page));
        linkIds.add(entry_header.attr("id"));
        // Append it to the root
        rootElement.appendChild(entry_header);

        // Get the body content for this page
        Element entry_content = content.getElementsByAttributeValue("class", "entry-content").first();

        // Remove all annotations
        entry_content.getElementsByAttributeValue("class", "alert alert-grey").remove();
        entry_content.getElementsByAttributeValue("class", "alert alert-info").remove();

        // Remove all styles
        for (Element element : entry_content.getElementsByAttribute("style")) {
            element.removeAttr("style");
        }

        // Remove all background colors
        for (Element element : entry_content.getElementsByAttribute("bgcolor")) {
            element.removeAttr("bgcolor");
        }

        // Remove all mentions of "Annotated" and credits for the annotated documents
        for (Element header : entry_content.getElementsByTag("h2")) {
            if (header.text().toLowerCase().contains("annotated")) {
                m2fLogger.log(m2fLogger.LogLevel.DEBUG, "Removed:\n" + header.text() + '\n');
                header.remove();
            } else if (header.text().toLowerCase().equals("credit")) {
                m2fLogger.log(m2fLogger.LogLevel.DEBUG, "Removed:\n" + header.text() + '\n');
                header.remove();
            }
        }
        for (Element paragraph : entry_content.getElementsByTag("p")) {
            if (paragraph.text().toLowerCase().contains("annotated")) {
                m2fLogger.log(m2fLogger.LogLevel.DEBUG, "Removed:\n" + paragraph.text() + '\n');
                paragraph.remove();
            } else if (paragraph.text().toLowerCase().contains("aipg")) {
                m2fLogger.log(m2fLogger.LogLevel.DEBUG, "Removed:\n" + paragraph.text() + '\n');
                paragraph.remove();
            }
        }

        // Replace all linked images with embedded base64 ones
        for (Element image : entry_content.getElementsByTag("img")) {
            try {
                // Get the image source, ensuring it's using https (normal http gets a redirect)
                String imgSrc = image.attr("src").replace("http:", "https:");
                // Download the image
                byte[] imageBytes = IOUtils.toByteArray(new URL(imgSrc));
                // Convert the image to base64
                String base64 = Base64.getEncoder().encodeToString(imageBytes);
                // Write the base64 image into the html
                image.attr("src", "data:image/" + getFileExtension(imgSrc) + "; base64, " + base64);

                m2fLogger.log(m2fLogger.LogLevel.DEBUG, "Embedded image: " + imgSrc);
            } catch (IOException e) {
                m2fLogger.logStackTrace(e);
            }
        }

        // Append the cleaned HTML to the root
        rootElement.appendChild(entry_content);
    }

    /**
     * Helper function to get the extension from a filename
     *
     * @param name The filename to get an extension from
     * @return The file extension
     */
    private static String getFileExtension(String name) {
        try {
            return name.substring(name.lastIndexOf(".") + 1);
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Return the last path segment for a given URI. It's the last part after a /
     *
     * @param str A URI
     * @return The last path segment for this URI
     */
    private static String getLastPathSegment(String str) {
        String[] parts = str.split("/");
        for (int i = parts.length - 1; i >= 0; i--) {
            if (!parts[i].trim().isEmpty()) {
                return parts[i].trim();
            }
        }
        return "";
    }
}
