package edu.illinois.cs.cs125.fall2019.mp;

import android.content.Intent;
import android.graphics.Point;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
//import android.widget.RadioButton;
//import android.widget.RadioButton;
//import android.widget.RadioButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

//import com.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the game creation screen, where the user configures a new game.
 */
public final class NewGameActivity extends AppCompatActivity {

    // This activity doesn't do much at first - it'll be worked on in Checkpoints 1 and 3

    /** The Google Maps view used to set the area for area mode. Null until getMapAsync finishes. */
    private GoogleMap areaMap;

    /** The Google Maps view used to set the area for target mode. Null until getMapAsync finishes. */

    private GoogleMap targetMap;

    /**
     * intent.
     */
    private Intent intent;

    /**
     * Array of Invited People.
     */
    private List<Invitee> inviteeList;

    /**
     * List<Marker> markerList.
     */
    private List<Marker> markerList;

    /**
     * Called by the Android system when the activity is created.
     * @param savedInstanceState state from the previously terminated instance (unused)
     */
    @Override
    @SuppressWarnings("ConstantConditions")
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_game); // app/src/main/res/layout/activity_new_game.xml
        setTitle(R.string.create_game); // Change the title in the top bar
        // Now that setContentView has been called, findViewById and findFragmentById work

        // Find the Google Maps component for the area map
        SupportMapFragment areaMapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.areaSizeMap);
        // Start the process of getting a Google Maps object
        areaMapFragment.getMapAsync(newMap -> {
            // NONLINEAR CONTROL FLOW: Code in this block is called later, after onCreate ends
            // It's a "callback" - it will be called eventually when the map is ready

            // Set the map variable so it can be used by other functions
            areaMap = newMap;
            // Center it on campustown
            centerMap(areaMap);
        });



        SupportMapFragment targetsMapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.targetsMap);
        // Start the process of getting a Google Maps object
        targetsMapFragment.getMapAsync(newMap -> {

            markerList = new ArrayList<>();

            // Set the map variable so it can be used by other functions
            targetMap = newMap;
            // Center it on campustown
            centerMap(targetMap);

            targetMap.setOnMapLongClickListener(location -> {
                // Code here runs whenever the user presses on the map.
                // location is the LatLng position where the user pressed.
                // 1. Create a Google Maps Marker at the provided coordinates.

                Marker m = targetMap.addMarker(new MarkerOptions().position(location));

                // 2. Add it to your targets list instance variable.
                markerList.add(m);

            });

            targetMap.setOnMarkerClickListener(clickedMarker -> {
                // Code here runs whenever the user taps a marker.
                // clickedMarker is the Marker object the user clicked.
                // 1. Remove the marker from the map with its remove function.

                clickedMarker.remove();

                // 2. Remove it from your targets list.
                for (int i = 0; i <  markerList.size(); i++) {
                    if (markerList.get(i) == clickedMarker) {
                        markerList.remove(i);
                        break;
                    }
                }
                return true; // This makes Google Maps not pan the map again
            });

        });


        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        inviteeList = new ArrayList<>();
        inviteeList.add(new Invitee(user.getEmail(), TeamID.OBSERVER));

        updatePlayersUI();

        Button addInvitee = findViewById(R.id.addInvitee);
        addInvitee.setOnClickListener(new View.OnClickListener() {
            public void onClick(final View v) {
                addInvitee();
            }
        });

        /*
         * Setting an ID for a control in the UI designer produces a constant on R.id
         * that can be passed to findViewById to get a reference to that control.
         * Here we get a reference to the Create Game button.
         */
        Button createGame = findViewById(R.id.createGame);
        /*
         * Now that we have a reference to the control, we can use its setOnClickListener
         * method to set the handler to run when the user clicks the button. That function
         * takes an OnClickListener instance. OnClickListener, like many types in Android,
         * has exactly one function which must be filled out, so Java allows instances of it
         * to be written as "lambdas", which are like small functions that can be passed around.
         * The part before the arrow is the argument list (Java infers the types); the part
         * after is the statement to run. Here we don't care about the argument, but it must
         * be there for the signature to match.
         */
        createGame.setOnClickListener(unused -> createGameClicked());
        /*
         * It's also possible to make lambdas for functions that take zero or multiple parameters.
         * In those cases, the parameter list needs to be wrapped in parentheses, like () for a
         * zero-argument lambda or (someArg, anotherArg) for a two-argument lambda. Lambdas that
         * run multiple statements, like the one passed to getMapAsync above, look more like
         * normal functions in that they need their body wrapped in curly braces. Multi-statement
         * lambdas for functions with a non-void return type need return statements, again like
         * normal functions.
         */

        // Suppose modeGroup is a RadioGroup variable (maybe an instance variable?)

        RadioGroup modeGroup = findViewById(R.id.gameModeGroup);

        LinearLayout targetLayout = (LinearLayout) findViewById(R.id.targetSettings);
        targetLayout.setVisibility(View.GONE);

        LinearLayout areaLayout = (LinearLayout) findViewById(R.id.areaSettings);
        areaLayout.setVisibility(View.GONE);

        modeGroup.setOnCheckedChangeListener((unused, checkedId) -> {
            // checkedId is the R.id constant of the currently checked RadioButton
            // Your code here: make only the selected mode's settings group visible
            if (checkedId == R.id.targetModeOption) {
                areaLayout.setVisibility(View.GONE);
                targetLayout.setVisibility(View.VISIBLE);
            }

            if (checkedId == R.id.areaModeOption) {
                targetLayout.setVisibility(View.GONE);
                areaLayout.setVisibility(View.VISIBLE);
            }

        });

    }

    /**
     * Sets up the area sizing map with initial settings: centering on campustown.
     * <p>
     * You don't need to alter or understand this function, but you will want to use it when
     * you add another map control in Checkpoint 3.
     * @param map the map to center
     */
    private void centerMap(final GoogleMap map) {
        // Bounds of campustown and some surroundings
        final double swLatitude = 40.098331;
        final double swLongitude = -88.246065;
        final double neLatitude = 40.116601;
        final double neLongitude = -88.213077;

        // Get the window dimensions (for the width)
        Point windowSize = new Point();
        getWindowManager().getDefaultDisplay().getSize(windowSize);

        // Convert 300dp (height of map control) to pixels
        final int mapHeightDp = 300;
        float heightPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, mapHeightDp,
                getResources().getDisplayMetrics());

        // Submit the camera update
        final int paddingPx = 10;
        map.moveCamera(CameraUpdateFactory.newLatLngBounds(new LatLngBounds(
                new LatLng(swLatitude, swLongitude),
                new LatLng(neLatitude, neLongitude)), windowSize.x, (int) heightPx, paddingPx));
    }

    // latest commit"
    /**
     * Code to run when the Create Game button is clicked.
     */
    private void createGameClicked() {
        //Intent intent = new Intent();
        // Checking if mode was selected

        JsonObject objJ = new JsonObject();
        JsonArray inviteelist = new JsonArray();

        JsonArray targetList = new JsonArray();

        RadioButton targetButton = findViewById(R.id.targetModeOption);
        boolean targetCheck = targetButton.isChecked();

        RadioButton areaButton = findViewById(R.id.areaModeOption);
        boolean areaCheck = areaButton.isChecked();

        intent = new Intent();

        for (int i = 0; i < inviteeList.size(); i++) {
            JsonObject newObj = new JsonObject();
            //property email
            newObj.addProperty("email", inviteeList.get(i).getEmail());
            //property team
            newObj.addProperty("team", inviteeList.get(i).getTeamId());
            //adding to inviteelist
            inviteelist.add(newObj);
        }

        objJ.add("invitees", inviteelist);

        if (targetCheck) {

            objJ.addProperty("mode", "target");

            EditText pTinTarget = findViewById(R.id.proximityThreshold);
            String valueStr = pTinTarget.getText().toString();

            if (valueStr.trim().length() != 0) {

                intent = new Intent(this, GameActivity.class);
                //finish();

                // Complete this function so that it populates the Intent with the user's settings (using putExtra)
                intent.putExtra("mode", "target");

                if (intent.getStringExtra("mode").equals("target")) {

                    // Proximity Threshold
                    int number = Integer.parseInt(valueStr);
                    intent.putExtra("proximityThreshold", number);

                    objJ.addProperty("proximityThreshold", number);

                }

                //startActivity(intent);

            }

            for (int i = 0; i < markerList.size(); i++) {
                JsonObject objO = new JsonObject();
                //property latitude
                objO.addProperty("latitude", markerList.get(i).getPosition().latitude);
                //property longitude
                objO.addProperty("longitude", markerList.get(i).getPosition().longitude);
                //adding to target list different from invitee list
                targetList.add(objO);
            }

            objJ.add("targets", targetList);

            WebApi.startRequest(this, WebApi.API_BASE + ("/games/create"), Request.Method.POST, objJ, response -> {
                // Code in this handler will run when the request completes successfully
                // Do something with the response?
                intent.putExtra("game", response.get("game").getAsString());
                // starting activity with new intent
                startActivity(intent);
                //finish activitiy
                finish();

            }, error -> {
                // Code in this handler will run if the request fails
                // Maybe notify the user of the error?
                    Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show();
                });

        }
        if (areaCheck) {

            objJ.addProperty("mode", "area");

            EditText cSinArea = findViewById(R.id.cellSize);
            String value = cSinArea.getText().toString();

            if (value.trim().length() != 0) {

                intent = new Intent(this, GameActivity.class);
                //finish();

                // Complete this function so that it populates the Intent with the user's settings (using putExtra)
                intent.putExtra("mode", "area");

                // Boundaries
                LatLngBounds bounds = areaMap.getProjection().getVisibleRegion().latLngBounds;
                intent.putExtra("areaNorth", bounds.northeast.latitude);
                intent.putExtra("areaSouth", bounds.southwest.latitude);
                intent.putExtra("areaEast", bounds.northeast.longitude);
                intent.putExtra("areaWest", bounds.southwest.longitude);

                // Cell Size
                int cellSize = Integer.parseInt(value);
                System.out.println("Cell Size:    " + cellSize);
                intent.putExtra("cellSize", cellSize);

                objJ.addProperty("cellSize", cellSize);

                // areaNorth, areaSouth, areaEast, areaWest

                objJ.addProperty("areaNorth", bounds.northeast.latitude);
                objJ.addProperty("areaSouth", bounds.southwest.latitude);
                objJ.addProperty("areaEast", bounds.northeast.longitude);
                objJ.addProperty("areaWest", bounds.southwest.longitude);

                WebApi.startRequest(this, WebApi.API_BASE + ("/games/create"), Request.Method.POST, objJ, response -> {
                    // Code in this handler will run when the request completes successfully
                    // Do something with the response?
                    intent.putExtra("game", response.get("game").getAsString());
                    //starting intent activity
                    startActivity(intent);
                    finish();

                }, error -> {
                    // Code in this handler will run if the request fails
                    // Maybe notify the user of the error?
                        Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show();
                    });

                //startActivity(intent);

            }
        }



        // If the user has set all necessary settings, launch the GameActivity and finish this activity
        System.out.println(intent.describeContents());



    }

    /**
     * Update Players UI function.
     */
    public void updatePlayersUI() {
        //System.out.println("Came here");
        System.out.println("Current list  " + inviteeList);

        LinearLayout playersList = findViewById(R.id.playersList);
        playersList.removeAllViews();

        for (int i = 0; i < inviteeList.size(); i++) {
            View inviteeChunk = getLayoutInflater().inflate(R.layout.chunk_invitee,
                    findViewById(R.id.playersList), false);

            Invitee current = inviteeList.get(i);

            TextView inviteeEmail = inviteeChunk.findViewById(R.id.inviteeEmail);
            inviteeEmail.setText(current.getEmail());

            int num = i;

            Spinner inviteeTeam = inviteeChunk.findViewById(R.id.inviteeTeam);
            inviteeTeam.setSelection(current.getTeamId());

            inviteeTeam.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(final AdapterView<?> parent, final View view,
                                           final int position, final long id) {
                    current.setTeamId(position);
                }

                @Override
                public void onNothingSelected(final AdapterView<?> parent) {
                    // Called when the selection becomes empty
                    // Not relevant to the MP - can be left blank
                }
            });

            Button removeButton = inviteeChunk.findViewById(R.id.removeInvitee);
            if (num == 0) {
                removeButton.setVisibility(View.GONE);
            } else {
                removeButton.setOnClickListener(new View.OnClickListener() {
                    public void onClick(final View v) {

                        removeButton.setVisibility(View.VISIBLE);
                        inviteeList.remove(current);
                        updatePlayersUI();

                    }
                });
            }

            playersList.addView(inviteeChunk);

        }

    }

    /**
     * Add Invitee function.
     */
    public void addInvitee() {

        //edit text not text view
        EditText newInviteeEmail = findViewById(R.id.newInviteeEmail);

        //checking whether its empty or not empty and has email input
        if (!newInviteeEmail.getText().toString().equals("")) {
            inviteeList.add(new Invitee((newInviteeEmail.getText().toString()), TeamID.OBSERVER));
        }

        newInviteeEmail.setText("");

        updatePlayersUI();
    }

}
