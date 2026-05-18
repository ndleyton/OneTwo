package com.nicue.onetwo.ui.chooser;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.nicue.onetwo.OneTwoApplication;
import com.nicue.onetwo.data.settings.SettingsRepository;
import com.nicue.onetwo.databinding.ChooserLayoutBinding;

public class ChooserFragment extends Fragment {
    private static final long INSTRUCTION_HIDE_DELAY_MS = 800L;

    private ChooserLayoutBinding binding;
    private ChooserViewModel viewModel;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable hideInstructionRunnable =
            new Runnable() {
                @Override
                public void run() {
                    if (binding != null) {
                        binding.chooserInstruction.setVisibility(View.GONE);
                    }
                }
            };

    @Nullable @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = ChooserLayoutBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @android.annotation.SuppressLint("ClickableViewAccessibility")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        SettingsRepository settingsRepository =
                ((OneTwoApplication) requireActivity().getApplication())
                        .getAppContainer()
                        .getSettingsRepository();
        viewModel =
                new ViewModelProvider(this, new ChooserViewModelFactory(settingsRepository))
                        .get(ChooserViewModel.class);
        viewModel
                .getChoosingOrder()
                .observe(
                        getViewLifecycleOwner(),
                        new androidx.lifecycle.Observer<Boolean>() {
                            @Override
                            public void onChanged(Boolean choosingOrder) {
                                boolean value = Boolean.TRUE.equals(choosingOrder);
                                binding.chooserView.setChoosingOrder(value);
                            }
                        });
        binding.chooserView.setOnTouchListener(
                new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View touchedView, MotionEvent event) {
                        int action = event.getActionMasked();
                        if (action == MotionEvent.ACTION_DOWN
                                || action == MotionEvent.ACTION_POINTER_DOWN) {
                            scheduleInstructionHide();
                        }
                        return false;
                    }
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null) {
            viewModel.refreshChoosingOrder();
        }
    }

    @Override
    public void onDestroyView() {
        handler.removeCallbacks(hideInstructionRunnable);
        binding = null;
        super.onDestroyView();
    }

    private void scheduleInstructionHide() {
        if (binding.chooserInstruction.getVisibility() != View.VISIBLE) {
            return;
        }
        handler.removeCallbacks(hideInstructionRunnable);
        handler.postDelayed(hideInstructionRunnable, INSTRUCTION_HIDE_DELAY_MS);
    }
}
