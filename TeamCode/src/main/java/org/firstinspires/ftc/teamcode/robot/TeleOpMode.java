package org.firstinspires.ftc.teamcode.robot;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.teamcode.buttons.BUTTON_TYPE;
import org.firstinspires.ftc.teamcode.buttons.ButtonHandler;
import org.firstinspires.ftc.teamcode.buttons.PAD_BUTTON;
import org.firstinspires.ftc.teamcode.config.BOT;
import org.firstinspires.ftc.teamcode.roadrunner.drive.StandardTrackingWheelLocalizer;
import org.firstinspires.ftc.teamcode.roadrunner.drive.TwoWheelTrackingLocalizer;
import org.firstinspires.ftc.teamcode.robot.auto.RingStackTF;
import org.firstinspires.ftc.teamcode.wheels.MOTOR_END;
import org.firstinspires.ftc.teamcode.wheels.MOTOR_SIDE;

@com.qualcomm.robotcore.eventloop.opmode.TeleOp(name = "TeleOp", group = "Prod")
public class TeleOpMode extends OpMode {

    // Devices and subsystems
    private Robot robot = null;
    private ButtonHandler buttons;
    private TwoWheelTrackingLocalizer odometry;
    private RingStackTF ringDetector;
    public int depot;

    private boolean controlLocked;
    private double shooterSpeed = 2590;

    //servo constants
    private static final float WGGripOpen = 0.45f;
    private static final float WGGripClosed = 0;

    private static final float MAGAZINE_UP = 0.1f;
    private static final float MAGAZINE_DOWN = 0.85f;

    private static final float COLLECT_MID = 0.55f;
    private static final float COLLECT_FULL = 0.32f;
    private static final float COLLECT_NO = 0.75f;


    private static final float FLIPPER_SHOOT = 0.85f;
    private static final float FLIPPER_IDLE = 0.62f;

    private static final float ARM_POS_OUT = 1;

    // other consts
    private static final float NORMAL_SPEED = 1.0f;
    private static final float SLOW_MODE = 0.35f;



    @Override
    public void init() {
        ringDetector = new RingStackTF(hardwareMap, telemetry);
        controlLocked = false;
        // Placate drivers
        telemetry.addData(">", "NOT READY");
        telemetry.update();

        // Init the common tasks elements
        robot = new Robot(hardwareMap, telemetry);
        robot.wheels.setTeleop(true);
        odometry = new TwoWheelTrackingLocalizer(hardwareMap);

        // Check robot
        if (robot.bot != BOT.PRODUCTION) {
            telemetry.log().add("Opmode not compatible with bot " + robot.bot);
            requestOpModeStop();
            return;
        }

        // Register buttons
        buttons = new ButtonHandler(robot);
        buttons.register("SLOW_MODE", gamepad1, PAD_BUTTON.b, BUTTON_TYPE.TOGGLE);
        buttons.register("REVERSE_COLLECTOR", gamepad1, PAD_BUTTON.y);
        buttons.register("FRONT_MID_COLLECT", gamepad1, PAD_BUTTON.left_bumper);
        buttons.register("BACK_MID_COLLECT", gamepad1, PAD_BUTTON.right_bumper);
        buttons.register("FRONT_FULL_COLLECT", gamepad1, PAD_BUTTON.left_trigger);
        buttons.register("BACK_FULL_COLLECT", gamepad1, PAD_BUTTON.right_trigger);
        buttons.register("TOGGLE_ARM", gamepad2, PAD_BUTTON.a, BUTTON_TYPE.TOGGLE);
        buttons.register("TOGGLE_GRIP", gamepad2, PAD_BUTTON.b, BUTTON_TYPE.TOGGLE);
        buttons.register("SHOOT", gamepad2, PAD_BUTTON.right_bumper);
        buttons.register("MANUAL_SHOOT", gamepad2, PAD_BUTTON.right_trigger);
        buttons.register("TOGGLE_MAGAZINE_POS", gamepad2, PAD_BUTTON.left_trigger, BUTTON_TYPE.TOGGLE);

        buttons.register("SHOOTER_SPEED_UP", gamepad2, PAD_BUTTON.dpad_up);
        buttons.register("SHOOTER_SPEED_DOWN", gamepad2, PAD_BUTTON.dpad_down);


        // Wait for the game to begin
        telemetry.addData(">", "Ready for game start");
        telemetry.update();

    }

    @Override
    public void init_loop() {
        depot = ringDetector.getDepot();
        telemetry.addData("Depot ", depot);
        telemetry.update();
    }

    @Override
    public void start() {
    }

    @Override
    public void loop() {
        // Update buttons
        buttons.update();
        // Update odometry
        odometry.update();

        // Move the robot


        if (!controlLocked) {
            driveBase();
            auxiliary();
        }

        telemetry.update();
    }

