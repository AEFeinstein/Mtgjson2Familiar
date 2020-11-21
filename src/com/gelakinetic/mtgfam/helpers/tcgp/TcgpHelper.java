package com.gelakinetic.mtgfam.helpers.tcgp;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;

import com.gelakinetic.mtgfam.helpers.tcgp.JsonObjects.CategoryGroups;
import com.gelakinetic.mtgfam.helpers.tcgp.JsonObjects.Group;
import com.google.gson.Gson;

public class TcgpHelper {
	
	TcgpApi api;
	String tokenStr;
	
	public TcgpHelper() throws FileNotFoundException, IOException {
        /* Initialize the API */
        api = new TcgpApi();

        /* Request a token. This will initialize the TcgpApi object */
        TcgpKeys keys = new Gson().fromJson(new InputStreamReader(new FileInputStream("tcgp_keys.json")), TcgpKeys.class);
        api.getAccessToken(keys.PUBLIC_KEY, keys.PRIVATE_KEY, keys.ACCESS_TOKEN);
	}
	
	/**
	 * Given a group ID, return the string expansion name. If it doesn't exist, use
	 * the API to download a list of all group IDs and names, then save the map to
	 * the disk
	 *
	 * @param api     The TcgpApi to query for group names
	 * @param context The context to access SharedPreferences
	 * @param groupId The group ID to query for
	 * @return The String name of the given group ID, or null
	 */
	public HashMap<Long, String> getGroupIds() throws IOException {
		HashMap<Long, String> groupIds = new HashMap<Long, String>();
		// Group is missing, download them all
		int[] offset = { 0 };
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
