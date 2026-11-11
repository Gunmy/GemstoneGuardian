package game;

//What items can give off and charge each other with (see itemInteractions). Lightning comes as strikes from items
//with shakeLightning; the others reach the items near the one giving them off (item.emits). CONDUCTION is the pull
//a conductor (copper) has on lightning stones: they charge up the closer they are to it, even lying still. COLD is what
//obsidian and ice give off: it turns melted and heated things back (water into ice, magma into coal). SHAKE is being
//jerked around hard on the cursor (see gui): it breaks glass back into sand.
//There are two kinds of lightning, kept apart: LIGHTNING (yellow, boltstone, conducted by copper, pulled with CONDUCTION)
//and COSMIC (purple, meteorite, conducted by healite, pulled with COSMIC_CONDUCTION). A strike charges, heats and is
//passed on only by what reacts to, heats with or conducts its own kind (see item.shakeLightning and item.conducts)
public enum itemInfluence {
    LIGHTNING, RADIATION, HEAT, CONDUCTION, COLD, SHAKE, COSMIC, COSMIC_CONDUCTION,
    //What healite gives off: it brings burnt-out coal back to life as magma and takes the salt out of salt water,
    //with hearts flying over to it
    HEALING,
    //What saltstone gives off: it turns water into salt water, with white grains flying over to it
    SALT;

    //The kinds that come as strikes rather than being given off with emits
    public boolean isLightning () {return this == LIGHTNING || this == COSMIC;}
}
