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
    private static final String KEY_IDS = "dice_ids";
    private static final String KEY_NEXT_ID = "dice_next_id";

    private final DiceRepository diceRepository;
    private final SavedStateHandle savedStateHandle;
    private final MutableLiveData<DiceUiState> uiState = new MutableLiveData<>();
    private final Random random = new Random();

    public DiceViewModel(DiceRepository diceRepository, SavedStateHandle savedStateHandle) {
        this.diceRepository = diceRepository;
        this.savedStateHandle = savedStateHandle;

        ArrayList<Integer> savedFaces = savedStateHandle.get(KEY_FACES);
        ArrayList<Integer> savedValues = savedStateHandle.get(KEY_VALUES);
        ArrayList<Long> savedIds = savedStateHandle.get(KEY_IDS);
        Long nextId = savedStateHandle.get(KEY_NEXT_ID);

        if (savedFaces == null) {
            savedFaces = new ArrayList<>(diceRepository.readDiceFaces());
        }
        if (savedValues == null || savedValues.size() != savedFaces.size()) {
            savedValues = new ArrayList<>();
            for (Integer face : savedFaces) {
                savedValues.add(face);
            }
        }
        if (savedIds == null || savedIds.size() != savedFaces.size()) {
            savedIds = new ArrayList<>();
            long currentId = 0;
            for (int i = 0; i < savedFaces.size(); i++) {
                savedIds.add(currentId++);
            }
            savedStateHandle.set(KEY_NEXT_ID, currentId);
        }
        updateState(savedFaces, savedValues, savedIds);
    }

    public LiveData<DiceUiState> getUiState() {
        return uiState;
    }

    public void addDie(int faces) {
        ArrayList<Integer> dieFaces = getFaces();
        ArrayList<Integer> dieValues = getValues();
        ArrayList<Long> dieIds = getIds();
        Long nextId = savedStateHandle.get(KEY_NEXT_ID);
        if (nextId == null) nextId = 0L;

        dieFaces.add(Math.max(2, faces));
        dieValues.add(Math.max(2, faces));
        dieIds.add(nextId);
        
        savedStateHandle.set(KEY_NEXT_ID, nextId + 1);
        persistFaces(dieFaces);
        updateState(dieFaces, dieValues, dieIds);
    }

    public void removeDie(int position) {
        ArrayList<Integer> dieFaces = getFaces();
        ArrayList<Integer> dieValues = getValues();
        ArrayList<Long> dieIds = getIds();
        if (position < 0 || position >= dieFaces.size()) {
            return;
        }
        dieFaces.remove(position);
        dieValues.remove(position);
        dieIds.remove(position);
        persistFaces(dieFaces);
        updateState(dieFaces, dieValues, dieIds);
    }

    public void rollDie(int position) {
        ArrayList<Integer> dieFaces = getFaces();
        ArrayList<Integer> dieValues = getValues();
        ArrayList<Long> dieIds = getIds();
        if (position < 0 || position >= dieFaces.size()) {
            return;
        }
        dieValues.set(position, roll(dieFaces.get(position)));
        updateState(dieFaces, dieValues, dieIds);
    }

    public void rollAllDice() {
        ArrayList<Integer> dieFaces = getFaces();
        ArrayList<Integer> dieValues = getValues();
        ArrayList<Long> dieIds = getIds();
        for (int i = 0; i < dieFaces.size(); i++) {
            dieValues.set(i, roll(dieFaces.get(i)));
        }
        updateState(dieFaces, dieValues, dieIds);
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

    private ArrayList<Long> getIds() {
        ArrayList<Long> values = savedStateHandle.get(KEY_IDS);
        return values == null ? new ArrayList<Long>() : new ArrayList<>(values);
    }

    private void updateState(ArrayList<Integer> dieFaces, ArrayList<Integer> dieValues, ArrayList<Long> dieIds) {
        savedStateHandle.set(KEY_FACES, new ArrayList<>(dieFaces));
        savedStateHandle.set(KEY_VALUES, new ArrayList<>(dieValues));
        savedStateHandle.set(KEY_IDS, new ArrayList<>(dieIds));
        ArrayList<DieUiModel> dice = new ArrayList<>();
        for (int i = 0; i < dieFaces.size(); i++) {
            dice.add(new DieUiModel(dieIds.get(i), dieFaces.get(i), dieValues.get(i)));
        }
        uiState.setValue(new DiceUiState(dice));
    }
}
