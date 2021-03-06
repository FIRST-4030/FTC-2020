package org.firstinspires.ftc.teamcode.calibration;

import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.teamcode.buttons.ButtonHandler;
import org.firstinspires.ftc.teamcode.buttons.PAD_BUTTON;
import org.firstinspires.ftc.teamcode.robot.Robot;
import org.firstinspires.ftc.teamcode.robot.calibration.*;

import java.util.Vector;

@Disabled
@TeleOp(name = "Calibration", group = "Test")
public class Calibration extends OpMode {
    private static final String NEXT_SUBSYSTEM = "NEXT_SUBSYSTEM";
    private static final String ESTOP = "ESTOP";

    // Devices & Buttons
    private Robot robot = null;
    private ButtonHandler buttons = null;

    // Subsystems
    private Subsystem current = null;
    private final Vector<Subsystem> subsystems = new Vector<>();

    @Override
    public void init() {
        robot = new Robot(hardwareMap, telemetry);
        buttons = new ButtonHandler(robot);

        // Our master switch
        buttons.register(NEXT_SUBSYSTEM, gamepad2, PAD_BUTTON.start);
        buttons.register(ESTOP, gamepad2, PAD_BUTTON.a);

        // Manual registration of subsystems
        // These subsystems will change year-to-year but the framework is bot-agnostic
        subsystems.add(new Distance(this, robot, buttons));
        subsystems.add(new Turns(this, robot, buttons));
        subsystems.add(new ShortDistance(this, robot, buttons));
        subsystems.add(new ShortTurns(this, robot, buttons));
    }

    @Override
    public void start() {
        // Select and activate the first subsystem
        current = next(current);
        current.activate();
    }

    @Override
    public void loop() {
        // Print the header
        telemetry.clear();
        telemetry.addData("Subsystem", current.name());

        // User input
        buttons.update();

        // Cycle through the registered subsystems
        if (buttons.get(NEXT_SUBSYSTEM)) {
            current.deactivate();
            telemetry.clearAll();
            current = next(current);
            current.activate();
        }
        if (buttons.get(ESTOP)) {
            robot.telemetry.log().add(ESTOP);
            if (current != null) {
                current.stop();
            }
        }

        // Do whatever the subsystem wants
        current.loop();

        // Publish
        telemetry.update();
    }

    private Subsystem next(Subsystem last) {
        Subsystem next;
        if (last == null) {
            next = subsystems.firstElement();
        } else {
            int i = subsystems.indexOf(last);
            i++;
            if (i >= subsystems.size()) {
                i = 0;
            }
            next = subsystems.elementAt(i);
        }
        return next;
    }
}
