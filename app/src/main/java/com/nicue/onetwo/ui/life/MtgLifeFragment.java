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
                                            cellBinding.tvLifeCount.setText("40");

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
                                            int rows = 1;
                                            int cols = 2;
                                            if (totalPlayers == 3 || totalPlayers == 4) {
                                                rows = 2;
                                                cols = 2;
                                            } else if (totalPlayers >= 5) {
                                                rows = 2;
                                                cols = 3;
                                            }

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
                                                    if (idx < damages.size()) {
                                                        CommanderDamageUiModel damage = damages.get(idx);
                                                        android.widget.TextView cellViewText = new android.widget.TextView(requireContext());
                                                        cellViewText.setGravity(android.view.Gravity.CENTER);
                                                        cellViewText.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 10);
                                                        cellViewText.setTypeface(null, android.graphics.Typeface.BOLD);

                                                        if (damage.isSelf()) {
                                                            cellViewText.setText("me");
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
                                            cellBinding.commanderDamageGrid.setContentDescription("Click to manage commander damage for player " + (seatIndex + 1));
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
                    String playersStr = setupBinding.playersInput.getText().toString();
                    String lifeStr = setupBinding.lifeInput.getText().toString();
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
        
        builder.setTitle(getString(R.string.mtg_commander_damage_dialog_title, defenderSeatIndex + 1));

        android.widget.LinearLayout container = new android.widget.LinearLayout(requireContext());
        container.setOrientation(android.widget.LinearLayout.VERTICAL);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        container.setPadding(padding, padding, padding, padding);

        for (CommanderDamageUiModel dmg : defender.getCommanderDamages()) {
            if (dmg.isSelf()) {
                continue;
            }

            int sourceSeat = dmg.getSourceSeatIndex();

            android.widget.LinearLayout row = new android.widget.LinearLayout(requireContext());
            row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            android.widget.LinearLayout.LayoutParams rowParams = new android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            );
            rowParams.setMargins(0, 0, 0, (int) (12 * getResources().getDisplayMetrics().density));
            row.setLayoutParams(rowParams);

            android.view.View colorIndicator = new android.view.View(requireContext());
            int indicatorSize = (int) (16 * getResources().getDisplayMetrics().density);
            android.widget.LinearLayout.LayoutParams indicatorParams = new android.widget.LinearLayout.LayoutParams(
                    indicatorSize, indicatorSize
            );
            indicatorParams.setMargins(0, 0, (int) (12 * getResources().getDisplayMetrics().density), 0);
            colorIndicator.setLayoutParams(indicatorParams);

            android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
            gd.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            gd.setColor(ContextCompat.getColor(requireContext(), dmg.getBackgroundColorRes()));
            colorIndicator.setBackground(gd);
            row.addView(colorIndicator);

            android.widget.TextView labelTv = new android.widget.TextView(requireContext());
            labelTv.setText(getString(R.string.mtg_commander_damage_player_label, sourceSeat + 1));
            labelTv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 16);
            labelTv.setTypeface(null, android.graphics.Typeface.BOLD);
            android.widget.LinearLayout.LayoutParams labelParams = new android.widget.LinearLayout.LayoutParams(
                    0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            );
            labelTv.setLayoutParams(labelParams);
            row.addView(labelTv);

            com.google.android.material.button.MaterialButton btnMinus =
                    new com.google.android.material.button.MaterialButton(requireContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
            btnMinus.setIcon(ContextCompat.getDrawable(requireContext(), R.drawable.ic_remove_24));
            btnMinus.setIconSize((int) (18 * getResources().getDisplayMetrics().density));
            btnMinus.setIconPadding(0);
            btnMinus.setInsetTop(0);
            btnMinus.setInsetBottom(0);
            android.widget.LinearLayout.LayoutParams btnMinusParams = new android.widget.LinearLayout.LayoutParams(
                    (int) (40 * getResources().getDisplayMetrics().density),
                    (int) (40 * getResources().getDisplayMetrics().density)
            );
            btnMinusParams.setMargins(0, 0, (int) (8 * getResources().getDisplayMetrics().density), 0);
            btnMinus.setLayoutParams(btnMinusParams);
            btnMinus.setOnClickListener(v -> viewModel.decrementCommanderDamage(defenderSeatIndex, sourceSeat));
            row.addView(btnMinus);

            android.widget.TextView valTv = new android.widget.TextView(requireContext());
            valTv.setText(String.valueOf(dmg.getAmount()));
            valTv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 18);
            valTv.setTypeface(null, android.graphics.Typeface.BOLD);
            valTv.setGravity(android.view.Gravity.CENTER);
            android.widget.LinearLayout.LayoutParams valParams = new android.widget.LinearLayout.LayoutParams(
                    (int) (36 * getResources().getDisplayMetrics().density),
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            );
            valParams.setMargins(0, 0, (int) (8 * getResources().getDisplayMetrics().density), 0);
            valTv.setLayoutParams(valParams);

            int defaultColor = valTv.getTextColors().getDefaultColor();
            valTv.setTag(defaultColor);

            if (dmg.isLethal()) {
                valTv.setTextColor(ContextCompat.getColor(requireContext(), R.color.secondAccent));
            }

            row.addView(valTv);
            activeDialogDamageTextViews.put(sourceSeat, valTv);

            com.google.android.material.button.MaterialButton btnPlus =
                    new com.google.android.material.button.MaterialButton(requireContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
            btnPlus.setIcon(ContextCompat.getDrawable(requireContext(), R.drawable.ic_add_24));
            btnPlus.setIconSize((int) (18 * getResources().getDisplayMetrics().density));
            btnPlus.setIconPadding(0);
            btnPlus.setInsetTop(0);
            btnPlus.setInsetBottom(0);
            android.widget.LinearLayout.LayoutParams btnPlusParams = new android.widget.LinearLayout.LayoutParams(
                    (int) (40 * getResources().getDisplayMetrics().density),
                    (int) (40 * getResources().getDisplayMetrics().density)
            );
            btnPlus.setLayoutParams(btnPlusParams);
            btnPlus.setOnClickListener(v -> viewModel.incrementCommanderDamage(defenderSeatIndex, sourceSeat));
            row.addView(btnPlus);

            container.addView(row);
        }

        builder.setView(container);
        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss());
        builder.setOnDismissListener(dialog -> {
            activeDialogDefenderSeatIndex = null;
            activeDialogDamageTextViews.clear();
        });

        builder.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        inputsInitialized = false;
        currentBoardPlayerCount = -1;
    }
}
