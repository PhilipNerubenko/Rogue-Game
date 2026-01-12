package org.example.presentation.dto;

public class VisibleMapDto {
    private final char[][] symbols;
    private final short[][] colors; // цвет для каждой клетки

    public VisibleMapDto(char[][] symbols, short[][] colors) {
        this.symbols = symbols;
        this.colors = colors;
    }

    public char getSymbol(int x, int y) { return symbols[y][x]; }
    public short getColor(int x, int y) { return colors[y][x]; }
    public int width() { return symbols[0].length; }
    public int height() { return symbols.length; }
}
