package org.firstinspires.ftc.teamcode.robot.config;

import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.actuators.ServoFTC;
import org.firstinspires.ftc.teamcode.actuators.ServoConfig;
import org.firstinspires.ftc.teamcode.robot.SERVOS;
import org.firstinspires.ftc.teamcode.config.BOT;
import org.firstinspires.ftc.teamcode.config.Configs;

public class ServoConfigs extends Configs {
    public ServoConfigs(HardwareMap map, Telemetry telemetry, BOT bot) {
        super(map, telemetry, bot);
    }

    public ServoFTC init(SERVOS name) {
        ServoConfig config = config(name);
        super.checkConfig(config, name);
        ServoFTC servo = new ServoFTC(map, telemetry, config);
        super.checkAvailable(servo, name);
        return servo;
    }

    public ServoConfig config(SERVOS servo) {
        super.checkBOT();
        checkNull(servo, SERVOS.class.getName());
        ServoConfig config = null;
        switch (bot) {
            case PRODUCTION:
                switch(servo) {
                    case WOBBLE_GOAL_GRIP:
                        config = new ServoConfig("Wobble Goal Grip", false, 0.0f, 1.0f);
                        break;
                    case BACK_RAISE_LOWER:
                        config = new ServoConfig("Back Raise/Lower", false, 0.0f, 1.0f);
                        break;
                    case FRONT_RAISE_LOWER:
                        config = new ServoConfig("Front Raise/Lower",  false, 0.0f, 1.0f);
                        break;
                    case SHOOTER_AIM:
                        config = new ServoConfig("Shooter Aim", false, 0.0f, 1.0f);
                        break;
                    case QUEUE_FLIPPER:
                        config = new ServoConfig("Queue Flipper", false, 0.0f, 1.0f);
                }
                break;

            case ARM:
                switch (servo) {
                    case LOWER:
                        config = new ServoConfig("Lower", true, 0.0f, 1.0f);
                        break;
                    case UPPER:
                        config = new ServoConfig("Upper", false, 0.0f, 1.0f);
                        break;
                    case ROTATION:
                        config = new ServoConfig("Rotation", false, 0.0f, 1.0f);
                        break;
                    case CLAW:
                        config = new ServoConfig("Claw", false, 0.5f, 0.8f);
                        break;
                    case WRIST:
                        config = new ServoConfig("Swivel", false, 0.0f, 1.0f);
                }
            case BLANK:
                switch (servo) {
                    case SERVO_BOI:
                        config = new ServoConfig("Servo Boi", true, 0.0f, 1.0f);
                        break;
                }
        }

        return config;
    }
}
