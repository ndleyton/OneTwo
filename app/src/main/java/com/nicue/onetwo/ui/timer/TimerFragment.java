package com.nicue.onetwo.ui.timer;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
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
import androidx.core.content.ContextCompat;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.color.MaterialColors;
import com.nicue.onetwo.OneTwoApplication;
import com.nicue.onetwo.R;
import com.nicue.onetwo.core.HandlerTimerScheduler;
import com.nicue.onetwo.databinding.ListItemTimerBinding;
import com.nicue.onetwo.databinding.MinutesAlertDialogBinding;
import com.nicue.onetwo.databinding.TimerLayoutBinding;
import java.util.ArrayList;
import java.util.List;

public class TimerFragment extends Fragment implements MenuProvider {
    private static final int MIN_TIMER_ITEM_HEIGHT_DP = 78;

    private TimerLayoutBinding binding;
    private final List<ListItemTimerBinding> timerBindings = new ArrayList<>();
    private TimerViewModel viewModel;
    private int lastFinishEventCount;

    @Nullable @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = TimerLayoutBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        TimerViewModelFactory factory =
                new TimerViewModelFactory(
                        ((OneTwoApplication) requireActivity().getApplication())
                                .getAppContainer()
                                .getTimerStateStore(),
                        new HandlerTimerScheduler());
        viewModel = new ViewModelProvider(this, factory).get(TimerViewModel.class);
        updateMaxTimers();
        binding.linearTimers.addOnLayoutChangeListener(
                new View.OnLayoutChangeListener() {
                    @Override
                    public void onLayoutChange(
                            View v,
                            int left,
                            int top,
                            int right,
                            int bottom,
                            int oldLeft,
                            int oldTop,
                            int oldRight,
                            int oldBottom) {
                        if (bottom - top != oldBottom - oldTop) {
                            updateMaxTimers();
                        }
                    }
                });

        // Buttons moved to toolbar

        viewModel.getUiState().observe(getViewLifecycleOwner(), this::renderState);
        viewModel
                .getFinishEvents()
                .observe(
                        getViewLifecycleOwner(),
                        new androidx.lifecycle.Observer<Integer>() {
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
        if (itemId == R.id.action_play_pause) {
            vibrate(30L);
            viewModel.togglePlayPause();
            return true;
        }
        if (itemId == R.id.action_settings) {
            vibrate(30L);
            showEditDialog();
            return true;
        }
        return false;
    }

    private void renderState(TimerUiState state) {
        if (binding == null || state == null) {
            return;
        }

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
            int bgColor = resolveButtonColor(timer, state.isPaused());
            int textColor = resolveTextColor(timer, state.isPaused());
            itemBinding.chrono.setBackgroundTintList(ColorStateList.valueOf(bgColor));
            itemBinding.chrono.setTextColor(textColor);
            itemBinding.cvTimer.setCardBackgroundColor(ColorStateList.valueOf(bgColor));
            itemBinding.cvTimer.setCardElevation(timer.isActive() && !state.isPaused() ? 12f : 0f);

            // Dim if active but paused
            itemBinding.cvTimer.setAlpha(timer.isActive() && state.isPaused() ? 0.7f : 1.0f);

            itemBinding.getRoot().setRotation(i == 0 && timers.size() == 2 ? 180f : 0f);
        }
    }

