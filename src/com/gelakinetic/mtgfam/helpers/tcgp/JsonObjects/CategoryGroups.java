package com.gelakinetic.mtgfam.helpers.tcgp.JsonObjects;

@SuppressWarnings("FieldCanBeLocal")
public class CategoryGroups {
    private final boolean success;
    public final String[] errors;
    public final Group[] results;
    private final long totalItems;

    public CategoryGroups() {
        totalItems = 0;
        success = false;
        errors = null;
        results = null;
    }
}
