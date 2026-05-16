package com.nicue.onetwo.ui.dice;

public class DieUiModel {
    private final long id;
    private final int faces;
    private final int value;

    public DieUiModel(long id, int faces, int value) {
        this.id = id;
        this.faces = faces;
        this.value = value;
    }

    public long getId() {
        return id;
    }

    public int getFaces() {
        return faces;
    }

    public int getValue() {
        return value;
    }
}
