package com.nicue.onetwo;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Random;


public class DiceListAdapter extends RecyclerView.Adapter<DiceListAdapter.ViewHolder> {

    private ArrayList<String> mFaces = new ArrayList<>();
    private ArrayList<String> mData = new ArrayList<>();
    private LayoutInflater mInflater;
    //private ItemClickListener mClickListener;
    private final DiceAdapterOnClickHandler mClickHandler;


    public DiceListAdapter(DiceAdapterOnClickHandler clickHandler) {
        mClickHandler = clickHandler;
    }

    public interface DiceAdapterOnClickHandler {
        void onClick(View v);
        void onClick(View v, int pos);
        //void onValueChanged(String obj, int num);
    }

    // inflates the cell layout from xml when needed
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        mInflater = LayoutInflater.from(parent.getContext());
        View view = mInflater.inflate(R.layout.dice_item, parent, false);
        ViewHolder viewHolder = new ViewHolder(view);
        return viewHolder;
    }

    // binds the data to the textview in each cell
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        String number = mData.get(position);
        String faces = mFaces.get(position);
        holder.mTextView.setText(number);
        holder.facesTextView.setText(faces);
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }


    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public TextView mTextView;
        public ImageView mImageView;
        public TextView facesTextView;
        public Button rollButton;

        public ViewHolder(View itemView) {
            super(itemView);
            facesTextView = (TextView) itemView.findViewById(R.id.tv_faces);
            mTextView = (TextView) itemView.findViewById(R.id.tv_dice);
            mImageView = (ImageView) itemView.findViewById(R.id.imageView_dice);
            rollButton = (Button) itemView.findViewById(R.id.throw_button);
            rollButton.setOnClickListener(this);
            //itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            int pos = getAdapterPosition();
            int max_dice = Integer.parseInt(mFaces.get(pos));
            Random r = new Random();
            int new_num = r.nextInt(max_dice) + 1;
            mData.set(pos, String.valueOf(new_num));
            //mClickHandler.onClick(view, pos);
            notifyDataSetChanged();
        }
    }
    /*

    // convenience method for getting data at click position
    public String getItem(int id) {
        return mData.get(id);
    }

    // allows clicks events to be caught
    public void setClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onItemClick(View view, int position);
    }
    */

    public void setmData(ArrayList<String> diceData, ArrayList<String> facesData){
        mData = diceData;
        mFaces = facesData;
        notifyDataSetChanged();
    }
}