package com.nicue.onetwo.data.dice;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class DicePrefsDataSource {
    private static final String PREF_FILE = "SHARED_PREFS_FILE";
    private static final String KEY_DICES = "DICES";
    private final SharedPreferences sharedPreferences;

    public DicePrefsDataSource(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
    }

    public List<Integer> readDiceFaces() {
        String rawValue = sharedPreferences.getString(KEY_DICES, "");
        ArrayList<Integer> faces = new ArrayList<>();
        try {
            JSONArray jsonArray = new JSONArray(rawValue);
            for (int i = 0; i < jsonArray.length(); i++) {
                faces.add(jsonArray.getInt(i));
            }
        } catch (JSONException ignored) {
            return new ArrayList<>();
        }
        return faces;
    }

    public void writeDiceFaces(List<Integer> faces) {
        JSONArray jsonArray = new JSONArray();
        for (Integer face : faces) {
            jsonArray.put(face);
        }
        sharedPreferences.edit().putString(KEY_DICES, jsonArray.toString()).apply();
    }
}
