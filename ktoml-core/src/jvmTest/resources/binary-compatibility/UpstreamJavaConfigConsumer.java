package com.akuleshov7.ktoml.binarycompatibility.fixtures;

import com.akuleshov7.ktoml.TomlInputConfig;

public final class UpstreamJavaConfigConsumer {
    public static TomlInputConfig construct() {
        return new TomlInputConfig(true, false, true, false, true, false);
    }

    public static TomlInputConfig copy(TomlInputConfig config) {
        return config.copy(false, true, false, true, false, true);
    }
}
