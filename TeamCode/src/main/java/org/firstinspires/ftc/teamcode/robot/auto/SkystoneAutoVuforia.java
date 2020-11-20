package org.firstinspires.ftc.teamcode.robot.auto;

import com.qualcomm.robotcore.eventloop.opmode.Disabled;
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


@com.qualcomm.robotcore.eventloop.opmode.Autonomous(name = "Skystone Side (Vuforia)", group = "Scissor")
@Disabled
public class SkystoneAutoVuforia extends OpMode {

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


    //Consts
    private static final float COLLECT_SPEED = 0.9f;

    private static final float ARM_HOME = 0.1f;
    private static final float ARM_OUT = 0.65f;
    private static final float CLAW_CLOSED = 0.6f;
    private static final float SMALL_OPEN = 0.35f;


    @Override
    public void init() {
        telemetry.addData(">", "Init…");
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
        //vuforia.start();
        //vuforia.enableCapture();

        // Register buttons
        buttons = new ButtonHandler(robot);
        buttons.register("SELECT_SIDE", gamepad1, PAD_BUTTON.y, BUTTON_TYPE.TOGGLE);
        buttons.register("AWAY_FROM_WALL", gamepad1, PAD_BUTTON.dpad_up);
        buttons.register("TOWARDS_WALL", gamepad1, PAD_BUTTON.dpad_down);
        buttons.register("CYCLE_SKYSTONE", gamepad1, PAD_BUTTON.x, BUTTON_TYPE.SINGLE_PRESS);

        // Move things to default positions
        robot.claw.setPosition(SMALL_OPEN);
        robot.capstone.setPosition(0.35f);

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
            telemetry.log().add("! STARTED BEFORE READY!");
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

        /*
         * Main State Machine
         * enum has descriptions of each state
         */
        switch (state) {
            case INIT:
                driver.done = false;
                robot.hookRight.max();
                robot.hookLeft.max();

                advance();
                break;

            case MOVE_OUT:
                driver.drive = common.drive.distance(InchesToMM(-24.0f));
                advance();
                break;

            case SPIN:
                if (color == Field.AllianceColor.RED)
                    driver.drive = common.drive.degrees(-90.0f);
                else
                    driver.drive = common.drive.degrees(90.0f);
                advance();
                break;

            case LOCATE_SKYSTONE:
                skystonePlacement = setSkystonePlacement();
                //skystonePlacement = 0;
                SkystoneOffset = 8 * skystonePlacement;
                advance();
                break;
                /*

            case ALIGN_WITH_SKYSTONE:
                driver.drive = common.drive.distance(InchesToMM(18.0f + SkystoneOffset));
                advance();
                break;

            case LOOK_AT_SKYSTONE:
                if (color == Field.AllianceColor.RED)
                    driver.drive = common.drive.degrees(45.0f);
                else
                    driver.drive = common.drive.degrees(-45.0f);
                advance();
                break;

            case EAT_SKYSTONE:
                robot.collectorLeft.setPower(COLLECT_SPEED);
                robot.collectorRight.setPower(COLLECT_SPEED);
                driver.drive = common.drive.distance(InchesToMM(-18.0f));
                advance();
                break;

            case MOVE_FORWARD:
                robot.collectorLeft.setPower(0);
                robot.collectorRight.setPower(0);
                driver.drive = common.drive.distance(InchesToMM(18.0f));
                advance();
                break;

            case CHOOSE_SIDE:
                if (stopByWall) {
                    float deg = 45;
                    if (color == Field.AllianceColor.BLUE) deg *= -1;
                    driver.drive = common.drive.degrees(deg);
                    driver.drive = common.drive.distance(InchesToMM(24.0f));
                    driver.drive = common.drive.degrees(-deg);
                }
                advance();
                break;

            case LOOK_AT_BRIDGE:
                driver.drive = common.drive.degrees(-45.0f);
                advance();
                break;

            case CROSS_BRIDGE:
                driver.drive = common.drive.distance(InchesToMM(24.0f));
                advance();
                break;

            case YEET_SKYSTONE:
                robot.claw.setPosition(CLAW_CLOSED);
                driver.drive = common.drive.sleep(1000);
                robot.flipper.setPosition(ARM_OUT);
                driver.drive = common.drive.sleep(3000);
                robot.claw.setPosition(SMALL_OPEN);
                driver.drive = common.drive.sleep(1000);
                robot.flipper.setPosition(ARM_HOME);
                driver.drive = common.drive.sleep(3000);
                advance();
                break;

            case PARK:
                driver.drive = common.drive.distance(InchesToMM(-12));
                */
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

        MOVE_OUT,

        SPIN,

        LOCATE_SKYSTONE,
        /*
        ALIGN_WITH_SKYSTONE, // Camera exactly 2 feet ahead of stone

        LOOK_AT_SKYSTONE,

        EAT_SKYSTONE,

        MOVE_FORWARD,

        LOOK_AT_BRIDGE,

        CHOOSE_SIDE,

        CROSS_BRIDGE,

        YEET_SKYSTONE,

        PARK,
         */

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

    public int setSkystonePlacement() {
        SkystoneAutoVuforia.Vu = new VuforiaFTC(hardwareMap, telemetry, BOT.SCISSOR);
        SkystoneAutoVuforia.Vu.init();
        SkystoneAutoVuforia.Vu.start();

        SkystoneAutoVuforia.Vu.enableCapture();

        if (SkystoneAutoVuforia.Vu.capturing()) {
            SkystoneAutoVuforia.Vu.capture();
            SkystoneAutoVuforia.Img = SkystoneAutoVuforia.Vu.getImage();

            if (!SkystoneAutoVuforia.Vu.isStale()) {
                SkystoneAutoVuforia.Img.savePNG("VuIMg");
                SkystoneAutoVuforia.Img.savePNGMyVo("VulMg2");

                int adj = 0;
                if (color == Field.AllianceColor.BLUE) {
                    adj = 280;
                }

                int[] startOfYellow = {-1, -1, -1};
                OOF:
                for (int i = SkystoneAutoVuforia.Img.getHeight(); i > 32; i--) {
                    for (int j = 0 + adj; j < SkystoneAutoVuforia.Img.getWidth(); j++) {
                        //first find the yellow
                        int[] c1 = {j, i};
                        int[] c2 = {j + 1046, i - 32};
                        int[] areaColor = SkystoneAutoVuforia.Img.hsl(c1, c2);
                        telemetry.addData("Color of area being measured h/s/l", areaColor);
                        if (areaColor[1] > 50) {
                            startOfYellow = areaColor;
                            break OOF;
                        }
                    }
                }
                System.out.println(startOfYellow);
            }
            SkystoneAutoVuforia.Vu.clearImage();

            SkystoneAutoVuforia.Vu.stop();
            return -2;
        } else {
            SkystoneAutoVuforia.Vu.clearImage();
            SkystoneAutoVuforia.Vu.stop();
            return -2;
        }
    }
}
