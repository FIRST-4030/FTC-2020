package org.firstinspires.ftc.teamcode.robot.auto;

import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.PIDFCoefficients;

import org.firstinspires.ftc.robotcore.external.navigation.VuforiaCurrentGame;
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
import org.firstinspires.ftc.teamcode.vuforia.VuforiaFTC;

//@Disabled
@com.qualcomm.robotcore.eventloop.opmode.Autonomous(name = "Auto", group = "Production")
public class AutoBoilerplate extends LinearOpMode {

    private static final float FLIPPER_SHOOT = 0.85f;
    private static final float FLIPPER_IDLE = 0.62f;

    private static final float MAGAZINE_UP = 0.1f;
    private static final float MAGAZINE_DOWN = 0.85f;

    private static final double SHOOTER_SPEED = 2590;

    private static final float WGGripOpen = 0.45f;
    private static final float WGGripClosed = 0;
    private int depot;
    private RingStackTF ringDetector;

    // Devices and subsystems
    private Robot robot = null;
    private VuforiaFTC vuforia = null;
    private ButtonHandler buttons;
    private NewAuto auto;
    // Runtime vars
    private AUTO_STATE state;
    private boolean gameReady = false;
    private Field.AllianceColor color = Field.AllianceColor.BLUE;
    private boolean stopByWall = true;

    private PIDFCoefficients newPIDF;
    private int selectedPid = 0;

    public void runOpMode() {
        telemetry.addLine("Init…");
        telemetry.update();
        ringDetector = new RingStackTF(hardwareMap, telemetry);

        // Init the common tasks elements
        robot = new Robot(hardwareMap, telemetry);
        vuforia = robot.vuforia;

        // Check robot
        if (robot.bot != BOT.PRODUCTION) {
            telemetry.log().add("Opmode not compatible with bot " + robot.bot);
            requestOpModeStop();
        }

        // Init the camera system
        //vuforia.start();
        //vuforia.enableCapture();

        auto = new NewAuto("BL","FR", hardwareMap);

        newPIDF = auto.getPIDFCoefficients();

        robot.wobbleGoalGrip.setPosition(WGGripClosed);
        sleep(350);
        //robot.wobbleGoalArm.setTarget(1300);
        //while (!robot.wobbleGoalArm.onTarget()){
        //    robot.wobbleGoalArm.setPower(1);
        //}
        // Register buttons
        buttons = new ButtonHandler(robot);
        buttons.register("SELECT_PID", gamepad1, PAD_BUTTON.y);
        buttons.register("GO", gamepad1, PAD_BUTTON.a);
        buttons.register("UP", gamepad1, PAD_BUTTON.dpad_up);
        buttons.register("DOWN", gamepad1, PAD_BUTTON.dpad_down);

        robot.queue.setPosition(MAGAZINE_DOWN);

        // Process driver input
        userSettings();
        while(!robot.gyro.isReady() && opModeIsActive()) {
            // Overall ready status
            gameReady = (robot.gyro.isReady());
            telemetry.addLine(gameReady ? "READY" : "NOT READY");

            // Detailed feedback
            telemetry.addData("Gyro", robot.gyro.isReady() ? "Ready" : "Calibrating…");

            // Update
            telemetry.update();
        }

        waitForStart();

        // STUFF STARTS HAPPENING
        telemetry.clearAll();
        depot = ringDetector.getTargetZone();
        telemetry.addLine("Depot " + depot);
        telemetry.update();
        // Log if we didn't exit init as expected
        if (!gameReady) {
            telemetry.log().add("! STARTED BEFORE READY !");
        }

        //robot.vuforia.start();
        //robot.vuforia.enableCapture();

        auto.drive(52, 0.75f);

        auto.rotate(90, 0.3f);

        robot.queue.setPosition(MAGAZINE_UP);
        robot.shooter.setVelocity(SHOOTER_SPEED);
        sleep(1500);
        for(int i = 0; i < 3; i++){
            robot.queue.setPosition(MAGAZINE_DOWN);
            sleep(20);
            robot.queue.setPosition(MAGAZINE_UP);
            sleep(50);
            robot.queueFlipper.setPosition(FLIPPER_SHOOT);
            sleep(350);
            robot.queueFlipper.setPosition(FLIPPER_IDLE);
            sleep(1300);
        }
        robot.shooter.setVelocity(0);
        robot.queue.setPosition(MAGAZINE_DOWN);

        /*robot.wobbleGoalArm.setTarget(0);
        robot.wobbleGoalArm.setPower(1);*/
        switch(depot){
            case 0:
                auto.rotate(-130, 1);
                auto.drive(10, 1);
                break;
            case 1:
                auto.rotate(-85, 1);
                auto.drive(24, 1);
                break;
            case 2:
                auto.rotate(-110, 1);
                auto.drive(48, 1);
                break;
        }
//woble goal ;)

        /* while (!robot.wobbleGoalArm.onTarget() && opModeIsActive());
        robot.wobbleGoalGrip.setPosition(WGGripOpen);
        sleep(500);
        robot.wobbleGoalArm.setTarget(1300);
        robot.wobbleGoalArm.setPower(1); */

        switch(depot){
            case 0:
                auto.drive(-10, 1);
                auto.rotate(70, 1);
                auto.drive(28, 1);
                break;
            case 1:
                auto.drive(-14, 1);
                break;
            case 2:
                auto.drive(-38, 1);
                break;
        }

        //while(opModeIsActive() && !robot.wobbleGoalArm.onTarget());

        }

    /**
     * Defines the order of the auto routine steps
     */
    enum AUTO_STATE implements OrderedEnum {
        INIT, // Initialization
        PARK_ON_LINE,

        DONE;

        public AUTO_STATE prev() { return OrderedEnumHelper.prev(this); }
        public AUTO_STATE next() { return OrderedEnumHelper.next(this); }
    }

    /**
     * Sets config booleans according to user input
     */
    private void userSettings(){
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

}
