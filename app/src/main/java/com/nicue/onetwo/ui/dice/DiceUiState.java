package com.nicue.onetwo.ui.dice;

import java.util.ArrayList;
import java.util.List;

public class DiceUiState {
    private final ArrayList<DieUiModel> dice;

    public DiceUiState(List<DieUiModel> dice) {
        this.dice = new ArrayList<>(dice);
    }

    public ArrayList<DieUiModel> getDice() {
        return new ArrayList<>(dice);
    }
}
