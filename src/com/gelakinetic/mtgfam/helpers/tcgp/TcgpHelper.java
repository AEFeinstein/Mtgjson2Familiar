package com.gelakinetic.mtgfam.helpers.tcgp;

import com.gelakinetic.mtgfam.helpers.tcgp.JsonObjects.CategoryGroups;
import com.gelakinetic.mtgfam.helpers.tcgp.JsonObjects.Group;
import com.google.gson.Gson;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class TcgpHelper {

    TcgpApi api;
    String tokenStr;

    public TcgpHelper(File keyFile) throws IOException {
        /* Initialize the API */
        api = new TcgpApi();

        /* Request a token. This will initialize the TcgpApi object */
        TcgpKeys keys = new Gson().fromJson(new InputStreamReader(new FileInputStream(keyFile), StandardCharsets.UTF_8), TcgpKeys.class);
        //noinspection ConstantConditions
        api.getAccessToken(keys.PUBLIC_KEY, keys.PRIVATE_KEY, keys.ACCESS_TOKEN);
    }

    /**
     * Given a group ID, return the string expansion name. If it doesn't exist, use
     * the API to download a list of all group IDs and names, then save the map to
     * the disk
     *
     * @return The String name of the given group ID, or null
     */
    public HashMap<Long, String> getGroupIds() throws IOException {
        HashMap<Long, String> groupIds = new HashMap<>();
        // Group is missing, download them all
        int[] offset = {0};
        while (true) {
            CategoryGroups groups = api.getCategoryGroups(offset);
            // If there are errors or no groups left, break the loop
            if (groups.errors.length > 0 || groups.results.length == 0) {
                break;
            }
            // Add all groups to the map
            for (Group group : groups.results) {
                groupIds.put(group.groupId, group.name);
            }
        }
        return groupIds;
    }
}
