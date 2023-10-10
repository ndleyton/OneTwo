package com.nicue.onetwo.adapters;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.TextView;

import com.nicue.onetwo.R;

import java.util.ArrayList;

import static android.util.Log.d;


public class ListAdapter extends RecyclerView.Adapter<ListAdapter.ListAdapterViewHolder> {

    private ArrayList<String> mObjectsToCount = new ArrayList<String>();
    private ArrayList<Integer> mNumbers = new ArrayList<Integer>();


    private final ListAdapterOnClickHandler mClickHandler;

    /**
     * The interface that receives onClick messages.
     */
    public interface ListAdapterOnClickHandler {
        void onClick(String obj);
        void onValueChanged(String obj, int num);
    }

    public ListAdapter(ListAdapterOnClickHandler clickHandler) {
        mClickHandler = clickHandler;
    }

    @Override
    public void onBindViewHolder(ListAdapter.ListAdapterViewHolder holder, int position) {
        String object = mObjectsToCount.get(position);
        int number = mNumbers.get(position);
        holder.mTextView.setText(object);
        holder.numberPicker.setValue(number);
    }

    @Override
    public int getItemCount() {
        if (null == mObjectsToCount) return 0;
        return mObjectsToCount.size();
    }

    @NonNull
    @Override
    public ListAdapter.ListAdapterViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        Context context = viewGroup.getContext();
        int layoutIdForListItem = R.layout.list_item;
        LayoutInflater inflater = LayoutInflater.from(context);
        boolean shouldAttachToParentImmediately = false;

        View view = inflater.inflate(layoutIdForListItem, viewGroup, shouldAttachToParentImmediately);
        return new ListAdapterViewHolder(view);
    }

    public class ListAdapterViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, NumberPicker.OnValueChangeListener, NumberPicker.OnScrollListener {
        public final TextView mTextView;
        public final NumberPicker numberPicker;
        public final Button removeButton;
        private int scrollState_p = 0;
        public ListAdapterViewHolder(View view) {
            super(view);
            mTextView = (TextView) view.findViewById(R.id.tv_object_data);
            removeButton = (Button) view.findViewById(R.id.remove_button);
            numberPicker = (NumberPicker) view.findViewById(R.id.number_picker);
            numberPicker.setMaxValue(99999);
            numberPicker.setMinValue(0);
            numberPicker.setOnScrollListener(this);
            numberPicker.setOnValueChangedListener(this);
            numberPicker.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    v.performClick();
                    return true;
                }
            });
            view.setOnClickListener(this);
        }

        @Override
        public void onScrollStateChange(NumberPicker view, int scrollState) {
            this.scrollState_p = scrollState;
            int pos = getAdapterPosition();
            Log.d("zpos",String.valueOf(pos));
            Log.d("zScrollState",String.valueOf(scrollState));
            if (scrollState == SCROLL_STATE_IDLE){
                int actual_value = view.getValue();
                int adapterPosition = getAdapterPosition();
                String obj = mObjectsToCount.get(adapterPosition);
                mClickHandler.onValueChanged(obj, actual_value);
            }
        }

        @Override
        public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
            int pos = getAdapterPosition();
            Log.d("zpos",String.valueOf(pos));
            Log.d("zScrollState",String.valueOf(this.scrollState_p));
            if (this.scrollState_p == SCROLL_STATE_IDLE) {
                int adapterPosition = getAdapterPosition();
                String obj = mObjectsToCount.get(adapterPosition);
                mClickHandler.onValueChanged(obj, newVal);
            }
        }

        /**
         * This gets called by the child views during a click.
         *
         * @param v The View that was clicked
         */
        @Override
        public void onClick(View v) {
            int adapterPosition = getAdapterPosition();
            String obj = mObjectsToCount.get(adapterPosition);
            mClickHandler.onClick(obj);
        }
    }
    public void setData(ArrayList<String> objects, ArrayList<Integer> numbers ){
        mObjectsToCount = objects;
        mNumbers = numbers;
        notifyDataSetChanged();
    }
}

