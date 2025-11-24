package com.gelakinetic.mtgJson2Familiar.mtgjsonClasses;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

@SuppressWarnings("unused")
public class mtgjson_card {
    public String artist;
    public String asciiName;
    public List<String> availability;
    public String borderColor;
    public List<String> colorIdentity;
    public List<String> colorIndicator;
    public List<String> colors;
    // public float convertedManaCost; replaced by manaValue
    public int count;
    public String duelDeck;
    public int edhrecRank;
    // public float faceConvertedManaCost; replaced by faceManaValue
    public String faceName;
    public String flavorName;
    public String printedName;
    public String flavorText;
    public List<mtgjson_foreignData> foreignData;
    public List<String> frameEffects;
    public String frameVersion;
    public String hand;
    public boolean hasContentWarning;
    // public boolean hasFoil; replaced by finishes
    public boolean hasAlternativeDeckLimit;
    // public boolean hasNonFoil; replaced by finishes
    public mtgjson_identifiers identifiers;
    public boolean isAlternative;
    public boolean isFoil;
    public boolean isFullArt;
    public boolean isOnlineOnly;
    public boolean isOversized;
    public boolean isPromo;
    public boolean isReprint;
    public boolean isReserved;
    public boolean isStarter;
    public boolean isStorySpotlight;
    public boolean isTextless;
    public boolean isTimeshifted;
    public List<String> keywords;
    public String layout;
    public mtgjson_leadershipSkills leadershipSkills;
    public HashMap<String, String> legalities;
    public String life;
    public String loyalty;
    public String manaCost;
    public String name;
    public String number;
    public String originalText;
    public String originalType;
    public List<String> otherFaceIds;
    public String power;
    public List<String> printings;
    public List<String> promoTypes;
    public mtgjson_purchaseUrls purchaseUrls;
    public String rarity;
    public List<mtgjson_ruling> rulings;
    public String setCode;
    public String side;
    public List<String> subtypes;
    public List<String> supertypes;
    public String text;
    public String toughness;
    public String type;
    public List<String> types;
    public String uuid;
    public List<String> variations;
    public String watermark;
    // 5.1.0
    public String originalReleaseDate;
    // 5.2.0
    public List<String> cardParts;
    public String faceFlavorName;
    public float faceManaValue;
    public List<String> finishes;
    public boolean isFunny;
    public boolean isRebalanced;
    public float manaValue;
    public List<String> originalPrintings;
    public List<String> rebalancedPrintings;
    public String securityStamp;
    public String signature;
    // 5.2.1
    public List<String> attractionLights;
    public List<String> boosterTypes;
    public String defense;
    public float edhrecSaltiness;
    public String language;
    public mtgjson_RelatedCards relatedCards;
    public List<String> subsets;

    // hashInit and hashVal were added by me
    private boolean hashInit = false;
    private int hashVal = 0;

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof mtgjson_card) {
            return this.uuid.equals(((mtgjson_card) obj).uuid);
        }
        return false;
    }

    @Override
    public int hashCode() {
        if (!hashInit) {
            // 7a5ab36e-15b0-51d1-87c0-b4b3a5cd93a2
            Scanner scanner = new Scanner(this.uuid);
            scanner.useDelimiter("-");
            while (scanner.hasNext()) {
                BigInteger bi = scanner.nextBigInteger(16);
                hashVal += bi.intValue();
            }
            hashInit = true;
        }
        return hashVal;
    }
}
