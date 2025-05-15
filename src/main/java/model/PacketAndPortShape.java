package model;

import java.awt.Color; // Import Color class

public enum PacketAndPortShape {
    SQUARE(4, Color.BLUE),
    TRIANGLE(3, Color.RED);

    private final int numberOfSides;
    private final Color color;

    private PacketAndPortShape(int numberOfSides, Color color) {
        this.numberOfSides = numberOfSides;
        this.color = color;
    }

    public int getNumberOfSides() {
        return numberOfSides;
    }

    public Color getColor() {
        return color;
    }
}