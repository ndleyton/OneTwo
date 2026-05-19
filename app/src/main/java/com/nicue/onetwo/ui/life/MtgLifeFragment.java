package com.nicue.onetwo.ui.life;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.Editable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.core.widget.TextViewCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.nicue.onetwo.R;
import com.nicue.onetwo.databinding.LifeBoard1Binding;
import com.nicue.onetwo.databinding.LifeBoard2Binding;
import com.nicue.onetwo.databinding.LifeBoard3Binding;
import com.nicue.onetwo.databinding.LifeBoard4Binding;
import com.nicue.onetwo.databinding.LifeBoard5Binding;
import com.nicue.onetwo.databinding.LifeBoard6Binding;
import com.nicue.onetwo.databinding.LifeFragmentBinding;
import com.nicue.onetwo.databinding.LifePlayerCellBinding;
import com.nicue.onetwo.databinding.LifeSetupContentBinding;
import com.nicue.onetwo.databinding.MinutesAlertDialogBinding;
import com.nicue.onetwo.utils.TimerBackend;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MtgLifeFragment extends Fragment implements MenuProvider {
    private static final int PREVIEW_PLAYER_COUNT = 4;
    private static final int LIFE_LONG_PRESS_DELTA = 10;
    private static final long LIFE_HOLD_REPEAT_INTERVAL_MS = 800L;

    private LifeFragmentBinding binding;
    private MtgLifeViewModel viewModel;
    private boolean inputsInitialized = false;
    private int currentBoardPlayerCount = -1;
    private Integer activeDialogDefenderSeatIndex = null;
    private final Map<Integer, TextView> activeDialogDamageTextViews = new HashMap<>();
    private final Handler holdRepeatHandler = new Handler(Looper.getMainLooper());
    private final Map<View, Runnable> activeLifeHoldRunnables = new HashMap<>();

    @Nullable @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = LifeFragmentBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(MtgLifeViewModel.class);
        LifeSetupContentBinding setupBinding =
                LifeSetupContentBinding.bind(binding.setupContent.getRoot());

        viewModel
                .getUiState()
                .observe(
                        getViewLifecycleOwner(),
                        uiState -> {
                            requireActivity().invalidateOptionsMenu();
                            syncActiveCommanderDamageDialog(uiState);

                            if (uiState.isShowingSetup()) {
                                renderSetupState(uiState, setupBinding);
                            } else {
                                renderGameState(uiState);
                            }
                        });

        setupBinding.startGameButton.setOnClickListener(
                v -> {
                    Editable playersText = setupBinding.playersInput.getText();
                    Editable lifeText = setupBinding.lifeInput.getText();
                    String playersStr = playersText != null ? playersText.toString() : "";
                    String lifeStr = lifeText != null ? lifeText.toString() : "";
                    boolean commanderEnabled = setupBinding.commanderDamageSwitch.isChecked();
                    boolean turnTimerEnabled = setupBinding.turnTimerSwitch.isChecked();
                    viewModel.validateAndStartGame(
                            playersStr, lifeStr, commanderEnabled, turnTimerEnabled);
                });

        binding.setupOverlay.setOnClickListener(v -> viewModel.dismissSetup());
        setupBinding.getRoot().setOnClickListener(v -> {});

        MenuHost menuHost = requireActivity();
        menuHost.addMenuProvider(this, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
    }

    @Override
    public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.life_actions, menu);
        MenuItem newGameItem = menu.findItem(R.id.action_new_game);
        MtgLifeUiState currentUiState =
                viewModel != null ? viewModel.getUiState().getValue() : null;
        if (newGameItem != null && currentUiState != null) {
            newGameItem.setVisible(!currentUiState.isShowingSetup());
        }
    }

    @Override
    public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
        if (menuItem.getItemId() == R.id.action_new_game) {
            viewModel.resetToSetup();
            return true;
        }
        return false;
    }

    private void syncActiveCommanderDamageDialog(MtgLifeUiState uiState) {
        if (activeDialogDefenderSeatIndex == null) {
            return;
        }

        LifePlayerUiModel defender =
                findPlayerBySeat(uiState.getPlayers(), activeDialogDefenderSeatIndex);
        if (defender == null) {
            return;
        }

        for (CommanderDamageUiModel damage : defender.getCommanderDamages()) {
            TextView damageTextView = activeDialogDamageTextViews.get(damage.getSourceSeatIndex());
            if (damageTextView == null) {
                continue;
            }

            damageTextView.setText(String.valueOf(damage.getAmount()));
            if (damage.isLethal()) {
                damageTextView.setTextColor(
                        ContextCompat.getColor(requireContext(), R.color.secondAccent));
            } else {
                Object tag = damageTextView.getTag();
                if (tag instanceof Integer color) {
                    damageTextView.setTextColor(color);
                }
            }
        }
    }

    private void renderSetupState(MtgLifeUiState uiState, LifeSetupContentBinding setupBinding) {
        binding.setupOverlay.setVisibility(View.VISIBLE);
        binding.setupContent.getRoot().setVisibility(View.VISIBLE);
        binding.boardContainer.setVisibility(View.VISIBLE);

        ensureBoardInflated(PREVIEW_PLAYER_COUNT);
        bindPreviewBoard();

        if (!inputsInitialized) {
            setupBinding.playersInput.setText(String.valueOf(uiState.getPlayerCount()));
            setupBinding.lifeInput.setText(String.valueOf(uiState.getStartingLife()));
            setupBinding.commanderDamageSwitch.setChecked(uiState.isCommanderDamageEnabled());
            setupBinding.turnTimerSwitch.setChecked(uiState.isTurnTimerEnabled());
            long durationMs = viewModel.getTurnTimerDurationMs();
            setupBinding.btnTurnTimerValue.setText(
                    TimerBackend.formatRemainingTime(durationMs, 10000L));
            setupBinding.turnTimerValueRow.setVisibility(
                    uiState.isTurnTimerEnabled() ? View.VISIBLE : View.GONE);

            setupBinding.turnTimerSwitch.setOnCheckedChangeListener(
                    (buttonView, isChecked) -> {
                        setupBinding.turnTimerValueRow.setVisibility(
                                isChecked ? View.VISIBLE : View.GONE);
                        viewModel.setTurnTimerEnabled(isChecked);
                    });

            setupBinding.btnTurnTimerValue.setOnClickListener(
                    v -> showTurnTimerDurationPicker(setupBinding));

            inputsInitialized = true;
        }

        setupBinding.playersInputLayout.setError(
                uiState.getPlayersErrorResId() != null
                        ? getString(uiState.getPlayersErrorResId())
                        : null);
        setupBinding.lifeInputLayout.setError(
                uiState.getLifeErrorResId() != null
                        ? getString(uiState.getLifeErrorResId())
                        : null);
    }

    private void renderGameState(MtgLifeUiState uiState) {
        binding.setupOverlay.setVisibility(View.GONE);
        binding.setupContent.getRoot().setVisibility(View.GONE);
        binding.boardContainer.setVisibility(View.VISIBLE);
        inputsInitialized = false;

        ensureBoardInflated(uiState.getPlayerCount());
        bindGameBoard(uiState);
    }

    private void ensureBoardInflated(int playerCount) {
        if (binding.boardContainer.getChildCount() != 0 && currentBoardPlayerCount == playerCount) {
            return;
        }

        binding.boardContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        currentBoardPlayerCount = playerCount;

        switch (playerCount) {
            case 1 -> LifeBoard1Binding.inflate(inflater, binding.boardContainer, true);
            case 2 -> LifeBoard2Binding.inflate(inflater, binding.boardContainer, true);
            case 3 -> LifeBoard3Binding.inflate(inflater, binding.boardContainer, true);
            case 4 -> LifeBoard4Binding.inflate(inflater, binding.boardContainer, true);
            case 5 -> LifeBoard5Binding.inflate(inflater, binding.boardContainer, true);
            case 6 -> LifeBoard6Binding.inflate(inflater, binding.boardContainer, true);
            default ->
                    throw new IllegalArgumentException("Unsupported player count: " + playerCount);
        }
    }

    private void bindPreviewBoard() {
        for (int seatIndex = 0; seatIndex < PREVIEW_PLAYER_COUNT; seatIndex++) {
            View seatView = binding.boardContainer.findViewById(getSeatViewId(seatIndex));
            if (seatView == null) {
                continue;
            }

            LifePlayerCellBinding cellBinding = LifePlayerCellBinding.bind(seatView);
            int backgroundColor =
                    ContextCompat.getColor(
                            requireContext(), getPreviewBackgroundColorRes(seatIndex));
            int foregroundColor = getForegroundColor(backgroundColor);

            cellBinding.playerCellContainer.setBackgroundColor(backgroundColor);
            cellBinding.tvLifeCount.setText(getString(R.string.default_mtg_life));
            cellBinding.tvLifeCount.setTextColor(foregroundColor);
            cellBinding.btnMinus.setIconTint(ColorStateList.valueOf(foregroundColor));
            cellBinding.btnPlus.setIconTint(ColorStateList.valueOf(foregroundColor));
            cellBinding.commanderDamageGrid.setVisibility(View.GONE);
            cellBinding.timerContainer.setVisibility(View.GONE);
            cellBinding.timerContainer.setOnClickListener(null);
            cellBinding.playerCellContainer.setRotation(0f);
            cellBinding.innerPlayerLayout.setRotation(seatIndex % 2 == 0 ? 90f : 270f);
            clearRecentLifeChange(cellBinding.tvRecentLifeChangeNegative);
            clearRecentLifeChange(cellBinding.tvRecentLifeChangePositive);
        }
    }

    private void bindGameBoard(MtgLifeUiState uiState) {
        int playerCount = uiState.getPlayerCount();
        for (int seatIndex = 0; seatIndex < playerCount; seatIndex++) {
            View seatView = binding.boardContainer.findViewById(getSeatViewId(seatIndex));
            if (seatView == null) {
                continue;
            }

            LifePlayerCellBinding cellBinding = LifePlayerCellBinding.bind(seatView);
            bindPlayerCell(cellBinding, uiState.getPlayers().get(seatIndex), playerCount);
        }
    }

    private void bindPlayerCell(
            LifePlayerCellBinding cellBinding, LifePlayerUiModel player, int playerCount) {
        int backgroundColor =
                player.isTimerExpired()
                        ? ContextCompat.getColor(requireContext(), R.color.mtg_expired_background)
                        : ContextCompat.getColor(requireContext(), player.getBackgroundColorRes());
        int foregroundColor =
                player.isTimerExpired()
                        ? ContextCompat.getColor(requireContext(), R.color.mtg_expired_foreground)
                        : getForegroundColor(backgroundColor);
        int seatIndex = player.getSeatIndex();

        TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                cellBinding.tvLifeCount,
                20,
                getLifeCountMaxSizeSp(playerCount),
                2,
                TypedValue.COMPLEX_UNIT_SP);

        cellBinding.tvLifeCount.setText(String.valueOf(player.getLifeTotal()));
        cellBinding.tvLifeCount.setContentDescription(String.valueOf(player.getLifeTotal()));
        cellBinding.tvLifeCount.setTextColor(foregroundColor);
        cellBinding.playerCellContainer.setBackgroundColor(backgroundColor);
        cellBinding.innerPlayerLayout.setRotation(player.getRotationDegrees());
        bindRecentLifeChange(
                cellBinding,
                player.getRecentLifeChange(),
                player.getRecentLifeChangeTimestampMs(),
                foregroundColor);

        cellBinding.btnMinus.setIconTint(ColorStateList.valueOf(foregroundColor));
        cellBinding.btnPlus.setIconTint(ColorStateList.valueOf(foregroundColor));
        cellBinding.lifeDecrementZone.setContentDescription(
                getString(R.string.mtg_btn_minus_desc, seatIndex + 1));
        cellBinding.lifeIncrementZone.setContentDescription(
                getString(R.string.mtg_btn_plus_desc, seatIndex + 1));
        cellBinding.lifeDecrementZone.setOnClickListener(v -> viewModel.decrementLife(seatIndex));
        cellBinding.lifeIncrementZone.setOnClickListener(v -> viewModel.incrementLife(seatIndex));
        cellBinding.lifeDecrementZone.setOnLongClickListener(
                v -> {
                    viewModel.decrementLifeBy(seatIndex, LIFE_LONG_PRESS_DELTA);
                    startLifeHoldRepeat(
                            v, () -> viewModel.decrementLifeBy(seatIndex, LIFE_LONG_PRESS_DELTA));
                    return true;
                });
        cellBinding.lifeIncrementZone.setOnLongClickListener(
                v -> {
                    viewModel.incrementLifeBy(seatIndex, LIFE_LONG_PRESS_DELTA);
                    startLifeHoldRepeat(
                            v, () -> viewModel.incrementLifeBy(seatIndex, LIFE_LONG_PRESS_DELTA));
                    return true;
                });
        cellBinding.lifeDecrementZone.setOnTouchListener(this::handleLifeHoldTouch);
        cellBinding.lifeIncrementZone.setOnTouchListener(this::handleLifeHoldTouch);

        if (player.isTimerVisible()) {
            cellBinding.timerContainer.setVisibility(View.VISIBLE);
            cellBinding.tvTurnTimer.setText(player.getTimerDisplay());
            cellBinding.tvTurnTimer.setTextColor(foregroundColor);
            cellBinding.tvTurnTimer.setContentDescription(
                    getString(
                            R.string.mtg_player_timer_desc,
                            seatIndex + 1,
                            player.getTimerDisplay()));

            cellBinding.tvTurnTimer.setAlpha(player.isTimerActive() ? 1.0f : 0.5f);
            cellBinding.tvTurnTimer.setTypeface(
                    null, player.isTimerActive() ? Typeface.BOLD : Typeface.NORMAL);

            cellBinding.btnPassTurn.setEnabled(player.isPassEnabled());
            cellBinding.btnPassTurn.setVisibility(
                    player.isPassEnabled() ? View.VISIBLE : View.INVISIBLE);
            cellBinding.btnPassTurn.setIconTint(ColorStateList.valueOf(foregroundColor));

            cellBinding.timerContainer.setEnabled(player.isPassEnabled());
            cellBinding.timerContainer.setContentDescription(
                    getString(R.string.mtg_pass_turn_desc, seatIndex + 1));
            cellBinding.timerContainer.setOnClickListener(
                    v -> {
                        vibrate(30L);
                        viewModel.passTurn(seatIndex);
                    });
        } else {
            cellBinding.timerContainer.setVisibility(View.GONE);
            cellBinding.timerContainer.setOnClickListener(null);
        }

        bindCommanderDamageSummary(cellBinding, player, playerCount);

        cellBinding.playerCellContainer.setRotation(0f);
    }

    private void bindCommanderDamageSummary(
            LifePlayerCellBinding cellBinding, LifePlayerUiModel player, int totalPlayers) {
        if (!player.isCommanderDamageVisible()) {
            cellBinding.commanderDamageGrid.setVisibility(View.GONE);
            cellBinding.commanderDamageGrid.setOnClickListener(null);
            return;
        }

        cellBinding.commanderDamageGrid.setVisibility(View.VISIBLE);
        cellBinding.commanderDamageGrid.removeAllViews();

        int rows = getCommanderGridRowCount(totalPlayers);
        int cols = getCommanderGridColumnCount(totalPlayers);
        List<CommanderDamageUiModel> damages = player.getCommanderDamages();
        int seatIndex = player.getSeatIndex();

        for (int rowIndex = 0; rowIndex < rows; rowIndex++) {
            LinearLayout rowLayout = new LinearLayout(requireContext());
            rowLayout.setOrientation(LinearLayout.HORIZONTAL);
            rowLayout.setLayoutParams(
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT));

            for (int columnIndex = 0; columnIndex < cols; columnIndex++) {
                int slotIndex = rowIndex * cols + columnIndex;
                int sourceSeatIndex = getSourceSeatForGridSlot(slotIndex, seatIndex, totalPlayers);
                if (sourceSeatIndex < damages.size()) {
                    rowLayout.addView(
                            createCommanderSummaryCell(damages.get(sourceSeatIndex), seatIndex));
                } else {
                    rowLayout.addView(createCommanderSummarySpacer());
                }
            }

            cellBinding.commanderDamageGrid.addView(rowLayout);
        }

        cellBinding.commanderDamageGrid.setOnClickListener(
                v -> showCommanderDamageDialog(seatIndex));
        cellBinding.commanderDamageGrid.setContentDescription(
                getString(R.string.mtg_commander_damage_manage_desc, seatIndex + 1));
    }

    private View createCommanderSummaryCell(CommanderDamageUiModel damage, int defenderSeatIndex) {
        TextView summaryCell = new TextView(requireContext());
        summaryCell.setGravity(Gravity.CENTER);
        summaryCell.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        summaryCell.setTypeface(null, Typeface.BOLD);
        summaryCell.setLayoutParams(createSmallGridLayoutParams(16));

        if (damage.isSelf()) {
            summaryCell.setVisibility(View.INVISIBLE);
            return summaryCell;
        }

        summaryCell.setText(String.valueOf(damage.getAmount()));

        int backgroundColor =
                ContextCompat.getColor(requireContext(), damage.getBackgroundColorRes());
        int foregroundColor =
                ContextCompat.getColor(requireContext(), damage.getForegroundColorRes());

        GradientDrawable background = new GradientDrawable();
        background.setCornerRadius(dpToPx(4));
        background.setColor(backgroundColor);
        summaryCell.setBackground(background);
        summaryCell.setTextColor(foregroundColor);
        summaryCell.setContentDescription(
                getString(
                        R.string.mtg_commander_damage_cell_desc,
                        damage.getSourceSeatIndex() + 1,
                        defenderSeatIndex + 1,
                        damage.getAmount()));

        return summaryCell;
    }

    private void bindRecentLifeChange(
            LifePlayerCellBinding cellBinding,
            int recentLifeChange,
            long recentLifeChangeTimestampMs,
            int foregroundColor) {
        long ageMs = SystemClock.elapsedRealtime() - recentLifeChangeTimestampMs;
        if (recentLifeChange == 0
                || recentLifeChangeTimestampMs <= 0
                || ageMs >= MtgLifeViewModel.RECENT_LIFE_CHANGE_WINDOW_MS) {
            clearRecentLifeChange(cellBinding.tvRecentLifeChangeNegative);
            clearRecentLifeChange(cellBinding.tvRecentLifeChangePositive);
            return;
        }

        if (recentLifeChange < 0) {
            clearRecentLifeChange(cellBinding.tvRecentLifeChangePositive);
            bindRecentLifeChangeIndicator(
                    cellBinding.tvRecentLifeChangeNegative,
                    String.valueOf(recentLifeChange),
                    recentLifeChangeTimestampMs,
                    foregroundColor,
                    false,
                    ageMs);
            return;
        }

        clearRecentLifeChange(cellBinding.tvRecentLifeChangeNegative);
        bindRecentLifeChangeIndicator(
                cellBinding.tvRecentLifeChangePositive,
                "+" + recentLifeChange,
                recentLifeChangeTimestampMs,
                foregroundColor,
                true,
                ageMs);
    }

    private void bindRecentLifeChangeIndicator(
            TextView textView,
            String indicatorText,
            long recentLifeChangeTimestampMs,
            int foregroundColor,
            boolean isPositive,
            long ageMs) {
        Object existingTag = textView.getTag();
        boolean isSameLifeChange =
                existingTag instanceof Long timestamp
                        && timestamp == recentLifeChangeTimestampMs
                        && textView.getVisibility() == View.VISIBLE;

        textView.setVisibility(View.VISIBLE);
        textView.setText(indicatorText);
        textView.setTextColor(foregroundColor);
        textView.setContentDescription(indicatorText);
        textView.setTag(recentLifeChangeTimestampMs);

        if (isSameLifeChange) {
            return;
        }

        float baseTranslationX = 0f;
        float startOffsetX = baseTranslationX + (isPositive ? dpToPx(8) : -dpToPx(8));
        float startOffsetY = -dpToPx(8);
        textView.animate().cancel();
        textView.setAlpha(0f);
        textView.setTranslationX(startOffsetX);
        textView.setTranslationY(startOffsetY);
        textView.animate()
                .alpha(1f)
                .translationX(baseTranslationX)
                .translationY(0f)
                .setDuration(300)
                .setInterpolator(new android.view.animation.OvershootInterpolator())
                .start();

        long remainingMs = MtgLifeViewModel.RECENT_LIFE_CHANGE_WINDOW_MS - ageMs;
        textView.postDelayed(
                () -> {
                    Object tag = textView.getTag();
                    if (tag instanceof Long timestamp && timestamp == recentLifeChangeTimestampMs) {
                        textView.animate()
                                .alpha(0f)
                                .translationX(startOffsetX)
                                .translationY(startOffsetY)
                                .setDuration(300)
                                .withEndAction(() -> clearRecentLifeChange(textView))
                                .start();
                    }
                },
                remainingMs);
    }

    private void clearRecentLifeChange(TextView textView) {
        textView.animate().cancel();
        textView.setVisibility(View.GONE);
        textView.setText(null);
        textView.setContentDescription(null);
        textView.setTag(null);
        textView.setAlpha(1f);
        textView.setTranslationX(0f);
        textView.setTranslationY(0f);
    }

    private View createCommanderSummarySpacer() {
        View spacer = new View(requireContext());
        spacer.setLayoutParams(createSmallGridLayoutParams(16));
        spacer.setVisibility(View.INVISIBLE);
        return spacer;
    }

    private LinearLayout.LayoutParams createSmallGridLayoutParams(int heightDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dpToPx(heightDp), 1f);
        int margin = dpToPx(2);
        params.setMargins(margin, margin, margin, margin);
        return params;
    }

    private void showCommanderDamageDialog(int defenderSeatIndex) {
        activeDialogDefenderSeatIndex = defenderSeatIndex;
        activeDialogDamageTextViews.clear();

        MtgLifeUiState uiState = viewModel.getUiState().getValue();
        if (uiState == null) {
            return;
        }

        LifePlayerUiModel defender = findPlayerBySeat(uiState.getPlayers(), defenderSeatIndex);
        if (defender == null) {
            return;
        }

        int totalPlayers = uiState.getPlayerCount();
        int rows = getCommanderGridRowCount(totalPlayers);
        int cols = getCommanderGridColumnCount(totalPlayers);

        FrameLayout dialogRoot = new FrameLayout(requireContext());
        dialogRoot.setLayoutParams(
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT));

        FrameLayout rotatedGrid = new FrameLayout(requireContext());
        FrameLayout.LayoutParams rotatedGridParams =
                new FrameLayout.LayoutParams(
                        dpToPx(rows == 1 ? 224 : 288), dpToPx(rows == 1 ? 224 : 288));
        rotatedGridParams.gravity = Gravity.CENTER;
        rotatedGrid.setLayoutParams(rotatedGridParams);
        rotatedGrid.setRotation(defender.getRotationDegrees());

        LinearLayout dialogContent = new LinearLayout(requireContext());
        dialogContent.setTag("commander_dialog_content");
        dialogContent.setOrientation(LinearLayout.VERTICAL);
        dialogContent.setGravity(Gravity.CENTER);
        dialogContent.setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4));
        dialogContent.setBackground(createDialogBackground());
        dialogContent.setLayoutParams(
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        Gravity.CENTER));

        List<CommanderDamageUiModel> damages = defender.getCommanderDamages();
        for (int rowIndex = 0; rowIndex < rows; rowIndex++) {
            LinearLayout rowLayout = new LinearLayout(requireContext());
            rowLayout.setOrientation(LinearLayout.HORIZONTAL);
            rowLayout.setLayoutParams(
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT));

            for (int columnIndex = 0; columnIndex < cols; columnIndex++) {
                int slotIndex = rowIndex * cols + columnIndex;
                int sourceSeatIndex =
                        getSourceSeatForGridSlot(slotIndex, defenderSeatIndex, totalPlayers);
                if (sourceSeatIndex < damages.size()) {
                    rowLayout.addView(
                            createCommanderDialogCell(
                                    damages.get(sourceSeatIndex), defenderSeatIndex));
                } else {
                    rowLayout.addView(createCommanderDialogSpacer());
                }
            }

            dialogContent.addView(rowLayout);
        }

        rotatedGrid.addView(dialogContent);
        dialogRoot.addView(rotatedGrid);

        MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(requireContext());
        dialogBuilder.setView(dialogRoot);

        Dialog dialog = dialogBuilder.create();
        dialog.setOnDismissListener(
                d -> {
                    activeDialogDefenderSeatIndex = null;
                    activeDialogDamageTextViews.clear();
                });
        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    private View createCommanderDialogCell(CommanderDamageUiModel damage, int defenderSeatIndex) {
        FrameLayout cellLayout = new FrameLayout(requireContext());
        cellLayout.setClipToOutline(true);
        cellLayout.setLayoutParams(createLargeGridLayoutParams());

        if (damage.isSelf()) {
            cellLayout.setVisibility(View.INVISIBLE);
            return cellLayout;
        }

        int backgroundColor =
                ContextCompat.getColor(requireContext(), damage.getBackgroundColorRes());
        int foregroundColor =
                ContextCompat.getColor(requireContext(), damage.getForegroundColorRes());

        GradientDrawable background = new GradientDrawable();
        background.setCornerRadius(dpToPx(8));
        background.setColor(backgroundColor);
        cellLayout.setBackground(background);

        TextView valueTextView = new TextView(requireContext());
        valueTextView.setText(String.valueOf(damage.getAmount()));
        valueTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);
        valueTextView.setTypeface(null, Typeface.BOLD);
        valueTextView.setGravity(Gravity.CENTER);
        valueTextView.setTextColor(foregroundColor);
        valueTextView.setTag(foregroundColor);
        if (damage.isLethal()) {
            valueTextView.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.secondAccent));
        }

        FrameLayout.LayoutParams valueParams =
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        Gravity.CENTER);
        valueTextView.setLayoutParams(valueParams);
        cellLayout.addView(valueTextView);
        activeDialogDamageTextViews.put(damage.getSourceSeatIndex(), valueTextView);

        View divider = new View(requireContext());
        FrameLayout.LayoutParams dividerParams =
                new FrameLayout.LayoutParams(dpToPx(1), FrameLayout.LayoutParams.MATCH_PARENT);
        dividerParams.gravity = Gravity.CENTER;
        dividerParams.topMargin = dpToPx(8);
        dividerParams.bottomMargin = dpToPx(8);
        divider.setLayoutParams(dividerParams);
        divider.setBackgroundColor(adjustAlpha(foregroundColor, 0.2f));
        cellLayout.addView(divider);

        LinearLayout tapZoneContainer = new LinearLayout(requireContext());
        tapZoneContainer.setOrientation(LinearLayout.HORIZONTAL);
        tapZoneContainer.setLayoutParams(
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT));
        tapZoneContainer.addView(
                createCommanderDamageTapZone(
                        false,
                        foregroundColor,
                        getString(
                                R.string.mtg_commander_damage_decrease_desc,
                                damage.getSourceSeatIndex() + 1,
                                defenderSeatIndex + 1),
                        v ->
                                viewModel.decrementCommanderDamage(
                                        defenderSeatIndex, damage.getSourceSeatIndex())));
        tapZoneContainer.addView(
                createCommanderDamageTapZone(
                        true,
                        foregroundColor,
                        getString(
                                R.string.mtg_commander_damage_increase_desc,
                                damage.getSourceSeatIndex() + 1,
                                defenderSeatIndex + 1),
                        v ->
                                viewModel.incrementCommanderDamage(
                                        defenderSeatIndex, damage.getSourceSeatIndex())));
        cellLayout.addView(tapZoneContainer);

        return cellLayout;
    }

    private View createCommanderDialogSpacer() {
        View spacer = new View(requireContext());
        spacer.setLayoutParams(createLargeGridLayoutParams());
        spacer.setVisibility(View.INVISIBLE);
        return spacer;
    }

    private LinearLayout.LayoutParams createLargeGridLayoutParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dpToPx(80), 1f);
        int margin = dpToPx(8);
        params.setMargins(margin, margin, margin, margin);
        return params;
    }

    private GradientDrawable createDialogBackground() {
        TypedValue typedValue = new TypedValue();
        requireContext()
                .getTheme()
                .resolveAttribute(android.R.attr.colorBackground, typedValue, true);

        GradientDrawable background = new GradientDrawable();
        background.setColor(typedValue.data);
        background.setCornerRadius(dpToPx(10));
        return background;
    }

    private View createCommanderDamageTapZone(
            boolean increment,
            int foregroundColor,
            String contentDescription,
            View.OnClickListener onClickListener) {
        View tapZone = new View(requireContext());
        tapZone.setLayoutParams(
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f));
        tapZone.setClickable(true);
        tapZone.setFocusable(true);
        tapZone.setContentDescription(contentDescription);
        tapZone.setOnClickListener(onClickListener);
        tapZone.setBackground(
                new RippleDrawable(
                        ColorStateList.valueOf(adjustAlpha(foregroundColor, 0.18f)), null, null));
        tapZone.setTag(increment ? "commander_increment_zone" : "commander_decrement_zone");
        return tapZone;
    }

    private LifePlayerUiModel findPlayerBySeat(List<LifePlayerUiModel> players, int seatIndex) {
        for (LifePlayerUiModel player : players) {
            if (player.getSeatIndex() == seatIndex) {
                return player;
            }
        }
        return null;
    }

    private int getSeatViewId(int seatIndex) {
        return switch (seatIndex) {
            case 0 -> R.id.player_1;
            case 1 -> R.id.player_2;
            case 2 -> R.id.player_3;
            case 3 -> R.id.player_4;
            case 4 -> R.id.player_5;
            case 5 -> R.id.player_6;
            default -> View.NO_ID;
        };
    }

    private int getPreviewBackgroundColorRes(int seatIndex) {
        return switch (seatIndex) {
            case 0 -> R.color.lifeCounterPlayer1;
            case 1 -> R.color.lifeCounterPlayer2;
            case 2 -> R.color.lifeCounterPlayer3;
            case 3 -> R.color.lifeCounterPlayer4;
            default -> R.color.lifeCounterPlayer1;
        };
    }

    private int getLifeCountMaxSizeSp(int playerCount) {
        return switch (playerCount) {
            case 1 -> 96;
            case 2 -> 80;
            case 3 -> 72;
            case 4 -> 60;
            case 5 -> 54;
            case 6 -> 48;
            default -> 60;
        };
    }

    private int getCommanderGridRowCount(int totalPlayers) {
        return totalPlayers > 2 ? 2 : 1;
    }

    private int getCommanderGridColumnCount(int totalPlayers) {
        return totalPlayers > 4 ? 3 : 2;
    }

    private int getForegroundColor(int backgroundColor) {
        return ColorUtils.calculateLuminance(backgroundColor) < 0.5 ? 0xFFFFFFFF : 0xFF000000;
    }

    private int adjustAlpha(int color, float alpha) {
        int scaledAlpha = Math.round(Color.alpha(color) * alpha);
        return ColorUtils.setAlphaComponent(color, scaledAlpha);
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private void startLifeHoldRepeat(View view, Runnable action) {
        stopLifeHoldRepeat(view);

        Runnable repeatRunnable =
                new Runnable() {
                    @Override
                    public void run() {
                        Runnable activeRunnable = activeLifeHoldRunnables.get(view);
                        if (activeRunnable != this || !view.isPressed()) {
                            stopLifeHoldRepeat(view);
                            return;
                        }

                        action.run();
                        holdRepeatHandler.postDelayed(this, LIFE_HOLD_REPEAT_INTERVAL_MS);
                    }
                };

        activeLifeHoldRunnables.put(view, repeatRunnable);
        holdRepeatHandler.postDelayed(repeatRunnable, LIFE_HOLD_REPEAT_INTERVAL_MS);
    }

    private boolean handleLifeHoldTouch(View view, MotionEvent event) {
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_UP
                || action == MotionEvent.ACTION_CANCEL
                || action == MotionEvent.ACTION_OUTSIDE) {
            stopLifeHoldRepeat(view);
        }
        return false;
    }

    private void stopLifeHoldRepeat(View view) {
        Runnable repeatRunnable = activeLifeHoldRunnables.remove(view);
        if (repeatRunnable != null) {
            holdRepeatHandler.removeCallbacks(repeatRunnable);
        }
    }

    private int getSourceSeatForGridSlot(int slotIndex, int defenderSeatIndex, int totalPlayers) {
        int[] mapping =
                switch (totalPlayers) {
                    case 2 -> defenderSeatIndex == 0 ? new int[] {1, 0} : new int[] {0, 1};
                    case 3 ->
                            switch (defenderSeatIndex) {
                                case 0 -> new int[] {2, 1, 3, 0};
                                case 1 -> new int[] {0, 2, 3, 1};
                                default -> new int[] {1, 0, 2, 3};
                            };
                    case 4 ->
                            switch (defenderSeatIndex) {
                                case 0, 2 -> new int[] {1, 3, 0, 2};
                                default -> new int[] {2, 0, 3, 1};
                            };
                    case 5 ->
                            defenderSeatIndex == 4
                                    ? new int[] {0, 1, 4, 2, 3, 5}
                                    : defenderSeatIndex % 2 == 0
                                            ? new int[] {1, 3, 5, 0, 2, 4}
                                            : new int[] {4, 2, 0, 5, 3, 1};
                    case 6 ->
                            defenderSeatIndex % 2 == 0
                                    ? new int[] {1, 3, 5, 0, 2, 4}
                                    : new int[] {4, 2, 0, 5, 3, 1};
                    default -> null;
                };

        if (mapping == null || slotIndex < 0 || slotIndex >= mapping.length) {
            return slotIndex;
        }
        return mapping[slotIndex];
    }

    private void showTurnTimerDurationPicker(LifeSetupContentBinding setupBinding) {
        long configuredDuration = viewModel.getTurnTimerDurationMs();
        MinutesAlertDialogBinding dialogBinding =
                MinutesAlertDialogBinding.inflate(getLayoutInflater());
        android.widget.NumberPicker minutePicker = dialogBinding.minutePicker;
        android.widget.NumberPicker secondPicker = dialogBinding.secondsPicker;

        // Hide increment fields in minutes_alert_dialog
        android.widget.TextView incrementLabel = dialogBinding.incrementLabel;
        android.widget.TextView baseTimeLabel = dialogBinding.baseTimeLabel;
        View incrementPickerContainer = (View) dialogBinding.incrementMinutePicker.getParent();

        incrementLabel.setVisibility(View.GONE);
        baseTimeLabel.setVisibility(View.GONE);
        incrementPickerContainer.setVisibility(View.GONE);

        // Configure duration pickers
        minutePicker.setMinValue(0);
        minutePicker.setMaxValue(999);
        minutePicker.setValue((int) ((configuredDuration / 1000L) / 60L));
        secondPicker.setMinValue(0);
        secondPicker.setMaxValue(59);
        secondPicker.setValue((int) ((configuredDuration / 1000L) % 60L));
        secondPicker.setFormatter(
                value -> String.format(java.util.Locale.getDefault(), "%02d", value));

        new AlertDialog.Builder(requireContext())
                .setView(dialogBinding.getRoot())
                .setTitle(R.string.timer_settings_title)
                .setPositiveButton(
                        android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                long durationMs =
                                        (minutePicker.getValue() * 60L + secondPicker.getValue())
                                                * 1000L;
                                viewModel.setTurnTimerDurationMs(durationMs);
                                setupBinding.btnTurnTimerValue.setText(
                                        TimerBackend.formatRemainingTime(durationMs, 10000L));
                            }
                        })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    @Override
    public void onStop() {
        if (!requireActivity().isChangingConfigurations()) {
            viewModel.pauseForBackground();
        }
        super.onStop();
    }

    private void vibrate(long milliseconds) {
        android.os.Vibrator vibrator =
                (android.os.Vibrator)
                        requireContext().getSystemService(android.content.Context.VIBRATOR_SERVICE);
        if (vibrator != null) {
            vibrator.vibrate(milliseconds);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        for (Runnable repeatRunnable : activeLifeHoldRunnables.values()) {
            holdRepeatHandler.removeCallbacks(repeatRunnable);
        }
        activeLifeHoldRunnables.clear();
        binding = null;
        inputsInitialized = false;
        currentBoardPlayerCount = -1;
        activeDialogDefenderSeatIndex = null;
        activeDialogDamageTextViews.clear();
    }
}
