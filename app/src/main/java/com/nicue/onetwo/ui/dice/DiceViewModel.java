package com.nicue.onetwo.ui.dice;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import com.nicue.onetwo.data.dice.DiceRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DiceViewModel extends ViewModel {
    private static final String KEY_FACES = "dice_faces";
    private static final String KEY_VALUES = "dice_values";

    private final DiceRepository diceRepository;
    private final SavedStateHandle savedStateHandle;
    private final MutableLiveData<DiceUiState> uiState = new MutableLiveData<>();
    private final Random random = new Random();

    public DiceViewModel(DiceRepository diceRepository, SavedStateHandle savedStateHandle) {
        this.diceRepository = diceRepository;
        this.savedStateHandle = savedStateHandle;

        ArrayList<Integer> savedFaces = savedStateHandle.get(KEY_FACES);
        ArrayList<Integer> savedValues = savedStateHandle.get(KEY_VALUES);
        if (savedFaces == null) {
            savedFaces = new ArrayList<>(diceRepository.readDiceFaces());
        }
        if (savedValues == null || savedValues.size() != savedFaces.size()) {
            savedValues = new ArrayList<>();
            for (Integer face : savedFaces) {
                savedValues.add(face);
            }
        }
        updateState(savedFaces, savedValues);
    }

    public LiveData<DiceUiState> getUiState() {
        return uiState;
    }

    public void addDie(int faces) {
        ArrayList<Integer> dieFaces = getFaces();
        ArrayList<Integer> dieValues = getValues();
        dieFaces.add(Math.max(2, faces));
        dieValues.add(Math.max(2, faces));
        persistFaces(dieFaces);
        updateState(dieFaces, dieValues);
    }

    public void removeDie(int position) {
        ArrayList<Integer> dieFaces = getFaces();
        ArrayList<Integer> dieValues = getValues();
        if (position < 0 || position >= dieFaces.size()) {
            return;
        }
        dieFaces.remove(position);
        dieValues.remove(position);
        persistFaces(dieFaces);
        updateState(dieFaces, dieValues);
    }

    public void rollDie(int position) {
        ArrayList<Integer> dieFaces = getFaces();
        ArrayList<Integer> dieValues = getValues();
        if (position < 0 || position >= dieFaces.size()) {
            return;
        }
        dieValues.set(position, roll(dieFaces.get(position)));
        updateState(dieFaces, dieValues);
    }

    public void rollAllDice() {
        ArrayList<Integer> dieFaces = getFaces();
        ArrayList<Integer> dieValues = getValues();
        for (int i = 0; i < dieFaces.size(); i++) {
            dieValues.set(i, roll(dieFaces.get(i)));
        }
        updateState(dieFaces, dieValues);
    }

    private int roll(int faces) {
        return random.nextInt(faces) + 1;
    }

    private void persistFaces(List<Integer> dieFaces) {
        diceRepository.writeDiceFaces(dieFaces);
    }

    private ArrayList<Integer> getFaces() {
        ArrayList<Integer> values = savedStateHandle.get(KEY_FACES);
        return values == null ? new ArrayList<Integer>() : new ArrayList<>(values);
    }

    private ArrayList<Integer> getValues() {
        ArrayList<Integer> values = savedStateHandle.get(KEY_VALUES);
        return values == null ? new ArrayList<Integer>() : new ArrayList<>(values);
    }

    private void updateState(ArrayList<Integer> dieFaces, ArrayList<Integer> dieValues) {
        savedStateHandle.set(KEY_FACES, new ArrayList<>(dieFaces));
        savedStateHandle.set(KEY_VALUES, new ArrayList<>(dieValues));
        ArrayList<DieUiModel> dice = new ArrayList<>();
        for (int i = 0; i < dieFaces.size(); i++) {
            dice.add(new DieUiModel(dieFaces.get(i), dieValues.get(i)));
        }
        uiState.setValue(new DiceUiState(dice));
    }
}
