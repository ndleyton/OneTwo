package com.nicue.onetwo.ui.life;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
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

public class MtgLifeFragment extends Fragment implements MenuProvider {

    private LifeFragmentBinding binding;
    private MtgLifeViewModel viewModel;
    private boolean inputsInitialized = false;
    private int currentBoardPlayerCount = -1;
    private Integer activeDialogDefenderSeatIndex = null;
    private final java.util.Map<Integer, android.widget.TextView> activeDialogDamageTextViews = new java.util.HashMap<>();

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

                            if (activeDialogDefenderSeatIndex != null) {
                                LifePlayerUiModel defender = null;
                                for (LifePlayerUiModel p : uiState.getPlayers()) {
                                    if (p.getSeatIndex() == activeDialogDefenderSeatIndex) {
                                        defender = p;
                                        break;
                                    }
                                }
                                if (defender != null) {
                                    for (CommanderDamageUiModel dmg : defender.getCommanderDamages()) {
                                        android.widget.TextView tv = activeDialogDamageTextViews.get(dmg.getSourceSeatIndex());
                                        if (tv != null) {
                                            tv.setText(String.valueOf(dmg.getAmount()));
                                            if (dmg.isLethal()) {
                                                tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.secondAccent));
                                            } else {
                                                Object tag = tv.getTag();
                                                if (tag instanceof Integer) {
                                                    tv.setTextColor((Integer) tag);
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            if (uiState.isShowingSetup()) {
                                binding.setupOverlay.setVisibility(View.VISIBLE);
                                binding.setupContent.getRoot().setVisibility(View.VISIBLE);
                                if (binding.boardContainer.getChildCount() == 0) {
                                    binding.boardContainer.removeAllViews();
                                    LayoutInflater inflater = LayoutInflater.from(requireContext());
                                    LifeBoard4Binding.inflate(
                                            inflater, binding.boardContainer, true);
                                    currentBoardPlayerCount = 4;

                                    for (int i = 0; i < 4; i++) {
                                        int seatId;
                                        switch (i) {
                                            case 0:
                                                seatId = R.id.player_1;
                                                break;
                                            case 1:
                                                seatId = R.id.player_2;
                                                break;
                                            case 2:
                                                seatId = R.id.player_3;
                                                break;
                                            case 3:
                                                seatId = R.id.player_4;
                                                break;
                                            default:
                                                continue;
                                        }
                                        View cellView = binding.boardContainer.findViewById(seatId);
                                        if (cellView != null) {
                                            LifePlayerCellBinding cellBinding =
                                                    LifePlayerCellBinding.bind(cellView);
                                            int bgColorRes;
                                            switch (i) {
                                                case 0:
                                                    bgColorRes = R.color.lifeCounterPlayer1;
                                                    break;
                                                case 1:
                                                    bgColorRes = R.color.lifeCounterPlayer2;
                                                    break;
                                                case 2:
                                                    bgColorRes = R.color.lifeCounterPlayer3;
                                                    break;
                                                case 3:
                                                    bgColorRes = R.color.lifeCounterPlayer4;
                                                    break;
                                                default:
                                                    bgColorRes = R.color.lifeCounterPlayer1;
                                                    break;
                                            }
                                            int bgColor =
                                                    ContextCompat.getColor(
                                                            requireContext(), bgColorRes);
                                            boolean isDark =
                                                    ColorUtils.calculateLuminance(bgColor) < 0.5;
                                            int fgColor = isDark ? 0xFFFFFFFF : 0xFF000000;

                                            cellBinding.playerCellContainer.setBackgroundColor(
                                                    bgColor);
                                            cellBinding.tvLifeCount.setTextColor(fgColor);
                                            cellBinding.tvLifeCount.setText(getString(R.string.default_mtg_life));

                                            cellBinding.btnMinus.setIconTint(
                                                    android.content.res.ColorStateList.valueOf(
                                                            fgColor));
                                            cellBinding.btnPlus.setIconTint(
                                                    android.content.res.ColorStateList.valueOf(
                                                            fgColor));

                                            float rotation = (i % 2 == 0) ? 90f : 270f;
                                            cellBinding.playerCellContainer.setRotation(0f);
                                            cellBinding.innerPlayerLayout.setRotation(rotation);
                                        }
                                    }
                                }
                                binding.boardContainer.setVisibility(View.VISIBLE);

                                if (!inputsInitialized) {
                                    setupBinding.playersInput.setText(
                                            String.valueOf(uiState.getPlayerCount()));
                                    setupBinding.lifeInput.setText(
                                            String.valueOf(uiState.getStartingLife()));
                                    setupBinding.commanderDamageSwitch.setChecked(
                                            uiState.isCommanderDamageEnabled());
                                    inputsInitialized = true;
                                }

                                if (uiState.getPlayersErrorResId() != null) {
                                    setupBinding.playersInputLayout.setError(
                                            getString(uiState.getPlayersErrorResId()));
                                } else {
                                    setupBinding.playersInputLayout.setError(null);
                                }

                                if (uiState.getLifeErrorResId() != null) {
                                    setupBinding.lifeInputLayout.setError(
                                            getString(uiState.getLifeErrorResId()));
                                } else {
                                    setupBinding.lifeInputLayout.setError(null);
                                }
                            } else {
                                binding.setupOverlay.setVisibility(View.GONE);
                                binding.setupContent.getRoot().setVisibility(View.GONE);
                                binding.boardContainer.setVisibility(View.VISIBLE);
                                inputsInitialized = false;

                                int playerCount = uiState.getPlayerCount();
                                if (binding.boardContainer.getChildCount() == 0
                                        || currentBoardPlayerCount != playerCount) {
                                    binding.boardContainer.removeAllViews();
                                    LayoutInflater inflater = LayoutInflater.from(requireContext());
                                    currentBoardPlayerCount = playerCount;

                                    switch (playerCount) {
                                        case 1:
                                            LifeBoard1Binding.inflate(
                                                    inflater, binding.boardContainer, true);
                                            break;
                                        case 2:
                                            LifeBoard2Binding.inflate(
                                                    inflater, binding.boardContainer, true);
                                            break;
                                        case 3:
                                            LifeBoard3Binding.inflate(
                                                    inflater, binding.boardContainer, true);
                                            break;
                                        case 4:
                                            LifeBoard4Binding.inflate(
                                                    inflater, binding.boardContainer, true);
                                            break;
                                        case 5:
                                            LifeBoard5Binding.inflate(
                                                    inflater, binding.boardContainer, true);
                                            break;
                                        case 6:
                                            LifeBoard6Binding.inflate(
                                                    inflater, binding.boardContainer, true);
                                            break;
                                    }
                                }

                                // Bind player tiles
                                for (int i = 0; i < playerCount; i++) {
                                    int seatId;
                                    switch (i) {
                                        case 0:
                                            seatId = R.id.player_1;
                                            break;
                                        case 1:
                                            seatId = R.id.player_2;
                                            break;
                                        case 2:
                                            seatId = R.id.player_3;
                                            break;
                                        case 3:
                                            seatId = R.id.player_4;
                                            break;
                                        case 4:
                                            seatId = R.id.player_5;
                                            break;
                                        case 5:
                                            seatId = R.id.player_6;
                                            break;
                                        default:
                                            continue;
                                    }
                                    View cellView = binding.boardContainer.findViewById(seatId);
                                    if (cellView != null) {
                                        LifePlayerCellBinding cellBinding =
                                                LifePlayerCellBinding.bind(cellView);
                                        LifePlayerUiModel player = uiState.getPlayers().get(i);

                                        int maxSizeSp;
                                        switch (playerCount) {
                                            case 1:
                                                maxSizeSp = 96;
                                                break;
                                            case 2:
                                                maxSizeSp = 80;
                                                break;
                                            case 3:
                                                maxSizeSp = 72;
                                                break;
                                            case 4:
                                                maxSizeSp = 60;
                                                break;
                                            case 5:
                                                maxSizeSp = 54;
                                                break;
                                            case 6:
                                                maxSizeSp = 48;
                                                break;
                                            default:
                                                maxSizeSp = 60;
                                                break;
                                        }
                                        androidx.core.widget.TextViewCompat
                                                .setAutoSizeTextTypeUniformWithConfiguration(
                                                        cellBinding.tvLifeCount,
                                                        20,
                                                        maxSizeSp,
                                                        2,
                                                        android.util.TypedValue.COMPLEX_UNIT_SP);

                                        cellBinding.tvLifeCount.setText(
                                                String.valueOf(player.getLifeTotal()));
                                        cellBinding.tvLifeCount.setContentDescription(
                                                String.valueOf(player.getLifeTotal()));

                                        int bgColor =
                                                ContextCompat.getColor(
                                                        requireContext(),
                                                        player.getBackgroundColorRes());
                                        boolean isDark =
                                                ColorUtils.calculateLuminance(bgColor) < 0.5;
                                        int fgColor = isDark ? 0xFFFFFFFF : 0xFF000000;

                                        cellBinding.playerCellContainer.setBackgroundColor(bgColor);
                                        cellBinding.tvLifeCount.setTextColor(fgColor);

                                        cellBinding.btnMinus.setIconTint(
                                                android.content.res.ColorStateList.valueOf(
                                                        fgColor));
                                        cellBinding.btnPlus.setIconTint(
                                                android.content.res.ColorStateList.valueOf(
                                                        fgColor));

                                        final int seatIndex = player.getSeatIndex();
                                        cellBinding.btnMinus.setOnClickListener(
                                                v -> viewModel.decrementLife(seatIndex));
                                        cellBinding.btnPlus.setOnClickListener(
                                                v -> viewModel.incrementLife(seatIndex));

                                        cellBinding.btnMinus.setContentDescription(
                                                getString(
                                                        R.string.mtg_btn_minus_desc,
                                                        seatIndex + 1));
                                        cellBinding.btnPlus.setContentDescription(
                                                getString(
                                                        R.string.mtg_btn_plus_desc, seatIndex + 1));

                                        if (player.isCommanderDamageVisible()) {
                                            cellBinding.commanderDamageGrid.setVisibility(View.VISIBLE);
                                            cellBinding.commanderDamageGrid.removeAllViews();

                                            int totalPlayers = uiState.getPlayerCount();
                                            int rows = totalPlayers > 2 ? 2 : 1;
                                            int cols = totalPlayers > 4 ? 3 : 2;

                                            java.util.List<CommanderDamageUiModel> damages = player.getCommanderDamages();
                                            for (int r = 0; r < rows; r++) {
                                                android.widget.LinearLayout rowLayout = new android.widget.LinearLayout(requireContext());
                                                rowLayout.setOrientation(android.widget.LinearLayout.HORIZONTAL);
                                                android.widget.LinearLayout.LayoutParams rowParams = new android.widget.LinearLayout.LayoutParams(
                                                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                                                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                                                );
                                                rowLayout.setLayoutParams(rowParams);

                                                for (int c = 0; c < cols; c++) {
                                                    int idx = r * cols + c;
                                                    int targetSeat = getSourceSeatForGridSlot(idx, seatIndex, totalPlayers);
                                                    if (targetSeat < damages.size()) {
                                                        CommanderDamageUiModel damage = damages.get(targetSeat);
                                                        android.widget.TextView cellViewText = new android.widget.TextView(requireContext());
                                                        cellViewText.setGravity(android.view.Gravity.CENTER);
                                                        cellViewText.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 10);
                                                        cellViewText.setTypeface(null, android.graphics.Typeface.BOLD);

                                                        if (damage.isSelf()) {
                                                            cellViewText.setText(getString(R.string.mtg_commander_damage_self_label));
                                                            cellViewText.setEnabled(false);
                                                        } else {
                                                            cellViewText.setText(String.valueOf(damage.getAmount()));
                                                        }

                                                        int cellBgColor = ContextCompat.getColor(requireContext(), damage.getBackgroundColorRes());
                                                        int cellFgColor = ContextCompat.getColor(requireContext(), damage.getForegroundColorRes());

                                                        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
                                                        gd.setCornerRadius(4 * getResources().getDisplayMetrics().density);
                                                        if (damage.isSelf()) {
                                                            gd.setStroke((int) (1 * getResources().getDisplayMetrics().density), cellFgColor);
                                                            gd.setColor(android.graphics.Color.TRANSPARENT);
                                                        } else {
                                                            gd.setColor(cellBgColor);
                                                        }
                                                        cellViewText.setBackground(gd);
                                                        cellViewText.setTextColor(cellFgColor);

                                                        android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(
                                                                0,
                                                                (int) (16 * getResources().getDisplayMetrics().density),
                                                                1f
                                                        );
                                                        int margin = (int) (2 * getResources().getDisplayMetrics().density);
                                                        params.setMargins(margin, margin, margin, margin);
                                                        cellViewText.setLayoutParams(params);

                                                        if (!damage.isSelf()) {
                                                            cellViewText.setContentDescription(getString(
                                                                    R.string.mtg_commander_damage_cell_desc,
                                                                    damage.getSourceSeatIndex() + 1,
                                                                    seatIndex + 1,
                                                                    damage.getAmount()
                                                            ));
                                                        } else {
                                                            cellViewText.setContentDescription(getString(R.string.mtg_commander_damage_self_desc));
                                                        }

                                                        rowLayout.addView(cellViewText);
                                                    } else {
                                                        android.view.View spacer = new android.view.View(requireContext());
                                                        android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(
                                                                0,
                                                                (int) (16 * getResources().getDisplayMetrics().density),
                                                                1f
                                                        );
                                                        int margin = (int) (2 * getResources().getDisplayMetrics().density);
                                                        params.setMargins(margin, margin, margin, margin);
                                                        spacer.setLayoutParams(params);
                                                        spacer.setVisibility(android.view.View.INVISIBLE);
                                                        rowLayout.addView(spacer);
                                                    }
                                                }
                                                cellBinding.commanderDamageGrid.addView(rowLayout);
                                            }

                                            cellBinding.commanderDamageGrid.setOnClickListener(v -> showCommanderDamageDialog(seatIndex));
                                            cellBinding.commanderDamageGrid.setContentDescription(getString(R.string.mtg_commander_damage_manage_desc, seatIndex + 1));
                                        } else {
                                            cellBinding.commanderDamageGrid.setVisibility(View.GONE);
                                        }

                                        float rotation = player.getRotationDegrees();
                                        cellBinding.playerCellContainer.setRotation(0f);
                                        cellBinding.innerPlayerLayout.setRotation(rotation);
                                    }
                                }
                            }
                        });

        setupBinding.startGameButton.setOnClickListener(
                v -> {
                    android.text.Editable playersText = setupBinding.playersInput.getText();
                    android.text.Editable lifeText = setupBinding.lifeInput.getText();
                    String playersStr = playersText != null ? playersText.toString() : "";
                    String lifeStr = lifeText != null ? lifeText.toString() : "";
                    boolean commanderEnabled = setupBinding.commanderDamageSwitch.isChecked();
                    viewModel.validateAndStartGame(playersStr, lifeStr, commanderEnabled);
                });

        binding.setupOverlay.setOnClickListener(v -> viewModel.dismissSetup());
        setupBinding
                .getRoot()
                .setOnClickListener(
                        v -> {
                            // Intercept clicks inside the setup card to prevent dismissing the
                            // modal
                        });

        MenuHost menuHost = requireActivity();
        menuHost.addMenuProvider(this, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
    }

    @Override
    public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.life_actions, menu);
        MenuItem newGameItem = menu.findItem(R.id.action_new_game);
        if (newGameItem != null && viewModel != null && viewModel.getUiState().getValue() != null) {
            newGameItem.setVisible(!viewModel.getUiState().getValue().isShowingSetup());
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

    private void showCommanderDamageDialog(int defenderSeatIndex) {
        activeDialogDefenderSeatIndex = defenderSeatIndex;
        activeDialogDamageTextViews.clear();

        MtgLifeUiState uiState = viewModel.getUiState().getValue();
        if (uiState == null) return;

        LifePlayerUiModel defender = null;
        for (LifePlayerUiModel p : uiState.getPlayers()) {
            if (p.getSeatIndex() == defenderSeatIndex) {
                defender = p;
                break;
            }
        }
        if (defender == null) return;

        com.google.android.material.dialog.MaterialAlertDialogBuilder builder =
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext());

        int totalPlayers = uiState.getPlayerCount();
        int rows = totalPlayers > 2 ? 2 : 1;
        int cols = totalPlayers > 4 ? 3 : 2;

        android.widget.FrameLayout dialogRoot = new android.widget.FrameLayout(requireContext());
        android.widget.FrameLayout.LayoutParams rootParams = new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
        );
        dialogRoot.setLayoutParams(rootParams);

