package com.nicue.onetwo.adapters;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Vibrator;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.nicue.onetwo.R;

import java.util.ArrayList;
import java.util.Random;


public class DiceListAdapter extends RecyclerView.Adapter<DiceListAdapter.ViewHolder> {

    private Context mContext;
    private final Handler handler = new Handler();

    public class RollingRunnable implements Runnable{
        private int mPosition;
        private int mMaxDice;

        public RollingRunnable (int position, int maxDice){
            mPosition = position;
            mMaxDice = maxDice;
        }
        @Override
        public void run() {
            realRun(mPosition, mMaxDice);
        }

        public void realRun(int pos, int maxDice){
            int new_num = random.nextInt(maxDice) + 1;
            mData.set(pos, String.valueOf(new_num));
            notifyDataSetChanged();
        }
    }

    public class SetViewRunnable implements Runnable{
        private TextView mView;
        private int mMaxDice;

        public SetViewRunnable (TextView v, int maxDice){
            mView = v;
            mMaxDice = maxDice;
        }
        @Override
        public void run() {
            realRun(mView, mMaxDice);
        }

        public void realRun(TextView v, int maxDice){
            int new_num = random.nextInt(maxDice) + 1;
            v.setText(String.valueOf(new_num));
        }
    }


    private ArrayList<String> mFaces = new ArrayList<>();
    private ArrayList<String> mData = new ArrayList<>();
    private LayoutInflater mInflater;
    private ItemClickListener mClickListener;
    private final DiceAdapterOnClickHandler mClickHandler;
    private Random random = new Random();


    public DiceListAdapter(DiceAdapterOnClickHandler clickHandler, ItemClickListener clickListener, Context context ) {
        mClickHandler = clickHandler;
        mClickListener = clickListener;
        mContext = context;
    }

    public interface DiceAdapterOnClickHandler {
        void onClick(View v);
        void onClick(View v, int pos);
    }

    // inflates the cell layout from xml when needed
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        mInflater = LayoutInflater.from(parent.getContext());
        View view = mInflater.inflate(R.layout.dice_item, parent, false);
        ViewHolder viewHolder = new ViewHolder(view);
        return viewHolder;
    }

    // binds the data to the textviews in each cell and changes the image depending on number of faces
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        String number = mData.get(position);
        String faces = mFaces.get(position);
        holder.mTextView.setText(number);
        holder.facesTextView.setText(faces);
        int faces_int = Integer.parseInt(faces);
        if (faces_int < 5){
            holder.mImageView.setImageResource(R.drawable.triangle_2);
            holder.mTextView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        }else if (faces_int < 7){
            holder.mImageView.setImageResource(R.drawable.square_1);
            holder.mTextView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        }else if (faces_int < 13){
            holder.mImageView.setImageResource(R.drawable.penta_3);
            holder.mTextView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        }else if (faces_int < 21){
            holder.mImageView.setImageResource(R.drawable.hexa_4);
            holder.mTextView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        }else if (faces_int <1000){
            holder.mImageView.setImageResource(R.drawable.hexa_0);
            holder.mTextView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        }

        /*
        Tried to add color randomly on dices, but they changed with every roll... maybe too psicodelic

        int faces_int = Integer.parseInt(faces);
        int random_color = random.nextInt(4)+1;
        try {
            if (faces_int < 5) {
                Field field = R.drawable.class.getDeclaredField(String.format("triangle_%d",random_color));
                int a = field.getInt(this);
                holder.mImageView.setImageResource(a);
            } else if (faces_int < 7) {
                Field field = R.drawable.class.getDeclaredField(String.format("square_%d",random_color));
                int a = field.getInt(this);
                holder.mImageView.setImageResource(a);
            } else if (faces_int < 13) {
                Field field = R.drawable.class.getDeclaredField(String.format("penta_%d",random_color));
                int a = field.getInt(this);
                holder.mImageView.setImageResource(a);
            } else if (faces_int < 21) {
                Field field = R.drawable.class.getDeclaredField(String.format("hexa_%d",random_color));
                int a = field.getInt(this);
                holder.mImageView.setImageResource(a);
            }
        }catch (Exception e){e.printStackTrace();}

         */
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }


    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
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
            itemView.setOnLongClickListener(this);
        }

        @Override
        public void onClick(View view) {
            Vibrator vibrator = (Vibrator) view.getContext().getSystemService(Context.VIBRATOR_SERVICE);
            long[] pattern = {0,15,10,15,10,15};
            vibrator.vibrate(pattern,-1);
            int pos = getLayoutPosition();  // getAdapterPosition() may return -1 and produce an error
            int max_dice = Integer.parseInt(mFaces.get(pos));
            int new_num = random.nextInt(max_dice) + 1;
            mData.set(pos, String.valueOf(new_num));
            notifyDataSetChanged();
            RollingRunnable rollingRunnable = new RollingRunnable(pos, max_dice);
            // SetViewRunnable just changes textView
            //SetViewRunnable setViewRunnable = new SetViewRunnable(mTextView, max_dice);
            handler.postDelayed(rollingRunnable, 60);
            handler.postDelayed(rollingRunnable, 130);
            handler.postDelayed(rollingRunnable, 210);
            handler.postDelayed(rollingRunnable, 320);
            if (random.nextBoolean()){
                handler.postDelayed(rollingRunnable, 500);
            }
        }

        @Override
        public boolean onLongClick(View v) {
            int pos = getAdapterPosition();
            mClickListener.onItemLongClick(v,pos);
            return true;
        }
    }

    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onItemLongClick(View view, int position);
    }

    public void setmData(ArrayList<String> diceData, ArrayList<String> facesData){
        mData = diceData;
        mFaces = facesData;
        notifyDataSetChanged();
    }

}