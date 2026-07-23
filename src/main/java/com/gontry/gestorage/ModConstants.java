package com.gontry.gestorage;

public final class ModConstants {
    private ModConstants() {}

    public static final int NORMAL_ENDER_SIZE = 27;
    public static final int LARGE_ENDER_SIZE = 54;
    public static final int EXTRA_LARGE_ENDER_SIZE = 228;
    public static final int SHULKER_BOX_SIZE = 27;
    public static final int VANILLA_ENDER_SIZE = 27;

    public static final int MODE_NORMAL = 0;
    public static final int MODE_LARGE = 1;
    public static final int MODE_EXTRA_LARGE = 2;

    public static final int MAX_SYNC_DELAY_TICKS = 5;

    public static int getEnderSizeForMode(int mode) {
        return switch (mode) {
            case MODE_LARGE -> LARGE_ENDER_SIZE;
            case MODE_EXTRA_LARGE -> EXTRA_LARGE_ENDER_SIZE;
            default -> NORMAL_ENDER_SIZE;
        };
    }
}
