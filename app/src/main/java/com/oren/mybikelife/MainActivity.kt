package com.oren.mybikelife

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PorterDuff
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.webkit.WebView
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat

import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.oren.mybikelife.data.Config
import com.oren.util.bluetooth.BluetoothLeService
import com.oren.util.bluetooth.UseGattAttributes
import com.oren.xml.Element
import com.oren.xml.XmlParser
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.content_main.*
import java.io.IOException
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener
        , OnMapReadyCallback, ActivityCompat.OnRequestPermissionsResultCallback { //,GoogleMap.OnMarkerClickListener

    private var cameraMng : CameraManager? = null
    private var mapFragment: SupportMapFragment? = null

    // 지도 핸들링 변수들
    private var sMap: GoogleMap? = null
    private var currentMarker: Marker? = null
    private var loadHistory :PolylineOptions? = null // = PolylineOptions()
//    private var pauseCnt : Int = 0

    private var historyData : Element = Element("loadLine")
//    private var loadPoints : ArrayList<LatLng>? = null

    //
    private val gpsEnableRequestCode = 2001
    private val updateIntervalMs = 1000L  // 1초
    private val fastestUpdateIntervalMs = 500L // 0.5초

    // onRequestPermissionsResult 에서 수신된 결과에서 ActivityCompat.requestPermissions 를 사용한 퍼미션 요청을 구별하기 위해 사용됩니다.
    private val permissionsRequestCode = 100
    private var needRequest = false

    private var requiredPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION
        , Manifest.permission.ACCESS_COARSE_LOCATION
        , Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    var mCurrentLocatiion: Location? = null
    var currentPosition: LatLng? = null

    private var mFusedLocationClient: FusedLocationProviderClient? = null
    private var locationRequest: LocationRequest? = null
    private var location: Location? = null

    private var bStartPlay = false
//    private var bSensor = false
    private var bFlash = false

    private var a = this

//    private val mLayout: RelativeLayout? = null  // Snackbar 사용하기 위해서는 View가 필요합니다.
    private lateinit var mLayout: View
    // (참고로 Toast에서는 Context가 필요했습니다.)

    override fun onStart() {
        super.onStart()
        if (checkPermission() && bStartPlay) {
            mFusedLocationClient?.requestLocationUpdates(locationRequest, locationCallback, null)
            sMap?.isMyLocationEnabled = true
            ibViewFixed.setImageResource(android.R.drawable.ic_menu_mylocation)
        }
    }

    override fun onStop() {
        super.onStop()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            cameraMng?.setTorchMode(getCameraId(), false)
        }
        if(bStartPlay) {
            mFusedLocationClient?.removeLocationUpdates(locationCallback)
        }
        UseGattAttributes.disconnect()
    }

