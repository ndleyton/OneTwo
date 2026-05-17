package com.nicue.onetwo.data.counter;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

@Dao
public interface CounterDao {
    @Query("SELECT * FROM Objects ORDER BY sort_order ASC, _id ASC")
    LiveData<List<CounterEntity>> observeCounters();

    @Query("SELECT COALESCE(MAX(sort_order), -1) + 1 FROM Objects")
    int getNextSortOrder();

    @Insert
    void insert(CounterEntity counterEntity);

    @Query("UPDATE Objects SET numbers = :value WHERE _id = :counterId")
    void updateValue(long counterId, int value);

    @Query("UPDATE Objects SET sort_order = :sortOrder WHERE _id = :counterId")
    void updateSortOrder(long counterId, int sortOrder);

    @Transaction
    default void updateSortOrders(List<Long> orderedCounterIds) {
        for (int i = 0; i < orderedCounterIds.size(); i++) {
            updateSortOrder(orderedCounterIds.get(i), i);
        }
    }

    @Query("DELETE FROM Objects WHERE _id = :counterId")
    void deleteById(long counterId);
}
