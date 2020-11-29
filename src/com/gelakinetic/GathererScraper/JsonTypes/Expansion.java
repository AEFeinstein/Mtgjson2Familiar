package com.gelakinetic.GathererScraper.JsonTypes;

import com.gelakinetic.mtgJson2Familiar.mtgjsonClasses.mtgjson_set;
import com.gelakinetic.mtgJson2Familiar.setCodeMapper;
import com.google.gson.Gson;
import org.imgscalr.Scalr;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;

/*
 * This class contains all information about an expansion to be parsed
 *
 * @author AEFeinstein
 *
 */
public class Expansion {

    // Name used by Gatherer
    public String mName_gatherer;

    // expansion code used by Gatherer
    public String mCode_gatherer;

    // expansion code used by magiccards.info
    public String mCode_mtgi;

    // expansion mName used by TCGPlayer.com
    public String mName_tcgp;

    // expansion name used by MagicCardMarket.eu
    public String mName_mkm;

    // Date the expansion was released
    public long mReleaseTimestamp = 0;

    // Whether or not this expansion has foil cards
    public boolean mCanBeFoil;

    // Whether this expansion is online-only or has paper printings
    public boolean mIsOnlineOnly;

    // The color of the border, either Black, White, or Silver
    public String mBorderColor;

    // MD5 digest for scraped cards, to see when things change
    public String mDigest;

    // List of image URLs
    public ArrayList<String> mExpansionImageURLs;

    public Expansion(mtgjson_set orig, HashMap<Long, String> tcgpIds, setCodeMapper scm) {

        // Metadata
        this.mBorderColor = orig.cards.get(0).borderColor;
        this.mBorderColor = this.mBorderColor.substring(0, 1).toUpperCase() + this.mBorderColor.substring(1);
        this.mCanBeFoil = !(orig.isNonFoilOnly || orig.isFoilOnly);
        this.mIsOnlineOnly = orig.isOnlineOnly;
        try {
            this.mReleaseTimestamp = new SimpleDateFormat("yyyy-MM-dd").parse(orig.releaseDate).getTime() / 1000;
        } catch (ParseException e) {
            System.err.println("TIMESTAMP NOT PARSED: ~" + orig.releaseDate + "~");
        }
        // Fill this later
        this.mExpansionImageURLs = new ArrayList<>(5);

        // Codes
        this.mCode_gatherer = scm.getFamiliarCode(orig.code);
        this.mCode_mtgi = orig.code;

        // Names
        this.mName_gatherer = orig.name;
        this.mName_tcgp = tcgpIds.get((long) orig.tcgplayerGroupId);
        if (null == orig.mcmName) {
            this.mName_mkm = this.mName_tcgp;
        } else {
            this.mName_mkm = orig.mcmName;
        }

        // Fill this in later
        this.mDigest = null;
    }

    /**
     * Constructor which 'clones' another Expansion, but overwrites names and codes
     *
     * @param other        The Expansion to clone
     * @param nameGatherer The new gatherer name
     * @param codeGatherer The new gatherer code
     * @param nameTcgp     The new TCGPlayer name
     */
    public Expansion(Expansion other, String nameGatherer, String codeGatherer, String nameTcgp) {
        this.mName_gatherer = nameGatherer;
        this.mCode_gatherer = codeGatherer;
        this.mCode_mtgi = other.mCode_mtgi;
        this.mName_tcgp = nameTcgp;
        this.mName_mkm = nameTcgp;
        this.mReleaseTimestamp = other.mReleaseTimestamp;
        this.mCanBeFoil = other.mCanBeFoil;
        this.mIsOnlineOnly = other.mIsOnlineOnly;
        this.mBorderColor = other.mBorderColor;
        // Fill this later
        this.mExpansionImageURLs = new ArrayList<>(1);
        this.mDigest = null;
    }

