package com.brandon3055.draconicevolution.common.lib;

import com.brandon3055.draconicevolution.DETags;

/**
 * Created by Brandon on 24/02/2015.
 */
public class VersionHandler {

    public static final String VERSION = DETags.VERSION;

    public static final int SNAPSHOT = 0;
    public static final String MCVERSION = "1.7.10";

    @SuppressWarnings({ "unused" })
    public static final String FULL_VERSION = VERSION + (SNAPSHOT > 0 ? "-snapshot_" + SNAPSHOT : "");
}
