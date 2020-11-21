package com.gelakinetic.mtgfam.helpers.tcgp.JsonObjects;

public class Group {
    public final long groupId;
    public final String name;
    final String abbreviation;
    final boolean isSupplemental;
    final String publishedOn;
    final long categoryId;
    final String modifiedOn;

    public Group() {
        groupId = 0;
        name = null;
        abbreviation = null;
        isSupplemental = false;
        publishedOn = null;
        categoryId = 0;
        modifiedOn = null;
    }
}