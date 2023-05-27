package com.example.weatherappnecoy

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.weatherappnecoy.Adapters.WeatherModel

class MainViewModel: ViewModel() {
    val liveDatacurrent = MutableLiveData<WeatherModel>()
    val liveDataList = MutableLiveData<List<WeatherModel>>()
}