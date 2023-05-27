package com.example.weatherappnecoy.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.weatherappnecoy.Adapters.WeatherAdapter
import com.example.weatherappnecoy.Adapters.WeatherModel
import com.example.weatherappnecoy.MainViewModel
import com.example.weatherappnecoy.R
import com.example.weatherappnecoy.databinding.FragmentDaysBinding


class DaysFragment : Fragment(), WeatherAdapter.Listener {
    private lateinit var adapter : WeatherAdapter

private lateinit var binding : FragmentDaysBinding

private val model : MainViewModel by activityViewModels ()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentDaysBinding.inflate(inflater,container,false)
        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
        model.liveDataList.observe(viewLifecycleOwner){
            adapter.submitList(it)
        }
    }
    private fun init()= with(binding){
        adapter = WeatherAdapter(this@DaysFragment)
        rcViewDays.layoutManager = LinearLayoutManager(activity)
        rcViewDays.adapter = adapter
    }

    companion object {

        @JvmStatic
        fun newInstance() = DaysFragment()

    }

    override fun onClick(item: WeatherModel) {
        model.liveDatacurrent.value = item

    }
}