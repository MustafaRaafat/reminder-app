package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import java.util.*

class SelectLocationFragment : BaseFragment() {

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding
    private lateinit var map: GoogleMap
    private var marker: Marker? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

        return binding.root
    }

    private fun setMapStyle(map: GoogleMap) {
        map.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.map_style))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        add the map setup implementation
        val callBackFromMap = OnMapReadyCallback { googleMap ->
            map = googleMap
            if (_viewModel.reminderSelectedLocationStr.value.isNullOrEmpty()) {
                val home = LatLng(30.04700281431639, 31.23511779506277)
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(home, 12f))
            } else {
                marker = map.addMarker(
                    MarkerOptions().position(_viewModel.selectedPOI.value!!.latLng)
                        .title(_viewModel.reminderSelectedLocationStr.value)
                )
                map.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        _viewModel.selectedPOI.value!!.latLng,
                        12f
                    )
                )
            }
//            add style to the map
            setMapStyle(map)
//            put a marker to location that the user selected
            setMarkOnMap(map)
//            zoom to the user location after taking his permission
            enableMyLocation()
        }

        val mapFragment =
            childFragmentManager.findFragmentById(R.id.map_fragment_id) as SupportMapFragment?
        mapFragment?.getMapAsync(callBackFromMap)

    }

    //    call this function after the user confirms on the selected location
    private fun onLocationSelected() {
//        When the user confirms on the selected location,
//        send back the selected location details to the view model
//        and navigate back to the previous fragment to save the reminder and add the geofence

        _viewModel.selectedPOI.value = PointOfInterest(marker!!.position, "1", "POIname")
        _viewModel.latitude.value = marker!!.position.latitude
        _viewModel.longitude.value = marker!!.position.longitude
        _viewModel.reminderSelectedLocationStr.value = marker!!.title
        _viewModel.navigationCommand.value = NavigationCommand.Back
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
//        Change the map type based on the user's selection.
        R.id.normal_map -> {
            map.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.hybrid_map -> {
            map.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.satellite_map -> {
            map.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.terrain_map -> {
            map.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun setMarkOnMap(map: GoogleMap) {
        map.setOnMapLongClickListener {
            val snippet = String.format(
                Locale.getDefault(),
                getString(R.string.lat_long_snippet),
                it.latitude,
                it.longitude
            )
            if (marker == null) {
                marker =
                    map.addMarker(MarkerOptions().position(it).title("pin here").snippet(snippet))
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(it, 15f))
            } else {
                marker!!.remove()
                marker =
                    map.addMarker(MarkerOptions().position(it).title("pin here").snippet(snippet))
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(it, 15f))
            }
        }
        map.setOnInfoWindowClickListener {
            onLocationSelected()
        }
        map.setOnMyLocationClickListener {
            marker = map.addMarker(
                MarkerOptions().position(LatLng(it.latitude, it.longitude)).title("my location")
            )
            onLocationSelected()
        }
        map.setOnPoiClickListener {
            if (marker == null) {
                marker = map.addMarker(MarkerOptions().position(it.latLng).title(it.name))
                marker?.showInfoWindow()
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(it.latLng, 15f))
            } else {
                marker!!.remove()
                marker = map.addMarker(MarkerOptions().position(it.latLng).title(it.name))
                marker?.showInfoWindow()
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(it.latLng, 15f))
            }
        }
    }

    private fun enableMyLocation() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            map.isMyLocationEnabled = true
        } else {
            requestLancher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    val requestLancher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permission ->
            when {
                permission.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                    enableMyLocation()
                }
                else -> {
                    _viewModel.showErrorMessage.value = "location access needed"
                }
            }
        }
}
