package edu.illinois.cs.cs125.fall2019.mp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
//import android.view.View;
import android.widget.Button;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

//import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;

// Latest commit

/**
 * Launch Activity.
 */
public class LaunchActivity extends AppCompatActivity {

    /**
     *
     */
    private static final int RC_SIGN_IN = 1;

    /**
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launch);
        if (FirebaseAuth.getInstance().getCurrentUser() != null) { // see below discussion
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        } else {
            // Choose authentication providers
            List<AuthUI.IdpConfig> providers = Arrays.asList(
                    new AuthUI.IdpConfig.EmailBuilder().build());
                    //only need email

            // Create and launch sign-in intent
            startActivityForResult(
                    AuthUI.getInstance()
                            .createSignInIntentBuilder()
                            .setAvailableProviders(providers)
                            .build(),
                    RC_SIGN_IN);
        }
    }

    /**
     * @param requestCode integer of the requested code
     * @param resultCode the result code
     * @param data intent data
     */
    @SuppressWarnings("checkstyle:DesignForExtension")
    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);

            if (resultCode == RESULT_OK) {
                // Successfully signed in
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                finish();

            } else {

                Button login = findViewById(R.id.goLogin);
                login.setOnClickListener(v -> {
                    if (FirebaseAuth.getInstance().getCurrentUser() != null) { // see below discussion
                        Intent intent = new Intent(this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        // Choose authentication providers
                        List<AuthUI.IdpConfig> providers = Arrays.asList(
                                new AuthUI.IdpConfig.EmailBuilder().build());
                        //only need email

                        // Create and launch sign-in intent
                        startActivityForResult(
                                AuthUI.getInstance()
                                        .createSignInIntentBuilder()
                                        .setAvailableProviders(providers)
                                        .build(),
                                RC_SIGN_IN);
                    }
                });


                // Sign in failed. If response is null the user canceled the
                // sign-in flow using the back button. Otherwise check
                // response.getError().getErrorCode() and handle the error.
                // ...
            }
        }
    }

}
