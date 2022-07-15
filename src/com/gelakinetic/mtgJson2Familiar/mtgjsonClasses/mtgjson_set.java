package com.gelakinetic.mtgJson2Familiar.mtgjsonClasses;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
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

    private static final List<String> bannedInHistoricJMP = Arrays.asList(
            "Ajani's Chosen",
            "Angelic Arbiter",
            "Path to Exile",
            "Read the Runes",
            "Rhystic Study",
            "Thought Scour",
            "Exhume",
            "Mausoleum Turnkey",
            "Reanimate",
            "Scourge of Nel Toth",
            "Ball Lightning",
            "Chain Lightning",
            "Draconic Roar",
            "Flametongue Kavu",
            "Goblin Lore",
            "Fa'adiyah Seer",
            "Scrounging Bandar",
            "Time to Feed");

    private static final List<String> bannedInHistoricJ21 = Arrays.asList(
            "Fog",
            "Kraken Hatchling",
            "Ponder",
            "Regal Force",
            "Stormfront Pegasus",
            "Force Spike",
            "Assault Strobe",
            "Tropical Island");

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
        legality.historic = "legal";
        legality.legacy = "legal";
        legality.modern = "legal";
        legality.pauper = "legal";
        legality.penny = "legal";
        legality.pioneer = "legal";
        legality.standard = "legal";
        legality.vintage = "legal";

        boolean isOnArena = isOnArena();
        // Figure out what, if any, arena patched cards are in this set
        ArrayList<String> arenaUnbalancedCards = new ArrayList<>();
        for (mtgjson_card c : this.cards) {
            // If this is an arena-only patched card
            if ( (c.isRebalanced || c.name.startsWith("A-")) && c.availability.contains("arena")) {
                // Add it to the patched and unpatched lists
                arenaUnbalancedCards.add(c.name.replaceAll("A-", ""));
            }
        }

        for (mtgjson_card c : this.cards) {

            // Ignore rebalanced alchemy cards for legality checks
            if ("alchemy".equals(this.type) &&
                    (null == c.legalities.brawl) &&
                    (null == c.legalities.commander) &&
                    (null == c.legalities.duel) &&
                    (null == c.legalities.future) &&
                    (null == c.legalities.frontier) &&
                    (null == c.legalities.historic) &&
                    (null == c.legalities.legacy) &&
                    (null == c.legalities.modern) &&
                    (null == c.legalities.pauper) &&
                    (null == c.legalities.penny) &&
                    (null == c.legalities.pioneer) &&
                    (null == c.legalities.standard) &&
                    (null == c.legalities.vintage)) {
                continue;
            }

            // These cards are conjured, not legal for deck construction
            if ((this.code.equals("JMP") && bannedInHistoricJMP.contains(c.name)) ||
                    (this.code.equals("J21") && bannedInHistoricJ21.contains(c.name))) {
                c.legalities.historic = "Banned";
            }

            // Ignore the patched arena cards for legality checks
            if (arenaUnbalancedCards.contains(c.name)) {
                continue;
            }

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
            // If this card is an unpatched version, ignore it for historic legality
            if (null == c.legalities.historic && !isOnArena && !arenaUnbalancedCards.contains(c.name)) {
                legality.historic = null;
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

    /**
     * It counts as a set on arena if more than 50% of the cards are available on arena
     *
     * @return true or false
     */
    public boolean isOnArena() {
        int arenaCards = 0;
        for (mtgjson_card card : this.cards) {
            if (card.availability.contains("arena")) {
                arenaCards++;
            }
        }
        return arenaCards > this.cards.size() / 2;
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
