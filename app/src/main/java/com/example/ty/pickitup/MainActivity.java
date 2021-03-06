package com.example.ty.pickitup;


import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewStub;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.ty.pickitup.Animate.LatLngInterpolator;
import com.example.ty.pickitup.Animate.MarkerAnimate;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.Gson;
import com.microsoft.projectoxford.vision.VisionServiceClient;
import com.microsoft.projectoxford.vision.VisionServiceRestClient;
import com.microsoft.projectoxford.vision.contract.AnalysisResult;
import com.microsoft.projectoxford.vision.rest.VisionServiceException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

import io.socket.client.Ack;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

/**
 * An activity that displays a map showing the place at the device's current location.
 */
public class MainActivity extends AppCompatActivity
        implements OnMapReadyCallback, LocationListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        GoogleMap.OnMarkerClickListener {

    ArrayList<LatLng> liveData = new ArrayList<>();
    ArrayList<LatLng> deadData = new ArrayList<>();
    private final int DURATION = 2000;
    private static final String TAG = MainActivity.class.getSimpleName();
    private GoogleMap mMap;
    private CameraPosition mCameraPosition;
    private SharedPreferences preferences;
    private double mLatitude, mLongitude;
    private ViewStub stub;
    private ViewStub stub2;
    private View stub_layout;
    private View stub_achievement;

    RelativeLayout progressLayout;

    private Marker marker;
    // The entry point to Google Play services, used by the Places API and Fused Location Provider.
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;


    // A default location (Sydney, Australia) and default zoom to use when location permission is
    // not granted.
    private final LatLng mDefaultLocation = new LatLng(32.8523341, -96.2106085);
    private static final int DEFAULT_ZOOM = 15;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private boolean mLocationPermissionGranted;

    // The geographical location where the device is currently located. That is, the last-known
    // location retrieved by the Fused Location Provider.
    private Location mLastKnownLocation;

    // Keys for storing activity state.
    private static final String KEY_CAMERA_POSITION = "camera_position";
    private static final String KEY_LOCATION = "location";
    private ImageView cameraView;

    //private ClarifaiClient client;
    // Camera Stuff
    private static final int TAKE_PICTURE = 1;
    private Uri imageUri;

    // Shocket
    Socket socket;

    String[] facts = {"9 billion tons of litter ends up in the ocean every year.", "$11.5 billion is spent every year to clean up litter", "50% of littered items are cigarette butts", "75% of people admitted that they littered in the past 5 years", "Plastic bags and other plastic garbage thrown into the ocean kill as many as 1,000,000 sea creatures every year.", "More than 1 million species have already faced extinction due to global warming.", "Rainforests have taken thousands of years to form but every second a portion the size of a football field is destroyed", "Thanks to the tremendous amount of reckless waste disposal, we have a garbage island floating in our ocean, mostly comprised of plastics – the size of India, Europe and Mexico combined!", "Plastic bags and other plastic garbage thrown into the ocean kill as many as 1,000,000 sea creatures every year.", "99 of the things we buy are trashed in the next six months",
            "Tobacco products make up more than one third of existing litter",
            "Use cloth bags when shopping to reduce plastic waste",
            "If every person in Texas picked up just two pieces of trash every month, our highways would be completely litter-free in just one year",
            "Single-use fast food, snack, tobacco, beverage, and other packaging make up nearly 46% of litter 4 inches and larger in the U.S.",
            "Plastic litter can take up to 600 years to degrade",
            "Aluminum cans can take 200 - 500 years to degrade",
            "Glass can take over a million years to degrade"};
    TextView fact;

    //// Vision
    private Uri mImageUri;
    private Bitmap mBitmap;
    private VisionServiceClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        client = new VisionServiceRestClient("8755aa9f415c4b2cb0b5f2f3fcd53fa7");


        final Animation alphaIn = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.alpha_in);
        alphaIn.setDuration(1200);
        final Animation alphaOut = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.alpha_out);

        // Retrieve location and camera position from saved instance state.
        if (savedInstanceState != null) {
            mLastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION);
            mCameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION);
        }
        //client = new ClarifaiBuilder("k0KKlwxw1VdIjoelm-rakt1Mf5UF7u9Lyjd4TC7t", "a4e2Re4nZfbJJWYSj6BWBy4FVo4jDraG2HUFt_DJ")
        //        .client(new OkHttpClient()) // OPTIONAL. Allows customization of OkHttp by the user
        //        .buildSync(); // or use .build() to get a Future<ClarifaiClient>

