package com.gelakinetic.mtgJson2Familiar;

import java.io.*;
import java.nio.file.Files;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JudgeDocConverter {
    /**
     * TODO
     *
     * @param args
     */
    public static void main(String args[]) {
        embedBase64Images("C:\\Users\\gelak\\Downloads\\mtg_ipg_5feb21_en_0-html.html", "ipg.html");
        embedBase64Images("C:\\Users\\gelak\\Downloads\\mtg_jar_25sep20_en-html.html", "jar.html");
        embedBase64Images("C:\\Users\\gelak\\Downloads\\mtg_mtr_23apr21_en_0-html.html", "mtr.html");
        embedBase64Images("C:\\Users\\gelak\\Downloads\\Magic-Digital-MTR-2-html.html", "dmtr.html");
        embedBase64Images("C:\\Users\\gelak\\Downloads\\Magic-Digital-IPG-1-html.html", "dipg.html");
    }

    /**
     * TODO
     *
     * @param f
     * @return
     */
    static String getFileExtension(File f) {
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
    static String getFilenameNoExtension(File f, int extraCharsToTrim) {
        String noExtension = "";

        int i = f.getName().lastIndexOf('.');
        if (i > 0) {
            noExtension = f.getName().substring(0, i - extraCharsToTrim);
        }
        return noExtension;
    }

    /**
     * TODO
     *
     * @param filename
     * @param outname
     */
    static void embedBase64Images(String filename, String outname) {
        File toEmbedIn = new File(filename);
        File directory = toEmbedIn.getParentFile();

        Pattern imgPattern = Pattern.compile("<img.*src=\"(\\S+)\"");
        Pattern hrefPattern = Pattern.compile("<a.*href=\"(" + getFilenameNoExtension(toEmbedIn, 4) + "(\\d+)\\.html)\"");

        Base64.Encoder encoder = Base64.getEncoder();

        try (BufferedReader br = new BufferedReader(new FileReader(toEmbedIn));
             BufferedWriter bw = new BufferedWriter(new FileWriter(new File(directory, outname)))) {
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
