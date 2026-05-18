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

                            if (uiState.isShowingSetup()) {
                                binding.setupContent.getRoot().setVisibility(View.VISIBLE);
                                binding.boardContainer.setVisibility(View.GONE);

                                if (!inputsInitialized) {
                                    setupBinding.playersInput.setText(
                                            String.valueOf(uiState.getPlayerCount()));
                                    setupBinding.lifeInput.setText(
                                            String.valueOf(uiState.getStartingLife()));
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
                                                requireContext()
                                                        .getColor(player.getBackgroundColorRes());
                                        int fgColor =
                                                requireContext()
                                                        .getColor(player.getForegroundColorRes());

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
                    viewModel.validateAndStartGame(playersStr, lifeStr);
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
