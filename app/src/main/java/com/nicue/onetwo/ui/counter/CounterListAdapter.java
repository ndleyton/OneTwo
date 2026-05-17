package com.nicue.onetwo.ui.counter;

import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.NumberPicker;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import com.nicue.onetwo.data.counter.CounterEntity;
import com.nicue.onetwo.databinding.ListItemBinding;
import java.util.ArrayList;
import java.util.List;

public class CounterListAdapter extends RecyclerView.Adapter<CounterListAdapter.CounterViewHolder> {
    private static final int DELETE_REVEAL_DISTANCE_DP = 56;

    public interface Listener {
        void onValueChanged(long counterId, int value);

        void onAdjustmentClicked(long counterId, int currentValue, boolean add);

        void onDeleteClicked(long counterId);

        void onDragHandleTouched(RecyclerView.ViewHolder viewHolder);
    }

    private final Listener listener;
    private List<CounterEntity> counters = new ArrayList<>();

    public CounterListAdapter(Listener listener) {
        this.listener = listener;
    }

    @NonNull @Override
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

    public void submitList(List<CounterEntity> newCounters) {
        if (newCounters == null) newCounters = new ArrayList<>();
        DiffUtil.DiffResult diffResult =
                DiffUtil.calculateDiff(new CounterDiffCallback(this.counters, newCounters));
        this.counters = new ArrayList<>(newCounters);
        diffResult.dispatchUpdatesTo(this);
    }

    public boolean moveItem(int fromPosition, int toPosition) {
        if (fromPosition < 0
                || toPosition < 0
                || fromPosition >= counters.size()
                || toPosition >= counters.size()) {
            return false;
        }
        CounterEntity movedCounter = counters.remove(fromPosition);
        counters.add(toPosition, movedCounter);
        notifyItemMoved(fromPosition, toPosition);
        return true;
    }

    public List<Long> getCounterIds() {
        List<Long> counterIds = new ArrayList<>();
        for (CounterEntity counter : counters) {
            counterIds.add(counter.getId());
        }
        return counterIds;
    }

    class CounterViewHolder extends RecyclerView.ViewHolder
            implements NumberPicker.OnValueChangeListener, NumberPicker.OnScrollListener {
        private final ListItemBinding binding;
        private CounterEntity currentCounter;
        private int scrollState = SCROLL_STATE_IDLE;
        private final float deleteRevealDistance;
        private float downX;
        private float foregroundStartTranslation;
        private boolean draggingForeground;

        CounterViewHolder(ListItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            deleteRevealDistance =
                    DELETE_REVEAL_DISTANCE_DP
                            * binding.getRoot().getResources().getDisplayMetrics().density;
            binding.numberPicker.setMaxValue(99999);
            binding.numberPicker.setMinValue(0);
            binding.numberPicker.setOnScrollListener(this);
            binding.numberPicker.setOnLongClickListener(
                    new View.OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View v) {
                            v.performClick();
                            return true;
                        }
                    });
            binding.gripDots.setOnTouchListener(
                    new View.OnTouchListener() {
                        @Override
                        public boolean onTouch(View v, MotionEvent event) {
                            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                                listener.onDragHandleTouched(CounterViewHolder.this);
                                return true;
                            } else if (event.getActionMasked() == MotionEvent.ACTION_UP) {
                                v.performClick();
                            }
                            return false;
                        }
                    });
            binding.removeButton.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (currentCounter != null) {
                                listener.onDeleteClicked(currentCounter.getId());
                            }
                        }
                    });
            binding.itemForeground.setOnTouchListener(
                    new View.OnTouchListener() {
                        @Override
                        public boolean onTouch(View v, MotionEvent event) {
                            return handleForegroundTouch(v, event);
                        }
                    });
            binding.subtractButton.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (currentCounter != null) {
                                listener.onAdjustmentClicked(
                                        currentCounter.getId(),
                                        binding.numberPicker.getValue(),
                                        false);
                            }
                        }
                    });
            binding.addButton.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (currentCounter != null) {
                                listener.onAdjustmentClicked(
                                        currentCounter.getId(),
                                        binding.numberPicker.getValue(),
                                        true);
                            }
                        }
                    });
        }

        void bind(CounterEntity counterEntity) {
            currentCounter = counterEntity;
            binding.tvObjectData.setText(counterEntity.getTitle());
            binding.itemForeground.setTranslationX(0f);
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

        private boolean handleForegroundTouch(View v, MotionEvent event) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    downX = event.getRawX();
                    foregroundStartTranslation = binding.itemForeground.getTranslationX();
                    draggingForeground = false;
                    return true;
                case MotionEvent.ACTION_MOVE:
                    float distance = event.getRawX() - downX;
                    if (distance > 0 || foregroundStartTranslation > 0f) {
                        draggingForeground = true;
                        float translation =
                                Math.max(
                                        0f,
                                        Math.min(
                                                deleteRevealDistance,
                                                foregroundStartTranslation + distance));
                        binding.itemForeground.setTranslationX(translation);
                        return true;
                    }
                    return false;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (draggingForeground) {
                        float openThreshold = deleteRevealDistance / 2f;
                        float targetTranslation =
                                binding.itemForeground.getTranslationX() >= openThreshold
                                        ? deleteRevealDistance
                                        : 0f;
                        binding.itemForeground
                                .animate()
                                .translationX(targetTranslation)
                                .setDuration(120)
                                .start();
                        draggingForeground = false;
                        return true;
                    } else if (event.getActionMasked() == MotionEvent.ACTION_UP) {
                        v.performClick();
                    }
                    return false;
                default:
                    return false;
            }
        }
    }

    private static class CounterDiffCallback extends DiffUtil.Callback {
        private final List<CounterEntity> oldList;
        private final List<CounterEntity> newList;

        CounterDiffCallback(List<CounterEntity> oldList, List<CounterEntity> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).getId() == newList.get(newItemPosition).getId();
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            CounterEntity oldItem = oldList.get(oldItemPosition);
            CounterEntity newItem = newList.get(newItemPosition);
            return oldItem.getValue() == newItem.getValue()
                    && oldItem.getTitle().equals(newItem.getTitle())
                    && oldItem.getSortOrder() == newItem.getSortOrder();
        }
    }
}