//    override fun onResume() {
//        super.onResume()
//        registerReceiver(mGattUpdateReceiver, UseGattAttributes.makeGattUpdateIntentFilter())
//        if (mBluetoothLeService != null) {
//            val result = mBluetoothLeService.connect(UseGattAttributes.mDeviceAddress)
//            android.util.Log.d("xy", "Connect request result=$result")
//        }
//    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            gpsEnableRequestCode ->
                //사용자가 GPS 활성 시켰는지 검사
                if (checkLocationServicesStatus()) {
                    if (checkLocationServicesStatus()) {
                        android.util.Log.d("xy", "onActivityResult : GPS 활성화 되있음")
                        needRequest = true
                        return
                    }
                }
        }
    }
    private var appInfo : PackageInfo? =null
    private lateinit var play :FloatingActionButton
    private var adRequest: AdRequest? = null
    private var adMobView: AdView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        android.util.Log.d("xy", "create start")
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        a = this
        UseGattAttributes.mainActi = this
        try {
            appInfo = packageManager.getPackageInfo(packageName, 0)
        } catch (e: Exception) {
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cameraMng = getSystemService(Context.CAMERA_SERVICE) as CameraManager
            flash.systemUiVisibility = FloatingActionButton.VISIBLE
        } else {
            flash.systemUiVisibility = FloatingActionButton.GONE
        }
        mLayout = this.findViewById(R.id.lyBody) as RelativeLayout

        loadHistory = PolylineOptions()
        loadHistory?.color(Color.BLUE)
        loadHistory?.width(7f)
        loadHistory?.startCap(RoundCap()) //ButtCap, CustomCap, RoundCap, SquareCap
        loadHistory?.endCap(RoundCap())

        // 스피드 케이던스 센서 on/off
        sensor.setOnClickListener {
            if(!sensor.isEnabled) {
                Toast.makeText(this, this.getString(R.string.device_disconnect), Toast.LENGTH_LONG).show()
                android.util.Log.d("xy",   "false")
                UseGattAttributes.disconnect();
                invalidateOptionsMenu()
                clearUI()
                UseGattAttributes.mConnected = false
            } else {
                Toast.makeText(this, this.getString(R.string.device_connection), Toast.LENGTH_LONG).show()
                android.util.Log.d("xy",   "true")
                var address : String? = null
                var address1 : String? = null
                var ii : Int = 0
                for (elDevice in Config.selectedElBike.gChildren("device")) {
                    if (elDevice.gAttrValue("connectDevice") == "true") {
                        if (elDevice.gAttrValue("deviceAddress") != "") {
                            if(ii == 0) {
                                address = elDevice.gAttrValue("deviceAddress")
                            }else {
                                address1 = elDevice.gAttrValue("deviceAddress")
                                break
                            };
                        }
                    }
                    ii++
                }
                UseGattAttributes.connect(address, address1)
                invalidateOptionsMenu()
            }
//            bSensor = !bSensor
        }
        // 손전등 On/Off
        flash.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if(bFlash) {
                    flash.backgroundTintMode = PorterDuff.Mode.DST_IN
                    android.util.Log.d("xy",   "false")
                } else {
                    flash.backgroundTintMode = PorterDuff.Mode.OVERLAY
                    android.util.Log.d("xy",   "true")
                }
                bFlash = !bFlash
                cameraMng?.setTorchMode(getCameraId(), bFlash)

            }
        }
        // 지도 고정여부
        ibViewFixed.setOnClickListener {
            if(sMap?.isMyLocationEnabled!!) {
                if (checkPermission()) {
                    sMap?.isMyLocationEnabled = false
                    ibViewFixed.setImageResource(android.R.drawable.ic_menu_myplaces)
                }
            } else {
                if (checkPermission()) {
                    sMap?.isMyLocationEnabled = true
                    ibViewFixed.setImageResource(android.R.drawable.ic_menu_mylocation)
                }
            }
        }
        // 버드뷰 여부
        ibViewMode.setOnClickListener {
            if(sMap?.cameraPosition!!.tilt < 67.5f) {
                ibViewMode.setImageResource(android.R.drawable.ic_menu_mapmode)
                val cameraUpdate = CameraUpdateFactory.newCameraPosition(CameraPosition(sMap?.cameraPosition!!.target, sMap?.cameraPosition!!.zoom,90f,sMap?.cameraPosition!!.bearing))
                sMap?.animateCamera(cameraUpdate)
            } else {
                ibViewMode.setImageResource(android.R.drawable.ic_menu_send)
                val cameraUpdate = CameraUpdateFactory.newCameraPosition(CameraPosition(sMap?.cameraPosition!!.target, sMap?.cameraPosition!!.zoom,0f,sMap?.cameraPosition!!.bearing))
                sMap?.animateCamera(cameraUpdate)
            }
        }

        // 지도 보기 여부
        val fab = this.findViewById(R.id.fab) as FloatingActionButton
        fab.setOnClickListener { view ->
            if(lyMap.visibility == RelativeLayout.VISIBLE) {
                hudTextColor(Color.WHITE)
                (view as FloatingActionButton).setImageResource(android.R.drawable.ic_dialog_map)
                lyMap.visibility = RelativeLayout.GONE
            } else {
                hudTextColor(Color.BLACK)
                (view as FloatingActionButton).setImageResource(android.R.drawable.ic_menu_revert)
                lyMap.visibility = RelativeLayout.VISIBLE
            }
        }
        // 주행 초기화
        val init = this.findViewById(R.id.init) as FloatingActionButton
        init.setOnClickListener {
            if(historyData.gChildren().size > 0) {
                val yesNo = AlertDialog.Builder(this@MainActivity)
                yesNo.setTitle(getString(R.string.ok))
                yesNo.setMessage(getString(R.string.init_msg) + "\n")
                yesNo.setPositiveButton(getString(R.string.yes)) { dialog, _ ->
                    initHud()
                    stopLocationUpdates()
                    play.setImageResource(android.R.drawable.ic_media_play)
                    dialog.dismiss()
                }
                yesNo.setNegativeButton(getString(R.string.no)) { dialog, _ ->
                    dialog.dismiss()
                }
                yesNo.create()
                yesNo.show()
            } else {
                initHud()
                stopLocationUpdates()
                play.setImageResource(android.R.drawable.ic_media_play)
            }
        }
        // 주행 기록 저장
        val save = this.findViewById(R.id.save) as FloatingActionButton
        save.setOnClickListener {
            android.util.Log.d("xy", "갯수 => "+historyData.gChildren().size.toString() )
            if(historyData.gChildren().size > 0) {
                if(util.saveHistory(a, historyData)) {
                    Toast.makeText(this, getString(R.string.save_history), Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, getString(R.string.save_history_fail), Toast.LENGTH_LONG).show()
                }
            }
            initHud()
            stopLocationUpdates()
            play.setImageResource(android.R.drawable.ic_media_play)
        }
        // 주행 시작 멈춤
        play = this.findViewById(R.id.play) as FloatingActionButton
        play.setOnClickListener { view ->
            if(bStartPlay) {
                stopLocationUpdates()
                Toast.makeText(this, getString(R.string.stop_play), Toast.LENGTH_LONG).show()
                (view as FloatingActionButton).setImageResource(android.R.drawable.ic_media_play)
            } else {
                startLocationUpdates()
                Toast.makeText(this, getString(R.string.start_play), Toast.LENGTH_LONG).show()
                (view as FloatingActionButton).setImageResource(android.R.drawable.ic_media_pause)
            }
            if(moveTime == 0L) // 프로그램 처음 실행시?
                moveTime = System.currentTimeMillis()
        }
