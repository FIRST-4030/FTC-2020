package org.firstinspires.ftc.teamcode.robot.auto;
import com.qualcomm.robotcore.hardware.HardwareMap;

import java.util.List;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaCurrentGame;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaLocalizer;
import org.firstinspires.ftc.robotcore.external.tfod.Recognition;
import org.firstinspires.ftc.robotcore.external.tfod.TfodCurrentGame;

public class RingStackTF {

    private VuforiaCurrentGame vuforiaUltimateGoal;
    private TfodCurrentGame tfodUltimateGoal;

    Recognition recognition;

    List<Recognition> recognitions;
    double recLength;
    double index;

    /**
     * This function is executed when this Op Mode is selected from the Driver Station.
     */
    private HardwareMap map;
    Telemetry telemetry;

    public RingStackTF(HardwareMap m, Telemetry telem) {

        telemetry = telem;
        map = m;
        vuforiaUltimateGoal = new VuforiaCurrentGame();
        tfodUltimateGoal = new TfodCurrentGame();

        // Sample TFOD Op Mode
        // Initialize Vuforia.
        vuforiaUltimateGoal.initialize(
                "", // vuforiaLicenseKey
                map.get(WebcamName.class, "Webcam 1"), // cameraName
                "", // webcamCalibrationFilename
                false, // useExtendedTracking
                false, // enableCameraMonitoring
                VuforiaLocalizer.Parameters.CameraMonitorFeedback.AXES, // cameraMonitorFeedback
                0, // dx
                0, // dy
                0, // dz
                0, // xAngle
                0, // yAngle
                0, // zAngle
                true); // useCompetitionFieldTargetLocations
        // Set min confidence threshold to 0.7
        tfodUltimateGoal.initialize(vuforiaUltimateGoal, 0.7F, true, true);
        // Initialize TFOD before waitForStart.
        // Init TFOD here so the object detection labels are visible
        // in the Camera Stream preview window on the Driver Station.
        tfodUltimateGoal.activate();
        // Enable following block to zoom in on target.
        tfodUltimateGoal.setZoom(1.5, 16 / 9);
    }

    // Wait for start command from Driver Station.
    public int getDepot() {
        // Get a list of recognitions from TFOD.
        recognitions = tfodUltimateGoal.getRecognitions();
        // Put loop blocks here.
        // If list is empty, inform the user. Otherwise, go
        // through list and display info for each recognition.
        if (recognitions.size() == 0) {
            return 0;
        } else {
            index = 0;
            // Iterate through list and call a function to
            // display info for each recognized object.
            for (Recognition recognition_item : recognitions) {
                recognition = recognition_item;
                // Display info.
                telemetry.addData("label " + index, recognition.getLabel());
                if (recognition.getWidth() > 90) {
                    return 2;
                } else if (recognition.getWidth() < 60) {
                    return 1;
                } else {
                    telemetry.addData("Target Zone", "Unknown");
                }
                // Increment index.
                index = index + 1;
            }
            return 3;
        }

    }



    /**
     * Display info (using telemetry) for a recognized object.
     */
    private void displayInfo(double i) {
        // Display label info.
        // Display the label and index number for the recognition.
        telemetry.addData("label " + i, recognition.getLabel());
        if (recognition.getWidth() > 90) {
            telemetry.addData("Target Zone", "C");
        } else if (recognition.getWidth() < 60) {
            telemetry.addData("Target Zone", "B");
        } else {
            telemetry.addData("Target Zone", "Unknown");
        }
    }
}
