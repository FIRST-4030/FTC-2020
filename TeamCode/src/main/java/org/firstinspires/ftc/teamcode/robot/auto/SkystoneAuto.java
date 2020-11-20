package org.firstinspires.ftc.teamcode.robot.auto;

import android.graphics.Color;

import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;

import org.firstinspires.ftc.robotcore.external.matrices.OpenGLMatrix;
import org.firstinspires.ftc.robotcore.external.matrices.VectorF;
import org.firstinspires.ftc.robotcore.external.navigation.Orientation;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaTrackable;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaTrackableDefaultListener;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaTrackables;
import org.firstinspires.ftc.teamcode.buttons.BUTTON_TYPE;
import org.firstinspires.ftc.teamcode.buttons.ButtonHandler;
import org.firstinspires.ftc.teamcode.buttons.PAD_BUTTON;
import org.firstinspires.ftc.teamcode.config.BOT;
import org.firstinspires.ftc.teamcode.driveto.AutoDriver;
import org.firstinspires.ftc.teamcode.field.Field;
import org.firstinspires.ftc.teamcode.robot.Robot;
import org.firstinspires.ftc.teamcode.robot.common.Common;
import org.firstinspires.ftc.teamcode.robot.config.VuforiaFTCConfig;
import org.firstinspires.ftc.teamcode.utils.OrderedEnum;
import org.firstinspires.ftc.teamcode.utils.OrderedEnumHelper;
import org.firstinspires.ftc.teamcode.utils.Round;
import org.firstinspires.ftc.teamcode.vuforia.VuforiaFTC;
import org.firstinspires.ftc.teamcode.vuforia.ImageFTC;

import java.util.ArrayList;
import java.util.List;

import static org.firstinspires.ftc.robotcore.external.navigation.AngleUnit.DEGREES;
import static org.firstinspires.ftc.robotcore.external.navigation.AxesOrder.XYZ;
import static org.firstinspires.ftc.robotcore.external.navigation.AxesReference.EXTRINSIC;

@com.qualcomm.robotcore.eventloop.opmode.Autonomous(name = "Skystone Side", group = "Scissor")
public class SkystoneAuto extends OpMode {

    // Devices and subsystems
    private Robot robot = null;
    private Common common = null;
    private VuforiaFTC vuforia = null;
    private ButtonHandler buttons;
    private AutoDriver driver = new AutoDriver();
    private static VuforiaFTC Vu = null;
    private static ImageFTC Img = null;
    private VuforiaTrackables targetsSkyStone = null;
    List<VuforiaTrackable> allTrackables = null;

    // Runtime vars
    private AUTO_STATE state;
    private boolean gameReady = false;
    private Field.AllianceColor color = Field.AllianceColor.BLUE;
    private boolean stopByWall = true;
    private int skystonePlacement = 0;
    private OpenGLMatrix lastLocation = null;
    private boolean targetVisible = false;

    //Consts
    private static final float COLLECT_SPEED = 0.9f;
    private static final float mmPerInch = 25.4f;
    private static final float ARM_HOME = 0.1f;
    private static final float ARM_OUT = 0.65f;
    private static final float CLAW_CLOSED = 0.6f;
    private static final float SMALL_OPEN = 0.35f;

    @Override
    public void init() {
        telemetry.addLine("Init…");
        telemetry.update();

        // Init the common tasks elements
        robot = new Robot(hardwareMap, telemetry);
        common = robot.common;
        vuforia = robot.vuforia;

        // Check robot
        if (robot.bot != BOT.SCISSOR) {
            telemetry.log().add("Opmode not compatible with bot " + robot.bot);
            requestOpModeStop();
        }

        // Init the camera system
        targetsSkyStone = new VuforiaFTCConfig().init(hardwareMap);
        allTrackables = new ArrayList<VuforiaTrackable>();
        allTrackables.addAll(targetsSkyStone);
        targetsSkyStone.activate();

        // Register buttons
        buttons = new ButtonHandler(robot);
        buttons.register("SELECT_SIDE", gamepad1, PAD_BUTTON.y, BUTTON_TYPE.TOGGLE);
        buttons.register("AWAY_FROM_WALL", gamepad1, PAD_BUTTON.dpad_up);
        buttons.register("TOWARDS_WALL", gamepad1, PAD_BUTTON.dpad_down);
        buttons.register("CYCLE_SKYSTONE", gamepad1, PAD_BUTTON.x, BUTTON_TYPE.SINGLE_PRESS);

        // Move things to default positions
        robot.claw.setPosition(SMALL_OPEN);
        robot.hookRight.max();
        robot.hookLeft.max();

        telemetry.update();
    }

