package edu.illinois.cs.cs125.fall2019.mp;

import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
//import android.provider.ContactsContract;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

//import java_cup.Main;

/**
 * Represents the main screen of the app, where the user will be able to view invitations and enter games.
 */
public final class MainActivity extends AppCompatActivity {

    /**
     * Called by the Android system when the activity is created.
     * @param savedInstanceState saved state from the previously terminated instance of this activity (unused)
     */
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        // This "super" call is required for all activities
        super.onCreate(savedInstanceState);
        // Create the UI from a layout resource
        setContentView(R.layout.activity_main);

        // This activity doesn't do anything yet - it immediately launches the game activity
        // Work on it will start in Checkpoint 1

        Button createGame = findViewById(R.id.createGame);
        createGame.setVisibility(View.VISIBLE);

        createGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                startActivity(new Intent(MainActivity.this, NewGameActivity.class));
            }
        });
        findViewById(R.id.ongoingGamesGroup).setVisibility(View.GONE);
        findViewById(R.id.invitationsGroup).setVisibility(View.GONE);

        connect();

        // Intents are Android's way of specifying what to do/launch
        // Here we create an Intent for launching GameActivity and act on it with startActivity
        // startActivity(new Intent(this, GameActivity.class));
        // End this activity so that it's removed from the history
        // Otherwise pressing the back button in the game would come back to a blank screen here
        finish();
    }

    // The functions below are stubs that will be filled out in Checkpoint 2

    /**
     * Starts an attempt to connect to the server to fetch/refresh games.
     */
    private void connect() {
        // Make any "loading" UI adjustments you like
        // Use WebApi.startRequest to fetch the games lists
        // In the response callback, call setUpUi with the received data
        LinearLayout invitationsList = findViewById(R.id.invitationsList);
        LinearLayout ongoingList = findViewById(R.id.ongoingGamesList);
        invitationsList.removeAllViews();
        ongoingList.removeAllViews();

        WebApi.startRequest(this, WebApi.API_BASE + "/games", response -> {
            // Code in this handler will run when the request completes successfully
            // Do something with the response?
            setUpUi(response);
        }, error -> {
            // Code in this handler will run if the request fails
            // Maybe notify the user of the error?
                Toast.makeText(this, "Oh no!", Toast.LENGTH_LONG).show();
            });
    }

    /**
     * Populates the games lists UI with data retrieved from the server.
     * @param result parsed JSON from the server
     */
    private void setUpUi(final JsonObject result) {

        FirebaseUser me = FirebaseAuth.getInstance().getCurrentUser();

        JsonArray games = result.get("games").getAsJsonArray();

        for (JsonElement g : games) {

            JsonObject objg = ((JsonObject) g);

            if (objg.get("state").getAsInt() == GameStateID.ENDED) {
                continue;

            } else if (objg.get("state").getAsInt() == GameStateID.PAUSED
                    || objg.get("state").getAsInt() == GameStateID.RUNNING) {

                JsonArray players = objg.get("players").getAsJsonArray();

                for (JsonElement p : players) {

                    JsonObject meP = ((JsonObject) p);

                    if (me.getEmail().equals((meP).get("email").getAsString())) {

                        if (meP.get("state").getAsInt() == PlayerStateID.INVITED) {

                            //inflate chunk(3.2) done
                            View invitationsChunk = getLayoutInflater().inflate(R.layout.chunk_invitations,
                                    findViewById(R.id.invitationsList), false);

                            //add info(3.4) done
                            TextView emailI = invitationsChunk.findViewById(R.id.iEmail);
                            String ownerEmail = objg.get("owner").getAsString();
                            emailI.setText(ownerEmail);

                            TextView colorI = invitationsChunk.findViewById(R.id.iColor);
                            int color = meP.get("team").getAsInt();
                            if (color == TeamID.OBSERVER) {
                                colorI.setText("Observer");
                            }
                            if (color == TeamID.TEAM_BLUE) {
                                colorI.setText("Blue");
                            }
                            if (color == TeamID.TEAM_GREEN) {
                                colorI.setText("Green");
                            }
                            if (color == TeamID.TEAM_RED) {
                                colorI.setText("Red");
                            }
                            if (color == TeamID.TEAM_YELLOW) {
                                colorI.setText("Yellow");
                            }

                            TextView modeI = invitationsChunk.findViewById(R.id.iMode);
                            String mode = objg.get("mode").getAsString();
                            modeI.setText(mode + " mode");

                            // 5.3:
                            Button accept = invitationsChunk.findViewById(R.id.accept);
                            Button decline = invitationsChunk.findViewById(R.id.decline);

                            accept.setOnClickListener(unused -> callPost(objg.get("id").getAsString() + "/accept"));
                            decline.setOnClickListener(unused -> callPost(objg.get("id").getAsString() + "/decline"));

                            // add chunk to invite linlayout(3.3)
                            LinearLayout iLL = findViewById(R.id.invitationsList);
                            iLL.addView(invitationsChunk);

                            findViewById(R.id.invitationsGroup).setVisibility(View.VISIBLE);
                            iLL.setVisibility(View.VISIBLE);

                        }

                        if (meP.get("state").getAsInt() == PlayerStateID.ACCEPTED
                                || meP.get("state").getAsInt() == PlayerStateID.PLAYING) {

                            //inflate chunk(3.2) done
                            View ongoingChunk = getLayoutInflater().inflate(R.layout.chunk_ongoing_game,
                                    findViewById(R.id.ongoingGamesList), false);

                            //add info(3.4) done
                            TextView emailO = ongoingChunk.findViewById(R.id.oEmail);
                            String ownerEmail = objg.get("owner").getAsString();
                            emailO.setText(ownerEmail);

                            TextView colorO = ongoingChunk.findViewById(R.id.oColor);
                            int color = meP.get("team").getAsInt();
                            if (color == TeamID.OBSERVER) {
                                colorO.setText("Observer");
                            }
                            if (color == TeamID.TEAM_BLUE) {
                                colorO.setText("Blue");
                            }
                            if (color == TeamID.TEAM_GREEN) {
                                colorO.setText("Green");
                            }
                            if (color == TeamID.TEAM_RED) {
                                colorO.setText("Red");
                            }
                            if (color == TeamID.TEAM_YELLOW) {
                                colorO.setText("Yellow");
                            }

                            TextView modeO = ongoingChunk.findViewById(R.id.oMode);
                            String mode = objg.get("mode").getAsString();
                            modeO.setText(mode + " mode");

                            // 5.3:
                            Button leave = ongoingChunk.findViewById(R.id.leave);
                            Button enter = ongoingChunk.findViewById(R.id.enter);

                            leave.setOnClickListener(unused -> callPost(objg.get("id").getAsString() + "/leave"));
                            enter.setOnClickListener(unused -> enterGame(objg.get("id").getAsString()));

                            if (me.getEmail().equals(ownerEmail)) {
                                leave.setVisibility(View.GONE);
                            }

                            // add chunk to invite linlayout(3.3) done
                            LinearLayout oLL = findViewById(R.id.ongoingGamesList);
                            oLL.addView(ongoingChunk);

                            findViewById(R.id.ongoingGamesGroup).setVisibility(View.VISIBLE);
                            oLL.setVisibility(View.VISIBLE);
                        }
                    }
                }
            }



        }

        // Hide any optional "loading" UI you added
        // Clear the games lists
        // Add UI chunks to the lists based on the result data
    }

    /**
     * Enters a game (shows the map).
     * @param gameId the ID of the game to enter
     */
    private void enterGame(final String gameId) {
        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra("game", gameId);
        startActivity(intent);
        // Launch GameActivity with the game ID in an intent extra
        // Do not finish - the user should be able to come back here
    }

    public void callPost(final String str) {

        WebApi.startRequest(this, WebApi.API_BASE + ("/games/" + str), Request.Method.POST, null, response -> {
            // Code in this handler will run when the request completes successfully
            // Do something with the response?
            connect();
        }, error -> {
            // Code in this handler will run if the request fails
            // Maybe notify the user of the error?
                Toast.makeText(this, "Oh no!", Toast.LENGTH_LONG).show();
            });



    }
}
