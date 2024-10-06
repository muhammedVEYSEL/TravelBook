package com.veys.kotlinmaps.view

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.room.Room

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import com.veys.kotlinmaps.R
import com.veys.kotlinmaps.databinding.ActivityMapsBinding
import com.veys.kotlinmaps.model.Place
import com.veys.kotlinmaps.roomdb.PlaceDao
import com.veys.kotlinmaps.roomdb.PlaceDatabase
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers

class MapsActivity : AppCompatActivity(), OnMapReadyCallback,GoogleMap.OnMapLongClickListener {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var locationManager: LocationManager
    private lateinit var locationListener: LocationListener
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    private lateinit var sharedPreferences: SharedPreferences
    private var truckBoolean: Boolean? = null
    private var selectedLatitud: Double? = null
    private var selectedLongitud: Double? = null
    private lateinit var db : PlaceDatabase
    private lateinit var placeDao: PlaceDao
    val compositeDisposable = CompositeDisposable()// uygulama içinde işlem yaptıkça hafızada yer tutmaya başlar
    // bunun önüne geçmek için bu işlemler compositedesposabel içinde yapılır ve kullanıldıktan sonra atılır
    // en altta olan on destroy fonksiyonu...

    var selectedPlace: Place? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        registerLauncher()
        sharedPreferences = getSharedPreferences("com.veys.kotlinmaps", MODE_PRIVATE)
        truckBoolean = false
        selectedLatitud = 0.0
        selectedLongitud = 0.0

        db = Room.databaseBuilder(applicationContext,PlaceDatabase::class.java,"Places")
            //.allowMainThreadQueries() -> bu yöntem ile Main thread içinde veri tabanı kayıt işlemi yapılabilir ama doğru yöntem bu değil
            .build()
        placeDao = db.placeDao()

        binding.savebutton.isEnabled = false

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.setOnMapLongClickListener(this)// listener direkt activity tarafından verildiği için this yeterli

        val intent = intent
        val info = intent.getStringExtra("info")
        if (info == "new"){
            binding.savebutton.visibility = View.VISIBLE
            binding.deletebutton.visibility = View.GONE // gone tamamen yerini boşaltır

            // kullanıcın konumunu almak için -->
            // casting işlemi
            locationManager = this.getSystemService(LOCATION_SERVICE) as LocationManager

            locationListener = object :LocationListener{
                override fun onLocationChanged(location: Location) {
                    //  UYGULAMA AÇILDIĞI ZAMAN BU FONKSİYON KONUM GÜNCELLEMESİ OLDUKÇA ÇALIŞIR VE KAMERAYI O BÖLGEYE GÖTÜRÜR BUNU
                    // İSTEMEYİZ BUNUN İÇİN truckBoolen değişkenini kullanabiliriz
                    truckBoolean = sharedPreferences.getBoolean("truckBoolean",false)
                    if (truckBoolean == false){
                        val userLocation = LatLng(location.latitude,location.longitude)
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation,15f))
                        sharedPreferences.edit().putBoolean("truckBoolean",true).apply()
                    }// uygulama ilk açıldığında son konumu görürüz daha sonra haritada hareket edebilirz


                }
            } //locationmanager ve listener tanımlandı

            if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.ACCESS_FINE_LOCATION)){
                    Snackbar.make(binding.root,"Give permission to location",Snackbar.LENGTH_INDEFINITE).setAction("Give Permission!"){
                        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }.show()
                }else{
                    permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)

                }
            }else{
                //konum sağlayıcı,ne kdar süre ve mesafede bir, gelen veriler listener a atanır ve ondan bize gelir
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0f,locationListener)
                val lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                if (lastLocation != null){
                    val lastUserLocation = LatLng(lastLocation.latitude,lastLocation.longitude)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastUserLocation,15f))

                }
                mMap.isMyLocationEnabled = true // harita üsütnde mavi imleç
            }
        }else{
            binding.savebutton.visibility = View.GONE
            binding.deletebutton.visibility = View.VISIBLE
            mMap.clear()

            selectedPlace = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
                intent.getSerializableExtra("selectedPlace",Place::class.java)
            }else{
                intent.getSerializableExtra("selectedPlace") as? Place
            }
            selectedPlace?.let { it->
                val latLng =LatLng(it.latitude,it.longitude)
                mMap.addMarker(MarkerOptions().position(latLng).title(it.name))
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng,15f))

                binding.editTextLocation.setText(it.name)


            }

        }
    }

    private fun registerLauncher(){
        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()){result -> // bu bize boolen döndürür
            if (result){
                if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED){
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0f,locationListener)
                    val lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    if (lastLocation != null){
                        val lastUserLocation = LatLng(lastLocation.latitude,lastLocation.longitude)
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastUserLocation,15f))
                    }
                    mMap.isMyLocationEnabled = true
                }
            }else{
                Toast.makeText(this@MapsActivity,"permission needed!",Toast.LENGTH_LONG).show()
            }

        }
    }

    override fun onMapLongClick(p0: LatLng) {
        mMap.clear()// ikinci bir marker eklendiği zaman ilki kalkar
        mMap.addMarker(MarkerOptions().position(p0))
        selectedLatitud = p0.latitude
        selectedLongitud = p0.longitude
        binding.savebutton.isEnabled = true
    }
    fun save(view:View){
        if(selectedLatitud != null && selectedLongitud != null){
            val place = Place(binding.editTextLocation.text.toString(),selectedLatitud!!,selectedLongitud!!)
            compositeDisposable.add(
                placeDao.insert(place)
                    .subscribeOn(Schedulers.io()) //işlemin nerede yapılacağı belirlenir -> io thread
                    .observeOn(AndroidSchedulers.mainThread()) // nerede gözlem yapılacağı belirlenir
                    .subscribe(this::hadleResponse)// composit bittikten sonra ne yapılacağı belirlenir
            )
        }
    }
    fun delete(view:View){
        if (selectedPlace != null){
            compositeDisposable.add(
                placeDao.delete(selectedPlace!!)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::hadleResponse)
            )
        }
    }
    private fun hadleResponse(){
        val intent= Intent(this@MapsActivity,MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP) // açık olan aktiviteler kapanır
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.clear()
    }

}