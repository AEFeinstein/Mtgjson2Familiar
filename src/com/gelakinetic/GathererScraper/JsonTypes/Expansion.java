package com.gelakinetic.GathererScraper.JsonTypes;

import com.gelakinetic.mtgJson2Familiar.mtgjsonClasses.mtgjson_set;
import com.gelakinetic.mtgJson2Familiar.setCodeMapper;
import com.google.gson.Gson;

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
        this.mBorderColor = orig.cards.get(0).borderColor;
        this.mCanBeFoil = orig.isFoilOnly;
        this.mCode_gatherer = scm.getFamiliarCode(orig.code);
        this.mCode_mtgi = null;
        this.mDigest = "";
        this.mExpansionImageURLs = checkRaritySymbols(orig.code);
        this.mIsOnlineOnly = orig.isOnlineOnly;
        this.mName_gatherer = orig.name;
        this.mName_mkm = orig.mcmName;
        this.mName_tcgp = tcgpIds.get((long) orig.tcgplayerGroupId);
        // groupId -> name
        try {
            this.mReleaseTimestamp = new SimpleDateFormat("yyyy-MM-dd").parse(orig.releaseDate).getTime();
        } catch (ParseException e) {
            System.err.println("TIMESTAMP NOT PARSED: ~" + orig.releaseDate + "~");
        }
    }

    private static ArrayList<String> checkRaritySymbols(String code) {
        // https://gatherer.wizards.com/Handlers/Image.ashx?type=symbol&set=5E&size=large&rarity=U
        final char[] rarities = {'C', 'U', 'R', 'M', 'T'};
        ArrayList<String> symbols = new ArrayList<>();
        for (char rarity : rarities) {
            try {
                URL imgUrl = new URL("https://gatherer.wizards.com/Handlers/Image.ashx?type=symbol&set=" + code
                        + "&size=large&rarity=" + rarity);
                if (checkURL(imgUrl)) {
                    symbols.add(imgUrl.toString());
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }

        return symbols;
    }

    private static boolean checkURL(URL toCheck) {
        try (InputStream is = toCheck.openStream()) {
            byte[] readBytes = is.readAllBytes();
            return readBytes.length > 0;
        } catch (IOException ignored) {
        }
        return false;
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
