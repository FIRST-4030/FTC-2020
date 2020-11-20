package org.firstinspires.ftc.teamcode.robot.auto;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;

import org.firstinspires.ftc.teamcode.buttons.BUTTON_TYPE;
import org.firstinspires.ftc.teamcode.buttons.ButtonHandler;
import org.firstinspires.ftc.teamcode.buttons.PAD_BUTTON;
import org.firstinspires.ftc.teamcode.config.BOT;
import org.firstinspires.ftc.teamcode.driveto.AutoDriver;
import org.firstinspires.ftc.teamcode.field.Field;
import org.firstinspires.ftc.teamcode.robot.Robot;
import org.firstinspires.ftc.teamcode.robot.common.Common;
import org.firstinspires.ftc.teamcode.utils.OrderedEnum;
import org.firstinspires.ftc.teamcode.utils.OrderedEnumHelper;
import org.firstinspires.ftc.teamcode.utils.Round;
import org.firstinspires.ftc.teamcode.vuforia.ImageFTC;
import org.firstinspires.ftc.teamcode.vuforia.VuforiaFTC;

@com.qualcomm.robotcore.eventloop.opmode.Autonomous(name = "Drive 2f", group = "Scissor")
public class SkystoneAutoBasic extends OpMode {

    // Devices and subsystems
    private Robot robot = null;
    private Common common = null;
    private VuforiaFTC vuforia = null;
    private ButtonHandler buttons;
    private AutoDriver driver = new AutoDriver();
    private static VuforiaFTC Vu = null;
    private static ImageFTC Img = null;

    // Runtime vars
    private AUTO_STATE state;
    private boolean gameReady = false;
    private Field.AllianceColor color = Field.AllianceColor.BLUE;
    private boolean stopByWall = true;
    private int skystonePlacement = 0;

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
        vuforia.start();
        vuforia.init();
        vuforia.enableCapture();

        // Register buttons
        buttons = new ButtonHandler(robot);
        buttons.register("SELECT_SIDE", gamepad1, PAD_BUTTON.y, BUTTON_TYPE.TOGGLE);
        buttons.register("AWAY_FROM_WALL", gamepad1, PAD_BUTTON.dpad_up);
        buttons.register("TOWARDS_WALL", gamepad1, PAD_BUTTON.dpad_down);
        buttons.register("CYCLE_SKYSTONE", gamepad1, PAD_BUTTON.x, BUTTON_TYPE.SINGLE_PRESS);

        // Move things to default positions
        robot.claw.setPosition(0.6f);
        robot.capstone.setPosition(0.4f);
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

        //tfod.activate();
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

        /*
         * Main State Machine
         * enum has descriptions of each state
         */
        switch (state) {
            case INIT:
                driver.done = false;
                advance();
                break;

            case CHOOSE_SIDE:
                if (!stopByWall) driver.drive = common.drive.distance(InchesToMM(35.0f));
                advance();
                break;

            case TURN:
                if (!stopByWall) {
                    if (color == Field.AllianceColor.BLUE) driver.drive = common.drive.heading(270);
                    else driver.drive = common.drive.heading(90);
                }
                advance();
                break;

            case MOVE:
                driver.drive = common.drive.distance(InchesToMM(24.0f));
                advance();
                break;


            case DONE:
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

        CHOOSE_SIDE,

        TURN,

        MOVE,

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
        while (img.rgb(w / 2, y) < 50) {

        }
        return pos;
    }


}