    public void fetchRaritySymbols(ArrayList<Card> mCards) {
        // https://gatherer.wizards.com/Handlers/Image.ashx?type=symbol&set=5E&size=large&rarity=U

        // See what rarities exist for this set
        ArrayList<Character> rarities = new ArrayList<>();
        for (Card c : mCards) {
            if (!rarities.contains(c.mRarity)) {
                rarities.add(c.mRarity);
            }
        }

        for (char rarity : rarities) {
            String symName = this.mCode_gatherer + "_" + rarity + ".png";
            File expansionSymbolFile = new File("symbols", symName);
            boolean addToList = false;
            if (expansionSymbolFile.exists()) {
                // Already exists, don't bother getting it again
                addToList = true;
            } else {
                // Attempt to download it
                URL imgUrl;
                try {
                    imgUrl = new URL("https://gatherer.wizards.com/Handlers/Image.ashx?type=symbol&set=" + this.mCode_gatherer + "&size=large&rarity=" + rarity);

                    // Download the image to RAM
                    try (InputStream is = imgUrl.openStream()) {
                        BufferedImage expansionSymbol = ImageIO.read(is);

                        // Make sure it downloaded
                        if (null != expansionSymbol) {
                            // Clip the transparent pixels
                            int minX = Integer.MAX_VALUE;
                            int maxX = 0;
                            int minY = Integer.MAX_VALUE;
                            int maxY = 0;
                            for (int x = 0; x < expansionSymbol.getWidth(); x++) {
                                for (int y = 0; y < expansionSymbol.getHeight(); y++) {
                                    if (((expansionSymbol.getRGB(x, y) >> 24) & 0xFF) != 0) {
                                        if (x < minX) {
                                            minX = x;
                                        }
                                        if (x > maxX) {
                                            maxX = x;
                                        }
                                        if (y < minY) {
                                            minY = y;
                                        }
                                        if (y > maxY) {
                                            maxY = y;
                                        }
                                    }
                                }
                            }
                            expansionSymbol = expansionSymbol.getSubimage(minX, minY, maxX - minX + 1, maxY - minY + 1);

                            // Scale the image to 72px high, at most
                            if (expansionSymbol.getHeight() > 72) {
                                double scale = 72.0 / expansionSymbol.getHeight();
                                expansionSymbol = Scalr.resize(expansionSymbol,
                                        Scalr.Method.ULTRA_QUALITY,
                                        (int) Math.round(scale * expansionSymbol.getWidth()),
                                        (int) Math.round(scale * expansionSymbol.getHeight()));
                            }

                            // Write the edited image
                            try (FileOutputStream fos = new FileOutputStream(expansionSymbolFile)) {
                                ImageIO.write(expansionSymbol, "png", fos);
                            }

                            // If nothing was actually written, delete the file
                            if (0 == expansionSymbolFile.length()) {
                                //noinspection ResultOfMethodCallIgnored
                                expansionSymbolFile.delete();
                            } else {
                                addToList = true;
                            }
                        } else {
                            System.err.println("Failed to get set symbol for ~~" + this.mCode_gatherer + ", " + rarity + "~~");
                        }
                    } catch (IOException e) {
                        System.err.println("Failed to get set symbol for ~~" + this.mCode_gatherer + ", " + rarity + "~~");
                        e.printStackTrace();
                    }
                } catch (MalformedURLException e) {
                    System.err.println("Failed to get set symbol for ~~" + this.mCode_gatherer + ", " + rarity + "~~");
                    e.printStackTrace();
                }
            }

            if (addToList) {
                this.mExpansionImageURLs.add("https://github.com/AEFeinstein/Mtgjson2Familiar/raw/main/symbols/" + symName);
            }
        }
    }

    /**
     * Calculate a MD5 digest for this expansion. It changes whenever any stored data changes
     *
     * @param gson   A Gson object to help convert data to bytes
     * @param mCards All the cards for this expansion
     */
    public void calculateDigest(Gson gson, ArrayList<Card> mCards) {
        this.mDigest = null;
        MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance("MD5");
            // Add all cards to the digest
            for (Card c : mCards) {
                messageDigest.update(gson.toJson(c).getBytes());
            }
            // Add set data to the digest
            messageDigest.update(gson.toJson(this).getBytes());

            StringBuilder sb = new StringBuilder();
            for (byte b : messageDigest.digest()) {
                sb.append(String.format("%02x", b));
            }
            this.mDigest = sb.toString();
        } catch (NoSuchAlgorithmException e) {
            /* This should never happen */
        }
    }
}
