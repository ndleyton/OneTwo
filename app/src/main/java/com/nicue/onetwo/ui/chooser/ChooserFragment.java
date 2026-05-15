package com.nicue.onetwo.ui.chooser;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.nicue.onetwo.databinding.ChooserLayoutBinding;

public class ChooserFragment extends Fragment {
    private ChooserLayoutBinding binding;
    private ChooserViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = ChooserLayoutBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this, new ChooserViewModelFactory())
                .get(ChooserViewModel.class);
        binding.chooserModeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                viewModel.setChoosingOrder(isChecked);
            }
        });
        viewModel.getChoosingOrder().observe(getViewLifecycleOwner(), new androidx.lifecycle.Observer<Boolean>() {
            @Override
            public void onChanged(Boolean choosingOrder) {
                boolean value = Boolean.TRUE.equals(choosingOrder);
                if (binding.chooserModeSwitch.isChecked() != value) {
                    binding.chooserModeSwitch.setChecked(value);
                }
                binding.chooserView.setChoosingOrder(value);
            }
        });
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}