    @Override
    public void init_loop() {
        // Process driver input
        userSettings();

        // Overall ready status
        gameReady = (robot.gyro.isReady());
        telemetry.addLine(gameReady ? "READY" : "NOT READY");

        // Detailed feedback
        telemetry.addData("Gyro", robot.gyro.isReady() ? "Ready" : "Calibrating…");

        //Skystone Placement
        telemetry.addData("Skystone", "" + skystonePlacement);

        // Update
        telemetry.update();
    }

    @Override
    public void start() {
        telemetry.clearAll();

        // Log if we didn't exit init as expected
        if (!gameReady) {
            telemetry.log().add("! STARTED BEFORE READY !");
        }

        // Set initial state
        state = AUTO_STATE.values()[0];

        //robot.vuforia.start();
        //robot.vuforia.enableCapture();
    }

    @Override
    public void loop() {
        // Handle AutoDriver driving
        driver = common.drive.loop(driver);
        float SkystoneOffset = 0;
        // Debug feedback
        telemetry.addData("State", state);
        telemetry.addData("Running", driver.isRunning(time));
        telemetry.addData("Gyro", Round.truncate(robot.gyro.getHeading()));
        telemetry.addData("Encoder", robot.wheels.getEncoder());

        // Cut the loop short while AutoDriver is driving
        // This prevents the state machine from running before the preceding state is complete
        if (driver.isRunning(time)) return;

        //Vuforia stuff
        targetVisible = false;
        for (VuforiaTrackable trackable : allTrackables) {
            if (((VuforiaTrackableDefaultListener) trackable.getListener()).isVisible()) {
                telemetry.addData("Visible Target", trackable.getName());
                targetVisible = true;

                // getUpdatedRobotLocation() will return null if no new information is available since
                // the last time that call was made, or if the trackable is not currently visible.
                OpenGLMatrix robotLocationTransform = ((VuforiaTrackableDefaultListener) trackable.getListener()).getUpdatedRobotLocation();
                if (robotLocationTransform != null) {
                    lastLocation = robotLocationTransform;
                }
                break;
            }
        }

        robot.capstone.setPosition(0.35f);
        float y = 0;
        // Provide feedback as to where the robot is located (if we know).
        if (targetVisible) {
            // express position (translation) of robot in inches.
            VectorF translation = lastLocation.getTranslation();
            telemetry.addData("Pos (in)", "{X, Y, Z} = %.1f, %.1f, %.1f",
                    translation.get(0) / mmPerInch, translation.get(1) / mmPerInch, translation.get(2) / mmPerInch);
            y = translation.get(1);
            // express the rotation of the robot in degrees.
            Orientation rotation = Orientation.getOrientation(lastLocation, EXTRINSIC, XYZ, DEGREES);
            telemetry.addData("Rot (deg)", "{Roll, Pitch, Heading} = %.0f, %.0f, %.0f", rotation.firstAngle, rotation.secondAngle, rotation.thirdAngle);
        } else {
            telemetry.addData("Visible Target", "none");
        }
        telemetry.update();

        /*
         * Main State Machine
         * enum has descriptions of each state
         */
        switch (state) {
            case INIT:
                driver.done = false;
                advance();
                break;



/*
            case LOCATE_SKYSTONE:
                // 1: Bridge
                // 0: Middle
                //-1: Wall
                if (y < 0) {
                    skystonePlacement = 1;
                } else if (y > 0) {
                    skystonePlacement = 0;
                } else {
                    skystonePlacement = -1;
                }

                advance();
                break;
*/





            case MOVE_TO_SKYSTONE:
                driver.drive = common.drive.distance(InchesToMM(24.0f));
                advance();
                break;

            case INCH:
                driver.drive = common.drive.distance(InchesToMM(6.0f));
                advance();
                break;

            case BACK:
                driver.drive = common.drive.distance(InchesToMM(-3.2f));
                advance();
                break;

            case EAT_SKYSTONE:
                robot.hookLeft.min();
                driver.drive = common.drive.sleep(700);
                advance();
                break;





            case CHOOSE_SIDE:
                if (stopByWall) {
                    driver.drive = common.drive.distance(InchesToMM(-25.0f));
                } else {
                    driver.drive = common.drive.distance(InchesToMM(-7.0f));
                }
                advance();
                break;

            case LOOK_AT_BRIDGE:
                float deg = 265;
                if (color == Field.AllianceColor.RED) deg = 85;
                driver.drive = common.drive.heading(deg);
                advance();
                break;

            case CROSS_BRIDGE:
                driver.drive = common.drive.distance(InchesToMM(43.0f));
                advance();
                break;

            case YEET_SKYSTONE:
                robot.hookLeft.max();
                driver.drive = common.drive.sleep(500);
                advance();
                break;

            case CROSS_BRIDGE_AGAIN:
                driver.drive = common.drive.distance(InchesToMM(-56.0f));
                advance();
                break;


            case LOOK_AT_STONE:
                driver.drive = common.drive.heading(0);
                advance();
                break;

            case DRIVE_TO_STONE:
                if (stopByWall) {
                    driver.drive = common.drive.distance(InchesToMM(18.0f));
                } else {
                    //driver.drive = common.drive.distance(InchesToMM(18.0f));
                }

                advance();
                break;




            case INCH2:
                driver.drive = common.drive.distance(InchesToMM(7.5f));
                advance();
                break;

            case BACK2:
                driver.drive = common.drive.distance(InchesToMM(-3.2f));
                advance();
                break;

            case EAT_SKYSTONE2:
                robot.hookLeft.min();
                driver.drive = common.drive.sleep(700);
                advance();
                break;




            case CHOOSE_SIDE2:
                if (stopByWall) {
                    driver.drive = common.drive.distance(InchesToMM(-26.0f));
                } else {
                    driver.drive = common.drive.distance(InchesToMM(-7.0f));
                }
                advance();
                break;

            case LOOK_AT_BRIDGE2:
                deg = 265;
                if (color == Field.AllianceColor.RED) deg = 95;
                driver.drive = common.drive.heading(deg);
                advance();
                break;

            case CROSS_BRIDGE2:
                driver.drive = common.drive.distance(InchesToMM(51.0f));
                advance();
                break;

            case YEET_SKYSTONE2:
                robot.hookLeft.max();
                driver.drive = common.drive.sleep(500);
                advance();
                break;

            case PARK:
                driver.drive = common.drive.distance(InchesToMM(-14.0f));
                advance();
                break;

            case DONE:
                targetsSkyStone.deactivate();
                driver.done = true;
                break;
        }

        // Update telemetry
        telemetry.update();
    }

