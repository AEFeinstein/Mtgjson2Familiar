package com.gelakinetic.mtgJson2Familiar.mtgjsonClasses;

import java.util.*;

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
        HashMap<String, Integer> cardsInFormat = new HashMap<>();
        cardsInFormat.put("brawl", 0);
        cardsInFormat.put("commander", 0);
        cardsInFormat.put("duel", 0);
        cardsInFormat.put("future", 0);
        cardsInFormat.put("frontier", 0);
        cardsInFormat.put("historic", 0);
        cardsInFormat.put("legacy", 0);
        cardsInFormat.put("modern", 0);
        cardsInFormat.put("pauper", 0);
        cardsInFormat.put("penny", 0);
        cardsInFormat.put("pioneer", 0);
        cardsInFormat.put("standard", 0);
        cardsInFormat.put("vintage", 0);

        int paperCards = 0;
        int arenaCards = 0;
        int rebalancedCards = 0;
        for (mtgjson_card c : this.cards) {

            if (c.availability.contains("paper") && !c.isRebalanced && !c.isOnlineOnly) {
                paperCards++;
            }
            if (c.availability.contains("arena")) {
                arenaCards++;
            }
            if (null != c.rebalancedPrintings) {
                rebalancedCards += c.rebalancedPrintings.size();
            }
            if (null != c.legalities.brawl) {
                cardsInFormat.put("brawl", cardsInFormat.get("brawl") + 1);
            }
            if (null != c.legalities.commander) {
                cardsInFormat.put("commander", cardsInFormat.get("commander") + 1);
            }
            if (null != c.legalities.duel) {
                cardsInFormat.put("duel", cardsInFormat.get("duel") + 1);
            }
            if (null != c.legalities.future) {
                cardsInFormat.put("future", cardsInFormat.get("future") + 1);
            }
            if (null != c.legalities.frontier) {
                cardsInFormat.put("frontier", cardsInFormat.get("frontier") + 1);
            }
            if (null != c.legalities.historic) {
                cardsInFormat.put("historic", cardsInFormat.get("historic") + 1);
            }
            if (null != c.legalities.legacy) {
                cardsInFormat.put("legacy", cardsInFormat.get("legacy") + 1);
            }
            if (null != c.legalities.modern) {
                cardsInFormat.put("modern", cardsInFormat.get("modern") + 1);
            }
            if (null != c.legalities.pauper) {
                cardsInFormat.put("pauper", cardsInFormat.get("pauper") + 1);
            }
            if (null != c.legalities.penny) {
                cardsInFormat.put("penny", cardsInFormat.get("penny") + 1);
            }
            if (null != c.legalities.pioneer) {
                cardsInFormat.put("pioneer", cardsInFormat.get("pioneer") + 1);
            }
            if (null != c.legalities.standard) {
                cardsInFormat.put("standard", cardsInFormat.get("standard") + 1);
            }
            if (null != c.legalities.vintage) {
                cardsInFormat.put("vintage", cardsInFormat.get("vintage") + 1);
            }
        }

        mtgjson_legalities legality = new mtgjson_legalities();
        if (this.isOnlineOnly) {
            legality.historic = (arenaCards > 0) ? "legal" : null;
        } else {
            legality.brawl = (cardsInFormat.get("brawl") >= (paperCards - rebalancedCards) ? "legal" : null);
            legality.commander = (cardsInFormat.get("commander") >= (paperCards - rebalancedCards) ? "legal" : null);
            legality.duel = (cardsInFormat.get("duel") >= (paperCards - rebalancedCards) ? "legal" : null);
            legality.future = (cardsInFormat.get("future") >= (paperCards - rebalancedCards) ? "legal" : null);
            legality.frontier = (cardsInFormat.get("frontier") >= (paperCards - rebalancedCards) ? "legal" : null);
            if (arenaCards > 0) {
                if (arenaCards == cardsInFormat.get("historic")) {
                    legality.historic = "legal";
                } else if ((arenaCards > 50) && cardsInFormat.get("historic") >= (arenaCards - rebalancedCards)) {
                    legality.historic = "legal";
                }
            }
            legality.legacy = (cardsInFormat.get("legacy") >= (paperCards - rebalancedCards) ? "legal" : null);
            legality.modern = (cardsInFormat.get("modern") >= (paperCards - rebalancedCards) ? "legal" : null);
            legality.pauper = (cardsInFormat.get("pauper") >= (paperCards - rebalancedCards) ? "legal" : null);
            legality.penny = (cardsInFormat.get("penny") >= (paperCards - rebalancedCards) ? "legal" : null);
            legality.pioneer = (cardsInFormat.get("pioneer") >= (paperCards - rebalancedCards) ? "legal" : null);
            legality.standard = (cardsInFormat.get("standard") >= (paperCards - rebalancedCards) ? "legal" : null);
            legality.vintage = (cardsInFormat.get("vintage") >= (paperCards - rebalancedCards) ? "legal" : null);
        }
        return legality;
    }

    /**
     * Remove all cards with duplicate uuids
     */
    public void removeDuplicateUuids() {
        // Create a HashSet from the cards, which is guaranteed to be unique
        HashSet<mtgjson_card> uniqueCards = new HashSet<>(this.cards.size());
        uniqueCards.addAll(this.cards);
        // If some cards were removed
        if (uniqueCards.size() != this.cards.size()) {
            // Clear the list and add only the unique cards
            this.cards.clear();
            this.cards.addAll(uniqueCards);
        }
    }

    public boolean isArenaOnly() {
        // First check if this is online only
        if (!this.isOnlineOnly) {
            return false;
        }

        // Then check if all cards are available on arena
        for (mtgjson_card card : this.cards) {
            if (!card.availability.contains("arena")) {
                return false;
            }
        }

        // Online only and all cards available on Arena
        return true;
    }
}
