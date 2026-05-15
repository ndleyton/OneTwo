package com.nicue.onetwo.ui.dice;

import java.util.ArrayList;
import java.util.List;

public class DiceUiState {
    private final ArrayList<DieUiModel> dice;
    private final int total;

    public DiceUiState(List<DieUiModel> dice) {
        this.dice = new ArrayList<>(dice);
        int total = 0;
        for (DieUiModel die : dice) {
            total += die.getValue();
        }
        this.total = total;
    }

    public ArrayList<DieUiModel> getDice() {
        return new ArrayList<>(dice);
    }

    public int getTotal() {
        return total;
    }
}
