package com.gelakinetic.GathererScraper.JsonTypes;

import com.gelakinetic.mtgJson2Familiar.mtgjsonClasses.mtgjson_card;
import com.gelakinetic.mtgJson2Familiar.mtgjsonClasses.mtgjson_foreignData;
import com.gelakinetic.mtgJson2Familiar.mtgjsonClasses.mtgjson_set;
import com.gelakinetic.mtgJson2Familiar.setCodeMapper;
import com.gelakinetic.mtgfam.helpers.database.CardDbAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/*
 * This class contains all information about a scraped card
 *
 * @author AEFeinstein
 *
 */
public class Card implements Comparable<Card> {

    // The card's name
    public String mName;

    // The card's mana cost
    public String mManaCost;

    // The card's converted mana cost
    public int mCmc;

    // The card's type, includes super and sub
    public String mType;

    // The card's text text
    public String mText;

    // The card's flavor text
    public String mFlavor;

    // The card's expansion
    public String mExpansion;

    // The card's rarity
    public char mRarity;

    // The card's collector's number. Not an integer (i.e. 181a, 181b)
    public String mNumber;

    // The card's artist
    public String mArtist;

    // The card's colors
    public String mColor;

    // The card's colors
    public String mColorIdentity;

    // The card's multiverse id
    public int mMultiverseId;

    // The card's power. Not an integer (i.e. *+1, X)
    public float mPower;

    // The card's toughness, see mPower
    public float mToughness;

    // The card's loyalty. An integer in practice
    public int mLoyalty;

    // All the card's foreign printings
    public ArrayList<ForeignPrinting> mForeignPrintings;

    // The card's loyalty. An integer in practice
    public String mWatermark;

    // Private class for encapsulating foreign printing information
    public static class ForeignPrinting implements Comparable<ForeignPrinting> {
        public String mName;
        public String mLanguageCode;

        @Override
        public int compareTo(ForeignPrinting o) {
            int comp;
            if (0 == (comp = (mLanguageCode.compareTo(o.mLanguageCode)))) {
                return mName.compareTo(o.mName);
            }
            return comp;
        }

        @Override
        public boolean equals(Object arg0) {
            if (arg0 instanceof ForeignPrinting) {
                return mLanguageCode.equals(((ForeignPrinting) arg0).mLanguageCode) &&
                        mName.equals(((ForeignPrinting) arg0).mName);
            }
            return false;
        }
    }

    static class Language {
        public static final String Chinese_Traditional = "zh_HANT";
        public static final String Chinese_Simplified = "zh_HANS";
        public static final String French = "fr";
        public static final String German = "de";
        public static final String Italian = "it";
        public static final String Japanese = "ja";
        public static final String Portuguese_Brazil = "pt_BR";
        public static final String Russian = "ru";
        public static final String Spanish = "es";
        public static final String Korean = "ko";
        @SuppressWarnings("unused")
        public static final String English = "en";

        public static final String Sanskrit = "sa";
        public static final String Hebrew = "he";
        public static final String Arabic = "ar";
        public static final String Latin = "la";
        public static final String Greek = "el";

        public static final String Phyrexian = "phy";
    }

