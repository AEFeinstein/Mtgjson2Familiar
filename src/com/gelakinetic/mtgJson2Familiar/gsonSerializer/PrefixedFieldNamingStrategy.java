package com.gelakinetic.mtgJson2Familiar.gsonSerializer;

import com.google.gson.FieldNamingStrategy;

import java.lang.reflect.Field;

/**
 * Implementation of a FieldNamingStrategy that translate a field's name by removing the prefix from it
 * and lowercase the following character.
 */
public class PrefixedFieldNamingStrategy implements FieldNamingStrategy {

    /**
     * The prefix to be match on the field's name
     */
    private final String mPrefix;

    /**
     * Create a PrefixedFieldNamingStrategy object that check for a prefix on the field's name,
     * and call transformName on the remaining of the field's name.
     *
     * @param prefix The prefix to check of the field's name.
     */
    public PrefixedFieldNamingStrategy(String prefix) {
        mPrefix = prefix;
    }

    /**
     * Return a string with the first letter being lowercase.
     *
     * @param s the string to transform.
     * @return A string with a lowercase first letter
     */
    private static String lowercaseFirstLetter(String s) {
        if (!s.isEmpty()) {
            char[] c = s.toCharArray();
            c[0] = Character.toLowerCase(c[0]);
            s = new String(c);
        }
        return s;
    }

    @Override
    public String translateName(final Field f) {
        String name = f.getName();
        if (name.startsWith(mPrefix)) {
            return PrefixedFieldNamingStrategy.lowercaseFirstLetter(name.substring(mPrefix.length()));
        } else {
            throw new IllegalArgumentException("Don't know how to handle field not starting with m prefix: " + name);
        }
    }
}
