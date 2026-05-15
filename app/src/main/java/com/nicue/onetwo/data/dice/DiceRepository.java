package com.nicue.onetwo.data.dice;

import java.util.List;

public class DiceRepository {
    private final DicePrefsDataSource dataSource;

    public DiceRepository(DicePrefsDataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<Integer> readDiceFaces() {
        return dataSource.readDiceFaces();
    }

    public void writeDiceFaces(List<Integer> faces) {
        dataSource.writeDiceFaces(faces);
    }
}