    public Card(mtgjson_card orig, mtgjson_set origSet, setCodeMapper scm) {
        if (null != orig.faceName) {
            this.mName = orig.faceName;
        } else {
            this.mName = orig.name;
        }
        // TODO Familiar treats half mana CMC incorrectly
        this.mCmc = (int) orig.convertedManaCost;
        this.mManaCost = orig.manaCost;

        this.mColor = colorListToString(orig.colors);
        this.mColorIdentity = colorListToString(orig.colorIdentity);

        this.mFlavor = htmlifyText(orig.flavorText);
        this.mArtist = orig.artist;
        this.mWatermark = orig.watermark;
        if (null != mWatermark) {
            this.mWatermark = this.mWatermark.substring(0, 1).toUpperCase() + this.mWatermark.substring(1);
        }
        this.mRarity = orig.rarity.toUpperCase().charAt(0);
        switch (this.mRarity) {
            case 'C':
            case 'U':
            case 'R':
            case 'M':
            case 'T':
                break;
            default:
                System.err.println("RARITY NOT PARSED: ~" + this.mRarity + "~");
                break;
        }

        this.mForeignPrintings = new ArrayList<>();
        for (mtgjson_foreignData fd : orig.foreignData) {
            ForeignPrinting fp = new ForeignPrinting();

            switch (fd.language) {
                case "German": {
                    fp.mLanguageCode = Language.German;
                    break;
                }
                case "Spanish": {
                    fp.mLanguageCode = Language.Spanish;
                    break;
                }
                case "French": {
                    fp.mLanguageCode = Language.French;
                    break;
                }
                case "Italian": {
                    fp.mLanguageCode = Language.Italian;
                    break;
                }
                case "Japanese": {
                    fp.mLanguageCode = Language.Japanese;
                    break;
                }
                case "Portuguese (Brazil)": {
                    fp.mLanguageCode = Language.Portuguese_Brazil;
                    break;
                }
                case "Russian": {
                    fp.mLanguageCode = Language.Russian;
                    break;
                }
                case "Chinese Simplified": {
                    fp.mLanguageCode = Language.Chinese_Simplified;
                    break;
                }
                case "Korean": {
                    fp.mLanguageCode = Language.Korean;
                    break;
                }
                case "Chinese Traditional": {
                    fp.mLanguageCode = Language.Chinese_Traditional;
                    break;
                }
                case "Sanskrit": {
                    fp.mLanguageCode = Language.Sanskrit;
                    break;
                }
                case "Hebrew": {
                    fp.mLanguageCode = Language.Hebrew;
                    break;
                }
                case "Ancient Greek": {
                    fp.mLanguageCode = Language.Greek;
                    break;
                }
                case "Latin": {
                    fp.mLanguageCode = Language.Latin;
                    break;
                }
                case "Arabic": {
                    fp.mLanguageCode = Language.Arabic;
                    break;
                }
                case "Phyrexian": {
                    fp.mLanguageCode = Language.Phyrexian;
                    break;
                }
                default: {
                    System.err.println("LANGUAGE NOT PARSED: ~" + fd.language + "~");
                    break;
                }
            }
            fp.mName = fd.name;
            this.mForeignPrintings.add(fp);
        }

        this.mPower = parsePTL(orig.power);
        this.mToughness = parsePTL(orig.toughness);
        this.mLoyalty = (int) parsePTL(orig.loyalty);

        this.mType = htmlifyText(orig.type);
        this.mText = htmlifyText(orig.text);

        try {
            this.mMultiverseId = Integer.parseInt(orig.identifiers.multiverseId);
        } catch (NumberFormatException e) {
            this.mMultiverseId = -1;
        }
        this.mNumber = orig.number;

        this.mExpansion = scm.getFamiliarCode(origSet.code);

        orig.legalities.checkStrings();
    }

    /**
     * Convert a list of colors into a single string
     *
     * @param colors A list of colors
     * @return A single color string
     */
    static String colorListToString(List<String> colors) {
        // Build the color string
        String[] allColors = {"W", "U", "B", "R", "G"};
        StringBuilder builder = new StringBuilder();
        for (String potentialColor : allColors) {
            for (String color : colors) {
                if (color.equalsIgnoreCase(potentialColor)) {
                    builder.append(potentialColor);
                    break;
                }
            }
        }

        // Error check
        List<String> allColorsAL = Arrays.asList(allColors);
        for (String color : colors) {
            if (!allColorsAL.contains(color)) {
                System.err.println("Invalid color ~" + color + "~");
            }
        }

        return builder.toString();
    }

    /**
     * Replace control chars with HTML elements
     * TODO add <i> tags, <a> tags for meld, partner
     *
     * @param text The text to HTML-ify
     * @return The HTML-ified text
     */
    private String htmlifyText(String text) {
        if (null != text) {
            text = text
                    .replace("*", "") // This un-italicizes text
                    .replace("\n", "<br>")
                    .replace("—", "-");

            text = text.replace("AskUrza.com", "<a href=\"http://www.AskUrza.com\">AskUrza.com</a>");

            for (char c : text.toCharArray()) {
                if (Character.isISOControl(c) || '\\' == c) {
                    System.err.println("Invalid char ~" + (int) c + "~");
                }
            }
        }
        return text;
    }

