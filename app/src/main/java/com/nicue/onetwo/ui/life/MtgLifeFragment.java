package com.nicue.onetwo.ui.life;

import android.app.Dialog;
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
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.core.widget.TextViewCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.nicue.onetwo.OneTwoApplication;
import com.nicue.onetwo.R;
import com.nicue.onetwo.data.settings.SettingsRepository;
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
    private PopupWindow coachMarkPopup;

    private static final class CommanderGridLayout {
        private final int rows;
        private final int cols;
        private final int[] sourceSeatIndicesBySlot;

        CommanderGridLayout(int rows, int cols, int[] sourceSeatIndicesBySlot) {
            this.rows = rows;
            this.cols = cols;
            this.sourceSeatIndicesBySlot = sourceSeatIndicesBySlot;
        }

        int getRows() {
            return rows;
        }

        int getCols() {
            return cols;
        }

        int getSourceSeatIndexForSlot(int slotIndex) {
            if (slotIndex < 0 || slotIndex >= sourceSeatIndicesBySlot.length) {
                return -1;
            }
            return sourceSeatIndicesBySlot[slotIndex];
        }
    }

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

        SettingsRepository repo =
                ((OneTwoApplication) requireActivity().getApplication())
                        .getAppContainer()
                        .getSettingsRepository();
        MtgLifeViewModelFactory factory = new MtgLifeViewModelFactory(this, null, repo);
        viewModel = new ViewModelProvider(this, factory).get(MtgLifeViewModel.class);
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

        androidx.navigation.NavController navController = null;
        try {
            navController = androidx.navigation.fragment.NavHostFragment.findNavController(this);
        } catch (IllegalStateException e) {
            // Safe to ignore in unit tests where the fragment is launched outside a NavHost
        }
        if (navController != null) {
            androidx.navigation.NavBackStackEntry currentEntry =
                    navController.getCurrentBackStackEntry();
            if (currentEntry != null) {
                androidx.lifecycle.LiveData<Integer> chosenSeatLiveData =
                        currentEntry.getSavedStateHandle().getLiveData("chosen_seat_index");
                chosenSeatLiveData.observe(
                        getViewLifecycleOwner(),
                        new androidx.lifecycle.Observer<Integer>() {
                            @Override
                            public void onChanged(Integer seatIndex) {
                                if (seatIndex != null) {
                                    viewModel.setStartingPlayer(seatIndex);
                                    currentEntry.getSavedStateHandle().remove("chosen_seat_index");
                                }
                            }
                        });
            }
        }

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

        setupBinding.chooseAndStartButton.setOnClickListener(
                v -> {
                    Editable playersText = setupBinding.playersInput.getText();
                    Editable lifeText = setupBinding.lifeInput.getText();
                    String playersStr = playersText != null ? playersText.toString() : "";
                    String lifeStr = lifeText != null ? lifeText.toString() : "";
                    boolean commanderEnabled = setupBinding.commanderDamageSwitch.isChecked();
                    boolean turnTimerEnabled = setupBinding.turnTimerSwitch.isChecked();
                    if (viewModel.validateAndStartGame(
                            playersStr, lifeStr, commanderEnabled, turnTimerEnabled)) {
                        Bundle args = new Bundle();
                        args.putBoolean("return_on_selection", true);
                        MtgLifeUiState state = viewModel.getUiState().getValue();
                        if (state != null) {
                            args.putInt("player_count", state.getPlayerCount());
                        }
                        androidx.navigation.fragment.NavHostFragment.findNavController(this)
                                .navigate(R.id.nav_chooser, args);
                    }
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
        if (currentUiState != null) {
            if (newGameItem != null) {
                newGameItem.setVisible(!currentUiState.isShowingSetup());
            }
        }
        showCoachMarkIfNecessary();
    }

    private void showCoachMarkIfNecessary() {
        if (viewModel == null || viewModel.isSetupCoachMarkDismissed()) {
            return;
        }
        MtgLifeUiState state = viewModel.getUiState().getValue();
        if (state != null && state.isShowingSetup()) {
            return;
        }

        View view = getView();
        if (view == null) return;
        view.post(
                () -> {
                    if (getActivity() == null) return;
                    View anchor = requireActivity().findViewById(R.id.action_new_game);
                    if (anchor != null
                            && anchor.isAttachedToWindow()
                            && anchor.getVisibility() == View.VISIBLE) {
                        if (viewModel.isSetupCoachMarkDismissed()) return;
                        showCoachMark(anchor);
                    }
                });
    }

    private void showCoachMark(View anchor) {
        if (coachMarkPopup != null) {
            coachMarkPopup.setOnDismissListener(null);
            coachMarkPopup.dismiss();
        }
        View popupView =
                LayoutInflater.from(requireContext()).inflate(R.layout.mtg_coach_mark, null);
        coachMarkPopup =
                new PopupWindow(
                        popupView,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        true);
        coachMarkPopup.setOutsideTouchable(true);
        coachMarkPopup.setElevation(dpToPx(8));
        coachMarkPopup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        popupView.setOnClickListener(
                v -> {
                    if (coachMarkPopup != null) {
                        coachMarkPopup.dismiss();
                    }
                    anchor.performClick();
                });

        coachMarkPopup.setOnDismissListener(
                () -> {
                    viewModel.markSetupCoachMarkDismissed();
                    coachMarkPopup = null;
                });

        popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int xOffset = anchor.getWidth() - popupView.getMeasuredWidth();
        coachMarkPopup.showAsDropDown(anchor, xOffset, dpToPx(4));
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
        disableClipping(binding.boardContainer);
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
            cellBinding.timerContainer.setOnLongClickListener(null);
            cellBinding.btnStartTimer.setVisibility(View.GONE);
            cellBinding.btnStartTimer.setOnClickListener(null);
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
                        : ContextCompat.getColor(requireContext(), player.getForegroundColorRes());
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
        cellBinding.lifeDecrementZone.setOnClickListener(
                v -> {
                    if (viewModel.isLifeCounterHapticFeedbackEnabled()) {
                        v.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP);
                    }
                    viewModel.decrementLife(seatIndex);
                    animateLifeChange(cellBinding.tvLifeCount, false);
                });
        cellBinding.lifeIncrementZone.setOnClickListener(
                v -> {
                    if (viewModel.isLifeCounterHapticFeedbackEnabled()) {
                        v.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP);
                    }
                    viewModel.incrementLife(seatIndex);
                    animateLifeChange(cellBinding.tvLifeCount, true);
                });
        cellBinding.lifeDecrementZone.setOnLongClickListener(
                v -> {
                    if (viewModel.isLifeCounterHapticFeedbackEnabled()) {
                        v.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP);
                    }
                    viewModel.decrementLifeBy(seatIndex, LIFE_LONG_PRESS_DELTA);
                    animateLifeChange(cellBinding.tvLifeCount, false);
                    startLifeHoldRepeat(
                            v,
                            () -> {
                                if (viewModel.isLifeCounterHapticFeedbackEnabled()) {
                                    v.performHapticFeedback(
                                            android.view.HapticFeedbackConstants.KEYBOARD_TAP);
                                }
                                viewModel.decrementLifeBy(seatIndex, LIFE_LONG_PRESS_DELTA);
                                animateLifeChange(cellBinding.tvLifeCount, false);
                            });
                    return true;
                });
        cellBinding.lifeIncrementZone.setOnLongClickListener(
                v -> {
                    if (viewModel.isLifeCounterHapticFeedbackEnabled()) {
                        v.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP);
                    }
                    viewModel.incrementLifeBy(seatIndex, LIFE_LONG_PRESS_DELTA);
                    animateLifeChange(cellBinding.tvLifeCount, true);
                    startLifeHoldRepeat(
                            v,
                            () -> {
                                if (viewModel.isLifeCounterHapticFeedbackEnabled()) {
                                    v.performHapticFeedback(
                                            android.view.HapticFeedbackConstants.KEYBOARD_TAP);
                                }
                                viewModel.incrementLifeBy(seatIndex, LIFE_LONG_PRESS_DELTA);
                                animateLifeChange(cellBinding.tvLifeCount, true);
                            });
                    return true;
                });
        cellBinding.lifeDecrementZone.setOnTouchListener(this::handleLifeHoldTouch);
        cellBinding.lifeIncrementZone.setOnTouchListener(this::handleLifeHoldTouch);

        if (player.isTimerVisible()) {
            cellBinding.timerContainer.setVisibility(View.VISIBLE);
            cellBinding.tvTurnTimer.setText(player.getTimerDisplay());

            boolean isDarkMode =
                    (requireContext().getResources().getConfiguration().uiMode
                                    & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                            == android.content.res.Configuration.UI_MODE_NIGHT_YES;

            int pillBgColor;
            int pillContentColor;
            float elevationVal;

            if (player.isTimerActive()) {
                pillBgColor = foregroundColor;
                pillContentColor = isDarkMode ? Color.WHITE : backgroundColor;
                elevationVal = dpToPx(8);
            } else {
                pillBgColor = Color.TRANSPARENT;
                pillContentColor = isDarkMode ? Color.WHITE : foregroundColor;
                elevationVal = dpToPx(2);
            }

            cellBinding.tvTurnTimer.setTextColor(pillContentColor);
            cellBinding.tvTurnTimer.setContentDescription(
                    getString(
                            R.string.mtg_player_timer_desc,
                            seatIndex + 1,
                            player.getTimerDisplay()));

            cellBinding.tvTurnTimer.setAlpha(player.isTimerActive() ? 1.0f : 0.5f);
            cellBinding.tvTurnTimer.setTypeface(
                    null, player.isTimerActive() ? Typeface.BOLD : Typeface.NORMAL);

            if (player.isTimerActive()) {
                GradientDrawable activeBg = new GradientDrawable();
                activeBg.setShape(GradientDrawable.RECTANGLE);
                activeBg.setCornerRadius(dpToPx(12));
                activeBg.setColor(pillBgColor);

                RippleDrawable rippleDrawable =
                        new RippleDrawable(
                                ColorStateList.valueOf(adjustAlpha(pillContentColor, 0.18f)),
                                activeBg,
                                null);
                cellBinding.timerContainer.setBackground(rippleDrawable);
            } else {
                cellBinding.timerContainer.setBackgroundResource(R.drawable.bg_timer_pill);
            }
            cellBinding.timerContainer.setElevation(elevationVal);

            cellBinding.btnPassTurn.setEnabled(player.isPassEnabled());
            cellBinding.btnPassTurn.setVisibility(
                    player.isPassEnabled() ? View.VISIBLE : View.INVISIBLE);
            cellBinding.btnPassTurn.setIconTint(ColorStateList.valueOf(pillContentColor));

            boolean pillActive = player.isStartTimerVisible() || player.isPassEnabled();
            cellBinding.timerContainer.setEnabled(pillActive);
            cellBinding.timerContainer.setContentDescription(
                    player.isStartTimerVisible()
                            ? getString(R.string.mtg_start_timer_desc, seatIndex + 1)
                            : getString(R.string.mtg_pass_turn_desc, seatIndex + 1));
            cellBinding.timerContainer.setOnClickListener(
                    v -> {
                        vibrate(30L);
                        if (player.isStartTimerVisible()) {
                            viewModel.startTimer();
                        } else {
                            viewModel.passTurn(seatIndex);
                        }
                    });
            cellBinding.timerContainer.setOnLongClickListener(
                    v -> {
                        vibrate(50L);
                        viewModel.togglePlayPause();
                        return true;
                    });

            cellBinding.btnStartTimer.setVisibility(
                    player.isStartTimerVisible() ? View.VISIBLE : View.GONE);
            cellBinding.btnStartTimer.setIconTint(ColorStateList.valueOf(pillContentColor));
        } else {
            cellBinding.timerContainer.setVisibility(View.GONE);
            cellBinding.timerContainer.setOnClickListener(null);
            cellBinding.timerContainer.setOnLongClickListener(null);
            cellBinding.btnStartTimer.setVisibility(View.GONE);
        }

        bindCommanderDamageSummary(cellBinding, player, playerCount);

        cellBinding.playerCellContainer.setRotation(0f);
    }

    private void animateLifeChange(View view, boolean isIncrement) {
        view.animate().cancel();
        float scaleTarget = isIncrement ? 1.05f : 0.95f;
        view.animate()
                .scaleX(scaleTarget)
                .scaleY(scaleTarget)
                .setDuration(50)
                .withEndAction(
                        () -> {
                            view.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                        })
                .start();
    }

    private void performLifeHapticFeedback(View view) {
        if (viewModel.isLifeCounterHapticFeedbackEnabled()) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        }
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

        GradientDrawable pillBackground = new GradientDrawable();
        pillBackground.setCornerRadius(dpToPx(12));
        pillBackground.setColor(0x20FFFFFF);
        pillBackground.setStroke(dpToPx(1), 0x30FFFFFF);
        cellBinding.commanderDamageGrid.setBackground(pillBackground);
        int pillPadding = dpToPx(4);
        cellBinding.commanderDamageGrid.setPadding(
                pillPadding, pillPadding, pillPadding, pillPadding);

        int seatIndex = player.getSeatIndex();
        CommanderGridLayout gridLayout =
                getCommanderGridLayout(seatIndex, totalPlayers, player.getRotationDegrees());
        int rows = gridLayout.getRows();
        int cols = gridLayout.getCols();
        List<CommanderDamageUiModel> damages = player.getCommanderDamages();

        for (int rowIndex = 0; rowIndex < rows; rowIndex++) {
            LinearLayout rowLayout = new LinearLayout(requireContext());
            rowLayout.setOrientation(LinearLayout.HORIZONTAL);
            rowLayout.setLayoutParams(
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT));

            for (int columnIndex = 0; columnIndex < cols; columnIndex++) {
                int slotIndex = rowIndex * cols + columnIndex;
                int sourceSeatIndex = gridLayout.getSourceSeatIndexForSlot(slotIndex);
                if (sourceSeatIndex >= 0 && sourceSeatIndex < damages.size()) {
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
        float startOffsetY = dpToPx(8); // Start below resting position to animate up
        float exitOffsetY = -dpToPx(8); // Continue floating up on exit
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
                                .translationY(exitOffsetY)
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
        CommanderGridLayout gridLayout =
                getCommanderGridLayout(
                        defenderSeatIndex, totalPlayers, defender.getRotationDegrees());
        int rows = gridLayout.getRows();
        int cols = gridLayout.getCols();

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
        dialogContent.setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));
        dialogContent.setBackground(createDialogBackground());
        dialogContent.setElevation(dpToPx(16));
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
                int sourceSeatIndex = gridLayout.getSourceSeatIndexForSlot(slotIndex);
                if (sourceSeatIndex >= 0 && sourceSeatIndex < damages.size()) {
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
                .resolveAttribute(
                        com.google.android.material.R.attr.colorSurface, typedValue, true);

        GradientDrawable background = new GradientDrawable();
        background.setColor(typedValue.data);
        background.setCornerRadius(dpToPx(24));

        TypedValue onSurfaceValue = new TypedValue();
        requireContext()
                .getTheme()
                .resolveAttribute(
                        com.google.android.material.R.attr.colorOnSurface, onSurfaceValue, true);
        int strokeColor = adjustAlpha(onSurfaceValue.data, 0.12f);
        background.setStroke(dpToPx(1), strokeColor);

        return background;
    }

    private void disableClipping(View view) {
        if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            vg.setClipChildren(false);
            vg.setClipToPadding(false);
            for (int i = 0; i < vg.getChildCount(); i++) {
                disableClipping(vg.getChildAt(i));
            }
        }
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

    private CommanderGridLayout getCommanderGridLayout(
            int defenderSeatIndex, int totalPlayers, int rotationDegrees) {
        return switch (totalPlayers) {
            case 2 ->
                    buildRotatedCommanderGridLayout(
                            2,
                            1,
                            new int[][] {
                                {0, 0},
                                {1, 0},
                            },
                            rotationDegrees);
            case 3 ->
                    buildRotatedCommanderGridLayout(
                            2,
                            2,
                            new int[][] {
                                {1, 0},
                                {0, 0},
                                {0, 1},
                            },
                            rotationDegrees);
            case 4 ->
                    new CommanderGridLayout(
                            2,
                            2,
                            defenderSeatIndex % 2 == 0
                                    ? new int[] {1, 3, 0, 2}
                                    : new int[] {2, 0, 3, 1});
            case 5 ->
                    buildRotatedCommanderGridLayout(
                            3,
                            2,
                            new int[][] {
                                {0, 0},
                                {0, 1},
                                {1, 0},
                                {1, 1},
                                {2, 0},
                            },
                            rotationDegrees);
            case 6 ->
                    new CommanderGridLayout(
                            2,
                            3,
                            defenderSeatIndex % 2 == 0
                                    ? new int[] {1, 3, 5, 0, 2, 4}
                                    : new int[] {4, 2, 0, 5, 3, 1});
            default -> new CommanderGridLayout(1, 1, new int[] {0});
        };
    }

    private CommanderGridLayout buildRotatedCommanderGridLayout(
            int absoluteRows,
            int absoluteCols,
            int[][] absoluteSeatPositions,
            int rotationDegrees) {
        int normalizedRotation = ((rotationDegrees % 360) + 360) % 360;
        int localRows =
                normalizedRotation == 90 || normalizedRotation == 270 ? absoluteCols : absoluteRows;
        int localCols =
                normalizedRotation == 90 || normalizedRotation == 270 ? absoluteRows : absoluteCols;
        int[] sourceSeatIndicesBySlot = new int[localRows * localCols];
        java.util.Arrays.fill(sourceSeatIndicesBySlot, -1);

        for (int sourceSeatIndex = 0;
                sourceSeatIndex < absoluteSeatPositions.length;
                sourceSeatIndex++) {
            int absoluteRow = absoluteSeatPositions[sourceSeatIndex][0];
            int absoluteCol = absoluteSeatPositions[sourceSeatIndex][1];
            int localRow;
            int localCol;
            switch (normalizedRotation) {
                case 90 -> {
                    localRow = absoluteCols - 1 - absoluteCol;
                    localCol = absoluteRow;
                }
                case 180 -> {
                    localRow = absoluteRows - 1 - absoluteRow;
                    localCol = absoluteCols - 1 - absoluteCol;
                }
                case 270 -> {
                    localRow = absoluteCol;
                    localCol = absoluteRows - 1 - absoluteRow;
                }
                default -> {
                    localRow = absoluteRow;
                    localCol = absoluteCol;
                }
            }
            sourceSeatIndicesBySlot[localRow * localCols + localCol] = sourceSeatIndex;
        }

        return new CommanderGridLayout(localRows, localCols, sourceSeatIndicesBySlot);
    }

    private void showTurnTimerDurationPicker(LifeSetupContentBinding setupBinding) {
        long configuredDuration = viewModel.getTurnTimerDurationMs();
        MinutesAlertDialogBinding dialogBinding =
                MinutesAlertDialogBinding.inflate(getLayoutInflater());
        EditText minuteInput = dialogBinding.minuteInput;
        EditText secondInput = dialogBinding.secondsInput;

        dialogBinding.dialogTitle.setText(R.string.mtg_setup_turn_timer);
        dialogBinding.timerCountRow.setVisibility(View.GONE);
        dialogBinding.baseTimeLabel.setText(R.string.mtg_setup_time);
        dialogBinding.clockSettingInputLayout.setVisibility(View.GONE);
        dialogBinding.customTimeLabel.setVisibility(View.GONE);
        dialogBinding.customBaseLabel.setVisibility(View.GONE);
        dialogBinding.baseTimeInputContainer.setPadding(0, 0, 0, 0);
        dialogBinding.incrementRow.setVisibility(View.GONE);

        configureDurationInputs(minuteInput, secondInput, configuredDuration);

        Dialog dialog =
                new MaterialAlertDialogBuilder(requireContext())
                        .setView(dialogBinding.getRoot())
                        .create();
        dialogBinding.applyButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        long durationMs =
                                (parseBoundedInt(minuteInput, 0, 999) * 60L
                                                + parseBoundedInt(secondInput, 0, 59))
                                        * 1000L;
                        viewModel.setTurnTimerDurationMs(durationMs);
                        setupBinding.btnTurnTimerValue.setText(
                                TimerBackend.formatRemainingTime(durationMs, 10000L));
                        dialog.dismiss();
                    }
                });
        dialogBinding.cancelButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                });
        dialog.show();
    }

    private void configureDurationInputs(
            EditText minuteInput, EditText secondInput, long durationMs) {
        int totalSeconds = (int) (durationMs / 1000L);
        minuteInput.setText(String.valueOf(totalSeconds / 60));
        secondInput.setText(
                String.format(java.util.Locale.getDefault(), "%02d", totalSeconds % 60));
    }

    private int parseBoundedInt(EditText input, int minValue, int maxValue) {
        String value = input.getText() == null ? "" : input.getText().toString();
        int parsed;
        try {
            parsed = value.isEmpty() ? minValue : Integer.parseInt(value);
        } catch (NumberFormatException e) {
            parsed = minValue;
        }
        if (parsed < minValue) {
            return minValue;
        }
        return Math.min(parsed, maxValue);
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
        if (coachMarkPopup != null) {
            coachMarkPopup.setOnDismissListener(null);
            coachMarkPopup.dismiss();
            coachMarkPopup = null;
        }
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
