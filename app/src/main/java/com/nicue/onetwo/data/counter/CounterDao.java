package com.nicue.onetwo.data.counter;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface CounterDao {
    @Query("SELECT * FROM Objects ORDER BY _id ASC")
    LiveData<List<CounterEntity>> observeCounters();

    @Insert
    void insert(CounterEntity counterEntity);

    @Query("UPDATE Objects SET numbers = :value WHERE _id = :counterId")
    void updateValue(long counterId, int value);

    @Query("DELETE FROM Objects WHERE _id = :counterId")
    void deleteById(long counterId);
}