    static float parsePTL(String ptl) {
        if (null == ptl) {
            return CardDbAdapter.NO_ONE_CARES;
        }
        try {
            return Float.parseFloat(ptl);
        } catch (NumberFormatException e) {
            // not a number;
            switch (ptl) {
                case "∞":
                    return CardDbAdapter.INFINITY;
                case "X":
                    return CardDbAdapter.X;
                case "?":
                    return CardDbAdapter.QUESTION_MARK;
                case "*":
                    return CardDbAdapter.STAR;
                case "1+*":
                case "*+1":
                    return CardDbAdapter.ONE_PLUS_STAR;
                case "2+*":
                    return CardDbAdapter.TWO_PLUS_STAR;
                case "7-*":
                    return CardDbAdapter.SEVEN_MINUS_STAR;
                case "*²":
                    return CardDbAdapter.STAR_SQUARED;
                case "1d4+1":
                    return CardDbAdapter.ONE_D_FOUR_PLUS_ONE;
                default:
                    System.err.println("PTL NOT PARSED: ~" + ptl + "~");
                    return 0;
            }
        }
    }

    /**
     * This function usually sorts by collector's number. However, gatherer
     * doesn't have collector's number for expansions before collector's number
     * was printed, and magiccards.info uses a strange numbering scheme. This
     * function does it's best
     */
    @Override
    public int compareTo(Card other) {

        /* Sort by collector's number */
        if (this.mNumber != null && other.mNumber != null && this.mNumber.length() > 0 && other.mNumber.length() > 0) {

            // Try comparing by integer number
            int compVal = Integer.compare(this.getNumberInteger(), other.getNumberInteger());
            if (0 == compVal) {
                // If they match, try comparing by letter after the number
                compVal = Character.compare(this.getNumberChar(), other.getNumberChar());
                if (0 == compVal) {
                    // If they match, try comparing by name
                    return this.mName.compareTo(other.mName);
                } else {
                    return compVal;
                }
            } else {
                return compVal;
            }
        }

        /* Battle Royale is pure alphabetical, except for basics, why not */
        if (this.mExpansion.equals("BR")) {
            if (this.mType.contains("Basic Land") && !other.mType.contains("Basic Land")) {
                return 1;
            }
            if (!this.mType.contains("Basic Land") && other.mType.contains("Basic Land")) {
                return -1;
            }
            return this.mName.compareTo(other.mName);
        }

        /*
         * Or if that doesn't exist, sort by color order. Weird for
         * magiccards.info
         */
        int compVal = Integer.compare(this.getNumFromColor(), other.getNumFromColor());
        if (compVal == 0) {
            // They match, try comparing by name
            return this.mName.compareTo(other.mName);
        } else {
            // Num from color doesn't match, return it
            return compVal;
        }
    }

    /**
     * Returns a number used for sorting by color. This is different for
     * Beatdown because magiccards.info is weird
     *
     * @return A number indicating how the card's color is sorted
     */
    private int getNumFromColor() {
        /* Because Beatdown properly sorts color */
        if (this.mExpansion.equals("BD")) {
            if (this.mColor.length() > 1) {
                return 7;
            }
            switch (this.mColor.charAt(0)) {
                case 'W': {
                    return 0;
                }
                case 'U': {
                    return 1;
                }
                case 'B': {
                    return 2;
                }
                case 'R': {
                    return 3;
                }
                case 'G': {
                    return 4;
                }
                case 'A': {
                    return 5;
                }
                case 'L': {
                    return 6;
                }
            }
        }
        /* And magiccards.info has weird numbering for everything else */
        else {
            if (this.mColor.length() > 1) {
                return 7;
            }
            switch (this.mColor.charAt(0)) {
                case 'B': {
                    return 0;
                }
                case 'U': {
                    return 1;
                }
                case 'G': {
                    return 2;
                }
                case 'R': {
                    return 3;
                }
                case 'W': {
                    return 4;
                }
                case 'A': {
                    return 5;
                }
                case 'L': {
                    return 6;
                }
            }
        }
        return 8;
    }

    private int getNumberInteger() {
        try {
            char c = this.mNumber.charAt(this.mNumber.length() - 1);
            if (('a' <= c && c <= 'z') || ('A' <= c && c <= 'Z')) {
                return Integer.parseInt(this.mNumber.substring(0, this.mNumber.length() - 1));
            }
            return Integer.parseInt(this.mNumber);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private char getNumberChar() {
        char c = this.mNumber.charAt(this.mNumber.length() - 1);
        if (('a' <= c && c <= 'z') || ('A' <= c && c <= 'Z')) {
            return c;
        }
        return 0;
    }
}