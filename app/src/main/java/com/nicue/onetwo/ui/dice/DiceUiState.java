package com.nicue.onetwo.ui.dice;

import java.util.ArrayList;
import java.util.List;

public class DiceUiState {
    private final ArrayList<DieUiModel> dice;
    private final int total;
    private final boolean hasLockedDice;
    private final boolean hasRollableDice;

    public DiceUiState(List<DieUiModel> dice) {
        this.dice = new ArrayList<>(dice);
        int total = 0;
        boolean hasLockedDice = false;
        boolean hasRollableDice = false;
        for (DieUiModel die : dice) {
            total += die.getValue();
            if (die.isLocked()) {
                hasLockedDice = true;
            } else {
                hasRollableDice = true;
            }
        }
        this.total = total;
        this.hasLockedDice = hasLockedDice;
        this.hasRollableDice = hasRollableDice;
    }

    public ArrayList<DieUiModel> getDice() {
        return new ArrayList<>(dice);
    }

    public int getTotal() {
        return total;
    }

    public boolean hasLockedDice() {
        return hasLockedDice;
    }

    public boolean hasRollableDice() {
        return hasRollableDice;
    }
}
