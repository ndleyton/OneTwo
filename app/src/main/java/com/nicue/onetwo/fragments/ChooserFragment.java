package com.nicue.onetwo.fragments;


import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.nicue.onetwo.R;
import com.nicue.onetwo.utils.TouchDisplayView;

public class ChooserFragment extends Fragment{
    private TouchDisplayView mView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View chooserView = inflater.inflate(R.layout.chooser_layout,container,false);
        mView = (TouchDisplayView) chooserView.findViewById(R.id.chooser_view);
        return chooserView;
    }
    public void setChoosingOrder(boolean b){
        mView.setChoosingOrder(b);
    }
}
