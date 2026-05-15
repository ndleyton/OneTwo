package com.nicue.onetwo.ui.counter;

import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.nicue.onetwo.OneTwoApplication;
import com.nicue.onetwo.databinding.ActivityAlertDialogBinding;
import com.nicue.onetwo.databinding.CounterLayoutBinding;

public class CounterFragment extends Fragment implements CounterListAdapter.Listener {
    private CounterLayoutBinding binding;
    private CounterListAdapter adapter;
    private CounterViewModel viewModel;
    private int lastCounterCount = -1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = CounterLayoutBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        adapter = new CounterListAdapter(this);

        LinearLayoutManager layoutManager = new LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.VERTICAL,
                false
        );
        binding.recyclerviewCounters.setLayoutManager(layoutManager);
        binding.recyclerviewCounters.setHasFixedSize(true);
        binding.recyclerviewCounters.addItemDecoration(new DividerItemDecoration(
                binding.recyclerviewCounters.getContext(),
                layoutManager.getOrientation()
        ));
        binding.recyclerviewCounters.setAdapter(adapter);

        binding.fab.setScaleX(0f);
        binding.fab.setScaleY(0f);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (binding == null) {
                    return;
                }
                binding.fab.animate().scaleX(1f)
                        .setInterpolator(new DecelerateInterpolator(2))
                        .start();
                binding.fab.animate().scaleY(1f)
                        .setInterpolator(new DecelerateInterpolator(2))
                        .start();
            }
        }, 300);
        binding.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddDialog();
            }
        });

        CounterViewModelFactory factory = new CounterViewModelFactory(
                ((OneTwoApplication) requireActivity().getApplication())
                        .getAppContainer()
                        .getCounterRepository()
        );
        viewModel = new ViewModelProvider(this, factory).get(CounterViewModel.class);
        viewModel.getCounters().observe(getViewLifecycleOwner(),
                new androidx.lifecycle.Observer<java.util.List<com.nicue.onetwo.data.counter.CounterEntity>>() {
                    @Override
                    public void onChanged(java.util.List<com.nicue.onetwo.data.counter.CounterEntity> newCounters) {
                        adapter.submitList(newCounters);
                        binding.tvInstructionCounter.setVisibility(
                                newCounters == null || newCounters.isEmpty() ? View.VISIBLE : View.INVISIBLE
                        );
                        int newCount = newCounters == null ? 0 : newCounters.size();
                        if (newCount > 0 && newCount > lastCounterCount) {
                            binding.recyclerviewCounters.smoothScrollToPosition(newCount - 1);
                        }
                        lastCounterCount = newCount;
                    }
                });
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }

    @Override
    public void onValueChanged(long counterId, int value) {
        viewModel.updateCounterValue(counterId, value);
    }

    @Override
    public void onDeleteClicked(long counterId) {
        viewModel.deleteCounter(counterId);
    }

    public static String sanitizeObjectName(String name) {
        if (name == null) return "";
        return name.replace("'", "\"");
    }

    public static int parseCountValue(String value) {
        if (value == null) return 0;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private void showAddDialog() {
        ActivityAlertDialogBinding dialogBinding = ActivityAlertDialogBinding.inflate(
                getLayoutInflater()
        );
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogBinding.getRoot())
                .setPositiveButton("Add", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int which) {
                        String title = sanitizeObjectName(dialogBinding.etToCount.getText().toString());
                        if (title.trim().isEmpty()) {
                            return;
                        }
                        int value = parseCountValue(dialogBinding.etNumber.getText().toString());
                        viewModel.addCounter(title, value);
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
}
