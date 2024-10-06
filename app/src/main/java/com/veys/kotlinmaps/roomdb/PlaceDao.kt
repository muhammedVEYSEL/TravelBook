package com.veys.kotlinmaps.roomdb

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.veys.kotlinmaps.model.Place
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Flowable


@Dao
interface PlaceDao {

    @Query("SELECT * FROM Place")
    fun getAll():Flowable<List<Place>>   //  flowbel ve completable bu sql işlemlerini asenkron olarak arka planda
                                         // yapılmasına olanak sağlar
    @Insert
    fun insert(place : Place) : Completable

    @Delete
    fun delete(place: Place) : Completable
}