//        client.addConcepts()
//                .plus(
//                        Concept.forID("")
//                ).executeSync();
        preferences = getSharedPreferences("achievements", MODE_PRIVATE);

        // Retrieve the content view that renders the map.
        setContentView(R.layout.activity_main);
        progressLayout = (RelativeLayout) findViewById(R.id.progress_layout);

        // Build the Play services client for use by the Fused Location Provider and the Places API.
        // Use the addApi() method to request the Google Places API and the Fused Location Provider.
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */,
                        this /* OnConnectionFailedListener */)
                .addConnectionCallbacks(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
        ImageButton achievementsButton = (ImageButton) findViewById(R.id.achievements_button);
        achievementsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MainActivity.this, AchievementsActivity.class);
                startActivity(i);
            }
        });
        ImageButton cameraButton = (ImageButton) findViewById(R.id.camera_button);
        cameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePhoto(v);

            }
        });

        Button pickupButton = (Button) findViewById(R.id.pickup_button);
        Button leaveButton = (Button) findViewById(R.id.leave_button);

        pickupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int count = preferences.getInt("litter", 0);
                preferences.edit().putInt("litter", count + 1).apply();
                findViewById(R.id.camera_container).setVisibility(View.GONE);
                socket.emit("pull_pin", mLatitude, mLongitude);
                int n = new Random().nextInt(facts.length);
                fact.setText(facts[n]);
                fact.startAnimation(alphaIn);


            }
        });
        final Animation.AnimationListener alphaInListener = new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                fact.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        fact.startAnimation(alphaOut);

                    }
                }, 3000);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        };

        final Animation.AnimationListener alphaOutListener = new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                fact.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        };

        alphaIn.setAnimationListener(alphaInListener);
        alphaOut.setAnimationListener(alphaOutListener);

        leaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Put a marker
                MarkerOptions trashMarker = new MarkerOptions();
                trashMarker.anchor(0.5f, 0.5f);

                trashMarker.icon(BitmapDescriptorFactory.fromResource(R.drawable.trash));
                trashMarker.position(new LatLng(mLatitude, mLongitude));
                findViewById(R.id.camera_container).setVisibility(View.GONE);
                marker = mMap.addMarker(trashMarker);
                marker.setTag(0);
                markers.add(marker);
                // Sending an object
                socket.emit("push_pin", mLatitude, mLongitude);
                markedSet.add(getLatLngString(marker.getPosition()));
                int n = new Random().nextInt(facts.length);
                fact.setText(facts[n]);
                fact.startAnimation(alphaIn);


            }
        });
        //test(mDefaultLocation);
        // Stub for when you pick up garbage
        stub = (ViewStub) findViewById(R.id.viewStub1);
        stub_layout = stub.inflate();

        stub2 = (ViewStub) findViewById(R.id.viewStub2);
        stub_achievement = stub2.inflate();

        // Set fact textview
        fact = (TextView) findViewById(R.id.facts);
    }


    private String process(Bitmap bitmap) throws VisionServiceException, IOException {
        Gson gson = new Gson();
        client = new VisionServiceRestClient("8755aa9f415c4b2cb0b5f2f3fcd53fa7");


        // Put the image into an input stream for detection.
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, output);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(output.toByteArray());
        System.out.println("fuck shit up");
        AnalysisResult v = this.client.describe(inputStream, 1);
        String result = gson.toJson(v);
        Log.d("result", result);

        return result;
    }

    public void takePhoto(View view) {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photo = new File(Environment.getExternalStorageDirectory(), "Pic.jpg");

//        final List<ClarifaiOutput<Concept>> predictionResults =
//                client.getDefaultModels().generalModel() // You can also do Clarifai.getModelByID("id") to get custom models
//                        .predict()
//                        .withInputs(
//                                ClarifaiInput.forImage(ClarifaiImage.of(photo))
//                        )
//                        .executeSync().get();
//
//        predictionResults.get(0).data().get(0).name();

        intent.putExtra(MediaStore.EXTRA_OUTPUT,
                Uri.fromFile(photo));
        imageUri = Uri.fromFile(photo);
        startActivityForResult(intent, TAKE_PICTURE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case TAKE_PICTURE:
                findViewById(R.id.camera_container).setVisibility(View.VISIBLE);

                if (resultCode == Activity.RESULT_OK) {
                    Uri selectedImage = imageUri;
                    getContentResolver().notifyChange(selectedImage, null);
                    cameraView = (ImageView) findViewById(R.id.trash_pic);
                    Button pickupButton = (Button) findViewById(R.id.pickup_button);
                    Button leaveButton = (Button) findViewById(R.id.leave_button);
                    ContentResolver cr = getContentResolver();
                    Bitmap bitmap;
                    try {
                        progressLayout.setVisibility(View.VISIBLE);
                        bitmap = android.provider.MediaStore.Images.Media
                                .getBitmap(cr, selectedImage);
                        new doRequest(bitmap).execute();
                        cameraView.setImageBitmap(bitmap);
                        cameraView.setVisibility(View.VISIBLE);
                        pickupButton.setVisibility(View.VISIBLE);
                        leaveButton.setVisibility(View.VISIBLE);

                    } catch (Exception e) {
                        Toast.makeText(this, "Failed to load", Toast.LENGTH_SHORT)
                                .show();
                        Log.e("Camera", e.toString());
                    }
                }
        }
    }

    /**
     * Saves the state of the map when the activity is paused.
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (mMap != null) {
            outState.putParcelable(KEY_CAMERA_POSITION, mMap.getCameraPosition());
            outState.putParcelable(KEY_LOCATION, mLastKnownLocation);
            super.onSaveInstanceState(outState);
        }
    }

    /**
     * Builds the map when the Google Play services client is successfully connected.
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        // Build the map.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        startLocationUpdates();
    }

    // Trigger new location updates at interval
    @TargetApi(23)
    protected void startLocationUpdates() {
        // Create the location request
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(DURATION)
                .setFastestInterval(DURATION);
        // Request location updates
//        if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(MainActivity.this,
//                                new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
//
//            return;
//        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,
                mLocationRequest, this);
    }


    /**
     * Handles failure to connect to the Google Play services client.
     */
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult result) {
        // Refer to the reference doc for ConnectionResult to see what error codes might
        // be returned in onConnectionFailed.
        Log.d(TAG, "Play services connection failed: ConnectionResult.getErrorCode() = "
                + result.getErrorCode());
    }

    /**
     * Handles suspension of the connection to the Google Play services client.
     */
    @Override
    public void onConnectionSuspended(int cause) {
        Log.d(TAG, "Play services connection suspended");
    }

    /**
     * Sets up the options menu.
     *
     * @param menu The options menu.
     * @return Boolean.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.current_place_menu, menu);
        return true;
    }

    /**
     * Handles a click on the menu option to get a place.
     *
     * @param item The menu item to handle.
     * @return Boolean.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.option_get_place) {
            //showCurrentPlace();
        }
        return true;
    }

    /**
     * Manipulates the map when it's available.
     * This callback is triggered when the map is ready to be used.
     */
    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;
        boolean success = mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style));
        // Use a custom info window adapter to handle multiple lines of text in the
        // info window contents.
        mMap.setOnMarkerClickListener(this);
        mMap.getUiSettings().setCompassEnabled(false);

        mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {

            @Override
            // Return null here, so that getInfoContents() is called next.
            public View getInfoWindow(Marker arg0) {
                return null;
            }

            @Override
            public View getInfoContents(Marker marker) {
                // Inflate the layouts for the info window, title and snippet.
                View infoWindow = getLayoutInflater().inflate(R.layout.custom_info_contents,
                        (FrameLayout) findViewById(R.id.map), false);

                TextView title = ((TextView) infoWindow.findViewById(R.id.title));
                title.setText(marker.getTitle());

                TextView snippet = ((TextView) infoWindow.findViewById(R.id.snippet));
                snippet.setText(marker.getSnippet());

                return infoWindow;
            }
        });


        // Get the current location of the device and set the position of the map.
        getDeviceLocation();

        // Establish connection with server ...
        // Done on map ready because communication depends on map interaction
        try {
            socket = IO.socket("http://52.43.59.155:3000/");
            socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    // On connection
                    System.out.println("lllll");
                }
            });
            socket.connect();

            final Ack ack = new Ack() {
                @Override
                public void call(Object... args) {
                    JSONArray arr_live = (JSONArray) args[0];
                    JSONArray arr_dead = (JSONArray) args[1];
                    System.out.println("In call: arr_live length " + arr_live.length());
                    System.out.println("In call: arr_Dead length " + arr_dead.length());
                    liveData = new ArrayList<>();
                    for (int i = 0; i < arr_live.length(); ++i) {
                        try {
                            JSONObject obj = arr_live.getJSONObject(i);
                            String latitude = obj.getString("latitude");
                            String longitude = obj.getString("longitude");
                            LatLng coords = new LatLng(Double.parseDouble(latitude), Double.parseDouble(longitude));
                            liveData.add(coords);
                            //System.out.println(latitude + ", " + longitude);

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    deadData = new ArrayList<>();
                    for (int i = 0; i < arr_dead.length(); ++i) {
                        try {
                            JSONObject obj = arr_dead.getJSONObject(i);
                            String latitude = obj.getString("latitude");
                            String longitude = obj.getString("longitude");
                            LatLng coords = new LatLng(Double.parseDouble(latitude), Double.parseDouble(longitude));
                            deadData.add(coords);
                            //System.out.println(latitude + ", " + longitude);

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }

            };
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    socket.emit("poll", ack);
                    System.out.println("polling");
                    handler.postDelayed(this, 30000);
                    updateEverything(liveData, deadData);
                }
            }, 0);

            OVERLAY_IMAGES[0] = BitmapDescriptorFactory.fromResource(R.drawable.green);
            OVERLAY_IMAGES[1] = BitmapDescriptorFactory.fromResource(R.drawable.yellow);
            OVERLAY_IMAGES[2] = BitmapDescriptorFactory.fromResource(R.drawable.red);


        } catch (URISyntaxException e) {

            e.printStackTrace();
        }


    }

    public void foundItem(boolean bottle) {
        stub_achievement.setVisibility(View.VISIBLE);
        ImageView image = (ImageView) stub_achievement.findViewById(R.id.achievement_icon);
        TextView text = (TextView) stub_achievement.findViewById(R.id.text);
        final Animation anim = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.translate_in);
        final Animation animOut = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.alpha_out);

        if(bottle) {
            text.setText("You've found a bottle!");
            image.setImageResource(R.mipmap.ic_bottle_blue_v2);
            int count = preferences.getInt("bottle", 0);
            preferences.edit().putInt("bottle", count + 1).apply();
        } else {
            text.setText("You've found a bag!");
            image.setImageResource(R.mipmap.ic_bag_blue);
            int count = preferences.getInt("bag", 0);
            preferences.edit().putInt("bag", count + 1).apply();
        }

        final Animation.AnimationListener alphaInListener = new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                stub_achievement.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        stub_achievement.startAnimation(animOut);

                    }
                }, 3000);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        };

        final Animation.AnimationListener peaceOut = new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                stub_achievement.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        };
        anim.setAnimationListener(alphaInListener);
        animOut.setAnimationListener(peaceOut);
        image.startAnimation(anim);
    }


    /**
     * Gets the current location of the device, and positions the map's camera.
     */
    private void getDeviceLocation() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        if (mLocationPermissionGranted) {
            mLastKnownLocation = LocationServices.FusedLocationApi
                    .getLastLocation(mGoogleApiClient);
        }

        if (mLastKnownLocation != null) {
            mLatitude = mLastKnownLocation.getLatitude();
            mLongitude = mLastKnownLocation.getLongitude();
        }

        // Set the map's camera position to the current location of the device.
        if (mCameraPosition != null) {
            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(mCameraPosition));
        } else if (mLastKnownLocation != null) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(mLastKnownLocation.getLatitude(),
                            mLastKnownLocation.getLongitude()), DEFAULT_ZOOM));
        } else {
            Log.d(TAG, "Current location is null. Using defaults.");
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mDefaultLocation, DEFAULT_ZOOM));
            mMap.getUiSettings().setMyLocationButtonEnabled(false);
        }
    }

    /**
     * Handles the result of the request for location permissions.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        mLocationPermissionGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true;
                }
            }
        }
        updateLocationUI();
    }

    /**
     * Updates the map's UI settings based on whether the user has granted location permission.
     */
    private void updateLocationUI() {
        if (mMap == null) {
            return;
        }

        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }

        if (mLocationPermissionGranted) {
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(true);
        } else {
            mMap.setMyLocationEnabled(false);
            mMap.getUiSettings().setMyLocationButtonEnabled(false);
            mLastKnownLocation = null;
        }
    }

    Marker mMarker;
    final LatLngInterpolator.Linear linear = new LatLngInterpolator.Linear();

    public void onLocationChanged(Location location) {

        //Bitmap bitmap = BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.character);
        //BitmapDescriptor icon = BitmapDescriptorFactory.fromBitmap(bitmap);
        mLatitude = location.getLatitude();
        mLongitude = location.getLongitude();

        if (mMarker == null) {
            MarkerOptions mp = new MarkerOptions();
            mp.position(new LatLng(location.getLatitude(), location.getLongitude()));
            mp.icon(BitmapDescriptorFactory.fromResource(R.drawable.green_character));
            mp.anchor(0.5f, 0.5f);
            mp.title("My position");


            mMarker = mMap.addMarker(mp);
            mMarker.setTag(-1);
            markers.add(mMarker);
        } else {

            MarkerAnimate.animateMarkerToGB(mMarker, new LatLng(location.getLatitude(), location.getLongitude()), linear, DURATION);
        }

        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(location.getLatitude(), location.getLongitude()), mMap.getCameraPosition().zoom));

    }


    @Override
    public boolean onMarkerClick(final Marker marker) {
        if (!marker.getTag().equals(-1)) {
            final LatLng markerLoc = marker.getPosition();
            Location locMarker = new Location("");
            locMarker.setLatitude(markerLoc.latitude);
            locMarker.setLongitude(markerLoc.longitude);

            Location locUser = new Location("");
            locUser.setLatitude(mLatitude);
            locUser.setLongitude(mLongitude);

            if (locMarker.distanceTo(locUser) < 50.0) {
                System.out.println("fuck you")
                ;
                stub_layout.setVisibility(View.VISIBLE);
                ImageView image = (ImageView) stub_layout.findViewById(R.id.achievement_icon);
                final Animation anim = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.translate_in);
                final Animation animOut = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.alpha_out);


                image.startAnimation(anim);

                image.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        stub_layout.startAnimation(animOut);
                        animOut.setAnimationListener(new Animation.AnimationListener() {
                            @Override
                            public void onAnimationStart(Animation animation) {

                            }

                            @Override
                            public void onAnimationEnd(Animation animation) {
                                stub_layout.setVisibility(View.INVISIBLE);

                                // Tell server that garbage has been picked up
                                socket.emit("pull_pin", markerLoc.latitude, markerLoc.longitude);
                                marker.remove();
                            }

                            @Override
                            public void onAnimationRepeat(Animation animation) {

                            }
                        });
                    }
                });


            } else {
                Toast.makeText(getApplicationContext(), "You're too far away from this litter", Toast.LENGTH_SHORT).show();
            }
        }
        return true;
    }


    /**
     * Kishan variables
     */


    /////// HashMap<Integer, ....>
    // When we place marker : send to alive list
    // WHen we throw away litter, send to dead data list
    // When we remove marker : <now for overlay> <server identifies it as dead data>
    // SERVER adds to list of dead data and removes from alive data
    // On poll : sever sends the dead + alive with JSON identifier
    // Iterate through list check with dead data hashmap
    // If dead and not in map --> update ground overlay with all fresh dead data


    private HashMap<String, GroundOverlay> overlayMap = new HashMap<>();
    private HashMap<String, Integer> litterMap = new HashMap<>();
    private HashSet<String> markedSet;

    private static final int[] LEVELS = {1, 25, 50};
    private static final double OVERLAY_SIZE = .03;


    private static BitmapDescriptor[] OVERLAY_IMAGES = new BitmapDescriptor[3];


    public void updateMaps(ArrayList<LatLng> deadData) {
        litterMap.clear();
        for (int i = 0; i < deadData.size(); i++) {
            // Represents a quadrant that could be covered by a ground overlay
            LatLngBounds bound = getStringToHash(deadData.get(i));
            String hashBound = getLatLngString(bound.southwest) +
                    getLatLngString(bound.northeast);
            // Represents the exact coordinate of marked trash
            String hashMark = getLatLngString(deadData.get(i));
            // We picked up litter at the same location as marked, so remove
            if (markedSet.contains(hashMark)) {
                markedSet.remove(hashMark);
            }
            // If we have already picked up litter in this quadrant, update
            // overlay heatmap and the litter count for this quadrant
            if (litterMap.containsKey(hashBound)) {
                int newLitterCount = litterMap.get(hashBound) + 1;
                litterMap.put(hashBound, newLitterCount);
                for (int j = 0; j < LEVELS.length; j++) {
                    if (newLitterCount == LEVELS[j]) {
                        GroundOverlay g = overlayMap.get(hashBound);
                        g.setImage(OVERLAY_IMAGES[j]);
                        g.setTransparency(.75f);
                    }
                }
            }
            // We have not picked up trash in this quadrant before, so create
            // an overlay for it and add the quadrant to the litterMap and
            // to the overlayMap
            else {
                litterMap.put(hashBound, 1);
                if (!overlayMap.containsKey(hashBound)) {
                    GroundOverlayOptions g = new GroundOverlayOptions()
                            .positionFromBounds(bound)
                            .visible(true)
                            .image(OVERLAY_IMAGES[0])
                            .transparency(.75f);
                    overlayMap.put(hashBound, mMap.addGroundOverlay(g));
                }
            }
        }
    }

    public LatLngBounds getStringToHash(LatLng current) {
        // Calculate LatLng Bound
        // Please check my logic/math
        // I def did not handle longitude/latitude overflow/underflow
        double latitude = current.latitude;
        double longitude = current.longitude;
        double southLatitude = roundToTwoPlaces(latitude - Math.abs(latitude % OVERLAY_SIZE));
        double westLongitude = roundToTwoPlaces(longitude - Math.abs(longitude % OVERLAY_SIZE));
        double northLatitude = roundToTwoPlaces(southLatitude + OVERLAY_SIZE);
        double eastLongitude = roundToTwoPlaces(westLongitude + OVERLAY_SIZE);

        LatLng sw = new LatLng(southLatitude, westLongitude);
        LatLng ne = new LatLng(northLatitude, eastLongitude);
        LatLngBounds bound = new LatLngBounds(sw, ne);

        return bound;
    }

    public double roundToTwoPlaces(double number) {
        number = Math.round(number * 100);
        return number / 100;
    }

    public String getLatLngString(LatLng obj) {
        return obj.latitude + " " + obj.longitude;
    }

    ArrayList<Marker> markers = new ArrayList<>();

    public void updateEverything(ArrayList<LatLng> liveData, ArrayList<LatLng> deadData) {
        markedSet = new HashSet<>();
        for (int i = 0; i < liveData.size(); i++)
            markedSet.add(getLatLngString(liveData.get(i)));
        // Creates the heat map and removes dead data hiding in the markedSet
        updateMaps(deadData);
        // Place All Markers from markedSet (liveData) onto the mMap
        for (Marker mike_ermantrout : markers) {
            mike_ermantrout.remove();
        }

        for (String pair : markedSet) {
            String[] arr = pair.split(" ");
            double latitude = Double.parseDouble(arr[0]);
            double longitude = Double.parseDouble(arr[1]);
            MarkerOptions trashMarker = new MarkerOptions();
            trashMarker.anchor(0.5f, 0.5f);
            trashMarker.icon(BitmapDescriptorFactory.fromResource(R.drawable.trash));
            trashMarker.position(new LatLng(latitude, longitude));
            Marker markyMark = mMap.addMarker(trashMarker);
            markyMark.setTag(0);
            markers.add(markyMark);
        }
        // INSERT CALL HERE TO MAKE THAT HAPPEN

        MarkerOptions mp = new MarkerOptions();
        mp.position(new LatLng(mLatitude, mLongitude));
        mp.icon(BitmapDescriptorFactory.fromResource(R.drawable.green_character));
        mp.anchor(0.5f, 0.5f);
        mp.title("My position");
        Marker jesus = mMap.addMarker(mp);
        jesus.setTag(-1);
        markers.add(jesus);
    }

    private class doRequest extends AsyncTask<String, String, String> {
        // Store error message
        private Exception e = null;
        private final Bitmap b;
        public doRequest(Bitmap b) {
            this.b = b;
        }

        @Override
        protected String doInBackground(String... args) {
            try {
                System.out.println("in doInbackground");

                return process(b);
            } catch (Exception e) {
                this.e = e;    // Store error
            }

            return null;
        }

        @Override
        protected void onPostExecute(String data) {
            super.onPostExecute(data);
            progressLayout.setVisibility(View.INVISIBLE);
            // Display based on error existence
            System.out.println("data: " + data);
            Gson gson = new Gson();
            AnalysisResult result = gson.fromJson(data, AnalysisResult.class);
            for (String tag: result.description.tags) {
                if(tag.equals("bottle")) {
                    foundItem(true);

                    break;
                } else if(tag.equals("bag")) {
                    foundItem(false); // yea yea yea, i'd use an enum if i was trying
                    break;
                }
            }
        }
    }

}