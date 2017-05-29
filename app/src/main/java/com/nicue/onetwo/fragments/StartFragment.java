package com.nicue.onetwo.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.nicue.onetwo.R;

public class StartFragment extends Fragment {



    /**
     * The interface that receives onClick messages.
     */
    public interface OnClickHandler {
        void onClick(View v);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.start_layout, container, false);
        return view;
    }

}
