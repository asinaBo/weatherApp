package com.example.weatherappnecoy.fragments

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.weatherappnecoy.Adapters.VpAdapter
import com.example.weatherappnecoy.Adapters.WeatherModel
import com.example.weatherappnecoy.DialogManager
import com.example.weatherappnecoy.MainViewModel
import com.example.weatherappnecoy.R
import com.example.weatherappnecoy.databinding.FragmentMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.material.tabs.TabLayoutMediator
import com.squareup.picasso.Picasso
import org.json.JSONObject
import java.util.Objects

const val API_KEY = "92359178a130484998f51026231103"

class MainFragment : Fragment() {
    private lateinit var fLocationClient: FusedLocationProviderClient

    private val flist = listOf(
        HoursFragment.newInstance(),
        DaysFragment.newInstance()

    )
    private val tlist = listOf(
        "Hours",
        "Days"
    )
    private lateinit var pLauncher: ActivityResultLauncher<String>
    private lateinit var binding: FragmentMainBinding
    private val model : MainViewModel by activityViewModels()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMainBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        checkPermission()
        init()
        updateCurrentCard()
       // requestWeatherData("Bishkek")
        //getLocation()


    }

    override fun onResume() {
        super.onResume()
        chechLocation()
    }
    private fun init() = with(binding){
        fLocationClient =  LocationServices.getFusedLocationProviderClient(requireContext())
        val adapter = VpAdapter(activity as FragmentActivity,flist)
        vp.adapter= adapter
        TabLayoutMediator(tabLayout,vp){
            tab, pos -> tab.text = tlist[pos]

        }.attach()
        ibSync.setOnClickListener {
            tabLayout.selectTab(tabLayout.getTabAt(0))
            //getLocation()
            chechLocation()
        }

        ibSearch.setOnClickListener {
            DialogManager.serachByNameDialog(requireContext(), object : DialogManager.Listener {
                override fun onClick(name: String?) {
                    name?.let { it1 -> requestWeatherData(it1) }
                }

            })
        }


    }
    private fun chechLocation(){
        if (isLocationEnabled()){
            getLocation()
        }else{
            DialogManager.locationSettingsDialog(requireContext(),object : DialogManager.Listener{
                override fun onClick(name : String?) {
                    startActivity(Intent(Settings.ACTION_LOCALE_SETTINGS))

                }

            })
        }
    }
    private fun isLocationEnabled (): Boolean{
        val lm = activity?.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER)

    }
    private fun getLocation(){
        if (!isLocationEnabled()){
            Toast.makeText(requireContext(),"location disabled",Toast.LENGTH_LONG).show()
            return
        }
        val ct = CancellationTokenSource()
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            return
        }
        fLocationClient
            .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY,ct.token)
            .addOnCompleteListener {
                requestWeatherData("${it.result.latitude},${it.result.longitude}")
            }

    }
private fun updateCurrentCard() = with(binding){
    model.liveDatacurrent.observe(viewLifecycleOwner){
        val  maxMinTemp = "${it.maxTemp}C/${it.minTemp}C"
        tvData.text=it.time
        tvCity.text = it.city
        tvCurrentTemp.text = it.currentTemp.ifEmpty { maxMinTemp}
        tvCondition.text = it.condition
        //tvMaxMin.text = maxMinTemp
        tvMaxMin.text = if (it.currentTemp.isEmpty()) "" else maxMinTemp
       Picasso.get().load("https:" + it.imageUrl).into(imWeather)

    }
}
private fun permissionListener(){
    pLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()){
        Toast.makeText(activity,"Permission is $it", Toast.LENGTH_LONG).show()
    }
}
    private fun checkPermission(){
        if(!isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION)){
            permissionListener()
            pLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }

    }

    private fun requestWeatherData(city: String){
        val url = "https://api.weatherapi.com/v1/forecast.json?key="+
        API_KEY+
        "&q="+
        city+
        "&days="+
        "3"+
        "&aqi=no&alerts=no\n"

        val queue = Volley.newRequestQueue(context)
        val request = StringRequest(
            Request.Method.GET,
            url,
            {
                result -> parceWeatherData(result)
            },
            {
                error ->
                Log.d ("MyLog","Error: $error")
            }
        )
        queue.add(request)
    }

    private fun parceWeatherData(result : String){
        val mainObject = JSONObject(result)
        val list = parceDays(mainObject)
        parceCurrentData(mainObject,list[0])

    }
    private fun parceDays(mainObject: JSONObject): List<WeatherModel>{
        val list = java.util.ArrayList<WeatherModel>()
        val dayArray = mainObject.getJSONObject("forecast")
            .getJSONArray("forecastday")
        val name = mainObject.getJSONObject("location").getString("name")

        for (i in 0 until dayArray.length()){
            val day = dayArray[i] as JSONObject
            val item = WeatherModel(
                name,
                day.getString("date"),
                day.getJSONObject("day")
                    .getJSONObject("condition")
                    .getString("text"),
                "",
                day.getJSONObject("day").getString("maxtemp_c").toFloat().toInt().toString(),
                day.getJSONObject("day").getString("mintemp_c").toFloat().toInt().toString(),
                day.getJSONObject("day")
                    .getJSONObject("condition")
                    .getString("icon"),
                day.getJSONArray("hour").toString()
            )
            list.add(item)
        }
        model.liveDataList.value = list
        return list


    }

    private fun parceCurrentData(mainObject: JSONObject, weatherItem : WeatherModel){
        val item = WeatherModel(
            mainObject.getJSONObject("location").getString("name"),
            mainObject.getJSONObject("current").getString("last_updated"),
            mainObject.getJSONObject("current")
                .getJSONObject("condition").getString("text"),
            mainObject.getJSONObject("current").getString("temp_c"),
            weatherItem.maxTemp,
            weatherItem.minTemp,
            mainObject.getJSONObject("current")
                .getJSONObject("condition").getString("icon"),
            weatherItem.hours

        )
        model.liveDatacurrent.value = item
      //  Log.d ("MyLog","max: ${item.maxTemp}")
       // Log.d ("MyLog","min: ${item.minTemp}")
       // Log.d ("MyLog","time: ${item.hours}")


    }
    companion object {

        @JvmStatic
        fun newInstance() = MainFragment()


    }
}