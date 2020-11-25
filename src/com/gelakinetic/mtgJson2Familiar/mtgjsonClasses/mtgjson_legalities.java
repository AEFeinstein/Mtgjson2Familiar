package com.gelakinetic.mtgJson2Familiar.mtgjsonClasses;

public class mtgjson_legalities {
    public String brawl;
    public String commander;
    public String duel;
    public String future;
    public String frontier;
    public String legacy;
    public String modern;
    public String pauper;
    public String penny;
    public String pioneer;
    public String standard;
    public String vintage;

    public void checkStrings() {
        checkString(this.brawl);
        checkString(this.commander);
        checkString(this.duel);
        checkString(this.future);
        checkString(this.frontier);
        checkString(this.legacy);
        checkString(this.modern);
        checkString(this.pauper);
        checkString(this.penny);
        checkString(this.pioneer);
        checkString(this.standard);
        checkString(this.vintage);
    }

    private static void checkString(String legality) {
        if (!(null == legality ||
                "Legal".equals(legality) ||
                "Banned".equals(legality) ||
                "Restricted".equals(legality))
        ) {
            System.err.println("Unknown legality ~~" + legality + "~~");
        }
    }
}
