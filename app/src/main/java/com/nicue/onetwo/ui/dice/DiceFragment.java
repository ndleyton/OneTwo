package com.nicue.onetwo.ui.dice;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import androidx.recyclerview.widget.GridLayoutManager;

import com.nicue.onetwo.OneTwoApplication;
import com.nicue.onetwo.R;
import com.nicue.onetwo.databinding.DiceAlertDialogBinding;
import com.nicue.onetwo.databinding.DiceLayoutBinding;

import java.util.List;

public class DiceFragment extends Fragment implements DiceAdapter.Listener, MenuProvider {
    private DiceLayoutBinding binding;
    private DiceAdapter adapter;
    private DiceViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = DiceLayoutBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        adapter = new DiceAdapter(this);
        binding.recyclerviewDice.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        binding.recyclerviewDice.setAdapter(adapter);

        binding.fabDice.setScaleX(0f);
        binding.fabDice.setScaleY(0f);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (binding == null) {
                    return;
                }
                binding.fabDice.animate().scaleX(1f)
                        .setInterpolator(new DecelerateInterpolator(2))
                        .start();
                binding.fabDice.animate().scaleY(1f)
                        .setInterpolator(new DecelerateInterpolator(2))
                        .start();
            }
        }, 300);
        binding.fabDice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddDieDialog();
            }
        });

        DiceViewModelFactory factory = new DiceViewModelFactory(
                ((OneTwoApplication) requireActivity().getApplication())
                        .getAppContainer()
                        .getDiceRepository()
        );
        viewModel = new ViewModelProvider(this, factory).get(DiceViewModel.class);
        viewModel.getUiState().observe(getViewLifecycleOwner(), new androidx.lifecycle.Observer<DiceUiState>() {
            @Override
            public void onChanged(DiceUiState state) {
                adapter.submitList(state.getDice());
                renderResultSummary(state);
            }
        });

        MenuHost menuHost = requireActivity();
        menuHost.addMenuProvider(this, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }

    @Override
    public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.dice_actions, menu);
    }

    @Override
    public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
        if (menuItem.getItemId() == R.id.action_roll_all) {
            vibrate(new long[]{0, 15, 10, 15, 10, 15, 10, 15});
            animateSummaryCard();
            viewModel.rollAllDice();
            return true;
        }
        return false;
    }

    private void animateSummaryCard() {
        binding.diceSummaryCard.animate()
                .scaleX(1.05f)
                .scaleY(1.05f)
                .translationZ(8f)
                .setDuration(150)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        binding.diceSummaryCard.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .translationZ(0f)
                                .setDuration(150)
                                .start();
                    }
                })
                .start();
    }

    @Override
    public void onRollDie(int position) {
        vibrate(new long[]{0, 15, 10, 15, 10, 15});
        viewModel.rollDie(position);
    }

    @Override
    public void onRemoveDie(int position) {
        viewModel.removeDie(position);
    }

    public static String normalizeFacesInput(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "6";
        }
        int faces;
        try {
            faces = Integer.parseInt(input.trim());
        } catch (NumberFormatException exception) {
            return "6";
        }
        if (faces < 2) {
            return "2";
        }
        return String.valueOf(faces);
    }

    private void showAddDieDialog() {
        DiceAlertDialogBinding dialogBinding = DiceAlertDialogBinding.inflate(getLayoutInflater());
        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogBinding.getRoot())
                .setTitle(getString(R.string.dice_title))
                .setPositiveButton("Add", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int which) {
                        String facesText = normalizeFacesInput(dialogBinding.etDice.getText().toString());
                        viewModel.addDie(Integer.parseInt(facesText));
                    }
                })
                .setNegativeButton("Cancel", null)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN
                            | WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE
            );
        }
        dialog.show();
    }

    private void renderResultSummary(DiceUiState state) {
        List<DieUiModel> dice = state.getDice();
        binding.tvDiceTotal.setText(String.valueOf(state.getTotal()));
        binding.tvDiceEmpty.setVisibility(dice.isEmpty() ? View.VISIBLE : View.GONE);
        binding.chipGroupDiceResults.removeAllViews();
        for (DieUiModel die : dice) {
            TextView chip = (TextView) getLayoutInflater().inflate(
                    R.layout.dice_result_chip,
                    binding.chipGroupDiceResults,
                    false
            );
            chip.setText(getString(R.string.dice_result_chip, die.getFaces(), die.getValue()));
            binding.chipGroupDiceResults.addView(chip);
        }
    }

    private void vibrate(long[] pattern) {
        Vibrator vibrator = (Vibrator) requireContext().getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null) {
            vibrator.vibrate(pattern, -1);
        }
    }
}
