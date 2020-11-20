package org.firstinspires.ftc.teamcode.robot;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;

import org.firstinspires.ftc.teamcode.buttons.BUTTON_TYPE;
import org.firstinspires.ftc.teamcode.buttons.ButtonHandler;
import org.firstinspires.ftc.teamcode.buttons.PAD_BUTTON;
import org.firstinspires.ftc.teamcode.config.BOT;
//Hi
//hello
@com.qualcomm.robotcore.eventloop.opmode.TeleOp(name = "TeleOp - Prod", group = "Scissor")
public class TeleOpMode extends OpMode {

    // Devices and subsystems
    private Robot robot = null;
    private ButtonHandler buttons;

    // Changies
    private float armPos = ARM_HOME;
    private boolean initCapstone = false;
    private float CollectSpeed;

    // Drive Speeds
    private static final float SLOW_MODE = 0.7f;
    private static final float MED_SPEED = 0.9f;
    private static final float NORMAL_SPEED = 1.0f;

    // Claw positions
    private static final float CLAW_CLOSED = 0.6f;
    private static final float SMALL_OPEN = 0.35f; // Open pos inside robot
    private static final float BIG_OPEN = 0.0f; // Open pos outside robot
    private static final float BIG_MIN_POS = 0.6f; // If arm > this, claw can be fully open

    // Flipper arm speeds and position
    private static final float ARM_SPEED = 0.02f; // Pos increases by this per loop (abt 150 Hz)
    private static final float ARM_HOME = 0.02f;
    private static final float ARM_OUT = 0.65f;

    // Collector speed multiplier
    private static final float SLOW_COLLECT_SPEED = 0.25f;
    private static final float SPEEDY_COLLECT_SPEED = 0.45f;


    @Override
    public void init() {
        // Placate drivers
        telemetry.addLine("> NOT READY");
        telemetry.update();

        // Init the common tasks elements
        robot = new Robot(hardwareMap, telemetry);
        robot.wheels.setTeleop(true);

        // Check robot
        if (robot.bot != BOT.SCISSOR) {
            // Usually this message doesn't appear, but the opmode won't run
            telemetry.log().add("Opmode not compatible with bot " + robot.bot);
            requestOpModeStop();
        }

        // Register buttons
        // Gamepad one controls movement, collector, and foundation hooks
        buttons = new ButtonHandler(robot);
        buttons.register("COLLECT", gamepad1, PAD_BUTTON.a, BUTTON_TYPE.TOGGLE);
        buttons.register("FOUNDATION_HOOK", gamepad1, PAD_BUTTON.y, BUTTON_TYPE.TOGGLE);
        buttons.register("SLOW_MODE", gamepad1, PAD_BUTTON.b, BUTTON_TYPE.TOGGLE);
        buttons.register("CAPSTONE1", gamepad1, PAD_BUTTON.x);
        buttons.register("CAPSTONE_INIT", gamepad1, PAD_BUTTON.dpad_left, BUTTON_TYPE.SINGLE_PRESS);


        // Gamepad two controls lift and all of its parts
        buttons.register("ARM_RESET", gamepad2, PAD_BUTTON.b, BUTTON_TYPE.SINGLE_PRESS);
        buttons.register("ARM_OUT", gamepad2, PAD_BUTTON.a, BUTTON_TYPE.SINGLE_PRESS);
        buttons.register("ARM_TO_1", gamepad2, PAD_BUTTON.right_bumper);
        buttons.register("ARM_TO_0", gamepad2, PAD_BUTTON.left_bumper);
        buttons.register("GRAB", gamepad2, PAD_BUTTON.x, BUTTON_TYPE.TOGGLE);
        buttons.register("GRAB_WIDE", gamepad2, PAD_BUTTON.x);
        buttons.register("CAPSTONE2", gamepad2, PAD_BUTTON.y);
        buttons.register("CAPSTONE_UP", gamepad2, PAD_BUTTON.dpad_up, BUTTON_TYPE.TOGGLE);
        buttons.getListener("ARM_TO_0").setLongHeldTimeout(0);
        buttons.getListener("ARM_TO_1").setLongHeldTimeout(0);
        buttons.getListener("ARM_TO_0").setAutokeyTimeout(0);
        buttons.getListener("ARM_TO_1").setAutokeyTimeout(0);

        // Move things to default positions
        robot.flipper.setPosition(ARM_HOME);
        robot.claw.setPosition(SMALL_OPEN);
        robot.hookLeft.max();
        robot.hookRight.max();
        robot.wheels.setSpeedScale(NORMAL_SPEED);

        // Wait for the game to begin
        telemetry.addLine("> Ready for game start");
        telemetry.update();
    }

