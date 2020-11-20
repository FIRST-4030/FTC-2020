package org.firstinspires.ftc.teamcode.sensors.color_range;

import org.firstinspires.ftc.teamcode.config.Config;

public class ColorRangeConfig implements Config {
    public final String name;
    public final COLOR_RANGE_TYPES type;

    public ColorRangeConfig(COLOR_RANGE_TYPES type, String name) {
        this.name = name;
        this.type = type;
    }
}
