package com.gelakinetic.mtgfam.helpers.tcgp.JsonObjects;

@SuppressWarnings({"FieldCanBeLocal", "unused"})
public class CategoryGroups {
    public final String[] errors;
    public final Group[] results;
    private final boolean success;
    private final long totalItems;

    public CategoryGroups() {
        totalItems = 0;
        success = false;
        errors = null;
        results = null;
    }
}
