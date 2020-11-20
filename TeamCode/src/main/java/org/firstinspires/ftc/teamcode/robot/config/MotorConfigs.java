package org.firstinspires.ftc.teamcode.robot.config;

import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.actuators.Motor;
import org.firstinspires.ftc.teamcode.actuators.MotorConfig;
import org.firstinspires.ftc.teamcode.robot.MOTORS;
import org.firstinspires.ftc.teamcode.config.BOT;
import org.firstinspires.ftc.teamcode.config.Configs;

public class MotorConfigs extends Configs {
    public MotorConfigs(HardwareMap map, Telemetry telemetry, BOT bot) {
        super(map, telemetry, bot);

    }

    public Motor init(MOTORS name) {
        MotorConfig config = config(name);
        super.checkConfig(config, name);
        Motor motor = new Motor(map, telemetry, config);
        super.checkAvailable(motor, name);
        return motor;
    }

    public MotorConfig config(MOTORS motor) {
        super.checkBOT();
        checkNull(motor, MOTORS.class.getName());

        MotorConfig config = null;
        switch (bot) {

            // IMPORTANT: If you need to change the *names* of the motors here, change them in PIDMotorConfigs too
            case PRODUCTION:
                //DELETE THIS ANNIE YOU FORGETFUL FUCK:
                //SHOOTER, COLLECTOR_BACK, COLLECTOR_FRONT, WOBBLE_GOAL_ARM
                switch(motor) {
                    case SHOOTER:
                        config = new MotorConfig("Shooter", true, true);
                        break;
                    case COLLECTOR_BACK:
                        config = new MotorConfig("Back Collector", true, true);
                        break;
                    case COLLECTOR_FRONT:
                        config = new MotorConfig("Front Collector", true, true);
                        break;
                    case WOBBLE_GOAL_ARM:
                        config = new MotorConfig("Wobble Goal Arm", true, true);
                }
                break;
            case BLANK:
                switch(motor) {
                    case MOTORY_BOI:
                        config = new MotorConfig("Motory Boi", true, true);
                        break;
                }
        }
        return config;
    }
}