    @Override
    public void loop() {
        // Update buttons
        buttons.update();

        // Move the robot
        driveBase();
        auxiliary();

        // Update telemetry
        telemetry.addData("Capstone Init", initCapstone);
        telemetry.update();
    }

    /**
     * Runs drive base related functions
     */
    private void driveBase() {
        robot.wheels.loop(gamepad1);
    }

    /**
     * Runs pretty much everything that isn't wheels
     */
    private void auxiliary() {

        // ====
        // LIFT
        // ====
        // Capstone safety
        if (gamepad2.right_trigger > 0.0f || gamepad1.right_trigger > 0.0f) initCapstone = true;

        robot.lift.setPower(gamepad2.right_trigger - gamepad2.left_trigger);


        // =========
        // COLLECTOR
        // =========

            if (buttons.get("COLLECT")) {
                CollectSpeed = SPEEDY_COLLECT_SPEED;
            } else {
                CollectSpeed = SLOW_COLLECT_SPEED;
            }
            robot.collectorLeft.setPower((gamepad1.left_trigger - gamepad1.right_trigger) * CollectSpeed);
            robot.collectorRight.setPower((gamepad1.left_trigger - gamepad1.right_trigger) * CollectSpeed);



        // =============
        // FLIPPER / ARM
        // =============
        // Manual "analog" movement
        if (buttons.autokey("ARM_TO_0")) {
            armPos -= ARM_SPEED;
            initCapstone = true; // Capstone safety
        }
        if (buttons.autokey("ARM_TO_1")) {
            armPos += ARM_SPEED;
            initCapstone = true; // Capstone safety
        }

        //Homes arm inside the robot
        if (buttons.get("ARM_RESET")) {
            armPos = ARM_HOME;
            initCapstone = true; // Capstone safety
        }

        //Quickly moves arm into a decent position for collecting
        if (buttons.get("ARM_OUT")) {
            // This will break the capstone system if it isn't initialized
            if (initCapstone) armPos = ARM_OUT;
        }

        // Limit arm position
        armPos = Math.min(armPos, robot.flipper.getMax());
        armPos = Math.max(armPos, robot.flipper.getMin());

        // Move arm
        robot.flipper.setPosition(armPos);
        telemetry.addData("Arm Pos", robot.flipper.getPosition());


        // ====
        // CLAW
        // ====
        if (!buttons.get("GRAB")) {
            //Ensures the arm doesn't open wide enough to get stuck in the robot
            if (robot.flipper.getPosition() > BIG_MIN_POS && buttons.get("GRAB_WIDE")) {
                robot.claw.setPosition(BIG_OPEN);
            } else {
                robot.claw.setPosition(SMALL_OPEN);
            }
        } else {
            robot.claw.setPosition(CLAW_CLOSED);
        }


        // ========
        // CAPSTONE
        // ========
        // initCapstone should be set to true whenever anything involving the lift or arm is used
        if (buttons.get("CAPSTONE_INIT")) initCapstone = true; // Capstone safety

        // Nuclear launch codes
        if (buttons.held("CAPSTONE1") && buttons.held("CAPSTONE2")) {
            robot.capstone.min();
        } else {
            if (initCapstone) {
                if (buttons.get("CAPSTONE_UP")) {
                    robot.capstone.setPosition(0.5f);
                } else {
                    robot.capstone.max();
                }
            }
        }


        // ===========================
        // FOUNDATION HOOKS / SLOWMODE
        // ===========================
        if (buttons.get("FOUNDATION_HOOK")) {
            robot.hookLeft.min();
            robot.hookRight.min();

            robot.wheels.setSpeedScale(SLOW_MODE);
            telemetry.addLine("slow mode w/ hooks");
        } else if (buttons.get("SLOW_MODE")) {
            robot.hookLeft.max();
            robot.hookRight.max();

            robot.wheels.setSpeedScale(MED_SPEED);
            telemetry.addLine("medium mode");
        } else {
            robot.hookLeft.max();
            robot.hookRight.max();

            robot.wheels.setSpeedScale(NORMAL_SPEED);
            telemetry.addLine("normal mode");
        }
    }
}