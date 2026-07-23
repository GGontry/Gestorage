package com.gontry.gestorage.client;

import com.gontry.gestorage.ModConstants;

public final class ClientState {
    private ClientState() {}

    private static int cachedSizeMode = ModConstants.MODE_NORMAL;

    public static int getCachedSizeMode() {
        return cachedSizeMode;
    }

    public static void setCachedSizeMode(int mode) {
        cachedSizeMode = mode;
    }
}
