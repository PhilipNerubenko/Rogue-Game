package org.example.domain.dto;

public record VisibleMapDto(char[][] symbols, short[][] colors) {
    public char getSymbol(int x, int y) { return symbols[y][x]; }
    public short getColor(int x, int y) { return colors[y][x]; }
    public int width() { return symbols[0].length; }
    public int height() { return symbols.length; }
}
