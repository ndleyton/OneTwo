package com.nicue.onetwo.ui.dice;

import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
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

    @NonNull
    @Override
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

    public void submitList(List<DieUiModel> dice) {
        this.dice = dice == null ? new ArrayList<DieUiModel>() : dice;
        notifyDataSetChanged();
    }

    class DiceViewHolder extends RecyclerView.ViewHolder {
        private final DiceItemBinding binding;

        DiceViewHolder(DiceItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            binding.throwButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onRollDie(position);
                    }
                }
            });
            binding.getRoot().setOnLongClickListener(new View.OnLongClickListener() {
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

        void bind(DieUiModel dieUiModel) {
            int faces = dieUiModel.getFaces();
            binding.tvDice.setText(String.valueOf(dieUiModel.getValue()));
            binding.tvFaces.setText(String.valueOf(faces));
            if (faces < 5) {
                binding.imageViewDice.setImageResource(R.drawable.triangle_2);
            } else if (faces < 7) {
                binding.imageViewDice.setImageResource(R.drawable.square_1);
            } else if (faces < 13) {
                binding.imageViewDice.setImageResource(R.drawable.penta_3);
            } else if (faces < 21) {
                binding.imageViewDice.setImageResource(R.drawable.hexa_4);
            } else {
                binding.imageViewDice.setImageResource(R.drawable.hexa_0);
            }
            binding.tvDice.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        }
    }
}
