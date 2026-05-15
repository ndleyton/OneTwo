package com.nicue.onetwo.ui.counter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.NumberPicker;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.nicue.onetwo.data.counter.CounterEntity;
import com.nicue.onetwo.databinding.ListItemBinding;

import java.util.ArrayList;
import java.util.List;

public class CounterListAdapter extends RecyclerView.Adapter<CounterListAdapter.CounterViewHolder> {
    public interface Listener {
        void onValueChanged(long counterId, int value);

        void onDeleteClicked(long counterId);
    }

    private final Listener listener;
    private List<CounterEntity> counters = new ArrayList<>();

    public CounterListAdapter(Listener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public CounterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        return new CounterViewHolder(ListItemBinding.inflate(inflater, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull CounterViewHolder holder, int position) {
        holder.bind(counters.get(position));
    }

    @Override
    public int getItemCount() {
        return counters.size();
    }

    public void submitList(List<CounterEntity> counters) {
        this.counters = counters == null ? new ArrayList<CounterEntity>() : counters;
        notifyDataSetChanged();
    }

    class CounterViewHolder extends RecyclerView.ViewHolder
            implements NumberPicker.OnValueChangeListener, NumberPicker.OnScrollListener {
        private final ListItemBinding binding;
        private CounterEntity currentCounter;
        private int scrollState = SCROLL_STATE_IDLE;

        CounterViewHolder(ListItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            binding.numberPicker.setMaxValue(99999);
            binding.numberPicker.setMinValue(0);
            binding.numberPicker.setOnScrollListener(this);
            binding.numberPicker.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    v.performClick();
                    return true;
                }
            });
            binding.removeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (currentCounter != null) {
                        listener.onDeleteClicked(currentCounter.getId());
                    }
                }
            });
        }

        void bind(CounterEntity counterEntity) {
            currentCounter = counterEntity;
            binding.tvObjectData.setText(counterEntity.getTitle());
            binding.numberPicker.setOnValueChangedListener(null);
            binding.numberPicker.setValue(counterEntity.getValue());
            binding.numberPicker.setOnValueChangedListener(this);
        }

        @Override
        public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
            if (currentCounter != null && scrollState == SCROLL_STATE_IDLE) {
                listener.onValueChanged(currentCounter.getId(), newVal);
            }
        }

        @Override
        public void onScrollStateChange(NumberPicker view, int scrollState) {
            this.scrollState = scrollState;
            if (currentCounter != null && scrollState == SCROLL_STATE_IDLE) {
                listener.onValueChanged(currentCounter.getId(), view.getValue());
            }
        }
    }
}
