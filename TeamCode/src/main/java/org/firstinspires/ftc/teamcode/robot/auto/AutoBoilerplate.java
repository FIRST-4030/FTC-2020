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
import org.firstinspires.ftc.teamcode.robot.RobotConstants;
import org.firstinspires.ftc.teamcode.robot.common.Common;
import org.firstinspires.ftc.teamcode.utils.OrderedEnum;
import org.firstinspires.ftc.teamcode.utils.OrderedEnumHelper;
import org.firstinspires.ftc.teamcode.utils.Round;
import org.firstinspires.ftc.teamcode.vuforia.VuforiaFTC;

//@Disabled
@com.qualcomm.robotcore.eventloop.opmode.Autonomous(name = "Auto", group = "Production")
public class AutoBoilerplate extends LinearOpMode implements RobotConstants {

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
        ringDetector = new RingStackTF(hardwareMap, telemetry);

        newPIDF = auto.getPIDFCoefficients();
        robot.frontRaiseLower.setPosition(COLLECT_NO);
        robot.backRaiseLower.setPosition(COLLECT_NO);
        robot.wobbleGoalGrip.setPosition(CLAW_CLOSED);
        sleep(350);
        robot.wobbleGoalArm.setPosition(ARM_IN);
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

        auto.drive(52, 1);

        auto.rotate(99, 1);

        robot.queue.setPosition(MAGAZINE_UP);
        robot.shooter.setVelocity(HIGH_SHOOTER_SPEED);
        sleep(1500);
        for(int i = 0; i < 3; i++){
            robot.queueFlipper.setPosition(FLIPPER_SHOOT);
            sleep(350);
            robot.queueFlipper.setPosition(FLIPPER_IDLE);
            sleep(1300);
        }
        robot.shooter.setVelocity(0);
        robot.queue.setPosition(MAGAZINE_DOWN);


        switch(depot){
            case 0:
                auto.rotate(-155, 1);
                auto.drive(7, 1);
                break;
            case 1:
                auto.rotate(-99, 1);
                auto.drive(34, 1);
                break;
            case 2:
                auto.rotate(-99, 1);
                auto.drive(55, 1);
                auto.rotate(-60, 1);
                auto.drive(4, 1);
                break;
        }
//woble goal ;)
        robot.wobbleGoalArm.setPosition(ARM_OUT);
        sleep(1000);
        robot.wobbleGoalGrip.setPosition(CLAW_OPEN);
        sleep(250);
        robot.wobbleGoalArm.setPosition(ARM_IN);
        sleep(500);

        switch(depot){
            case 0:
                auto.drive(-20, 1);
                auto.rotate(90, 1);
                auto.drive(25, 1);
                break;
            case 1:
                auto.drive(-14, 1);
                break;
            case 2:
                auto.drive(-55, 1);
                break;
        }


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
