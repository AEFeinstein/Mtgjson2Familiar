package com.gelakinetic.GathererScraper.JsonTypes;

import com.gelakinetic.mtgJson2Familiar.Filenames;
import com.gelakinetic.mtgJson2Familiar.m2fLogger;
import com.gelakinetic.mtgJson2Familiar.mtgjsonClasses.mtgjson_card;
import com.gelakinetic.mtgJson2Familiar.mtgjsonClasses.mtgjson_set;
import com.gelakinetic.mtgJson2Familiar.setCodeMapper;
import org.imgscalr.Scalr;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.TimeZone;

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

    // Whether this expansion has foil cards
    public boolean mCanBeFoil;

    // Whether this expansion is online-only or has paper printings
    public boolean mIsOnlineOnly;

    // The color of the border, either Black, White, or Silver
    public String mBorderColor;

    // The type of set
    public String mType;

    // MD5 digest for scraped cards, to see when things change
    public String mDigest;

    // List of image URLs
    public ArrayList<String> mExpansionImageURLs;

    public Expansion(mtgjson_set orig, HashMap<Long, String> tcgpIds, setCodeMapper scm) {

        // Adjust border color to things Familiar knows about
        this.mBorderColor = orig.cards.get(0).borderColor;
        if ("borderless".equals(this.mBorderColor)) {
            this.mBorderColor = "black";
        }
        if ("gold".equals(this.mBorderColor)) {
            this.mBorderColor = "silver";
        }
        if (orig.type.equals("funny")) {
            this.mBorderColor = "silver";
        }

        // Error check
        if (!"white".equals(this.mBorderColor) &&
                !"black".equals(this.mBorderColor) &&
                !"silver".equals(this.mBorderColor)) {
            m2fLogger.log(m2fLogger.LogLevel.ERROR, "Unknown border color ~" + this.mBorderColor + "~");
        }
        this.mBorderColor = this.mBorderColor.substring(0, 1).toUpperCase() + this.mBorderColor.substring(1);
        this.mCanBeFoil = !(orig.isNonFoilOnly || orig.isFoilOnly);
        this.mIsOnlineOnly = orig.isOnlineOnly;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            sdf.setTimeZone(TimeZone.getTimeZone("America/New_York"));
            this.mReleaseTimestamp = sdf.parse(orig.releaseDate).getTime() / 1000;
        } catch (ParseException e) {
            m2fLogger.log(m2fLogger.LogLevel.ERROR, "TIMESTAMP NOT PARSED: ~" + orig.releaseDate + "~");
        }
        // Fill this later
        this.mExpansionImageURLs = new ArrayList<>(5);

        // Codes
        this.mCode_gatherer = scm.getFamiliarCode(orig.code);
        this.mCode_mtgi = orig.code;

        // Names
        this.mName_gatherer = orig.name;

        // Check if this should be overridden
        if (orig.name.toLowerCase().contains("promo")) {
            // Count how many cards have multiverse IDs
            int numMultiverseIdCards = 0;
            for (mtgjson_card card : orig.cards) {
                if (card.identifiers.multiverseId != null) {
                    numMultiverseIdCards++;
                }
            }

            if (1 == numMultiverseIdCards) {
                this.mCode_gatherer = "MBP";
                this.mCode_mtgi = "MBP";
                this.mName_gatherer = "Promo set for Gatherer";
                orig.tcgplayerGroupId = 72;
            }
        }

        // Override the tcgplayerGroupId for a few sets with issues
        String overrideValue = null;
        if (orig.name.contains("Duel Decks Anthology")) {
            overrideValue = "Duel Decks: Anthology";
        } else if (orig.name.equals("M19 Gift Pack")) {
            overrideValue = "Gift Boxes and Promos";
        }
        if (null != overrideValue) {
            for (Long key : tcgpIds.keySet()) {
                if (tcgpIds.get(key).equals(overrideValue)) {
                    orig.tcgplayerGroupId = Math.toIntExact(key);
                    break;
                }
            }
        }
        this.mName_tcgp = tcgpIds.get((long) orig.tcgplayerGroupId);

        if (null == orig.mcmName) {
            this.mName_mkm = this.mName_tcgp;
        } else {
            this.mName_mkm = orig.mcmName;
        }

        this.mType = orig.type;

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
        this.mType = other.mType;
        // Fill this later
        this.mExpansionImageURLs = new ArrayList<>(1);
        this.mDigest = null;
    }

    public void fetchRaritySymbols(ArrayList<Card> mCards) {
        // https://gatherer-static.wizards.com/set_symbols/DST/large-common-DST.png
        // https://gatherer-static.wizards.com/set_symbols/ONS/ONS.png

        // See what rarities exist for this set
        ArrayList<Character> rarities = new ArrayList<>();
        for (Card c : mCards) {
            if (!rarities.contains(c.mRarity)) {
                rarities.add(c.mRarity);
            }
        }

        for (char rarity : rarities) {
            String symName = this.mCode_gatherer + "_" + rarity + ".png";
            File expansionSymbolFile = new File(Filenames.SYMBOLS_DIR, symName);
            boolean addToList = false;
            if (expansionSymbolFile.exists()) {
                // Already exists, don't bother getting it again
                addToList = true;
            } else {
                HashMap<Character, String> rarityMap = new HashMap<>();
                rarityMap.put('c', "common");
                rarityMap.put('u', "uncommon");
                rarityMap.put('r', "rare");
                rarityMap.put('m', "mythic");
                String gathererRarity = rarityMap.getOrDefault(Character.toLowerCase(rarity), "common");

                if (!rarityMap.containsKey(Character.toLowerCase(rarity))) {
                    m2fLogger.log(m2fLogger.LogLevel.ERROR, "Rarity not in map: " + rarity);
                }

                // Attempt to download it
                String[] strUrls = {
                        "https://gatherer-static.wizards.com/set_symbols/" + this.mCode_gatherer + "/large-" + gathererRarity + "-" + this.mCode_gatherer + ".png",
                        "https://images1.mtggoldfish.com/mtg_sets/" + this.mCode_gatherer + "_" + rarity + ".png",
                        "https://images1.mtggoldfish.com/mtg_sets/" + this.mCode_gatherer.toLowerCase() + "_expsym_" + (rarity + "").toLowerCase() + "_web_en.png",
                        "https://images1.mtggoldfish.com/mtg_sets/" + this.mCode_gatherer.toLowerCase() + "_expsym_" + (rarity + "").toLowerCase() + "_web.png",
                        "https://images1.mtggoldfish.com/mtg_sets/" + this.mCode_gatherer.toLowerCase() + "_" + rarityCharToStr(rarity) + ".png",
                };

                for (String strUrl : strUrls) {
                    try {
                        URL imgUrl = new URL(strUrl);

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
                                    // Break now that one image was downloaded
                                    break;
                                }
                            }
                        } catch (IOException e) {
                            // Carry on
                        }
                    } catch (MalformedURLException e) {
                        m2fLogger.log(m2fLogger.LogLevel.ERROR, "Bad URL: " + e.getMessage());
                        m2fLogger.logStackTrace(m2fLogger.LogLevel.ERROR, e);
                    }
                }
            }

            if (addToList) {
                String url = "https://github.com/AEFeinstein/Mtgjson2Familiar/raw/main/symbols/" + symName;
                if (!this.mExpansionImageURLs.contains(url)) {
                    this.mExpansionImageURLs.add(url);
                }
            } else {
                m2fLogger.log(m2fLogger.LogLevel.ERROR, "Failed to get set symbol for ~~ " + this.mCode_gatherer + "_" + rarity + ".png ~~");
            }
        }
    }

    private String rarityCharToStr(char rarity) {
        switch (rarity) {
            case 'c':
                return "Common";
            case 'u':
                return "Uncommon";
            case 'r':
                return "Rare";
            case 'm':
                return "Mythic";
            case 't':
                return "Timeshifted";
        }
        return "";
    }

    /**
     * Calculate a MD5 digest for this expansion. It changes whenever any stored data changes
     *
     * @param mCards All the cards for this expansion
     */
    public void calculateDigest(ArrayList<Card> mCards) {
        this.mDigest = null;
        // Add all cards to the digest
        BigInteger digest = BigInteger.ZERO;
        for (Card c : mCards) {
            digest = digest.add(c.getDigest());
        }
        // Add set data to the digest
        digest = digest.add(this.getDigest());

        StringBuilder sb = new StringBuilder();
        for (byte b : digest.toByteArray()) {
            sb.append(String.format("%02x", b));
        }
        this.mDigest = sb.toString();
    }

    private BigInteger getDigest() {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");

            ArrayList<String> digestStrings = new ArrayList<>();
            digestStrings.add("mName_gatherer:" + mName_gatherer);
            digestStrings.add("mCode_gatherer:" + mCode_gatherer);
            digestStrings.add("mCode_mtgi:" + mCode_mtgi);
            digestStrings.add("mName_tcgp:" + mName_tcgp);
            digestStrings.add("mName_mkm:" + mName_mkm);
            digestStrings.add("mReleaseTimestamp:" + mReleaseTimestamp);
            digestStrings.add("mCanBeFoil:" + mCanBeFoil);
            digestStrings.add("mIsOnlineOnly:" + mIsOnlineOnly);
            digestStrings.add("mBorderColor:" + mBorderColor);
            digestStrings.add("mType:" + mType);
            digestStrings.add("mDigest:" + mDigest);
            for (String imageUrl : mExpansionImageURLs) {
                digestStrings.add("mExpansionImageURLs:" + imageUrl);
            }

            // Sort strings to make the digest order-invariant
            Collections.sort(digestStrings);

            for (String s : digestStrings) {
                messageDigest.update(s.getBytes(StandardCharsets.UTF_8));
            }

            return new BigInteger(messageDigest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
