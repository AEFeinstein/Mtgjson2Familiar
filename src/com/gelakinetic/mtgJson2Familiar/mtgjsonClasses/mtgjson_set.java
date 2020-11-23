package com.gelakinetic.mtgJson2Familiar.mtgjsonClasses;

import java.util.List;

public class mtgjson_set {
    public int baseSetSize;
    public String block;
    public mtgjson_booster booster;
    public List<mtgjson_card> cards;
    public String code;
    public String codeV3;
    public boolean isForeignOnly;
    public boolean isFoilOnly;
    public boolean isNonFoilOnly;
    public boolean isOnlineOnly;
    public boolean isPaperOnly;
    public boolean isPartialPreview;
    public String keyruneCode;
    public String mcmName;
    public int mcmId;
    public String mtgoCode;
    public String name;
    public String parentCode;
    public String releaseDate;
    public int tcgplayerGroupId;
    public List<mtgjson_token> tokens;
    public int totalSetSize;
    public mtgjson_translation translations;
    public String type;

    /**
     * @return The legality of this set, based on the legality of the cards in this set
     */
    public mtgjson_legalities checkSetLegality() {
        mtgjson_legalities legality = new mtgjson_legalities();
        legality.brawl = "legal";
        legality.commander = "legal";
        legality.duel = "legal";
        legality.future = "legal";
        legality.frontier = "legal";
        legality.legacy = "legal";
        legality.modern = "legal";
        legality.pauper = "legal";
        legality.penny = "legal";
        legality.pioneer = "legal";
        legality.standard = "legal";
        legality.vintage = "legal";

        for (mtgjson_card c : this.cards) {
            if (null == c.legalities.brawl) {
                legality.brawl = null;
            }
            if (null == c.legalities.commander) {
                legality.commander = null;
            }
            if (null == c.legalities.duel) {
                legality.duel = null;
            }
            if (null == c.legalities.future) {
                legality.future = null;
            }
            if (null == c.legalities.frontier) {
                legality.frontier = null;
            }
            if (null == c.legalities.legacy) {
                legality.legacy = null;
            }
            if (null == c.legalities.modern) {
                legality.modern = null;
            }
            if (null == c.legalities.pauper) {
                legality.pauper = null;
            }
            if (null == c.legalities.penny) {
                legality.penny = null;
            }
            if (null == c.legalities.pioneer) {
                legality.pioneer = null;
            }
            if (null == c.legalities.standard) {
                legality.standard = null;
            }
            if (null == c.legalities.vintage) {
                legality.vintage = null;
            }
        }
        return legality;
    }
}
