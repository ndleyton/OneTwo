package com.nicue.onetwo.ui.dice;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import com.nicue.onetwo.R;
import com.nicue.onetwo.databinding.DiceItemBinding;
import java.util.ArrayList;
import java.util.List;

public class DiceAdapter extends RecyclerView.Adapter<DiceAdapter.DiceViewHolder> {
    public interface Listener {
        void onRollDie(int position);

        void onRemoveDie(int position);
    }

    private final Listener listener;
    private List<DieUiModel> dice = new ArrayList<>();

    public DiceAdapter(Listener listener) {
        this.listener = listener;
    }

    @NonNull @Override
    public DiceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        return new DiceViewHolder(DiceItemBinding.inflate(inflater, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull DiceViewHolder holder, int position) {
        holder.bind(dice.get(position));
    }

    @Override
    public int getItemCount() {
        return dice.size();
    }

    public void submitList(final List<DieUiModel> newDice) {
        DiffUtil.DiffResult diffResult =
                DiffUtil.calculateDiff(
                        new DiffUtil.Callback() {
                            @Override
                            public int getOldListSize() {
                                return dice.size();
                            }

                            @Override
                            public int getNewListSize() {
                                return newDice == null ? 0 : newDice.size();
                            }

                            @Override
                            public boolean areItemsTheSame(
                                    int oldItemPosition, int newItemPosition) {
                                DieUiModel oldItem = dice.get(oldItemPosition);
                                DieUiModel newItem = newDice.get(newItemPosition);
                                return oldItem.getId() == newItem.getId();
                            }

                            @Override
                            public boolean areContentsTheSame(
                                    int oldItemPosition, int newItemPosition) {
                                DieUiModel oldItem = dice.get(oldItemPosition);
                                DieUiModel newItem = newDice.get(newItemPosition);
                                return oldItem.getFaces() == newItem.getFaces()
                                        && oldItem.getValue() == newItem.getValue();
                            }
                        });
        this.dice = newDice == null ? new ArrayList<DieUiModel>() : new ArrayList<>(newDice);
        diffResult.dispatchUpdatesTo(this);
    }

    public void animateAllVisibleItems(RecyclerView recyclerView, final Runnable endAction) {
        int animatedItemCount = 0;
        for (int i = 0; i < recyclerView.getChildCount(); i++) {
            RecyclerView.ViewHolder holder =
                    recyclerView.getChildViewHolder(recyclerView.getChildAt(i));
            if (holder instanceof DiceViewHolder) {
                animatedItemCount++;
            }
        }
        if (animatedItemCount == 0) {
            endAction.run();
            return;
        }

        final int[] remainingAnimations = {animatedItemCount};
        for (int i = 0; i < recyclerView.getChildCount(); i++) {
            RecyclerView.ViewHolder holder =
                    recyclerView.getChildViewHolder(recyclerView.getChildAt(i));
            if (holder instanceof DiceViewHolder) {
                ((DiceViewHolder) holder)
                        .animateRoll(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        remainingAnimations[0]--;
                                        if (remainingAnimations[0] == 0) {
                                            endAction.run();
                                        }
                                    }
                                });
            }
        }
    }

    class DiceViewHolder extends RecyclerView.ViewHolder {
        private final DiceItemBinding binding;
        private final int[] diceColors = {
            R.color.diceColor0, R.color.diceColor1, R.color.diceColor2,
            R.color.diceColor3, R.color.diceColor4, R.color.diceColor5,
            R.color.diceColor6, R.color.diceColor7, R.color.diceColor8,
            R.color.diceColor9
        };

        DiceViewHolder(DiceItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            binding.getRoot()
                    .setOnClickListener(
                            new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    if (getAdapterPosition() != RecyclerView.NO_POSITION) {
                                        animateRoll(
                                                new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        int position = getAdapterPosition();
                                                        if (position != RecyclerView.NO_POSITION) {
                                                            listener.onRollDie(position);
                                                        }
                                                    }
                                                });
                                    }
                                }
                            });
            binding.getRoot()
                    .setOnLongClickListener(
                            new View.OnLongClickListener() {
                                @Override
                                public boolean onLongClick(View v) {
                                    int position = getAdapterPosition();
                                    if (position != RecyclerView.NO_POSITION) {
                                        listener.onRemoveDie(position);
                                    }
                                    return true;
                                }
                            });
        }

        public void animateRoll(final Runnable endAction) {
            binding.getRoot().animate().cancel();
            binding.getRoot().setRotation(0f);
            binding.getRoot().setScaleX(1f);
            binding.getRoot().setScaleY(1f);
            binding.getRoot().setTranslationZ(0f);

            binding.getRoot()
                    .animate()
                    .rotationBy(360f)
                    .scaleX(1.15f)
                    .scaleY(1.15f)
                    .translationZ(16f)
                    .setDuration(120)
                    .withEndAction(
                            new Runnable() {
                                @Override
                                public void run() {
                                    binding.getRoot()
                                            .animate()
                                            .scaleX(1f)
                                            .scaleY(1f)
                                            .translationZ(0f)
                                            .setDuration(120)
                                            .withEndAction(
                                                    new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            binding.getRoot().setRotation(0f);
                                                            binding.getRoot().setScaleX(1f);
                                                            binding.getRoot().setScaleY(1f);
                                                            binding.getRoot().setTranslationZ(0f);
                                                            if (endAction != null) {
                                                                endAction.run();
                                                            }
                                                        }
                                                    })
                                            .start();
                                }
                            })
                    .start();
        }

        void bind(DieUiModel dieUiModel) {
            int faces = dieUiModel.getFaces();
            int position = getAdapterPosition();
            int colorRes = diceColors[position % diceColors.length];
            int color = binding.getRoot().getContext().getResources().getColor(colorRes);

            binding.getRoot().animate().cancel();
            binding.getRoot().setRotation(0f);
            binding.getRoot().setScaleX(1f);
            binding.getRoot().setScaleY(1f);
            binding.getRoot().setTranslationZ(0f);

            binding.diceCv.setCardBackgroundColor(color);

            boolean isDark = androidx.core.graphics.ColorUtils.calculateLuminance(color) < 0.5;
            int textColor = isDark ? 0xFFFFFFFF : 0xFF000000;
            int iconTint = isDark ? 0x88FFFFFF : 0x66000000;

            binding.tvDice.setTextColor(textColor);
            binding.tvDieType.setTextColor(textColor);
            binding.ivRollIndicator.setImageResource(getDieIconRes(faces));
            binding.ivRollIndicator.setColorFilter(iconTint);

            binding.tvDice.setText(String.valueOf(dieUiModel.getValue()));
            binding.tvDieType.setText(
                    binding.getRoot().getContext().getString(R.string.dice_type_label, faces));
        }

        @DrawableRes
        private int getDieIconRes(int faces) {
            switch (faces) {
                case 4:
                    return R.drawable.ic_die_d4_line;
                case 6:
                    return R.drawable.ic_die_d6_line;
                case 8:
                    return R.drawable.ic_die_d8_line;
                case 10:
                    return R.drawable.ic_die_d10_line;
                case 12:
                    return R.drawable.ic_die_d12_line;
                case 20:
                    return R.drawable.ic_die_d20_line;
                default:
                    return R.drawable.ic_die_custom_wheel;
            }
        }
    }
}
