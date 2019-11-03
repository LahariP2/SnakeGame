package edu.illinois.cs.cs125.fall2019.mp;

import android.app.Dialog;
import android.content.ComponentName;
import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.test.core.app.ApplicationProvider;

import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.Polyline;
import com.google.firebase.auth.FirebaseAuth;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowDialog;
import org.robolectric.shadows.ShadowToast;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Random;

import edu.illinois.cs.cs125.fall2019.mp.shadows.MockedWrapperInstantiator;
import edu.illinois.cs.cs125.fall2019.mp.shadows.ShadowGoogleMap;
import edu.illinois.cs.cs125.fall2019.mp.shadows.ShadowMarker;
import edu.illinois.cs.cs125.fall2019.mp.shadows.ShadowSupportMapFragment;
import edu.illinois.cs.cs125.gradlegrader.annotations.Graded;
import edu.illinois.cs.cs125.robolectricsecurity.PowerMockSecurity;
import edu.illinois.cs.cs125.robolectricsecurity.Trusted;

@RunWith(RobolectricTestRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@PowerMockIgnore({"org.mockito.*", "org.powermock.*", "org.robolectric.*", "android.*", "androidx.*", "com.google.android.*", "edu.illinois.cs.cs125.fall2019.mp.shadows.*"})
@PrepareForTest({WebApi.class, FirebaseAuth.class})
@Trusted
public class Checkpoint3Test {

    @Rule
    public PowerMockRule mockStaticClasses = new PowerMockRule();

    @Before
    public void setup() {
        PowerMockSecurity.secureMockMethodCache();
        FirebaseMocker.mock();
        FirebaseMocker.setEmail(SampleData.USER_EMAIL);
        WebApiMocker.interceptHttp();
    }

    @After
    public void teardown() {
        WebApiMocker.reset();
    }

    @Test(timeout = 60000)
    @Graded(points = 15)
    public void testTargetClass() {
        // Create the map
        GoogleMap map = MockedWrapperInstantiator.create(GoogleMap.class);
        ShadowGoogleMap shadowMap = Shadow.extract(map);

        // Check class design
        for (Field field : Target.class.getDeclaredFields()) {
            Assert.assertTrue("Target's fields should be private", Modifier.isPrivate(field.getModifiers()));
        }

        // Test constructor/getters
        LatLng position = new LatLng(40.097249, -88.229229);
        Target target = new Target(map, position, TeamID.OBSERVER);
        Assert.assertEquals("Target didn't remember the team", TeamID.OBSERVER, target.getTeam());
        Assert.assertTrue("Target didn't remember the position", LatLngUtils.same(position, target.getPosition()));

        // Make sure it added a marker to the map
        Assert.assertEquals("Creating a Target should add a marker to the map", 1, shadowMap.getMarkers().size());
        Marker marker = shadowMap.getMarkers().get(0);
        Assert.assertTrue("Creating a Target placed the marker at the wrong location", LatLngUtils.same(position, marker.getPosition()));
        ShadowMarker shadowMarker = Shadow.extract(marker);
        Assert.assertEquals("Unclaimed target markers should be violet",
                BitmapDescriptorFactory.HUE_VIOLET, shadowMarker.getHue(), 1e-3);

        // Claim the target
        target.setTeam(TeamID.TEAM_BLUE);
        Assert.assertEquals("Target didn't remember the updated team", TeamID.TEAM_BLUE, target.getTeam());
        Assert.assertEquals("Updating a Target's team should not create duplicate markers", 1, shadowMap.getMarkers().size());
        marker = shadowMap.getMarkers().get(0);
        Assert.assertTrue("Updating a Target's team should not change the location", LatLngUtils.same(position, marker.getPosition()));
        shadowMarker = Shadow.extract(marker);
        Assert.assertEquals("Setting a target's team should turn the marker the team color",
                BitmapDescriptorFactory.HUE_BLUE, shadowMarker.getHue(), 1e-3);

        // Try several markers
        for (int i = 2; i <= 25; i++) {
            LatLng randomPos = new LatLng(RandomHelper.randomLat(), RandomHelper.randomLng());
            int team = RandomHelper.randomTeam();
            target = new Target(map, randomPos, team);
            Assert.assertEquals("Target didn't remember the team", team, target.getTeam());
            Assert.assertTrue("Target didn't remember the position", LatLngUtils.same(randomPos, target.getPosition()));
            Assert.assertEquals("Creating more targets shouldn't create duplicates or affect existing targets", i, shadowMap.getMarkers().size());
            marker = shadowMap.getMarkerAt(randomPos);
            Assert.assertNotNull("Creating another Target placed the marker at the wrong location", marker);
            shadowMarker = Shadow.extract(marker);
            Assert.assertNotEquals("Target markers should be the team color", BitmapDescriptorFactory.HUE_VIOLET, shadowMarker.getHue());
        }
    }

    @Test(timeout = 60000)
    @Graded(points = 10)
    @SuppressWarnings("JavaReflectionMemberAccess")
    public void testLatLngRefactor() throws Throwable {
        // Make sure addLine was refactored or removed
        try {
            GameActivity.class.getMethod("addLine", double.class, double.class, double.class, double.class, int.class);
            Assert.fail("addLine's four double parameters should be refactored into two LatLng parameters");
        } catch (NoSuchMethodException e) {
            // Good - the old method should no longer exist
        }

        // Start the activity
        WebSocketMocker webSocketControl = WebSocketMocker.expectConnection();
        Intent intent = new Intent();
        intent.putExtra("game", RandomHelper.randomId());
        intent.putExtra("mode", "target");
        intent.putExtra("proximityThreshold", 15);
        GameActivityLauncher launcher = new GameActivityLauncher(intent);

        // Test addLine
        if (webSocketControl.isConnected()) {
            // Working on a late Checkpoint 4 submission
            testLatLngRefactorS4(webSocketControl, launcher);
        } else {
            // Working on Checkpoint 3
            Method refactoredAddLine;
            try {
                refactoredAddLine = GameActivity.class.getMethod("addLine", LatLng.class, LatLng.class, int.class);
                int[] teamColors = ApplicationProvider.getApplicationContext().getResources().getIntArray(R.array.team_colors);
                ShadowGoogleMap map = Shadow.extract(launcher.getMap());
                for (int i = 0; i < 10; i++) {
                    LatLng start = new LatLng(RandomHelper.randomLat(), RandomHelper.randomLng());
                    LatLng end = new LatLng(RandomHelper.randomLat(), RandomHelper.randomLng());
                    int color = teamColors[RandomHelper.randomTeam()];
                    refactoredAddLine.invoke(launcher.getActivity(), start, end, color);
                    Assert.assertNotEquals("addLine should add a line to the map", 0, map.getPolylines().size());
                    Assert.assertEquals("Each line should consist of two Polyline objects", (i + 1) * 2, map.getPolylines().size());
                    Assert.assertEquals("addLine didn't add the line with the correct endpoints", 2, map.getPolylinesConnecting(start, end).size());
                }
            } catch (NoSuchMethodException e) {
                // Actually a malfunctioning Checkpoint 4 submission
                Assert.fail("No known addLine signature was found and no Checkpoint 4 websocket connection was attempted");
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        }

        // Make sure linesCross was refactored
        try {
            String linesCrossFail = "The refactored linesCross function is incorrect";
            Method linesCrossMethod = LineCrossDetector.class.getMethod("linesCross", LatLng.class, LatLng.class, LatLng.class, LatLng.class);
            Assert.assertFalse(linesCrossFail, (Boolean) linesCrossMethod.invoke(null,
                    new LatLng(40.1, -88.6), new LatLng(40.5, -89.0),
                    new LatLng(40.6, -88.1), new LatLng(40.9, -88.2)));
            Assert.assertTrue(linesCrossFail, (Boolean) linesCrossMethod.invoke(null,
                    new LatLng(40.2, -88.3), new LatLng(40.2, -88.7),
                    new LatLng(40.1, -88.5), new LatLng(40.3, -88.5)));

            // Randomized tests (from JSON)
            for (JsonObject test : JsonResourceLoader.loadArray("linescross")) {
                double startLat1 = test.get("sla1").getAsDouble();
                double startLng1 = test.get("sln1").getAsDouble();
                double endLat1 = test.get("ela1").getAsDouble();
                double endLng1 = test.get("eln1").getAsDouble();
                double startLat2 = test.get("sla2").getAsDouble();
                double startLng2 = test.get("sln2").getAsDouble();
                double endLat2 = test.get("ela2").getAsDouble();
                double endLng2 = test.get("eln2").getAsDouble();
                Assert.assertEquals(linesCrossFail, test.get("answer").getAsBoolean(),
                        linesCrossMethod.invoke(null, new LatLng(startLat1, startLng1), new LatLng(endLat1, endLng1),
                                new LatLng(startLat2, startLng2), new LatLng(endLat2, endLng2)));
            }
        } catch (NoSuchMethodException e) {
            Assert.fail("LineCrossDetector.linesCross should now take four LatLng parameters");
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
        for (Method method : LineCrossDetector.class.getDeclaredMethods()) {
            int coordParams = 0;
            for (Class<?> paramType : method.getParameterTypes()) {
                if (paramType.equals(double.class) || paramType.equals(float.class)) coordParams++;
            }
            Assert.assertFalse("The line-crossing detection function that took eight loose coordinates should be removed", coordParams >= 8);
        }
    }

    private void testLatLngRefactorS4(WebSocketMocker webSocketControl, GameActivityLauncher launcher) {
        StackTraceElement[][] stackTraceHolder = new StackTraceElement[1][];
        Shadow.<ShadowGoogleMap>extract(launcher.getMap()).setComponentAdditionListener(obj -> {
            if (!(obj instanceof Polyline)) return;
            stackTraceHolder[0] = Thread.currentThread().getStackTrace();
            boolean foundAddPolyline = false;
            for (StackTraceElement frame : stackTraceHolder[0]) {
                if (frame.getMethodName().equals("addPolyline")) {
                    foundAddPolyline = true;
                } else if (foundAddPolyline) {
                    Assert.assertNotEquals("GameActivity should no longer be directly responsible for rendering the map",
                            GameActivity.class.getName(), frame.getClassName());
                    return;
                }
            }
            Assert.fail("Impossible: addPolyline not found in stack trace of polyline addition");
        });
        webSocketControl.sendData(SampleData.createTargetModeTestGame());
        Assert.assertNotNull("Player paths in target mode should be shown with polylines", stackTraceHolder[0]);
    }

    @Test(timeout = 60000)
    @Graded(points = 20)
    public void testInvitees() {
        // Get IDs
        @IdRes int rIdPlayersList = IdLookup.require("playersList");
        @IdRes int rIdInviteeEmail = IdLookup.require("inviteeEmail");
        @IdRes int rIdRemoveInvitee = IdLookup.require("removeInvitee");
        @IdRes int rIdInviteeTeam = IdLookup.require("inviteeTeam");
        @IdRes int rIdAddInvitee = IdLookup.require("addInvitee");
        @IdRes int rIdNewInviteeEmail = IdLookup.require("newInviteeEmail");

        // Check the initial invitee list
        NewGameActivity activity = Robolectric.buildActivity(NewGameActivity.class).create().start().resume().get();
        LinearLayout playersList = activity.findViewById(rIdPlayersList);
        Assert.assertNotNull("NewGameActivity should have a LinearLayout for the invitees", playersList);
        View inviteeSection = playersList.getChildAt(0);
        Assert.assertNotNull("The invitees list should initially have an entry for the user", inviteeSection);
        TextView inviteeEmailLabel = inviteeSection.findViewById(rIdInviteeEmail);
        Assert.assertEquals("The first invitee entry should have the user's email", SampleData.USER_EMAIL, inviteeEmailLabel.getText().toString());
        Button removeButton = inviteeSection.findViewById(rIdRemoveInvitee);
        if (removeButton != null) {
            Assert.assertEquals("The user's own invitee row should have no Remove button", View.GONE, removeButton.getVisibility());
        }
        Spinner teamSpinner = inviteeSection.findViewById(rIdInviteeTeam);
        Assert.assertEquals("The user's invitee row's default team selection should be Observer", "Observer", teamSpinner.getSelectedItem());

        // Check team names
        String[] teamNames = activity.getResources().getStringArray(R.array.team_choices);
        for (int t = TeamID.MIN_TEAM; t <= TeamID.MAX_TEAM; t++) {
            teamSpinner.setSelection(t);
            Assert.assertEquals("Team choices list is incorrect - set the entries attribute", teamNames[t], teamSpinner.getSelectedItem());
        }

        // Try pressing the Add button before entering an email
        Button addButton = activity.findViewById(rIdAddInvitee);
        addButton.performClick();
        Assert.assertEquals("Pressing the Add button without typing an invitee email should do nothing", 1, playersList.getChildCount());

        // Add an invitee
        EditText newInviteeEmail = activity.findViewById(rIdNewInviteeEmail);
        String firstInvitee = RandomHelper.randomEmail();
        newInviteeEmail.setText(firstInvitee);
        addButton.performClick();
        Assert.assertEquals("Adding an invitee should clear the add-invitee email text box", "", newInviteeEmail.getText().toString());
        Assert.assertEquals("Adding an invitee should be reflected in the invitees list", 2, playersList.getChildCount());
        inviteeSection = playersList.getChildAt(1);
        inviteeEmailLabel = inviteeSection.findViewById(rIdInviteeEmail);
        Assert.assertEquals("The added invitee's email should be shown in the invitees list", firstInvitee, inviteeEmailLabel.getText().toString());
        removeButton = inviteeSection.findViewById(rIdRemoveInvitee);
        Assert.assertEquals("Added invitee sections should have Remove buttons", View.VISIBLE, removeButton.getVisibility());
        teamSpinner = inviteeSection.findViewById(rIdInviteeTeam);
        Assert.assertEquals("The default invitee role should be Observer", "Observer", teamSpinner.getSelectedItem());

        // Add another invitee
        String secondInvitee = RandomHelper.randomEmail(firstInvitee);
        newInviteeEmail.setText(secondInvitee);
        addButton.performClick();
        Assert.assertEquals("Adding another invitee should be reflected in the invitees list", 3, playersList.getChildCount());
        inviteeSection = playersList.getChildAt(2);
        inviteeEmailLabel = inviteeSection.findViewById(rIdInviteeEmail);
        Assert.assertEquals("The added invitee's email should be shown in the invitees list", secondInvitee, inviteeEmailLabel.getText().toString());
        removeButton = inviteeSection.findViewById(rIdRemoveInvitee);
        Assert.assertEquals("Added invitee sections should have Remove buttons", View.VISIBLE, removeButton.getVisibility());
        teamSpinner = inviteeSection.findViewById(rIdInviteeTeam);
        teamSpinner.setSelection(TeamID.TEAM_YELLOW);

        // Remove the first invitee
        playersList.getChildAt(1).findViewById(rIdRemoveInvitee).performClick();
        Assert.assertEquals("Clicking an invitee Remove button should remove that record from the list", 2, playersList.getChildCount());
        inviteeSection = playersList.getChildAt(1);
        inviteeEmailLabel = inviteeSection.findViewById(rIdInviteeEmail);
        Assert.assertEquals("Removing an invitee should not affect other invitees", secondInvitee, inviteeEmailLabel.getText().toString());
        teamSpinner = inviteeSection.findViewById(rIdInviteeTeam);
        Assert.assertEquals("Removing an invitee should not affect other invitees' roles", teamNames[TeamID.TEAM_YELLOW], teamSpinner.getSelectedItem());

        // Remove the second invitee
        removeButton = inviteeSection.findViewById(rIdRemoveInvitee);
        removeButton.performClick();
        Assert.assertEquals("Clicking an invitee Remove button should remove that record from the list", 1, playersList.getChildCount());
        inviteeSection = playersList.getChildAt(0);
        inviteeEmailLabel = inviteeSection.findViewById(rIdInviteeEmail);
        Assert.assertEquals("Removing another invitee should not affect the user's row", SampleData.USER_EMAIL, inviteeEmailLabel.getText().toString());
        teamSpinner = inviteeSection.findViewById(rIdInviteeTeam);
        Assert.assertEquals("Removing another invitee should not affect the user's team selection", teamNames[TeamID.MAX_TEAM], teamSpinner.getSelectedItem());
    }

    @Test(timeout = 60000)
    @Graded(points = 15)
    public void testTargetMap() {
        // Start the activity
        NewGameActivity activity = Robolectric.buildActivity(NewGameActivity.class).create().start().resume().get();
        ShadowSupportMapFragment mapFragment = Shadow.extract(activity.getSupportFragmentManager()
                .findFragmentById(IdLookup.require("targetsMap")));
        mapFragment.notifyMapReady();
        ShadowGoogleMap map = Shadow.extract(mapFragment.getMap());

        // Select target mode
        RadioButton targetOption = activity.findViewById(IdLookup.require("targetModeOption"));
        targetOption.setChecked(true);
        Assert.assertEquals("The targets map should have no markers initially", 0, map.getMarkers().size());

        // Long-press the map to add a target
        LatLng position = new LatLng(40.108212, -88.229250);
        map.longPress(position);
        Assert.assertEquals("Long-pressing the targets map should create a marker", 1, map.getMarkers().size());
        Marker marker = map.getMarkers().get(0);
        Assert.assertTrue("Long-pressing the targets map should create a marker at the pressed point",
                LatLngUtils.same(position, marker.getPosition()));

        // Add another target
        LatLng anotherPosition = new LatLng(40.100122, -88.250371);
        map.longPress(anotherPosition);
        Assert.assertEquals("Long-pressing another spot should create another marker", 2, map.getMarkers().size());
        Marker anotherMarker = map.getMarkerAt(anotherPosition);
        Assert.assertNotNull("The new target should be placed where the long-press occurred", anotherMarker);
        marker = map.getMarkerAt(position);
        Assert.assertNotNull("Adding new targets should not affect existing ones", marker);

        // Click the targets to remove them
        map.clickMarker(marker);
        Assert.assertEquals("Clicking a marker should remove the target", 1, map.getMarkers().size());
        anotherMarker = map.getMarkerAt(anotherPosition);
        Assert.assertNotNull("Removing a target should not affect other targets", anotherMarker);
        map.clickMarker(anotherMarker);
        Assert.assertEquals("Clicking another marker should remove its target", 0, map.getMarkers().size());

        // Add many targets
        LatLng[] positions = new LatLng[50];
        Marker[] markers = new Marker[positions.length];
        for (int i = 0; i < positions.length; i++) {
            LatLng randomPosition = new LatLng(RandomHelper.randomLat(), RandomHelper.randomLng());
            positions[i] = randomPosition;
            map.longPress(positions[i]);
            Assert.assertEquals("Long-pressing the map again should create another marker", i + 1, map.getMarkers().size());
            marker = map.getMarkerAt(randomPosition);
            Assert.assertNotNull("The new target should be placed where the long-press occurred", marker);
            markers[i] = marker;
            for (int j = 0; j < i; j++) {
                LatLng prevPosition = positions[j];
                marker = map.getMarkerAt(prevPosition);
                Assert.assertNotNull("Adding more targets shouldn't affect existing ones", marker);
            }
        }

        // Remove some of the targets
        for (int i = 0; i < positions.length; i += 2) {
            Marker toRemove = markers[i];
            map.clickMarker(toRemove);
            Assert.assertEquals("Clicking a marker should remove it", positions.length - (i / 2) - 1, map.getMarkers().size());
            markers[i] = null;
            for (Marker remaining : markers) {
                if (remaining == null) continue;
                marker = map.getMarkerAt(remaining.getPosition());
                Assert.assertNotNull("Removing a target shouldn't affect others", marker);
            }
        }
    }

    @Test(timeout = 60000)
    @Graded(points = 20, friendlyName = "testTargetPresets (optional)")
    @SuppressWarnings("ConstantConditions")
    public void testTargetPresets_extraCredit() {
        // Get IDs
        @IdRes int rIdTargetsMap = IdLookup.require("targetsMap");
        @IdRes int rIdTargetModeOption = IdLookup.require("targetModeOption");
        @IdRes int rIdLoadPresetTargets = IdLookup.require("loadPresetTargets");

        // Start the activity
        NewGameActivity activity = Robolectric.buildActivity(NewGameActivity.class).create().start().resume().get();
        ShadowSupportMapFragment mapFragment = Shadow.extract(activity.getSupportFragmentManager()
                .findFragmentById(rIdTargetsMap));
        mapFragment.notifyMapReady();
        ShadowGoogleMap map = Shadow.extract(mapFragment.getMap());

        // Select target mode
        RadioButton targetOption = activity.findViewById(rIdTargetModeOption);
        targetOption.setChecked(true);

        // Click the Presets button to open the dialog
        JsonObject presetsResponse = SampleData.createTargetPresetsResponse();
        Button loadPreset = activity.findViewById(rIdLoadPresetTargets);
        Assert.assertEquals("The Load Preset button should be visible when target mode is selected",
                View.VISIBLE, loadPreset.getVisibility());
        ShadowDialog.reset();
        loadPreset.performClick();
        WebApiMocker.processOne("Clicking Load Preset should request the target presets from the server",
                (path, method, body, callback, errorListener) -> {
                    Assert.assertEquals("Incorrect API endpoint for getting target presets", "/presets", path);
                    Assert.assertEquals("/presets should be accessed with a GET request", Request.Method.GET, method);
                    Assert.assertNull("No request body may be specified for /presets", body);
                    callback.onResponse(presetsResponse.deepCopy());
                });
        Dialog dialog = ShadowDialog.getLatestDialog();
        Assert.assertNotNull("A dialog should show the available presets", dialog);
        @IdRes int rIdPresetOptions = IdLookup.require("presetOptions");
        RadioGroup radioGroup = dialog.findViewById(rIdPresetOptions);
        Assert.assertNotNull("The dialog should have a RadioGroup to hold the preset options", radioGroup);
        Assert.assertEquals("The RadioGroup should have one entry per preset", 2, radioGroup.getChildCount());
        RadioButton presetOption = (RadioButton) radioGroup.getChildAt(0);
        Assert.assertEquals("Each RadioButton should show the name of its preset", "Empty", presetOption.getText().toString());
        presetOption = (RadioButton) radioGroup.getChildAt(1);
        Assert.assertEquals("Each RadioButton should show the name of its preset", "Walmart Stores", presetOption.getText().toString());
        Button positiveButton = dialog.findViewById(android.R.id.button1);
        Assert.assertEquals("The presets dialog's positive button's title is incorrect",
                "LOAD", positiveButton.getText().toString().toUpperCase());
        Button neutralButton = dialog.findViewById(android.R.id.button3);
        if (neutralButton != null) {
            Assert.assertEquals("The presets dialog should not have a neutral button", View.GONE, neutralButton.getVisibility());
        }

        // Select the second preset
        presetOption.setChecked(true);
        positiveButton.performClick();
        Assert.assertTrue("Choosing a preset and pressing Load should dismiss the dialog", Shadows.shadowOf(dialog).hasBeenDismissed());
        Assert.assertEquals("Loading a preset should add its targets to the map", 3, map.getMarkers().size());
        for (LatLng position : new LatLng[] {
                new LatLng(40.146879, -88.254737),
                new LatLng(40.048544, -88.254927),
                new LatLng(40.111625, -88.159373)}) {
            Marker marker = map.getMarkerAt(position);
            Assert.assertNotNull("Loading a preset should place markers at the targets' positions", marker);
        }

        // Select an empty preset
        loadPreset.performClick();
        WebApiMocker.process((path, method, body, callback, errorListener) -> {
            Assert.assertEquals("Incorrect path for target presets endpoint", "/presets", path);
            callback.onResponse(presetsResponse);
        });
        dialog = ShadowDialog.getLatestDialog();
        radioGroup = dialog.findViewById(rIdPresetOptions);
        presetOption = (RadioButton) radioGroup.getChildAt(0);
        presetOption.setChecked(true);
        dialog.findViewById(android.R.id.button1).performClick();
        Assert.assertTrue("Pressing Load with a selected preset should dismiss the dialog", Shadows.shadowOf(dialog).hasBeenDismissed());
        Assert.assertEquals("Loading a preset should clear previously set targets", 0, map.getMarkers().size());

        // Test randomized presets
        for (int run = 0; run < 10; run++) {
            // Start the activity and get controls
            activity = Robolectric.buildActivity(NewGameActivity.class).create().start().resume().get();
            mapFragment = Shadow.extract(activity.getSupportFragmentManager().findFragmentById(rIdTargetsMap));
            mapFragment.notifyMapReady();
            map = Shadow.extract(mapFragment.getMap());
            targetOption = activity.findViewById(rIdTargetModeOption);
            targetOption.setChecked(true);
            loadPreset = activity.findViewById(rIdLoadPresetTargets);

            // Create test data
            Random random = new Random();
            JsonObject randomPresetsResponse = new JsonObject();
            JsonArray presets = new JsonArray();
            int presetsCount = random.nextInt(10) + 1;
            int selectedPreset = random.nextInt(presetsCount);
            LatLng[] selectedTargetPositions = null;
            String selectedPresetName = null;
            for (int i = 0; i < presetsCount; i++) {
                JsonObject preset = new JsonObject();
                String name = RandomHelper.randomId();
                preset.addProperty("name", name);
                JsonArray targets = new JsonArray();
                int targetsCount = random.nextInt(196);
                if (i == selectedPreset) {
                    selectedTargetPositions = new LatLng[targetsCount];
                    selectedPresetName = name;
                }
                for (int j = 0; j < targetsCount; j++) {
                    double lat = RandomHelper.randomLat();
                    double lng = RandomHelper.randomLat();
                    targets.add(JsonHelper.position(lat, lng));
                    if (i == selectedPreset) {
                        selectedTargetPositions[j] = new LatLng(lat, lng);
                    }
                }
                preset.add("targets", targets);
                presets.add(preset);
            }
            randomPresetsResponse.add("presets", presets);

            // Use the dialog
            ShadowDialog.reset();
            loadPreset.performClick();
            WebApiMocker.process((path, method, body, callback, errorListener) -> {
                Assert.assertEquals("Incorrect path for target presets endpoint", "/presets", path);
                callback.onResponse(randomPresetsResponse);
            });
            dialog = ShadowDialog.getLatestDialog();
            radioGroup = dialog.findViewById(rIdPresetOptions);
            Assert.assertEquals("The presets RadioGroup should have one entry per preset",
                    presetsCount, radioGroup.getChildCount());
            presetOption = (RadioButton) radioGroup.getChildAt(selectedPreset);
            Assert.assertEquals("Each preset RadioButton should have the preset's name",
                    selectedPresetName, presetOption.getText().toString());
            presetOption.setChecked(true);
            dialog.findViewById(android.R.id.button1).performClick();

            // Check the map
            Assert.assertEquals("Loading a preset should place its targets on the map",
                    selectedTargetPositions.length, map.getMarkers().size());
            for (LatLng pos : selectedTargetPositions) {
                Marker marker = map.getMarkerAt(pos);
                Assert.assertNotNull("Loading a preset should place markers on the targets' positions on the map", marker);
            }
        }
    }

    @Test(timeout = 60000)
    @Graded(points = 20)
    public void testApiRequest() {
        // Get IDs
        @IdRes int rIdPlayersList = IdLookup.require("playersList");
        @IdRes int rIdInviteeTeam = IdLookup.require("inviteeTeam");
        @IdRes int rIdNewInviteeEmail = IdLookup.require("newInviteeEmail");
        @IdRes int rIdAddInvitee = IdLookup.require("addInvitee");
        @IdRes int rIdRemoveInvitee = IdLookup.require("removeInvitee");
        @IdRes int rIdTargetModeOption = IdLookup.require("targetModeOption");
        @IdRes int rIdAreaModeOption = IdLookup.require("areaModeOption");
        @IdRes int rIdTargetsMap = IdLookup.require("targetsMap");
        @IdRes int rIdAreaSizeMap = IdLookup.require("areaSizeMap");
        @IdRes int rIdProximityThreshold = IdLookup.require("proximityThreshold");
        @IdRes int rIdCellSize = IdLookup.require("cellSize");
        @IdRes int rIdCreateGame = IdLookup.require("createGame");

        // Start the activity
        NewGameActivity targetActivity = Robolectric.buildActivity(NewGameActivity.class).create().start().resume().get();
        ShadowSupportMapFragment mapFragment = Shadow.extract(targetActivity.getSupportFragmentManager().findFragmentById(rIdTargetsMap));
        mapFragment.notifyMapReady();
        ShadowGoogleMap map = Shadow.extract(mapFragment.getMap());

        // Configure players
        LinearLayout playersList = targetActivity.findViewById(rIdPlayersList);
        View inviteeSection = playersList.getChildAt(0);
        Spinner teamSpinner = inviteeSection.findViewById(rIdInviteeTeam);
        teamSpinner.setSelection(TeamID.TEAM_RED);
        TextView newInviteeEmail = targetActivity.findViewById(rIdNewInviteeEmail);
        newInviteeEmail.setText("removed@example.com");
        Button addButton = targetActivity.findViewById(rIdAddInvitee);
        addButton.performClick();
        inviteeSection = playersList.getChildAt(1);
        inviteeSection.findViewById(rIdRemoveInvitee).performClick();
        newInviteeEmail.setText("someone@example.com");
        addButton.performClick();
        inviteeSection = playersList.getChildAt(1);
        teamSpinner = inviteeSection.findViewById(rIdInviteeTeam);
        teamSpinner.setSelection(TeamID.TEAM_GREEN);

        // Configure target mode
        RadioButton targetModeOption = targetActivity.findViewById(rIdTargetModeOption);
        targetModeOption.setChecked(true);
        LatLng firstPosition = new LatLng(40.112334, -88.226938);
        map.longPress(firstPosition);
        LatLng secondPosition = new LatLng(40.107532, -88.224715);
        map.longPress(secondPosition);
        LatLng thirdPosition = new LatLng(40.107466, -88.228180);
        map.longPress(thirdPosition);
        map.clickMarker(map.getMarkerAt(thirdPosition));
        TextView proximityThreshold = targetActivity.findViewById(rIdProximityThreshold);
        proximityThreshold.setText("25");

        // Create the game
        String gameId = RandomHelper.randomId();
        targetActivity.findViewById(rIdCreateGame).performClick();
        WebApiMocker.processOne("Pressing Create Game should send an API request",
                (path, method, body, callback, errorListener) -> {
                    // Check request
                    Assert.assertEquals("Incorrect API endpoint for game creation call", "/games/create", path);
                    Assert.assertEquals("The game creation call should be a POST request", Request.Method.POST, method);
                    Assert.assertNotNull("The game creation request should include the game configuration in the body", body);
                    JsonObject config = body.getAsJsonObject();

                    // Check game setup
                    Assert.assertTrue("The request should specify the game mode", config.has("mode"));
                    Assert.assertEquals("The mode should be 'target' for target mode",
                            "target", config.get("mode").getAsString());
                    for (String unneededProperty : new String[] {"cellSize", "areaNorth", "areaEast", "areaSouth", "areaWest"}) {
                        Assert.assertFalse(unneededProperty + " should not be specified for target mode", config.has(unneededProperty));
                    }
                    Assert.assertTrue("The request should specify the proximity threshold", config.has("proximityThreshold"));
                    Assert.assertEquals("Incorrect proximity threshold in POST body",
                            25, config.get("proximityThreshold").getAsInt());
                    Assert.assertTrue("The request should specify the targets for target mode", config.has("targets"));
                    JsonArray targets = config.getAsJsonArray("targets");
                    Assert.assertNotEquals("Removed targets should not be included in the request", 3, targets.size());
                    Assert.assertEquals("The targets array should have one object per target", 2, targets.size());
                    checkConfiguredTargetAt(targets, firstPosition);
                    checkConfiguredTargetAt(targets, secondPosition);
                    Assert.assertTrue("The request should specify the invitees", config.has("invitees"));
                    JsonArray players = config.getAsJsonArray("invitees");
                    Assert.assertNotEquals("Removed invitees should not be included in the POST body", 3, players.size());
                    Assert.assertEquals("Each invitee (including the user) should be one object in the invitees array",
                            2, players.size());
                    checkConfiguredInvitee(players, SampleData.USER_EMAIL, TeamID.TEAM_RED);
                    checkConfiguredInvitee(players, "someone@example.com", TeamID.TEAM_GREEN);

                    // Make sure the activity didn't end early
                    Assert.assertNull("No activity should be launched before the new game's ID is known",
                            Shadows.shadowOf(targetActivity).peekNextStartedActivity());
                    Assert.assertFalse("NewGameActivity shouldn't finish before the new game's ID is known",
                            targetActivity.isFinishing());

                    // Send the created-game response
                    JsonObject response = new JsonObject();
                    response.addProperty("game", gameId);
                    callback.onResponse(response);
                });

        // Make sure the game ID was passed to the game activity
        Intent intent = Shadows.shadowOf(targetActivity).peekNextStartedActivity();
        Assert.assertNotNull("GameActivity should be started when the game-created response is received", intent);
        Assert.assertEquals("The intent should launch GameActivity",
                intent.getComponent(), new ComponentName(targetActivity, GameActivity.class));
        Assert.assertTrue("The intent should include the game ID in the 'game' extra", intent.hasExtra("game"));
        Assert.assertEquals("Incorrect game ID in intent", gameId, intent.getStringExtra("game"));
        Assert.assertTrue("NewGameActivity should finish() after starting GameActivity", targetActivity.isFinishing());
        Shadows.shadowOf(targetActivity).clearNextStartedActivities();

        // Create the activity again
        NewGameActivity areaActivity = Robolectric.buildActivity(NewGameActivity.class).create().start().resume().get();
        mapFragment = Shadow.extract(areaActivity.getSupportFragmentManager().findFragmentById(rIdAreaSizeMap));
        mapFragment.notifyMapReady();
        map = Shadow.extract(mapFragment.getMap());

        // Configure players
        playersList = areaActivity.findViewById(rIdPlayersList);
        inviteeSection = playersList.getChildAt(0);
        teamSpinner = inviteeSection.findViewById(rIdInviteeTeam);
        teamSpinner.setSelection(TeamID.TEAM_YELLOW);
        newInviteeEmail = areaActivity.findViewById(rIdNewInviteeEmail);
        newInviteeEmail.setText("another@example.com");
        addButton = areaActivity.findViewById(rIdAddInvitee);
        addButton.performClick();
        inviteeSection = playersList.getChildAt(1);
        teamSpinner = inviteeSection.findViewById(rIdInviteeTeam);
        teamSpinner.setSelection(TeamID.OBSERVER);
        newInviteeEmail.setText("third@example.com");
        addButton.performClick();
        inviteeSection = playersList.getChildAt(2);
        teamSpinner = inviteeSection.findViewById(rIdInviteeTeam);
        teamSpinner.setSelection(TeamID.TEAM_BLUE);

        // Configure area mode
        RadioButton areaModeOption = areaActivity.findViewById(rIdAreaModeOption);
        areaModeOption.setChecked(true);
        LatLngBounds bounds = new LatLngBounds(new LatLng(40.076407, -88.209322), new LatLng(40.083721, -88.199968));
        map.setVisibleRegion(bounds);
        TextView cellSize = areaActivity.findViewById(rIdCellSize);
        cellSize.setText("30");

        // Check handling of network/server errors
        Button createGame = areaActivity.findViewById(rIdCreateGame);
        String[] errors = new String[] {"Connection timed out.", "third@example.com is not a registered player.", "Internal server error."};
        for (String error : errors) {
            createGame.performClick();
            WebApiMocker.processOne("Pressing Create Game should send an API request",
                    (path, method, body, callback, errorListener) -> {
                        errorListener.onErrorResponse(new VolleyError(error));
                        Assert.assertNotNull("Errors should be shown in a Toast", ShadowToast.getLatestToast());
                        Assert.assertEquals("The exact error message should be shown in the toast",
                                error, ShadowToast.getTextOfLatestToast());
                        Assert.assertNull("No activity should be started when the game couldn't be created",
                                Shadows.shadowOf(areaActivity).peekNextStartedActivity());
                    });
        }

        // Create the game
        areaActivity.findViewById(rIdCreateGame).performClick();
        WebApiMocker.processOne("Pressing Create Game should send an API request",
                (path, method, body, callback, errorListener) -> {
                    JsonObject config = body.getAsJsonObject();
                    Assert.assertTrue("The request should specify the game mode", config.has("mode"));
                    Assert.assertEquals("The mode should be 'area' for area mode",
                            "area", config.get("mode").getAsString());
                    for (String unneededProperty : new String[] {"proximityThreshold", "targets"}) {
                        Assert.assertFalse(unneededProperty + " should not be specified for area mode", config.has(unneededProperty));
                    }
                    for (String bound : new String[] {"North", "East", "South", "West"}) {
                        String property = "area" + bound;
                        Assert.assertTrue(bound + " boundary (" + property + " property) must be specified for area mode",
                                config.has(property));
                    }
                    Assert.assertEquals("Incorrect north boundary",
                            bounds.northeast.latitude, config.get("areaNorth").getAsDouble(), 1e-7);
                    Assert.assertEquals("Incorrect east boundary",
                            bounds.northeast.longitude, config.get("areaEast").getAsDouble(), 1e-7);
                    Assert.assertEquals("Incorrect south boundary",
                            bounds.southwest.latitude, config.get("areaSouth").getAsDouble(), 1e-7);
                    Assert.assertEquals("Incorrect west boundary",
                            bounds.southwest.longitude, config.get("areaWest").getAsDouble(), 1e-7);
                    Assert.assertTrue("Cell size must be specified for area mode", config.has("cellSize"));
                    Assert.assertEquals("Incorrect cell size in POST request",
                            30, config.get("cellSize").getAsInt());
                    Assert.assertTrue("The request should specify the invitees", config.has("invitees"));
                    JsonArray players = config.getAsJsonArray("invitees");
                    checkConfiguredInvitee(players, SampleData.USER_EMAIL, TeamID.TEAM_YELLOW);
                    checkConfiguredInvitee(players, "another@example.com", TeamID.OBSERVER);
                    checkConfiguredInvitee(players, "third@example.com", TeamID.TEAM_BLUE);
                });
    }

    private void checkConfiguredTargetAt(JsonArray targets, LatLng position) {
        for (JsonElement t : targets) {
            JsonObject target = t.getAsJsonObject();
            Assert.assertTrue("Each target should have a latitude", target.has("latitude"));
            Assert.assertTrue("Each target should have a longitude", target.has("longitude"));
            LatLng targetPos = new LatLng(target.get("latitude").getAsDouble(), target.get("longitude").getAsDouble());
            if (LatLngUtils.same(targetPos, position)) {
                return;
            }
        }
        Assert.fail("Incorrect target coordinates: no target found at " + position.latitude + ", " + position.longitude);
    }

    private void checkConfiguredInvitee(JsonArray invitees, String email, int team) {
        for (JsonElement i : invitees) {
            JsonObject invitee = i.getAsJsonObject();
            Assert.assertTrue("Each invitee should have an email", invitee.has("email"));
            Assert.assertTrue("Each invitee should have a team", invitee.has("team"));
            String inviteeEmail = invitee.get("email").getAsString();
            if (inviteeEmail.equals(email)) {
                Assert.assertEquals("Incorrect team for " + email, team, invitee.get("team").getAsInt());
                return;
            }
        }
        Assert.fail("Missing invitees entry for " + email);
    }

}
