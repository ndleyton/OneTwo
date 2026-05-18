package com.nicue.onetwo.ui.life;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.nicue.onetwo.R;
import com.nicue.onetwo.databinding.LifeFragmentBinding;
import com.nicue.onetwo.databinding.LifeSetupContentBinding;

public class MtgLifeFragment extends Fragment {

    private LifeFragmentBinding binding;
    private MtgLifeViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = LifeFragmentBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(MtgLifeViewModel.class);
        
        LifeSetupContentBinding setupBinding = LifeSetupContentBinding.bind(binding.setupContent.getRoot());

        viewModel.getUiState().observe(getViewLifecycleOwner(), uiState -> {
            if (uiState.isShowingSetup()) {
                binding.setupContent.getRoot().setVisibility(View.VISIBLE);
                binding.boardContainer.setVisibility(View.GONE);
                
                setupBinding.playersInput.setText(String.valueOf(uiState.getPlayerCount()));
                setupBinding.lifeInput.setText(String.valueOf(uiState.getStartingLife()));
                setupBinding.playersInputLayout.setError(null);
                setupBinding.lifeInputLayout.setError(null);
            } else {
                binding.setupContent.getRoot().setVisibility(View.GONE);
                binding.boardContainer.setVisibility(View.VISIBLE);
            }
        });

        setupBinding.startGameButton.setOnClickListener(v -> {
            String playersStr = setupBinding.playersInput.getText().toString();
            String lifeStr = setupBinding.lifeInput.getText().toString();
            
            boolean valid = true;
            try {
                int players = Integer.parseInt(playersStr);
                if (players < 1 || players > 6) {
                    setupBinding.playersInputLayout.setError(getString(R.string.mtg_setup_players_error));
                    valid = false;
                } else {
                    setupBinding.playersInputLayout.setError(null);
                }
            } catch (NumberFormatException e) {
                setupBinding.playersInputLayout.setError(getString(R.string.mtg_setup_players_error));
                valid = false;
            }

            try {
                int life = Integer.parseInt(lifeStr);
                if (life <= 0) {
                    setupBinding.lifeInputLayout.setError(getString(R.string.mtg_setup_life_error));
                    valid = false;
                } else {
                    setupBinding.lifeInputLayout.setError(null);
                }
            } catch (NumberFormatException e) {
                setupBinding.lifeInputLayout.setError(getString(R.string.mtg_setup_life_error));
                valid = false;
            }

            if (valid) {
                viewModel.validateAndStartGame(playersStr, lifeStr);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