//        메뉴 열기
        val toggle = ActionBarDrawerToggle(
                this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        nav_view.setNavigationItemSelectedListener(this)

        locationRequest = LocationRequest()
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
            .setInterval(updateIntervalMs)
            .setFastestInterval(fastestUpdateIntervalMs)

        val builder = LocationSettingsRequest.Builder()
        builder.addLocationRequest(locationRequest as LocationRequest)
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment?.getMapAsync(this)
        initHud()

        // 광고 처리 시작 ===========================================================================================================================================
        // 디바이스 아이디 갤노트FE 7BA887C29CCF8C5A
        MobileAds.initialize(this) // 테스트 아이디 : ca-app-pub-3940256099942544/6300978111  실제 아이디 ca-app-pub-5662815491108033/2507532391
        adMobView = this.findViewById(R.id.gwangview) as AdView
        adRequest = AdRequest.Builder().build()  //. AddTestDevice("7BA887C29CCF8C5A")
        adMobView!!.adListener = object: AdListener() {
            override fun onAdLoaded() {}
            override fun onAdFailedToLoad(errorCode : Int) {
                android.util.Log.d("xy", "errorCode : $errorCode")
                myAdView(this@MainActivity)
            }
            override fun onAdOpened() {}
            override fun onAdLeftApplication() {}
            override fun onAdClosed() {}
        }
        // 광고 처리 마침 ===========================================================================================================================================
        thread.isDaemon = true
        thread.start()

        Config.loadConfig(this)
        historyData.sAttr("bikeId", Config.selectedElBike.gAttrValue("id"))
        android.util.Log.d("xy", "create end")
    }

    // 프로그램 정보 모여 홈페이지에서 가져오는 루틴
    @Suppress("DEPRECATION")
    private var thread = Thread {
        try {
            val conn = URL("http://app.moyeo.org/bikelife.xml").openConnection()
            val doc = XmlParser().parse(conn.getInputStream())
            if (doc != null) {
                val root = doc.gRootElem()
                setAdType(Integer.parseInt(root.gAttrValue("ad-type")))
                val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    appInfo?.longVersionCode!!
                } else {
                    appInfo?.versionCode!!.toLong()
                }
                android.util.Log.d("xy", "versionCode : $versionCode")
                if (versionCode < Integer.parseInt(root.gAttrValue("version-code")).toLong()) {
                    this.runOnUiThread {
                        val yesNo = AlertDialog.Builder(this@MainActivity)
                        yesNo.setTitle(getString(R.string.yes))
                        val msg = root.gText()
                        yesNo.setMessage(msg + "\n")
                        yesNo.setPositiveButton(getString(R.string.yes)){dialog, _ ->
                            val intent = Intent(Intent.ACTION_VIEW)
                            intent.data =  Uri.parse("market://details?id=com.oren.mybikelife")
                            this@MainActivity.startActivity(intent)
                            dialog.dismiss()
                            finish()
                        }
                        yesNo.setNegativeButton(getString(R.string.no)) {dialog, _ ->
                            dialog.dismiss()
                        }
                        yesNo.create()
                        yesNo.show()
                    }
                }
            }
        } catch (e:Exception) {
            android.util.Log.i("xy", e.toString())
        }
    }


    /*
    * 케이던스 속도 센서 블루투스 정보 넘어오는 부분    *
     */
    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private var prev_cadence_rpm : Int = 0
    private var prev_cadence_time : Long = 0
    private var prev_speed_rpm: Int = 0
    private var prev_speed_time : Long = 0
    val mGattUpdateReceiver = object : BroadcastReceiver() {
        @SuppressLint("SetTextI18n")
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothLeService.ACTION_GATT_CONNECTED -> {
                    UseGattAttributes.mConnected = true
                    lyCadence.visibility = View.VISIBLE
                    invalidateOptionsMenu()
                    UseGattAttributes.startService()
                    sensor.isEnabled = false
                    Toast.makeText(context, context.getString(R.string.device_connect), Toast.LENGTH_LONG).show()
                }
                BluetoothLeService.ACTION_GATT_DISCONNECTED -> {
                    UseGattAttributes.mConnected = false
                    invalidateOptionsMenu()
                    clearUI()
                    sensor.isEnabled = true;
                    Toast.makeText(context, context.getString(R.string.device_not_connect), Toast.LENGTH_LONG).show()
                }

                // Show all the supported services and characteristics on the user interface.
                BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED -> {
                    UseGattAttributes.receiveData()
                }
                BluetoothLeService.ACTION_DATA_AVAILABLE -> {
                    val strss = intent.getStringExtra(UseGattAttributes.mBluetoothLeService.EXTRA_DATA); //.split(":")
                    data_available(strss);
                }
            }
        }
    }
    fun data_available(strss:String) {
        val strs = strss.split(":");
        if(strs.size < 5) return
        val curr_speed_rpm = strs[1].toInt()
        if(curr_speed_rpm >= 0) {
            val curr_speed_time = strs[2].toLong()
            if (prev_speed_time != curr_speed_time) {
                if (prev_speed_time < curr_speed_time) {
                    val rpm = curr_speed_rpm - prev_speed_rpm
                    val time = curr_speed_time - prev_speed_time
                    // 속도 = 바퀴의 지름 *.3.14 * 초당회전수 * 3,600/ 1,000,000 (km/h)
                    val speed =
                        Config.selectWeelSize * 3.14 * (rpm.toFloat() / (time.toFloat() / 1000)) * 3600.0 / 1000000.0
                    if (lyHud.visibility == View.VISIBLE) {
                        tvSpeed.text = String.format("%.1f", speed)
                        when (speed >= 40f) {
                            true -> tvSpeed.setTextColor(Color.RED)
                            false -> when (speed >= 30f) {
                                true -> tvSpeed.setTextColor(Color.parseColor("#FFff5555"))
                                false -> when (speed >= 25f) {
                                    true -> tvSpeed.setTextColor(Color.parseColor("#FFffaaaa"))
                                    false -> when (lyMap.visibility == View.VISIBLE) {
                                        true -> tvSpeed.setTextColor(Color.BLACK)
                                        false -> tvSpeed.setTextColor(Color.WHITE)
                                    }
                                }
                            }
                        }
                        if (maxSpeed < speed) {
                            maxSpeed = speed.toFloat()
                            tvMaxSpeed.text = String.format("%.1f Km/h", maxSpeed)
                        }
                    }
//                } else {
//                    tvSpeed.text = "0.0 Km/h"
                }
                prev_speed_rpm = curr_speed_rpm
                prev_speed_time = curr_speed_time
            } else {
                tvSpeed.text = "0.0"
            }
        }
        val curr_cadence_rpm = strs[3].toInt()
        if(curr_cadence_rpm >= 0) {
            val curr_cadence_time = strs[4].toLong()
            if (prev_cadence_time != curr_cadence_time) {
                if (prev_cadence_time < curr_cadence_time) {
                    val cadence = curr_cadence_rpm - prev_cadence_rpm
                    val time = curr_cadence_time - prev_cadence_time
                    val rpm = cadence.toFloat() / (time.toFloat() / 1024.0 / 60.0)
                    tvCadence.text = String.format("%.1f", rpm) + " RPM"
                    if (maxCadence < rpm) {
                        maxCadence = rpm.toFloat()
                    }
//                } else {
//                    tvCadence.text = "0.0 RPM"
                }
                prev_cadence_rpm = curr_cadence_rpm
                prev_cadence_time = curr_cadence_time
            } else {
                tvCadence.text = "0.0 RPM"
            }
        }
    }
    private fun clearUI() {
        lyCadence.visibility = View.GONE
    }
    /*
    * GPS 정보 넘어오는 부분    *
     */
    var maxSpeed : Float = 0f
    var maxCadence : Float = 0f
    var moveLen : Double = 0.0
    var moveTime : Long  = 0L
    var locationPrev :Location? = null
    private var locationCallback:LocationCallback = object:LocationCallback() {
        override fun onLocationResult(locationResult:LocationResult) {
            super.onLocationResult(locationResult)
            val locationList = locationResult.locations
            var speed = 0f
            if (locationList.size > 0) {
                location = locationList[locationList.size - 1]
                currentPosition = LatLng((location as Location).latitude, (location as Location).longitude)
                if(lyMap.visibility == View.VISIBLE) { // 지도가 활성화 되어 있을때 주소를 가져온다.
                    val markerTitle = getCurrentAddress(currentPosition as LatLng)
                    val markerSnippet = "위도:" + ((location as Location).latitude).toString() + " 경도:" + ((location as Location).longitude).toString()
                    android.util.Log.d("xy", "onLocationResult : $markerSnippet")
                    //현재 위치에 마커 생성하고 이동
                    setCurrentLocation(location as Location, markerTitle, markerSnippet)
                    mCurrentLocatiion = location
                }
                speed = location?.speed.toString().toFloat() * 3.6f
                if(lyHud.visibility == View.VISIBLE ) {
                    if(!UseGattAttributes.mConnected) {
                        tvSpeed.text = String.format("%.1f", speed)
//                    android.util.Log.d("xy", location?.speed.toString())
                        when (speed >= 40f) {
                            true -> tvSpeed.setTextColor(Color.RED)
                            false -> when (speed >= 30f) {
                                true -> tvSpeed.setTextColor(Color.parseColor("#FFff5555"))
                                false -> when (speed >= 25f) {
                                    true -> tvSpeed.setTextColor(Color.parseColor("#FFffaaaa"))
                                    false -> when (lyMap.visibility == View.VISIBLE) {
                                        true -> tvSpeed.setTextColor(Color.BLACK)
                                        false -> tvSpeed.setTextColor(Color.WHITE)
                                    }
                                }
                            }
                        }
                        if (maxSpeed < speed) {
                            maxSpeed = speed
                            tvMaxSpeed.text = String.format("%.1f Km/h", maxSpeed)
                        }
                    }
                    if(sMap?.isMyLocationEnabled!!) {
                        val camPos = CameraPosition.builder(sMap?.cameraPosition)
                            .bearing(location?.bearing!!.toFloat()).build()
                        sMap?.animateCamera(CameraUpdateFactory.newCameraPosition(camPos))
                    }
//                    android.util.Log.d("xy", "bearing : " + location?.bearing!!.toString())
                }
            }
            if (locationPrev != null) {
                val mTime = (System.currentTimeMillis() - moveTime)
                val moveTime = mTime- 9*60*60*1000
//                val strMoveTime =timeFormat.format(moveTime)
                tvMoveTime.text =  timeFormat.format(moveTime)         //String.format("%00.f:%00.f", hour, min)
//                val len = gcalc.distance(locationPrev?.latitude as Double, locationPrev?.longitude as Double, location?.latitude as Double, location?.longitude as Double)/1000
                val len = locationPrev?.distanceTo(location)!! * 0.001
                android.util.Log.d("xy", "len : $len")
                if(len > 0.003f && len < 0.3f) {
//                    pauseCnt = 0 // 잠시 기능 보류
                    moveLen += len
                    locationPrev = location

                    historyData.aChild("latLng")
                        .sAttr("timeMillis",System.currentTimeMillis().toString()).sAttr("len",String.format("%.2f Km", moveLen))
                        .sAttr("speed", tvSpeed.text as String?).sAttr("cadence", tvCadence.text as String?)
                        .sText(location?.latitude.toString() + " "+location?.longitude.toString()+ " "+String.format("%.2f Km", location?.altitude))
                    tvMoveLen.text = String.format("%.2f Km", moveLen)
                    // 거리/시간 평균속도
                    tvAveSpeed.text = String.format("%.1f Km/h", moveLen/ (mTime*0.001/3600 )) //* 3.6f)
                    android.util.Log.d("xy", "moveLen : "+moveLen + " mTime:"+mTime+"  "+(mTime*0.001/3600))
                    loadHistory?.add(LatLng(location?.latitude as Double, location?.longitude as Double))
//                    if(lyMap.visibility == View.VISIBLE) { // 지도가 활성화 되어 있을때 이동 흔적을 그린다.
                        sMap?.addPolyline(loadHistory)
//                    }
                    historyData.sAttr("maxSpeed",tvMaxSpeed.text.toString()).sAttr("aveSpeed",tvAveSpeed.text.toString())
                        .sAttr("moveLen",tvMoveLen.text.toString()).sAttr("moveTime",tvMoveTime.text.toString()).sAttr("maxCadence", maxCadence.toString())
                    loadHistory?.points!!.removeAt(0)
//                    android.util.Log.d("xy", loadHistory?.points!!.size.toString())
//                } else { // 잠시 기능 보류
//                    pauseCnt++  // 5번 이상 정확하지 움직임이 없으면 잠시 운행기록을 내부적으로 정지 시킨다.
//                    if(pauseCnt > 5) {
//
//                    }
                }
                android.util.Log.d("xy", "moveLen : "+moveLen +"  "+tvMoveLen.text+"  :: Time : "+tvMoveTime.text+" ::  Ave Speed: " +tvAveSpeed.text)
//                android.util.Log.d("xy", historyData.gDoc().xmlToString())
            } else {
                locationPrev = location
                historyData.sAttr("startTimeMillis", ""+System.currentTimeMillis().toString())
                historyData.aChild("latLng").sAttr("time",System.currentTimeMillis().toString())
                    .sText(location?.latitude.toString() + " "+location?.longitude.toString()+ " "+String.format("%.2f Km", location?.altitude))
                loadHistory?.add(LatLng(location?.latitude as Double, location?.longitude as Double))
            }
        }
    }
