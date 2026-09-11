package com.nicue.onetwo.ui.dice;

public class DieUiModel {
    private final long id;
    private final int faces;
    private final int value;
    private final boolean locked;

    public DieUiModel(long id, int faces, int value, boolean locked) {
        this.id = id;
        this.faces = faces;
        this.value = value;
        this.locked = locked;
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

    public boolean isLocked() {
        return locked;
    }
}
