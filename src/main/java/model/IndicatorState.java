package model;

public enum IndicatorState {
    ON, OFF;

    public java.awt.Color getColor() {
        switch (this) {
            case ON:
                return java.awt.Color.CYAN;
            case OFF:
                return new java.awt.Color(60, 60, 60);
            default:
                return java.awt.Color.GRAY;
        }
    }
}