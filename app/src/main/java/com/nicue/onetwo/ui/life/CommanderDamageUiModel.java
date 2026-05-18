package com.nicue.onetwo.ui.life;

public class CommanderDamageUiModel {
    private final int sourceSeatIndex;
    private final int amount;
    private final boolean self;
    private final boolean lethal;
    private final int backgroundColorRes;
    private final int foregroundColorRes;

    public CommanderDamageUiModel(
            int sourceSeatIndex,
            int amount,
            boolean self,
            boolean lethal,
            int backgroundColorRes,
            int foregroundColorRes) {
        this.sourceSeatIndex = sourceSeatIndex;
        this.amount = amount;
        this.self = self;
        this.lethal = lethal;
        this.backgroundColorRes = backgroundColorRes;
        this.foregroundColorRes = foregroundColorRes;
    }

    public int getSourceSeatIndex() {
        return sourceSeatIndex;
    }

    public int getAmount() {
        return amount;
    }

    public boolean isSelf() {
        return self;
    }

    public boolean isLethal() {
        return lethal;
    }

    public int getBackgroundColorRes() {
        return backgroundColorRes;
    }

    public int getForegroundColorRes() {
        return foregroundColorRes;
    }
}
