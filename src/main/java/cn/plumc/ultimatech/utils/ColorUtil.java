package cn.plumc.ultimatech.utils;

import org.joml.Vector3f;

public class ColorUtil {
    public static Vector3f toColor(int color) {
        return new Vector3f(color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF);
    }
}
