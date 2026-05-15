package com.nicue.onetwo.ui.chooser;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.nicue.onetwo.OneTwoApplication;
import com.nicue.onetwo.data.settings.SettingsRepository;
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
        SettingsRepository settingsRepository = ((OneTwoApplication) requireActivity().getApplication())
                .getAppContainer()
                .getSettingsRepository();
        viewModel = new ViewModelProvider(this, new ChooserViewModelFactory(settingsRepository))
                .get(ChooserViewModel.class);
        viewModel.getChoosingOrder().observe(getViewLifecycleOwner(), new androidx.lifecycle.Observer<Boolean>() {
            @Override
            public void onChanged(Boolean choosingOrder) {
                boolean value = Boolean.TRUE.equals(choosingOrder);
                binding.chooserView.setChoosingOrder(value);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null) {
            viewModel.refreshChoosingOrder();
        }
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}