//    private val sDF = SimpleDateFormat("yyyy.MM.dd.HH.mm.ss")
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    private fun initHud() {
        maxSpeed = 0f
        moveLen  = 0.0
        moveTime = 0L
        tvSpeed.text = String.format("0.0")
        tvMaxSpeed.text = String.format("0.0 Km/h")
        tvAveSpeed.text = String.format("0.0 Km/h")
        tvMoveLen.text = String.format("0.0 Km")
        tvMoveTime.text = String.format("00:00")
        historyData.rAll()
        loadHistory?.points!!.clear()
        sMap?.clear()
//        loadHistory?.
    }
    private fun stopLocationUpdates() {
        mFusedLocationClient?.removeLocationUpdates(locationCallback)
        bStartPlay = false
    }
    private fun startLocationUpdates() {
        if (!checkLocationServicesStatus()) {
//            android.util.Log.d("xy", "startLocationUpdates : call showDialogForLocationServiceSetting")
            showDialogForLocationServiceSetting()
        } else {
            val hasFineLocationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            val hasCoarseLocationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            if ((hasFineLocationPermission != PackageManager.PERMISSION_GRANTED || hasCoarseLocationPermission != PackageManager.PERMISSION_GRANTED))
            {
                android.util.Log.d("xy", "startLocationUpdates : 허가되지 않았습니다.")
                return
            }
            android.util.Log.d("xy", "startLocationUpdates : call mFusedLocationClient.requestLocationUpdates")
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
            mFusedLocationClient?.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper())
            if (checkPermission()) {
                sMap?.isMyLocationEnabled = true
                ibViewFixed.setImageResource(android.R.drawable.ic_menu_mylocation)
            }
            bStartPlay = true
        }
    }
    fun getCurrentAddress(latLng:LatLng):String {
        //지오코더... GPS를 주소로 변환
        val geoCoder = Geocoder(this, Locale.getDefault())
        val addresses:List<Address>
        try {
            addresses = geoCoder.getFromLocation(latLng.latitude,latLng.longitude,1)
            if (addresses.isNotEmpty()) {
                val address = addresses[0]
                return address.getAddressLine(0).toString()
            }
            Toast.makeText(this, getString(R.string.unknown_loc), Toast.LENGTH_LONG).show()
            return getString(R.string.unknown_loc)
        } catch (ioException: IOException) {
            //네트워크 문제
            Toast.makeText(this, "지오코더 서비스 사용불가", Toast.LENGTH_LONG).show()
            return "지오코더 서비스 사용불가"
        } catch (illegalArgumentException:IllegalArgumentException) {
            Toast.makeText(this, "잘못된 GPS 좌표", Toast.LENGTH_LONG).show()
            return "잘못된 GPS 좌표"
        }
    }
    private fun checkLocationServicesStatus():Boolean {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        return (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
    }
    fun setCurrentLocation(location:Location, markerTitle:String, markerSnippet:String) {
        if (currentMarker != null) currentMarker?.remove()
        val currentLatLng = LatLng(location.latitude, location.longitude)
        val markerOptions = MarkerOptions()
        markerOptions.position(currentLatLng)
        markerOptions.title(markerTitle)
        markerOptions.snippet(markerSnippet)
        markerOptions.draggable(true)
        currentMarker = sMap?.addMarker(markerOptions)
        if(sMap?.isMyLocationEnabled!!) {
            val cameraUpdate = CameraUpdateFactory.newLatLng(currentLatLng)
            sMap?.moveCamera(cameraUpdate)
        }
    }
    private fun setDefaultLocation() {
        //디폴트 위치, Seoul
        val defaultLocation = LatLng(37.56, 126.97)
        val markerTitle = "위치정보 가져올 수 없음"
        val markerSnippet = "위치 퍼미션과 GPS 활성 요부 확인하세요"
        if (currentMarker != null) currentMarker?.remove()

        val markerOptions = MarkerOptions()
        markerOptions.position(defaultLocation)
        markerOptions.title(markerTitle)
        markerOptions.snippet(markerSnippet)
        markerOptions.draggable(true)
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))

        currentMarker = sMap?.addMarker(markerOptions)
        val cameraUpdate = CameraUpdateFactory.newCameraPosition(CameraPosition(defaultLocation, 18f,90f,0f))
        ibViewMode.setImageResource(android.R.drawable.ic_menu_mapmode)
        sMap?.moveCamera(cameraUpdate)
    }
    //여기부터는 런타임 로케이션 퍼미션 처리을 위한 메소드들
    private fun checkPermission():Boolean {
        android.util.Log.d("xy", "checkPermission start")
        val hasFineLocationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val hasCoarseLocationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        val hasWriteStoragePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if (hasFineLocationPermission == PackageManager.PERMISSION_GRANTED && hasCoarseLocationPermission == PackageManager.PERMISSION_GRANTED
            && hasWriteStoragePermission == PackageManager.PERMISSION_GRANTED) {
            android.util.Log.d("xy", "checkPermission true")
            return true
        } else {
            ActivityCompat.requestPermissions(this, requiredPermissions, permissionsRequestCode)
        }
        android.util.Log.d("xy", "checkPermission false")
        return false
    }
    //여기부터는 GPS 활성화를 위한 메소드들
    private fun showDialogForLocationServiceSetting() {
        val builder = AlertDialog.Builder(this@MainActivity)
        builder.setTitle("위치 서비스 비활성화")
        builder.setMessage(("앱을 사용하기 위해서는 위치 서비스가 필요합니다.\n" + "위치 설정을 수정하실래요?"))
        builder.setCancelable(true)
        builder.setPositiveButton("설정"){_, _ ->
            val callGPSSettingIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivityForResult(callGPSSettingIntent, gpsEnableRequestCode)
        }
        builder.setNegativeButton("취소"){dialog, _ ->
            dialog.cancel()
        }
        builder.create().show()
    }

    // 지도모드냐 허드 모드냐에 따라 텍스트 색상 변경
    private fun hudTextColor(color : Int) {
        tvAveSpeedTitle.setTextColor(color)
        tvAveSpeed.setTextColor(color)
        tvMaxSpeedTitle.setTextColor(color)
        tvMaxSpeed.setTextColor(color)
        tvSpeed.setTextColor(color)
        tvUnit.setTextColor(color)
        tvMoveLenTitle.setTextColor(color)
        tvMoveLen.setTextColor(color)
        tvMoveTimeTitle.setTextColor(color)
        tvMoveTime.setTextColor(color)
        tvCadenceTitle.setTextColor(color)
        tvCadence.setTextColor(color)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        android.util.Log.d("xy", "onMapReady start")
        if(googleMap == null) {
            android.util.Log.d("xy", "onMapReady stop : googleMap null")
            return
        }
        sMap = googleMap
        //런타임 퍼미션 요청 대화상자나 GPS 활성 요청 대화상자 보이기전에
        //지도의 초기위치를 서울로 이동
//        android.util.Log.d("xy", "onMapReady middle")
        setDefaultLocation()
        sMap?.isMyLocationEnabled = true

        //런타임 퍼미션 처리
        // 1. 위치 퍼미션을 가지고 있는지 체크합니다.
        val hasFineLocationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val hasCoarseLocationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        val hasWriteStoragePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)

        if ((hasFineLocationPermission != PackageManager.PERMISSION_GRANTED
                    || hasCoarseLocationPermission != PackageManager.PERMISSION_GRANTED || hasWriteStoragePermission != PackageManager.PERMISSION_GRANTED)) {

            //2. 퍼미션 요청을 허용한 적이 없다면 퍼미션 요청이 필요합니다. 2가지 경우(3-1, 4-1)가 있습니다.
            // 3-1. 사용자가 퍼미션 거부를 한 적이 있는 경우에는
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, requiredPermissions[0])) {
                // 3-2. 요청을 진행하기 전에 사용자가에게 퍼미션이 필요한 이유를 설명해줄 필요가 있습니다.
                val snack = Snackbar.make(mLayout,"이 앱을 실행하려면 위치 접근 권한이 필요합니다.", Snackbar.LENGTH_LONG)
                    snack.setAction("확인") {
                        // 3-3. 사용자에게 퍼미션 요청을 합니다. 요청 결과는 onRequestPermissionResult 에서 수신됩니다.
                        ActivityCompat.requestPermissions(this@MainActivity, requiredPermissions, permissionsRequestCode)
                    }
                snack.show()
            } else {
                // 4-1. 사용자가 퍼미션 거부를 한 적이 없는 경우에는 퍼미션 요청을 바로 합니다.
                // 요청 결과는 onRequestPermissionResult에서 수신됩니다.
                ActivityCompat.requestPermissions(this, requiredPermissions, permissionsRequestCode)
            }
        }
        android.util.Log.d("xy", "onMapReady end")
    }
    /*
     * ActivityCompat.requestPermissions 를 사용한 퍼미션 요청의 결과를 리턴받는 메소드입니다.
     */
    override fun onRequestPermissionsResult(permsRequestCode:Int, permissions:Array<String>, grandResults:IntArray) {
        if (permsRequestCode == permissionsRequestCode && grandResults.size == requiredPermissions.size)
        {
            // 요청 코드가 PERMISSIONS_REQUEST_CODE 이고, 요청한 퍼미션 개수만큼 수신되었다면
            var checkResult = true
            // 모든 퍼미션을 허용했는지 체크합니다.
            for (result in grandResults)
            {
                if (result != PackageManager.PERMISSION_GRANTED)
                {
                    checkResult = false
                    break
                }
            }
            if (!checkResult) {
                // 거부한 퍼미션이 있다면 앱을 사용할 수 없는 이유를 설명해주고 앱을 종료합니다.2 가지 경우가 있습니다.
                if ((ActivityCompat.shouldShowRequestPermissionRationale(this, requiredPermissions[0]) || ActivityCompat.shouldShowRequestPermissionRationale(this, requiredPermissions[1])))
                {
                    // 사용자가 거부만 선택한 경우에는 앱을 다시 실행하여 허용을 선택하면 앱을 사용할 수 있습니다.
                    val snack = Snackbar.make(mLayout,"퍼미션이 거부되었습니다. 앱을 다시 실행하여 퍼미션을 허용해주세요. ", Snackbar.LENGTH_LONG)
                    snack.setAction("확인") {
                        // 3-3. 사용자에게 퍼미션 요청을 합니다. 요청 결과는 onRequestPermissionResult에서 수신됩니다.
                        finish()
                    }
                    snack.show()
                } else {
                    // "다시 묻지 않음"을 사용자가 체크하고 거부를 선택한 경우에는 설정(앱 정보)에서 퍼미션을 허용해야 앱을 사용할 수 있습니다.
                    val snack = Snackbar.make(mLayout,"퍼미션이 거부되었습니다. 설정(앱 정보)에서 퍼미션을 허용해야 합니다. ", Snackbar.LENGTH_LONG)
                    snack.setAction("확인") {
                        // 3-3. 사용자에게 퍼미션 요청을 합니다. 요청 결과는 onRequestPermissionResult에서 수신됩니다.
                        finish()
                    }
                    snack.show()
                }
            }
        }
    }
    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
