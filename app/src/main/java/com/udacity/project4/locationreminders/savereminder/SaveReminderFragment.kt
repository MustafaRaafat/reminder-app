package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.TargetApi
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.ReminderDescriptionActivity
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.sendNotification
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import java.util.concurrent.TimeUnit

class SaveReminderFragment : BaseFragment() {
    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()

    private lateinit var geoFencingClient: GeofencingClient
    private var isLocationEnabole: Boolean = false
    val runQorH = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q

    private lateinit var binding: FragmentSaveReminderBinding
    private var REQUEST_BACKGROUND_PERMISSION = false
    private var REQUEST_FOREGROUND_ONLY_PERMISSIONS = false


    val requestForground = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        REQUEST_FOREGROUND_ONLY_PERMISSIONS = it
        if (!it) {
            _viewModel.showToast.value = "location access needed"
        }
    }
    val requestBackground = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        REQUEST_BACKGROUND_PERMISSION = it
        if (!it) {
            _viewModel.showToast.value = "background location access needed"
        }
    }

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(requireContext(), GeofenceBroadcastReceiver::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
        } else {
            PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)

        setDisplayHomeAsUpEnabled(true)

        binding.viewModel = _viewModel

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        REQUEST_BACKGROUND_PERMISSION = if (runQorH) {
            PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        } else {
            true
        }
        REQUEST_FOREGROUND_ONLY_PERMISSIONS =
            (PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ))
        binding.selectLocation.setOnClickListener {
//            Navigate to another fragment to get the user location
            _viewModel.navigationCommand.value =
                NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
        }

        binding.saveReminder.setOnClickListener {
            val title = _viewModel.reminderTitle.value
            val description = _viewModel.reminderDescription
            val location = _viewModel.reminderSelectedLocationStr.value
            val latitude = _viewModel.latitude
            val longitude = _viewModel.longitude.value

//             use the user entered reminder details to:
//             1) add a geofencing request
//             2) save the reminder to the local db
            geoFencingClient = LocationServices.getGeofencingClient(requireActivity())

            val data =
                ReminderDataItem(title, description.value, location, latitude.value, longitude)
            addGeofenceAndSave(data)
        }
    }


    private fun addGeofenceAndSave(data: ReminderDataItem) {
        if (REQUEST_FOREGROUND_ONLY_PERMISSIONS && REQUEST_BACKGROUND_PERMISSION) {
            checkDeviceLocationSettingsAndStartGeofence()
        } else {
            requestForegroundAndBackgroundLocationPermissions()
        }
        if (isLocationEnabole) {
            val geofence = Geofence.Builder()
                .setRequestId("data.id")
                .setCircularRegion(_viewModel.latitude.value!!, _viewModel.longitude.value!!, 100f)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .build()
            val geofeneRequest = GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence)
                .build()
            geoFencingClient.addGeofences(geofeneRequest, geofencePendingIntent).run {
                addOnSuccessListener {
                    _viewModel.validateAndSaveReminder(data)
                    _viewModel.showSnackBarInt.value = R.string.geofence_entered
                }
                addOnFailureListener {
                    _viewModel.showSnackBarInt.value = R.string.geofences_not_added
                }
            }
        }
//        geofencingClient.removeGeofences(geofencePendingIntent).run {
//            addOnCompleteListener {
//                geofencingClient.addGeofences(geofeneRequest, geofencePendingIntent).run {
//                    addOnSuccessListener {
//                        Toast.makeText(context, "Toast success", Toast.LENGTH_SHORT).show()
//                    }
//                    addOnFailureListener {
//                        Toast.makeText(context, "Toast failed", Toast.LENGTH_SHORT).show()
//                    }
//                }
//            }
//        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 3) {
            checkDeviceLocationSettingsAndStartGeofence(false)
        }
    }


    @TargetApi(29)
    private fun requestForegroundAndBackgroundLocationPermissions() {
        if (runQorH) {
            if (REQUEST_BACKGROUND_PERMISSION && REQUEST_FOREGROUND_ONLY_PERMISSIONS) return
            if (!REQUEST_FOREGROUND_ONLY_PERMISSIONS) requestForground.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            if (!REQUEST_BACKGROUND_PERMISSION) requestBackground.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)

        } else {
            if (REQUEST_FOREGROUND_ONLY_PERMISSIONS) return
            requestForground.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun checkDeviceLocationSettingsAndStartGeofence(resolve: Boolean = true) {
        val locareq = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locareq)
        val settingClint = LocationServices.getSettingsClient(requireActivity())
        val locSetResponceTask = settingClint.checkLocationSettings(builder.build())
        locSetResponceTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && resolve) {
                try {
                    exception.startResolutionForResult(requireActivity(), 3)
                } catch (sendEx: IntentSender.SendIntentException) {
                    _viewModel.showSnackBar.value =
                        "Error getting location settings resolution: $sendEx"
                }
            } else {
                _viewModel.showSnackBarInt.value = R.string.location_required_error
            }
        }
        locSetResponceTask.addOnCompleteListener {
            if (it.isSuccessful) {
                isLocationEnabole = true
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }
}
