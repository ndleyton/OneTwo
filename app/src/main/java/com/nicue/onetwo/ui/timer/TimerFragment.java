package com.nicue.onetwo.ui.timer;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.NumberPicker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;

import com.nicue.onetwo.OneTwoApplication;
import com.nicue.onetwo.R;
import com.nicue.onetwo.core.HandlerTimerScheduler;
import com.nicue.onetwo.databinding.ListItemTimerBinding;
import com.nicue.onetwo.databinding.MinutesAlertDialogBinding;
import com.nicue.onetwo.databinding.TimerLayoutBinding;

import java.util.ArrayList;
import java.util.List;

public class TimerFragment extends Fragment implements MenuProvider {
    private static final int ACTIVE_COLOR = 0xffbd2430;
    private static final int IDLE_COLOR = 0xff850009;
    private static final int FINISHED_COLOR = 0xff424242;

    private TimerLayoutBinding binding;
    private final List<ListItemTimerBinding> timerBindings = new ArrayList<>();
    private TimerViewModel viewModel;
    private int lastFinishEventCount;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = TimerLayoutBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        TimerViewModelFactory factory = new TimerViewModelFactory(
                ((OneTwoApplication) requireActivity().getApplication())
                        .getAppContainer()
                        .getTimerStateStore(),
                new HandlerTimerScheduler()
        );
        viewModel = new ViewModelProvider(this, factory).get(TimerViewModel.class);
        viewModel.setMaxTimers(maxTimers());

        binding.playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                vibrate(30L);
                viewModel.togglePlayPause();
            }
        });
        binding.editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                vibrate(30L);
                showEditDialog();
            }
        });

        viewModel.getUiState().observe(getViewLifecycleOwner(), this::renderState);
        viewModel.getFinishEvents().observe(getViewLifecycleOwner(), new androidx.lifecycle.Observer<Integer>() {
            @Override
            public void onChanged(Integer eventCount) {
                if (eventCount == null || eventCount <= lastFinishEventCount) {
                    return;
                }
                lastFinishEventCount = eventCount;
                vibrate(300L);
            }
        });

        MenuHost menuHost = requireActivity();
        menuHost.addMenuProvider(this, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
    }

    @Override
    public void onStop() {
        if (!requireActivity().isChangingConfigurations()) {
            viewModel.pauseForBackground();
        }
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        timerBindings.clear();
        binding = null;
        super.onDestroyView();
    }

    @Override
    public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.timer_actions, menu);
    }

    @Override
    public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
        int itemId = menuItem.getItemId();
        if (itemId == R.id.action_add_timer) {
            viewModel.addTimer(maxTimers());
            return true;
        }
        if (itemId == R.id.action_remove_timer) {
            viewModel.removeTimer();
            return true;
        }
        return false;
    }

    private void renderState(TimerUiState state) {
        if (binding == null || state == null) {
            return;
        }
        binding.playButton.setText(state.isPaused() ? R.string.play : R.string.pause);

        List<TimerItemUiModel> timers = state.getTimers();
        if (timerBindings.size() != timers.size()) {
            rebuildTimerViews(timers.size());
        }
        for (int i = 0; i < timers.size(); i++) {
            TimerItemUiModel timer = timers.get(i);
            ListItemTimerBinding itemBinding = timerBindings.get(i);
            itemBinding.chrono.setText(timer.getDisplayTime());
            itemBinding.chrono.setEnabled(timer.isEnabled());
            itemBinding.chrono.setClickable(timer.isEnabled());
            itemBinding.chrono.setBackgroundColor(resolveButtonColor(timer));
            itemBinding.cvTimer.setCardElevation(timer.isActive() && !state.isPaused() ? 30f : 2f);
            itemBinding.getRoot().setRotation(i == 0 && timers.size() == 2 ? 180f : 0f);
        }
    }

    private void rebuildTimerViews(int count) {
        timerBindings.clear();
        binding.linearTimers.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(binding.getRoot().getContext());
        for (int i = 0; i < count; i++) {
            ListItemTimerBinding itemBinding = ListItemTimerBinding.inflate(
                    inflater,
                    binding.linearTimers,
                    false
            );
            itemBinding.chrono.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    vibrate(30L);
                    viewModel.advanceTimer();
                }
            });
            timerBindings.add(itemBinding);
            binding.linearTimers.addView(itemBinding.getRoot());
        }
    }

    private void showEditDialog() {
        TimerUiState state = viewModel.getUiState().getValue();
        long configuredDuration = state == null ? 300000L : state.getConfiguredDurationMs();
        MinutesAlertDialogBinding dialogBinding = MinutesAlertDialogBinding.inflate(getLayoutInflater());
        NumberPicker minutePicker = dialogBinding.minutePicker;
        NumberPicker secondPicker = dialogBinding.secondsPicker;
        minutePicker.setMinValue(0);
        minutePicker.setMaxValue(999);
        minutePicker.setValue((int) ((configuredDuration / 1000L) / 60L));
        secondPicker.setMinValue(0);
        secondPicker.setMaxValue(59);
        secondPicker.setValue((int) ((configuredDuration / 1000L) % 60L));
        secondPicker.setFormatter(new NumberPicker.Formatter() {
            @Override
            public String format(int value) {
                return String.format("%02d", value);
            }
        });

        new AlertDialog.Builder(requireContext())
                .setView(dialogBinding.getRoot())
                .setTitle("Set Time:")
                .setPositiveButton("Set", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        long durationMs = (minutePicker.getValue() * 60L + secondPicker.getValue()) * 1000L;
                        viewModel.editDuration(durationMs);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private int resolveButtonColor(TimerItemUiModel timer) {
        if (timer.isFinished()) {
            return FINISHED_COLOR;
        }
        return timer.isActive() && timer.isEnabled() ? ACTIVE_COLOR : IDLE_COLOR;
    }

    private void vibrate(long milliseconds) {
        Vibrator vibrator = (Vibrator) requireContext().getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null) {
            vibrator.vibrate(milliseconds);
        }
    }

    public static int calculateMaxTimers(int screenHeightDp) {
        return Math.max(1, (screenHeightDp - 22) / 78);
    }

    private int maxTimers() {
        Configuration configuration = requireContext().getResources().getConfiguration();
        return calculateMaxTimers(configuration.screenHeightDp);
    }
}