        int squareDimDp = (rows == 1) ? 176 : 228;
        int squareDim = (int) (squareDimDp * getResources().getDisplayMetrics().density);
        
        android.widget.FrameLayout gridSquare = new android.widget.FrameLayout(requireContext());
        android.widget.FrameLayout.LayoutParams squareParams = new android.widget.FrameLayout.LayoutParams(
                squareDim, squareDim
        );
        squareParams.gravity = android.view.Gravity.CENTER;
        gridSquare.setLayoutParams(squareParams);
        gridSquare.setRotation(defender.getRotationDegrees());

        android.widget.LinearLayout container = new android.widget.LinearLayout(requireContext());
        container.setOrientation(android.widget.LinearLayout.VERTICAL);
        container.setGravity(android.view.Gravity.CENTER);
        
        android.graphics.drawable.GradientDrawable containerBg = new android.graphics.drawable.GradientDrawable();
        containerBg.setColor(0xE61A1A1A); // Translucent near-black background
        containerBg.setCornerRadius(10 * getResources().getDisplayMetrics().density);
        container.setBackground(containerBg);
        
        int pad = (int) (4 * getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad, pad, pad);

        android.widget.FrameLayout.LayoutParams containerParams = new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
        );
        containerParams.gravity = android.view.Gravity.CENTER;
        container.setLayoutParams(containerParams);

        java.util.List<CommanderDamageUiModel> damages = defender.getCommanderDamages();
        for (int r = 0; r < rows; r++) {
            android.widget.LinearLayout rowLayout = new android.widget.LinearLayout(requireContext());
            rowLayout.setOrientation(android.widget.LinearLayout.HORIZONTAL);
            android.widget.LinearLayout.LayoutParams rowParams = new android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            );
            rowLayout.setLayoutParams(rowParams);

            for (int c = 0; c < cols; c++) {
                int idx = r * cols + c;
                int targetSeat = getSourceSeatForGridSlot(idx, defenderSeatIndex, totalPlayers);
                if (targetSeat < damages.size()) {
                    CommanderDamageUiModel damage = damages.get(targetSeat);
                    
                    android.widget.FrameLayout cellLayout =
                            new android.widget.FrameLayout(requireContext());
                    cellLayout.setClipToOutline(true);
                    
                    int cellBgColor = ContextCompat.getColor(requireContext(), damage.getBackgroundColorRes());
                    int cellFgColor = ContextCompat.getColor(requireContext(), damage.getForegroundColorRes());

                    android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
                    gd.setCornerRadius(8 * getResources().getDisplayMetrics().density);
                    if (damage.isSelf()) {
                        gd.setStroke((int) (1.5f * getResources().getDisplayMetrics().density), cellFgColor);
                        gd.setColor(android.graphics.Color.TRANSPARENT);
                    } else {
                        gd.setColor(cellBgColor);
                    }
                    cellLayout.setBackground(gd);

                    android.widget.LinearLayout.LayoutParams cellParams = new android.widget.LinearLayout.LayoutParams(
                            0,
                            (int) (64 * getResources().getDisplayMetrics().density),
                            1f
                    );
                    int margin = (int) (4 * getResources().getDisplayMetrics().density);
                    cellParams.setMargins(margin, margin, margin, margin);
                    cellLayout.setLayoutParams(cellParams);

                    if (damage.isSelf()) {
                        android.widget.TextView selfTv = new android.widget.TextView(requireContext());
                        selfTv.setGravity(android.view.Gravity.CENTER);
                        selfTv.setText(getString(R.string.mtg_commander_damage_self_label));
                        selfTv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 18);
                        selfTv.setTypeface(null, android.graphics.Typeface.BOLD);
                        selfTv.setTextColor(cellFgColor);
                        selfTv.setEnabled(false);
                        
                        android.widget.LinearLayout.LayoutParams selfParams = new android.widget.LinearLayout.LayoutParams(
                                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                                android.widget.LinearLayout.LayoutParams.MATCH_PARENT
                        );
                        selfTv.setLayoutParams(selfParams);
                        cellLayout.addView(selfTv);
                    } else {
                        int sourceSeat = damage.getSourceSeatIndex();
                        android.widget.TextView valTv = new android.widget.TextView(requireContext());
                        valTv.setText(String.valueOf(damage.getAmount()));
                        valTv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 20);
                        valTv.setTypeface(null, android.graphics.Typeface.BOLD);
                        valTv.setGravity(android.view.Gravity.CENTER);
                        valTv.setTextColor(cellFgColor);
                        
                        android.widget.LinearLayout.LayoutParams valParams = new android.widget.LinearLayout.LayoutParams(
                                0,
                                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                                1f
                        );
                        valTv.setLayoutParams(valParams);

                        valTv.setTag(cellFgColor);

                        if (damage.isLethal()) {
                            valTv.setTextColor(ContextCompat.getColor(requireContext(), R.color.secondAccent));
                        }

                        cellLayout.addView(valTv);
                        activeDialogDamageTextViews.put(sourceSeat, valTv);

                        android.view.View divider = new android.view.View(requireContext());
                        android.widget.FrameLayout.LayoutParams dividerParams =
                                new android.widget.FrameLayout.LayoutParams(
                                        (int) (1 * getResources().getDisplayMetrics().density),
                                        android.widget.FrameLayout.LayoutParams.MATCH_PARENT);
                        dividerParams.gravity = android.view.Gravity.CENTER;
                        int dividerInset = (int) (8 * getResources().getDisplayMetrics().density);
                        dividerParams.topMargin = dividerInset;
                        dividerParams.bottomMargin = dividerInset;
                        divider.setLayoutParams(dividerParams);
                        divider.setBackgroundColor(adjustAlpha(cellFgColor, 0.2f));
                        cellLayout.addView(divider);

                        android.widget.LinearLayout tapZoneContainer =
                                new android.widget.LinearLayout(requireContext());
                        tapZoneContainer.setOrientation(android.widget.LinearLayout.HORIZONTAL);
                        android.widget.FrameLayout.LayoutParams tapZoneParams =
                                new android.widget.FrameLayout.LayoutParams(
                                        android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                                        android.widget.FrameLayout.LayoutParams.MATCH_PARENT);
                        tapZoneContainer.setLayoutParams(tapZoneParams);

                        android.view.View decrementZone =
                                createCommanderDamageTapZone(
                                        false,
                                        cellFgColor,
                                        getString(
                                                R.string
                                                        .mtg_commander_damage_decrease_desc,
                                                sourceSeat + 1,
                                                defenderSeatIndex + 1),
                                        v ->
                                                viewModel.decrementCommanderDamage(
                                                        defenderSeatIndex, sourceSeat));
                        android.view.View incrementZone =
                                createCommanderDamageTapZone(
                                        true,
                                        cellFgColor,
                                        getString(
                                                R.string
                                                        .mtg_commander_damage_increase_desc,
                                                sourceSeat + 1,
                                                defenderSeatIndex + 1),
                                        v ->
                                                viewModel.incrementCommanderDamage(
                                                        defenderSeatIndex, sourceSeat));

                        tapZoneContainer.addView(decrementZone);
                        tapZoneContainer.addView(incrementZone);
                        cellLayout.addView(tapZoneContainer);
                    }

                    rowLayout.addView(cellLayout);
                } else {
                    android.view.View spacer = new android.view.View(requireContext());
                    android.widget.LinearLayout.LayoutParams spacerParams = new android.widget.LinearLayout.LayoutParams(
                            0,
                            (int) (64 * getResources().getDisplayMetrics().density),
                            1f
                    );
                    int margin = (int) (4 * getResources().getDisplayMetrics().density);
                    spacerParams.setMargins(margin, margin, margin, margin);
                    spacer.setLayoutParams(spacerParams);
                    spacer.setVisibility(android.view.View.INVISIBLE);
                    rowLayout.addView(spacer);
                }
            }
            container.addView(rowLayout);
        }

        gridSquare.addView(container);
        dialogRoot.addView(gridSquare);

        builder.setView(dialogRoot);

        android.app.Dialog dialog = builder.create();
        dialog.setOnDismissListener(d -> {
            activeDialogDefenderSeatIndex = null;
            activeDialogDamageTextViews.clear();
        });

        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }
    }

    private android.view.View createCommanderDamageTapZone(
            boolean increment,
            int foregroundColor,
            String contentDescription,
            android.view.View.OnClickListener onClickListener) {
        android.view.View tapZone = new android.view.View(requireContext());
        android.widget.LinearLayout.LayoutParams params =
                new android.widget.LinearLayout.LayoutParams(
                        0, android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 1f);
        tapZone.setLayoutParams(params);
        tapZone.setClickable(true);
        tapZone.setFocusable(true);
        tapZone.setContentDescription(contentDescription);
        tapZone.setOnClickListener(onClickListener);

        android.content.res.ColorStateList rippleColor =
                android.content.res.ColorStateList.valueOf(adjustAlpha(foregroundColor, 0.18f));
        android.graphics.drawable.RippleDrawable rippleDrawable =
                new android.graphics.drawable.RippleDrawable(rippleColor, null, null);
        tapZone.setBackground(rippleDrawable);
        tapZone.setTag(increment ? "commander_increment_zone" : "commander_decrement_zone");

        return tapZone;
    }

    private int adjustAlpha(int color, float alpha) {
        int scaledAlpha = Math.round(android.graphics.Color.alpha(color) * alpha);
        return androidx.core.graphics.ColorUtils.setAlphaComponent(color, scaledAlpha);
    }

    private int getSourceSeatForGridSlot(int idx, int defenderSeatIndex, int totalPlayers) {
        if (totalPlayers == 4) {
            if (defenderSeatIndex == 0 || defenderSeatIndex == 2) {
                switch (idx) {
                    case 0: return 1;
                    case 1: return 3;
                    case 2: return 0;
                    case 3: return 2;
                }
            } else {
                switch (idx) {
                    case 0: return 2;
                    case 1: return 0;
                    case 2: return 3;
                    case 3: return 1;
                }
            }
        } else if (totalPlayers == 3) {
            if (defenderSeatIndex == 0) {
                switch (idx) {
                    case 0: return 2;
                    case 1: return 1;
                    case 2: return 3; // spacer
                    case 3: return 0;
                }
            } else if (defenderSeatIndex == 1) {
                switch (idx) {
                    case 0: return 0;
                    case 1: return 2;
                    case 2: return 3; // spacer
                    case 3: return 1;
                }
            } else {
                switch (idx) {
                    case 0: return 1;
                    case 1: return 0;
                    case 2: return 2;
                    case 3: return 3; // spacer
                }
            }
        } else if (totalPlayers == 2) {
            if (defenderSeatIndex == 0) {
                return (idx == 0) ? 1 : 0;
            } else {
                return (idx == 0) ? 0 : 1;
            }
        } else if (totalPlayers == 6 || totalPlayers == 5) {
            if (defenderSeatIndex == 4 && totalPlayers == 5) {
                // Bottom player in 5-player game
                switch (idx) {
                    case 0: return 0;
                    case 1: return 2;
                    case 2: return 4;
                    case 3: return 1;
                    case 4: return 3;
                    default: return 5; // spacer
                }
            }
            if (defenderSeatIndex % 2 == 0) {
                switch (idx) {
                    case 0: return 1;
                    case 1: return 3;
                    case 2: return 5;
                    case 3: return 0;
                    case 4: return 2;
                    default: return 4;
                }
            } else {
                switch (idx) {
                    case 0: return 4;
                    case 1: return 2;
                    case 2: return 0;
                    case 3: return 5;
                    case 4: return 3;
                    default: return 1;
                }
            }
        }
        return idx;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        inputsInitialized = false;
        currentBoardPlayerCount = -1;
    }
}
