package com.gelakinetic.GathererScraper.JsonTypes;

import java.util.ArrayList;

/**
 * This class contains all information of a patch.
 * It is mainly used to export to/import from a json file.:
 */
public class Patch {

    // The patch's expansion
    public Expansion mExpansion;

    // The patch's cards
    public ArrayList<Card> mCards;

    public Patch(Expansion exp, ArrayList<Card> crd) {
        this.mExpansion = exp;
        this.mCards = crd;
    }
}
