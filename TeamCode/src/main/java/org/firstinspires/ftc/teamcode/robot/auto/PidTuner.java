package org.firstinspires.ftc.teamcode.robot.auto;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.PIDFCoefficients;

import org.firstinspires.ftc.teamcode.buttons.ButtonHandler;
import org.firstinspires.ftc.teamcode.buttons.PAD_BUTTON;
import org.firstinspires.ftc.teamcode.config.BOT;
import org.firstinspires.ftc.teamcode.driveto.AutoDriver;
import org.firstinspires.ftc.teamcode.field.Field;
import org.firstinspires.ftc.teamcode.robot.Robot;
import org.firstinspires.ftc.teamcode.utils.OrderedEnum;
import org.firstinspires.ftc.teamcode.utils.OrderedEnumHelper;
import org.firstinspires.ftc.teamcode.vuforia.VuforiaFTC;

//@Disabled
@com.qualcomm.robotcore.eventloop.opmode.Autonomous(name = "Pid", group = "Production")
public class PidTuner extends LinearOpMode {


    // Devices and subsystems
    private Robot robot = null;
    private VuforiaFTC vuforia = null;
    private ButtonHandler buttons;
    private NewAuto auto;
    private double increment = 0.1;
    // Runtime vars
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

        auto = new NewAuto("BL", "FR", hardwareMap);

        newPIDF = auto.getPIDFCoefficients();

        buttons = new ButtonHandler(robot);
        buttons.register("SELECT_PID", gamepad1, PAD_BUTTON.y);
        buttons.register("GO", gamepad1, PAD_BUTTON.a);
        buttons.register("UP", gamepad1, PAD_BUTTON.dpad_up);
        buttons.register("DOWN", gamepad1, PAD_BUTTON.dpad_down);
        buttons.register("INC_DOWN", gamepad1, PAD_BUTTON.dpad_left);
        buttons.register("INC_UP", gamepad1, PAD_BUTTON.dpad_right);


        // Process driver input
        while (!robot.gyro.isReady() && opModeIsActive()) {
            // Overall ready status
            gameReady = (robot.gyro.isReady());
            telemetry.addLine(gameReady ? "READY" : "NOT READY");

            // Detailed feedback
            telemetry.addData("Gyro", robot.gyro.isReady() ? "Ready" : "Calibrating…");

            // Update
            telemetry.update();
        }

        waitForStart();
        telemetry.clearAll();

        // Log if we didn't exit init as expected
        if (!gameReady) {
            telemetry.log().add("! STARTED BEFORE READY !");
        }


        while(opModeIsActive()){
            buttons.update();
            if (buttons.get("SELECT_PID")) selectedPid++;

            if (buttons.get("INC_DOWN")) increment *= 0.1;
            if (buttons.get("INC_UP")) increment *= 10;

            if (buttons.get("UP")) {
                switch (selectedPid % 3) {
                    case 0:
                        newPIDF.p += increment;
                        break;
                    case 1:
                        newPIDF.i += increment;
                        break;
                    case 2:
                        newPIDF.d += increment;
                        break;
                }
                auto.setPIDFCoefficients(newPIDF);
            }


            if (buttons.get("DOWN")) {
                switch (selectedPid % 3) {
                    case 0:
                        newPIDF.p -= increment;
                        break;
                    case 1:
                        newPIDF.i -= increment;
                        break;
                    case 2:
                        newPIDF.d -= increment;
                        break;
                }
                auto.setPIDFCoefficients(newPIDF);
            }
            if(buttons.get("GO")) auto.drive(48, 0.7f);
            telemetry.addData("Selected", selectedPid%3);
            telemetry.addData("Increment", increment);
            telemetry.addData("0 - P", newPIDF.p);
            telemetry.addData("1 - I", newPIDF.i);
            telemetry.addData("2 - D", newPIDF.d);
            telemetry.update();


        }


    }

}
