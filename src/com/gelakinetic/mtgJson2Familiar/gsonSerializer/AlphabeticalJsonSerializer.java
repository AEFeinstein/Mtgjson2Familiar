package com.gelakinetic.mtgJson2Familiar.gsonSerializer;

import com.gelakinetic.GathererScraper.JsonTypes.Card;
import com.gelakinetic.GathererScraper.JsonTypes.Expansion;
import com.gelakinetic.GathererScraper.JsonTypes.LegalityData;
import com.gelakinetic.GathererScraper.JsonTypes.Manifest;
import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Custom class to serialize Card objects and omit mIsToken if it is false
 */
public class AlphabeticalJsonSerializer<T> implements JsonSerializer<T> {

    private static class KeyValuePair implements Comparable<KeyValuePair> {
        final String key;
        final JsonElement value;

        KeyValuePair(String k, JsonElement v) {
            key = k;
            value = v;
        }

        @Override
        public int compareTo(KeyValuePair o) {
            return this.key.compareTo(o.key);
        }
    }

    private final Gson mGson;

    public AlphabeticalJsonSerializer(boolean recursive) {
        GsonBuilder builder = new GsonBuilder()
                .setFieldNamingStrategy((new PrefixedFieldNamingStrategy("m")))
                .disableHtmlEscaping()
                .setPrettyPrinting();

        // Add serializers for nested objects, but only if non-recursive
        // Calling an object's constructor from the constructor is an easy way to do infinite recursion
        if (!recursive) {
            // These classes can be nested objects, so serializers need to be added here
            AlphabeticalJsonSerializer<Card> cardSerializer = new AlphabeticalJsonSerializer<>(true);
            AlphabeticalJsonSerializer<Expansion> expansionSerializer = new AlphabeticalJsonSerializer<>(true);
            AlphabeticalJsonSerializer<Manifest.ManifestEntry> manifestEntrySerializer = new AlphabeticalJsonSerializer<>(true);
            AlphabeticalJsonSerializer<LegalityData.Format> formatSerializer = new AlphabeticalJsonSerializer<>(true);

            builder.registerTypeHierarchyAdapter(Card.class, cardSerializer)
                    .registerTypeHierarchyAdapter(Expansion.class, expansionSerializer)
                    .registerTypeHierarchyAdapter(Manifest.ManifestEntry.class, manifestEntrySerializer)
                    .registerTypeHierarchyAdapter(LegalityData.Format.class, formatSerializer);
        }
        mGson = builder.create();
    }

    /**
     * Serialize the object, but sort the keys to be in alphabetical order
     *
     * @param obj                      The card to serialize
     * @param type                     The type, ignored
     * @param jsonSerializationContext A context, ignored
     * @return The serialized card as a JsonElement
     */
    @Override
    public JsonElement serialize(T obj, Type type, JsonSerializationContext jsonSerializationContext) {
        // Turn the object into a JSON element
        JsonElement je = mGson.toJsonTree(obj);

        // Extract all keys
        ArrayList<KeyValuePair> kvPairs = new ArrayList<>();
        for (String key : je.getAsJsonObject().keySet()) {
            boolean shouldAddField = true;

            // Hide some fields when serializing cards
            if (obj instanceof Card) {
                Card card = (Card) obj;
                if (!card.mIsToken && "isToken".equals(key)) {
                    shouldAddField = false;
                }
                if (!card.mIsOnlineOnly && "isOnlineOnly".equals(key)) {
                    shouldAddField = false;
                }
                if (shouldAddField) {
                    kvPairs.add(new KeyValuePair(key, je.getAsJsonObject().get(key)));
                }
            }

            kvPairs.add(new KeyValuePair(key, je.getAsJsonObject().get(key)));
        }
        // Sort all pairs by key
        Collections.sort(kvPairs);

        // Create a new JSON object
        JsonObject jObj = new JsonObject();

        // Add sorted keys, in order
        for (KeyValuePair kvp : kvPairs) {
            jObj.add(kvp.key, kvp.value);
        }

        // Return the JSON object, sorted by key
        return jObj;
    }
}