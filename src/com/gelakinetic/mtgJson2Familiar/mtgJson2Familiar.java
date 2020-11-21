package com.gelakinetic.mtgJson2Familiar;

import com.gelakinetic.GathererScraper.JsonTypes.Card;
import com.gelakinetic.GathererScraper.JsonTypes.Card.ForeignPrinting;
import com.gelakinetic.GathererScraper.JsonTypes.Expansion;
import com.gelakinetic.mtgJson2Familiar.mtgjsonClasses.mtgjson_card;
import com.gelakinetic.mtgJson2Familiar.mtgjsonClasses.mtgjson_foreignData;
import com.gelakinetic.mtgJson2Familiar.mtgjsonClasses.mtgjson_set;
import com.gelakinetic.mtgJson2Familiar.mtgjsonFiles.mtgjson_allPrintings;
import com.gelakinetic.mtgfam.helpers.database.CardDbAdapter;
import com.gelakinetic.mtgfam.helpers.tcgp.TcgpHelper;
import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class mtgJson2Familiar {

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
        public static final String English = "en";

        public static final String Sanskrit = "sa";
        public static final String Hebrew = "he";
        public static final String Arabic = "ar";
        public static final String Latin = "la";
        public static final String Greek = "el";
    }

    public static void main(String[] args) {

        try {
            // Get TCGPlayer.com group IDs
            TcgpHelper tHelper = new TcgpHelper();
            HashMap<Long, String> ids = tHelper.getGroupIds();

            // Read in the AllPrintings.json file
            mtgjson_allPrintings printings = new Gson().fromJson(new String(Files.readAllBytes(Paths.get("AllPrintings.json"))), mtgjson_allPrintings.class);

            int cardsParsed = 0;
            int setsParsed = 0;

            // print stuff
            System.out.println("Parsed the following sets:");
            for (String key : printings.data.keySet()) {
                mtgjson_set set = printings.data.get(key);

                if (!set.cards.isEmpty()) {
                    Expansion familiarExpansion = initExpansionFromMtgJson(set, ids);
                    System.out.println(key + " " + familiarExpansion.mName_gatherer + "{" + set.type + "}");
                } else {
                    System.out.println("!!! Empty set " + set.name);
                }

                for (mtgjson_card orig : set.cards) {
                    Card familiarCard = initFamiliarCardFromMtgjson(orig, set);
                    if(familiarCard.mMultiverseId > -1) {
                        System.out.println("  Parsed " + familiarCard.mName + "{" + familiarCard.mMultiverseId + "}");
                    }
                    cardsParsed++;
                }
                setsParsed++;
            }
            System.out.println(cardsParsed + " cards parsed, " + setsParsed + " sets parsed");

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    static Expansion initExpansionFromMtgJson(mtgjson_set orig, HashMap<Long, String> tcgpIds) {
        Expansion newExpansion = new Expansion();

        newExpansion.mBorderColor = orig.cards.get(0).borderColor;
        newExpansion.mCanBeFoil = orig.isFoilOnly;
        newExpansion.mCode_gatherer = orig.code; // TODO these codes dont match gatherer for old sets...
        newExpansion.mCode_mtgi = null;
        newExpansion.mDigest = ""; // TODO calculate digest
        newExpansion.mExpansionImageURLs = checkRaritySymbols(orig.code);
        newExpansion.mIsOnlineOnly = orig.isOnlineOnly;
        newExpansion.mName_gatherer = orig.name;
        newExpansion.mName_mkm = orig.mcmName;
        newExpansion.mName_tcgp = tcgpIds.get((long) orig.tcgplayerGroupId);
        // groupId -> name
        try {
            newExpansion.mReleaseTimestamp = new SimpleDateFormat("yyyy-MM-dd").parse(orig.releaseDate).getTime();
        } catch (ParseException e) {
            System.err.println("TIMESTAMP NOT PARSED: ~" + orig.releaseDate + "~");
        }

        return newExpansion;
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

    static Card initFamiliarCardFromMtgjson(mtgjson_card orig, mtgjson_set origSet) {
        Card newCard = new Card();

        newCard.mName = orig.name;

        // TODO Familiar treats half mana CMC incorrectly
        newCard.mCmc = (int) orig.convertedManaCost;
        newCard.mManaCost = orig.manaCost;

        newCard.mColor = colorListToString(orig.colors);
        newCard.mColorIdentity = colorListToString(orig.colorIdentity);

        newCard.mFlavor = htmlifyText(orig.flavorText);
        newCard.mArtist = orig.artist;
        newCard.mWatermark = orig.watermark;
        newCard.mRarity = orig.rarity.toUpperCase().charAt(0);
        switch (newCard.mRarity) {
            case 'C':
            case 'U':
            case 'R':
            case 'M':
            case 'T':
                break;
            default:
                System.err.println("RARITY NOT PARSED: ~" + newCard.mRarity + "~");
        }

        newCard.mForeignPrintings = new ArrayList<>();
        for (mtgjson_foreignData fd : orig.foreignData) {
            ForeignPrinting fp = newCard.new ForeignPrinting();

            switch (fd.language) {
                case "German":
                    fp.mLanguageCode = Language.German;
                    break;
                case "Spanish":
                    fp.mLanguageCode = Language.Spanish;
                    break;
                case "French":
                    fp.mLanguageCode = Language.French;
                    break;
                case "Italian":
                    fp.mLanguageCode = Language.Italian;
                    break;
                case "Japanese":
                    fp.mLanguageCode = Language.Japanese;
                    break;
                case "Portuguese (Brazil)":
                    fp.mLanguageCode = Language.Portuguese_Brazil;
                    break;
                case "Russian":
                    fp.mLanguageCode = Language.Russian;
                    break;
                case "Chinese Simplified":
                    fp.mLanguageCode = Language.Chinese_Simplified;
                    break;
                case "Korean":
                    fp.mLanguageCode = Language.Korean;
                    break;
                case "Chinese Traditional":
                    fp.mLanguageCode = Language.Chinese_Traditional;
                    break;
                case "Sanskrit":
                    fp.mLanguageCode = Language.Sanskrit;
                    break;
                case "Hebrew":
                    fp.mLanguageCode = Language.Hebrew;
                    break;
                case "Ancient Greek":
                    fp.mLanguageCode = Language.Greek;
                    break;
                case "Latin":
                    fp.mLanguageCode = Language.Latin;
                    break;
                case "Arabic":
                    fp.mLanguageCode = Language.Arabic;
                    break;
                default:
                    System.err.println("LANGUAGE NOT PARSED: ~" + fd.language + "~");
                    break;
            }
            fp.mName = fd.name;
            newCard.mForeignPrintings.add(fp);
        }

        newCard.mPower = parsePTL(orig.power);
        newCard.mToughness = parsePTL(orig.toughness);
        newCard.mLoyalty = (int) parsePTL(orig.loyalty);

        newCard.mType = orig.type;
        newCard.mText = htmlifyText(orig.text);

        try {
            newCard.mMultiverseId = Integer.parseInt(orig.identifiers.multiverseId);
        } catch (NumberFormatException e) {
            newCard.mMultiverseId = -1;
        }
        newCard.mNumber = orig.number;

        newCard.mExpansion = origSet.code;

        return newCard;
    }

    /**
     * TODO add <br>
     * , <i> tags, <a> tags for meld, partner
     *
     * @param text
     * @return
     */
    private static String htmlifyText(String text) {
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
     * TODO color found check
     *
     * @param colors
     * @return
     */
    static String colorListToString(List<String> colors) {
        String allColors[] = {"W", "U", "B", "R", "G"};
        StringBuilder builder = new StringBuilder();
        for (String potentialColor : allColors) {
            for (String color : colors) {
                if (color.equalsIgnoreCase(potentialColor)) {
                    builder.append(potentialColor);
                    break;
                }
            }
        }
        return builder.toString();
    }

}
