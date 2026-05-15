package com.nicue.onetwo.ui.dice;

public class DieUiModel {
    private final int faces;
    private final int value;

    public DieUiModel(int faces, int value) {
        this.faces = faces;
        this.value = value;
    }

    public int getFaces() {
        return faces;
    }

    public int getValue() {
        return value;
    }
}