    /**
     * Defines the order of the auto routine steps
     */
    enum AUTO_STATE implements OrderedEnum {
        INIT, // Initialization



        MOVE_TO_SKYSTONE,


        INCH,

        BACK,

        EAT_SKYSTONE,

        CHOOSE_SIDE,

        LOOK_AT_BRIDGE,

        CROSS_BRIDGE,

        YEET_SKYSTONE,

        CROSS_BRIDGE_AGAIN,

        LOOK_AT_STONE,

        DRIVE_TO_STONE,

        INCH2,

        BACK2,

        EAT_SKYSTONE2,

        CHOOSE_SIDE2,

        LOOK_AT_BRIDGE2,

        CROSS_BRIDGE2,

        YEET_SKYSTONE2,

        PARK,

        DONE;

        public AUTO_STATE prev() {
            return OrderedEnumHelper.prev(this);
        }

        public AUTO_STATE next() {
            return OrderedEnumHelper.next(this);
        }
    }

    /**
     * Sets config booleans according to user input
     */
    private void userSettings() {
        buttons.update();

        if (buttons.get("SELECT_SIDE")) {
            color = Field.AllianceColor.RED;
        } else {
            color = Field.AllianceColor.BLUE;
        }

        telemetry.addData("Team Color", color.toString());

        if (buttons.get("AWAY_FROM_WALL")) stopByWall = false;
        if (buttons.get("TOWARDS_WALL")) stopByWall = true;
        telemetry.addData("Stop by wall?", stopByWall);

        if (buttons.get("CYCLE_SKYSTONE")) {
            skystonePlacement++;
            if (skystonePlacement == 2) skystonePlacement = -1;
        }
    }

    /**
     * Utility function to delegate AutoDriver to an external provider
     * AutoDriver is handed back up to caller when the delegate sets done to true
     *
     * @param autoDriver AutoDriver to be delegated
     * @return AutoDriver once delegate finishes
     */
    private AutoDriver delegateDriver(AutoDriver autoDriver) {
        if (autoDriver.isDone()) {
            autoDriver.done = false;
        }

        return autoDriver;
    }

    /**
     * does what it says on the tin
     *
     * @param inches inches
     * @return those inches but in millimeters
     */
    private int InchesToMM(float inches) {
        return (int) (inches * 25.4);
    }

    /**
     * just does state = state.next()
     * i don't want to keep writing that out
     */
    private void advance() {
        state = state.next();
    }

    private int getSkystonePosition() {
        int pos = 0;
        vuforia.capture();
        ImageFTC img = vuforia.getImage();
        int h = img.getHeight();
        int w = img.getWidth();
        int y = h;
        while (Color.green(img.rgb(w / 2, y)) < 50) {
            y--;
        }
        y -= 50;
        int g0 = Color.green(img.rgb(w / 4, y));
        int g1 = Color.green(img.rgb(w / 2, y));
        int g2 = Color.green(img.rgb(2 * w / 4, y));
        telemetry.addData("-1,0,1", g0 + "," + g1 + "," + g2);
        return pos;
    }


}
