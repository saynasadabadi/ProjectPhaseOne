package model;

import java.awt.Color;

public enum PacketAndPortShape {
    SQUARE(4, new Color(0, 150, 255), "Square"),
    TRIANGLE(3, new Color(255, 80, 80), "Triangle");

    private final int numberOfSides;
    private final Color color;
    private final String displayName;

    PacketAndPortShape(int numberOfSides, Color color, String displayName) {
        this.numberOfSides = numberOfSides;
        this.color = color;
        this.displayName = displayName;
    }

    public int getNumberOfSides() {
        return numberOfSides;
    }

    public Color getColor() {
        return color;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static PacketAndPortShape fromDisplayName(String name) {
        for (PacketAndPortShape shape : values()) {
            if (shape.displayName.equalsIgnoreCase(name)) {
                return shape;
            }
        }
        return SQUARE;
    }
}