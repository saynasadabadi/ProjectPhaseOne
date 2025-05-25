package model;

import java.awt.Color;

public enum PacketAndPortShape {
    SQUARE(4, new Color(0, 150, 255), "Square", 1),
    TRIANGLE(3, new Color(255, 80, 80), "Triangle", 2);

    private final int numberOfSides;
    private final Color color;
    private final String displayName;
    private final int coinValue;

    PacketAndPortShape(int numberOfSides, Color color, String displayName, int coinValue) {
        this.numberOfSides = numberOfSides;
        this.color = color;
        this.displayName = displayName;
        this.coinValue = coinValue;
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

    public int getCoinValue() {
        return coinValue;
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