//        menuInflater.inflate(R.menu.main, menu)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
//        return true
        when (item.itemId) {
            R.id.action_settings -> return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {
            R.id.ridingHistory -> {
                val intent = Intent(this, HistoryActivity::class.java)
                startActivity(intent)
            }
            R.id.device_cadence -> {
                val intent = Intent(this, PopupActivity::class.java)
                intent.putExtra("Layout_Type", R.layout.bluetooth_select)
                startActivity(intent)
            }
            R.id.bikeInformation -> {
                val intent = Intent(this, MyBikeActivity::class.java)
                startActivity(intent)
            }
        }

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    private var cameraId :String?=null
    private fun getCameraId(): String {
        if(cameraId != null) return cameraId as String
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            Toast.makeText(this, "Lollipop supports more versions.", Toast.LENGTH_LONG).show()
            return "0"
        }
        try {
            if (cameraMng != null) {
                for (i in 0 until cameraMng?.cameraIdList!!.size) {
                    val c = cameraMng?.getCameraCharacteristics(cameraMng?.cameraIdList!![i])
                    val flashAvailable = c!!.get(CameraCharacteristics.FLASH_INFO_AVAILABLE)
                    val lensFacing = c.get(CameraCharacteristics.LENS_FACING)
                    if(flashAvailable == true && lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
                        cameraId = cameraMng?.cameraIdList!![i]
                        return cameraId as String
                    }
                }
            }
            return "0"
        } catch (ee : java.lang.Exception) {
            return "-1"
        }
    }
    // 광고 처리 함수 시작 ===========================================================================================================================================
    private fun setAdType(type:Int) {
        when (type) {
            9 ->  myAdView(this@MainActivity)
            1 -> {
                android.util.Log.d("xy", "type : $type")
                this.runOnUiThread {
                    adMobView?.loadAd(adRequest)
                }
            }
            else -> {
                myAdView(this@MainActivity)
            }
        }
    }

    private fun myAdView(a: Activity) {
        a.runOnUiThread {
            val myAdView = a.findViewById(R.id.myadview) as WebView
//            myAdView.settings.javaScriptEnabled = true
            myAdView.clearCache(true)
            myAdView.clearHistory()
            myAdView.clearFormData()
            adMobView?.visibility = View.GONE
            myAdView.visibility = WebView.VISIBLE
            myAdView.loadUrl("http://app.moyeo.org/bikelife-ad.html")
        }
    }
    // 광고 처리 함수 마침 ===========================================================================================================================================
//    override fun onNewIntent(intent: Intent) {
//        super.onNewIntent(intent)
//        handleIntent(intent)
//    }
//
//    private fun handleIntent(intent: Intent) {
//        val appLinkAction = intent.action
//        val appLinkData: Uri? = intent.data
//        if (Intent.ACTION_VIEW == appLinkAction) {
//            appLinkData?.lastPathSegment?.also { recipeId ->
//                Uri.parse("content://com.recipe_app/recipe/")
//                    .buildUpon()
//                    .appendPath(recipeId)
//                    .build().also { appData ->
//                        showRecipe(appData)
//                    }
//            }
//        }
//    }
}