    private void driveBase() {
        if (buttons.get("SLOW_MODE")) {
            robot.wheels.setSpeedScale(SLOW_MODE);
        } else {
            robot.wheels.setSpeedScale(NORMAL_SPEED);
        }
        robot.wheels.loop(gamepad1);
    }

    private void auxiliary() {
        odometry.getPoseEstimate().getX();
        telemetry.addData("L:", robot.wheels.getEncoder(MOTOR_SIDE.LEFT, MOTOR_END.FRONT));
        telemetry.addData("R:", robot.wheels.getEncoder(MOTOR_SIDE.RIGHT, MOTOR_END.FRONT));
        telemetry.addData("V:", robot.shooter.getVelocity());
        telemetry.addData("S:", shooterSpeed);


        //CLAW
        if (buttons.get("TOGGLE_GRIP")) {
            robot.wobbleGoalGrip.setPosition(WGGripOpen);
        } else {
            robot.wobbleGoalGrip.setPosition(WGGripClosed);
        }
        //ARM
        if(buttons.get("TOGGLE_ARM")){
            robot.wobbleGoalArm.setTarget(-1300);
            robot.wobbleGoalArm.setPower(1);
        } else {
            robot.wobbleGoalArm.setTarget(0);
            robot.wobbleGoalArm.setPower(1);
        }

        //robot.wobbleGoalArm.setPower(gamepad2.right_stick_y);

        //COLLECT SAFEGUARD
        if(!buttons.get("TOGGLE_MAGAZINE_POS")) {

            // ==[FRONT COLLECTOR]==
            if (buttons.held("FRONT_FULL_COLLECT")) {
               robot.frontRaiseLower.setPosition(COLLECT_FULL);
                robot.collectorFront.setPower(1.0f);
            } else if (buttons.held("FRONT_MID_COLLECT")) {
                robot.frontRaiseLower.setPosition(COLLECT_MID);
                robot.collectorFront.setPower(1.0f);
            } else if (buttons.held("REVERSE_COLLECTOR")) {
                robot.frontRaiseLower.setPosition(COLLECT_MID);
            } else {
                robot.frontRaiseLower.setPosition(COLLECT_NO);
                robot.collectorFront.setPower(0.0f);
            }

            // ==[BACK COLLECTOR]==
            if (buttons.held("BACK_FULL_COLLECT")) {
                robot.backRaiseLower.setPosition(COLLECT_FULL);
                robot.collectorBack.setPower(1.0f);
            } else if (buttons.held("BACK_MID_COLLECT")) {
                robot.backRaiseLower.setPosition(COLLECT_MID);
                robot.collectorBack.setPower(1.0f);

            } else if (buttons.held("REVERSE_COLLECTOR")) {
                robot.backRaiseLower.setPosition(COLLECT_MID);
            } else {
                robot.backRaiseLower.setPosition(COLLECT_NO);
                robot.collectorBack.setPower(0.0f);
            }
        } else {
            robot.backRaiseLower.setPosition(COLLECT_NO);
            robot.collectorBack.setPower(0.0f);
            robot.frontRaiseLower.setPosition(COLLECT_NO);
            robot.collectorFront.setPower(0.0f);
        }

        if (buttons.held("REVERSE_COLLECTOR")) {
            robot.collectorFront.setPower(-1.0f);
            robot.collectorBack.setPower(-1.0f);
        }

        if (buttons.get("TOGGLE_MAGAZINE_POS")) {
            robot.queue.setPosition(MAGAZINE_UP);
            robot.shooter.setVelocity(shooterSpeed);
        } else {
            robot.queue.setPosition(MAGAZINE_DOWN);
            robot.shooter.setVelocity(0);
        }

        if (buttons.get("SHOOTER_SPEED_UP")) {
            shooterSpeed += 10;
            shooterSpeed = Math.min(3000, shooterSpeed);
        } else if (buttons.get("SHOOTER_SPEED_DOWN")) {
            shooterSpeed -= 10;
            shooterSpeed = Math.max(0, shooterSpeed);
        }
        if (buttons.held("MANUAL_SHOOT") && buttons.get("TOGGLE_MAGAZINE_POS")) {
            robot.queueFlipper.setPosition(FLIPPER_SHOOT);
            //controlLocked = true;
        } else {
            if(buttons.held("SHOOT") && robot.shooter.getVelocity() >= 2580 && robot.shooter.getVelocity() <= 2600) robot.queueFlipper.setPosition(FLIPPER_SHOOT);


            else robot.queueFlipper.setPosition(FLIPPER_IDLE);
        }
    }



    public void stop() {
    }
}