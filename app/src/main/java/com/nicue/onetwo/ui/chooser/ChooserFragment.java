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
import android.animation.ValueAnimator;
import android.view.animation.AccelerateDecelerateInterpolator;
import com.nicue.onetwo.data.settings.SettingsRepository;
import com.nicue.onetwo.databinding.ChooserLayoutBinding;

public class ChooserFragment extends Fragment {
    private static final long INSTRUCTION_HIDE_DELAY_MS = 800L;

    private ChooserLayoutBinding binding;
    private ChooserViewModel viewModel;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable navigateBackRunnable;
    private ValueAnimator breathingAnimator;
    private final Runnable hideInstructionRunnable =
            new Runnable() {
                @Override
                public void run() {
                    if (binding != null && binding.chooserInstructionContainer.getVisibility() == View.VISIBLE) {
                        binding.chooserInstructionContainer.animate()
                                .alpha(0f)
                                .setDuration(300L)
                                .withEndAction(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (binding != null) {
                                            binding.chooserInstructionContainer.setVisibility(View.GONE);
                                            if (breathingAnimator != null) {
                                                breathingAnimator.cancel();
                                                breathingAnimator = null;
                                            }
                                        }
                                    }
                                })
                                .start();
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

        // Breathing pulse and color morphing animation for the fingerprint outline icon
        final int colorStart = androidx.core.content.ContextCompat.getColor(
                requireContext(), com.nicue.onetwo.R.color.colorPrimaryLight);
        final int colorEnd = androidx.core.content.ContextCompat.getColor(
                requireContext(), com.nicue.onetwo.R.color.colorAccent);
        final android.animation.ArgbEvaluator argbEvaluator = new android.animation.ArgbEvaluator();

        breathingAnimator = ValueAnimator.ofFloat(0f, 1f);
        breathingAnimator.setDuration(2400L); // Gentle duration for breathing and color transition
        breathingAnimator.setRepeatCount(ValueAnimator.INFINITE);
        breathingAnimator.setRepeatMode(ValueAnimator.REVERSE);
        breathingAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        breathingAnimator.addUpdateListener(
                new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        if (binding != null && binding.chooserInstructionIcon != null) {
                            float val = (float) animation.getAnimatedValue();

                            // Scale animation (0.95 to 1.05)
                            float scale = 0.95f + (val * 0.1f);
                            binding.chooserInstructionIcon.setScaleX(scale);
                            binding.chooserInstructionIcon.setScaleY(scale);

                            // Dynamic color morphing
                            int color = (int) argbEvaluator.evaluate(val, colorStart, colorEnd);
                            binding.chooserInstructionIcon.setImageTintList(
                                    android.content.res.ColorStateList.valueOf(color));
                        }
                    }
                });
        breathingAnimator.start();

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

        Bundle arguments = getArguments();
        boolean returnOnSelection =
                arguments != null && arguments.getBoolean("return_on_selection", false);
        if (returnOnSelection) {
            binding.chooserView.setOnSelectionListener(
                    new com.nicue.onetwo.utils.TouchDisplayView.OnSelectionListener() {
                        @Override
                        public void onSelectionMade() {
                            if (arguments != null && arguments.containsKey("player_count")) {
                                int playerCount = arguments.getInt("player_count", 0);
                                if (playerCount >= 1 && playerCount <= 6) {
                                    float chosenX = binding.chooserView.getSelectionRevealCenterX();
                                    float chosenY = binding.chooserView.getSelectionRevealCenterY();
                                    int width = binding.chooserView.getWidth();
                                    int height = binding.chooserView.getHeight();
                                    if (width > 0 && height > 0) {
                                        int closestSeatIndex =
                                                getClosestSeatIndex(
                                                        chosenX,
                                                        chosenY,
                                                        width,
                                                        height,
                                                        playerCount);
                                        try {
                                            androidx.navigation.NavBackStackEntry prevEntry =
                                                    androidx.navigation.fragment.NavHostFragment
                                                            .findNavController(ChooserFragment.this)
                                                            .getPreviousBackStackEntry();
                                            if (prevEntry != null) {
                                                prevEntry
                                                        .getSavedStateHandle()
                                                        .set("chosen_seat_index", closestSeatIndex);
                                            }
                                        } catch (Exception e) {
                                            // Handle/ignore if navigation entry not available
                                        }
                                    }
                                }
                            }

                            if (navigateBackRunnable != null) {
                                handler.removeCallbacks(navigateBackRunnable);
                            }
                            navigateBackRunnable =
                                    new Runnable() {
                                        @Override
                                        public void run() {
                                            if (isAdded()) {
                                                androidx.navigation.fragment.NavHostFragment
                                                        .findNavController(ChooserFragment.this)
                                                        .popBackStack();
                                            }
                                        }
                                    };
                            handler.postDelayed(
                                    navigateBackRunnable,
                                    com.nicue.onetwo.utils.TouchDisplayView
                                                    .SELECTION_REVEAL_DURATION_MS
                                            + 1500L);
                        }
                    });
        }
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
        if (breathingAnimator != null) {
            breathingAnimator.cancel();
            breathingAnimator = null;
        }
        if (navigateBackRunnable != null) {
            handler.removeCallbacks(navigateBackRunnable);
            navigateBackRunnable = null;
        }
        if (binding != null && binding.chooserView != null) {
            binding.chooserView.setOnSelectionListener(null);
        }
        binding = null;
        super.onDestroyView();
    }

    private void scheduleInstructionHide() {
        if (binding.chooserInstructionContainer.getVisibility() != View.VISIBLE) {
            return;
        }
        handler.removeCallbacks(hideInstructionRunnable);
        handler.postDelayed(hideInstructionRunnable, INSTRUCTION_HIDE_DELAY_MS);
    }

    static float[] getSeatCenter(int seatIndex, int playerCount, float width, float height) {
        float x = width / 2f;
        float y = height / 2f;

        switch (playerCount) {
            case 1:
                break;
            case 2:
                if (seatIndex == 0) {
                    y = height / 4f;
                } else if (seatIndex == 1) {
                    y = 3f * height / 4f;
                }
                break;
            case 3:
                if (seatIndex == 0) {
                    y = 3f * height / 4f;
                } else if (seatIndex == 1) {
                    x = width / 4f;
                    y = height / 4f;
                } else if (seatIndex == 2) {
                    x = 3f * width / 4f;
                    y = height / 4f;
                }
                break;
            case 4:
                if (seatIndex == 0) {
                    x = width / 4f;
                    y = height / 4f;
                } else if (seatIndex == 1) {
                    x = 3f * width / 4f;
                    y = height / 4f;
                } else if (seatIndex == 2) {
                    x = width / 4f;
                    y = 3f * height / 4f;
                } else if (seatIndex == 3) {
                    x = 3f * width / 4f;
                    y = 3f * height / 4f;
                }
                break;
            case 5:
                if (seatIndex == 0) {
                    x = width / 4f;
                    y = height / 6f;
                } else if (seatIndex == 1) {
                    x = 3f * width / 4f;
                    y = height / 6f;
                } else if (seatIndex == 2) {
                    x = width / 4f;
                    y = height / 2f;
                } else if (seatIndex == 3) {
                    x = 3f * width / 4f;
                    y = height / 2f;
                } else if (seatIndex == 4) {
                    y = 5f * height / 6f;
                }
                break;
            case 6:
                if (seatIndex == 0) {
                    x = width / 4f;
                    y = height / 6f;
                } else if (seatIndex == 1) {
                    x = 3f * width / 4f;
                    y = height / 6f;
                } else if (seatIndex == 2) {
                    x = width / 4f;
                    y = height / 2f;
                } else if (seatIndex == 3) {
                    x = 3f * width / 4f;
                    y = height / 2f;
                } else if (seatIndex == 4) {
                    x = width / 4f;
                    y = 5f * height / 6f;
                } else if (seatIndex == 5) {
                    x = 3f * width / 4f;
                    y = 5f * height / 6f;
                }
                break;
        }

        return new float[] {x, y};
    }

    static int getClosestSeatIndex(
            float touchX, float touchY, float width, float height, int playerCount) {
        int closestIndex = 0;
        double minDistanceSq = Double.MAX_VALUE;

        for (int i = 0; i < playerCount; i++) {
            float[] center = getSeatCenter(i, playerCount, width, height);
            double dx = touchX - center[0];
            double dy = touchY - center[1];
            double distanceSq = dx * dx + dy * dy;
            if (distanceSq < minDistanceSq) {
                minDistanceSq = distanceSq;
                closestIndex = i;
            }
        }

        return closestIndex;
    }
}
