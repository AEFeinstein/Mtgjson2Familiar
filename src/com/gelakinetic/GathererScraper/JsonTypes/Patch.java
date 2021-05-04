package com.gelakinetic.GathererScraper.JsonTypes;

import com.gelakinetic.mtgJson2Familiar.m2fLogger;

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

    /**
     * Set metadata and name for this patch, potentially from another patch
     *
     * @param otherPatch The other patch to maybe use metadata from
     * @param nameToUse  The name to write to metadata
     * @param mdToUse    The name of the expansion to use metadata from
     */
    public void setMetaData(Patch otherPatch, String nameToUse, String mdToUse) {
        //noinspection StatementWithEmptyBody
        if (this.mExpansion.mName_gatherer.equals(mdToUse)) {
            // Already using the right metadata
            this.mExpansion.mName_gatherer = nameToUse;
        } else if (otherPatch.mExpansion.mName_gatherer.equals(mdToUse)) {
            this.mExpansion = otherPatch.mExpansion;
            this.mExpansion.mName_gatherer = nameToUse;
        } else {
            m2fLogger.log(m2fLogger.LogLevel.ERROR, "Error setting metadata, ~" + this.mExpansion.mName_gatherer + "~, ~" + otherPatch.mExpansion.mName_gatherer + "~");
        }
    }
}