    private void rebuildTimerViews(int count) {
        timerBindings.clear();
        binding.linearTimers.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(binding.getRoot().getContext());
        for (int i = 0; i < count; i++) {
            ListItemTimerBinding itemBinding =
                    ListItemTimerBinding.inflate(inflater, binding.linearTimers, false);
            itemBinding.chrono.setOnClickListener(
                    new View.OnClickListener() {
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
        long configuredIncrement = state == null ? 0L : state.getConfiguredIncrementMs();
        int currentTimerCount = state == null ? 2 : state.getTimers().size();

        MinutesAlertDialogBinding dialogBinding =
                MinutesAlertDialogBinding.inflate(getLayoutInflater());
        NumberPicker timerCountPicker = dialogBinding.timerCountPicker;
        NumberPicker minutePicker = dialogBinding.minutePicker;
        NumberPicker secondPicker = dialogBinding.secondsPicker;
        NumberPicker incrementMinutePicker = dialogBinding.incrementMinutePicker;
        NumberPicker incrementSecondPicker = dialogBinding.incrementSecondsPicker;

        timerCountPicker.setMinValue(1);
        timerCountPicker.setMaxValue(maxTimers());
        timerCountPicker.setValue(currentTimerCount);
        timerCountPicker.setWrapSelectorWheel(false);

        configureDurationPicker(minutePicker, secondPicker, configuredDuration);
        configureDurationPicker(incrementMinutePicker, incrementSecondPicker, configuredIncrement);

        AlertDialog dialog =
                new AlertDialog.Builder(requireContext()).setView(dialogBinding.getRoot()).create();
        dialogBinding.applyButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        long durationMs =
                                (minutePicker.getValue() * 60L + secondPicker.getValue()) * 1000L;
                        long incrementMs =
                                (incrementMinutePicker.getValue() * 60L
                                                + incrementSecondPicker.getValue())
                                        * 1000L;
                        viewModel.setTimerCount(timerCountPicker.getValue(), maxTimers());
                        viewModel.editDuration(durationMs, incrementMs);
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
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    private void configureDurationPicker(
            NumberPicker minutePicker, NumberPicker secondPicker, long durationMs) {
        minutePicker.setMinValue(0);
        minutePicker.setMaxValue(999);
        minutePicker.setValue((int) ((durationMs / 1000L) / 60L));
        secondPicker.setMinValue(0);
        secondPicker.setMaxValue(59);
        secondPicker.setValue((int) ((durationMs / 1000L) % 60L));
        secondPicker.setFormatter(
                new NumberPicker.Formatter() {
                    @Override
                    public String format(int value) {
                        return String.format(java.util.Locale.getDefault(), "%02d", value);
                    }
                });
    }

    private int resolveButtonColor(TimerItemUiModel timer, boolean isPaused) {
        Context context = requireContext();
        if (timer.isFinished()) {
            return MaterialColors.getColor(
                    context,
                    com.google.android.material.R.attr.colorErrorContainer,
                    "TimerFragment");
        }
        return timer.isActive() && timer.isEnabled()
                ? MaterialColors.getColor(
                        context, com.google.android.material.R.attr.colorTertiary, "TimerFragment")
                : ContextCompat.getColor(context, R.color.timer_idle_background);
    }

    private int resolveTextColor(TimerItemUiModel timer, boolean isPaused) {
        Context context = requireContext();
        if (timer.isFinished()) {
            return MaterialColors.getColor(
                    context,
                    com.google.android.material.R.attr.colorOnErrorContainer,
                    "TimerFragment");
        }
        return timer.isActive() && timer.isEnabled()
                ? MaterialColors.getColor(
                        context,
                        com.google.android.material.R.attr.colorOnTertiary,
                        "TimerFragment")
                : ContextCompat.getColor(context, R.color.timer_idle_foreground);
    }

    private void vibrate(long milliseconds) {
        Vibrator vibrator = (Vibrator) requireContext().getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null) {
            vibrator.vibrate(milliseconds);
        }
    }

    public static int calculateMaxTimers(int availableHeightDp) {
        return Math.max(1, availableHeightDp / MIN_TIMER_ITEM_HEIGHT_DP);
    }

    private int maxTimers() {
        if (binding != null && binding.linearTimers.getHeight() > 0) {
            float density = getResources().getDisplayMetrics().density;
            int availableHeightDp = (int) (binding.linearTimers.getHeight() / density);
            return calculateMaxTimers(availableHeightDp);
        }
        Configuration configuration = requireContext().getResources().getConfiguration();
        return calculateMaxTimers(configuration.screenHeightDp);
    }

    private void updateMaxTimers() {
        viewModel.setMaxTimers(maxTimers());
    }
}
