package com.gelakinetic.mtgJson2Familiar.mtgjsonClasses;

import com.gelakinetic.GathererScraper.JsonTypes.Card;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

@SuppressWarnings("unused")
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
    public HashMap<String, String> checkSetLegality() {
        HashMap<String, Integer> cardsInFormat = new HashMap<>();

//        int paperCards = 0;
//        int arenaCards = 0;
//        int rebalancedCards = 0;
        for (mtgjson_card c : this.cards) {

//            if (c.availability.contains("paper") && !c.isRebalanced && !c.isOnlineOnly) {
//                paperCards++;
//            }
//            if (c.availability.contains("arena")) {
//                arenaCards++;
//            }
//            if (null != c.rebalancedPrintings) {
//                rebalancedCards += c.rebalancedPrintings.size();
//            }

            for (String format : c.legalities.keySet()) {
                if (Card.isUsedFormat(format)) {
                    switch (c.legalities.get(format)) {
                        case "Legal":
                        case "Banned":
                        case "Restricted": {
                            cardsInFormat.merge(Card.beautifyFormat(format), 1, Integer::sum);
                            break;
                        }
                        default: {
                            break;
                        }
                    }
                }
            }
        }

        HashMap<String, String> setLegality = new HashMap<>();
        for (String format : cardsInFormat.keySet()) {
            int numCards = cardsInFormat.get(format);
            if (numCards > 0.8f * this.totalSetSize) {
                setLegality.put(format, "Legal");
            }
        }
        return setLegality;
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
