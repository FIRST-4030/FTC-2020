package org.firstinspires.ftc.teamcode.robot.common;

import org.firstinspires.ftc.teamcode.driveto.AutoDriver;
import org.firstinspires.ftc.teamcode.driveto.DriveTo;
import org.firstinspires.ftc.teamcode.driveto.DriveToListener;
import org.firstinspires.ftc.teamcode.driveto.DriveToParams;
import org.firstinspires.ftc.teamcode.driveto.PIDParams;
import org.firstinspires.ftc.teamcode.robot.Robot;
import org.firstinspires.ftc.teamcode.utils.Heading;
import org.firstinspires.ftc.teamcode.utils.OrderedEnum;
import org.firstinspires.ftc.teamcode.utils.OrderedEnumHelper;
import org.firstinspires.ftc.teamcode.utils.Round;
import org.firstinspires.ftc.teamcode.wheels.MOTOR_SIDE;

public class Drive implements CommonTask, DriveToListener {
    private static final boolean DEBUG = true;

    // ===================
    // PID TURN PARAMETERS
    // ===================
    // Cutoff between short and long turns (in degrees)
    private static final float TURN_CUTOFF = 90.0f;

    // Short turns
    private static final float SHORT_TURN_TOLERANCE = 1.0f; // Permitted heading error in degrees
    private static final float SHORT_TURN_DIFF_TOLERANCE = 0.001f; // Permitted error change rate
    public static final PIDParams SHORT_TURN_PARAMS = new PIDParams(0.009f, 0.01f, 2.1f,
            40.0f, true, true);

    // Long turns
    private static final float TURN_TOLERANCE = 3.0f; // Permitted heading error in degrees
    private static final float TURN_DIFF_TOLERANCE = 0.01f; // Permitted error change rate
    public static final PIDParams TURN_PARAMS = new PIDParams(0.009f, 0.012f, 2.0f,
            40.0f, true, true);

    // ====================
    // PID DRIVE PARAMETERS
    // ====================
    // Cutoff between short and long drives (in mm)
    private static final float DRIVE_CUTOFF = 12.0f * 25.4f;

    // Short drives
    private static final float SHORT_DRIVE_TOLERANCE = 7.0f; // Permitted distance error in encoder ticks
    private static final float SHORT_DRIVE_DIFF_TOLERANCE = 0.1f; // Permitted error change rate
    public static final PIDParams SHORT_DRIVE_PARAMS = new PIDParams(0.002f, 0.004f, 0.8f,
            150.0f, true, true);

    // Long drives
    private static final float DRIVE_TOLERANCE = 15.0f; // Permitted distance error in encoder ticks
    private static final float DRIVE_DIFF_TOLERANCE = 0.1f; // Permitted error change rate
    public static final PIDParams DRIVE_PARAMS = new PIDParams(0.02f, 0.00023f, 1.2f,
            1000.0f, true, true);

    // ==========================
    // PID TRANSLATION PARAMETERS
    // ==========================
    private static final float TRANSL_TOLERANCE = 20.0f; // Permitted distance error in encoder ticks
    private static final float TRANSL_DIFF_TOLERANCE = 0.1f; // Permitted error change rate
    public static final PIDParams TRANSL_PARAMS = new PIDParams(0.0182f, 0.00028f, 0.754f,
            2000.0f, true, true);

    // Runtime
    private final Robot robot;

    private HEADING_DIST_STATE headingDistState = HEADING_DIST_STATE.INIT;
    private HEADING_DIST_HEADING_STATE headingDistHeadingState = HEADING_DIST_HEADING_STATE.INIT;


    public Drive(Robot robot) {
        this.robot = robot;
    }

    public AutoDriver loop(AutoDriver driver) {
        if (driver.drive != null) {
            driver.drive.drive();

            // Remember timeouts (until the next drive())
            driver.timeout = driver.drive.isTimeout();

            // Cancel AutoDrive when we're done
            if (driver.drive.isDone()) {
                driver.drive = null;
                robot.wheels.setTeleop(true);
            }
        }
        return driver;
    }

    // Sensor reference types for our DriveTo callbacks
    public enum SENSOR_TYPE {
        DRIVE_ENCODER,
        REVHUB_ENCODER,
        GYROSCOPE,
        TIME,
        TIME_TURN
    }

    public DriveTo sleep(int mills) {
        return time(mills, 0.0f);
    }

    public DriveTo time(int mills, float speed) {
        return time(mills, speed, false);
    }

    public DriveTo timeTurn(int mills, float speed) {
        return time(mills, speed, true);
    }

    private DriveTo time(int mills, float speed, boolean turn) {
        robot.wheels.setTeleop(false);

        SENSOR_TYPE type = turn ? SENSOR_TYPE.TIME_TURN : SENSOR_TYPE.TIME;
        DriveToParams param = new DriveToParams(this, type);
        param.greaterThan(1);
        param.limitRange = speed;
        param.timeout = Math.abs(mills);
        return new DriveTo(new DriveToParams[]{param});
    }

    public DriveTo translate(int millimeters) {
        robot.wheels.setTeleop(false);

        DriveToParams param = new DriveToParams(this, SENSOR_TYPE.DRIVE_ENCODER);
        int target = (int) ((float) millimeters * robot.wheels.getTranslationTicksPerMM()) + robot.wheels.getEncoder();
        param.translationPid(target, TRANSL_PARAMS, TRANSL_TOLERANCE, TRANSL_DIFF_TOLERANCE);
        return new DriveTo(new DriveToParams[]{param});
    }

    public DriveTo distance(int millimeters, float speed) {
        robot.wheels.setTeleop(false);

        DriveToParams param = new DriveToParams(this, SENSOR_TYPE.REVHUB_ENCODER);
        int target = (int) ((float) millimeters * robot.wheels.getTicksPerMM()) + robot.wheels.getEncoder();
        param.timeout = target;
        param.limitRange = speed;
        return new DriveTo(new DriveToParams[]{param});
    }

    public DriveTo distance(int millimeters) {
        robot.wheels.setTeleop(false);

        DriveToParams param = new DriveToParams(this, SENSOR_TYPE.DRIVE_ENCODER);
        int target = (int) ((float) millimeters * robot.wheels.getTicksPerMM()) + robot.wheels.getEncoder();
        param.timeout = DriveTo.TIMEOUT_DEFAULT;

        // Short vs long
        if (millimeters > DRIVE_CUTOFF) {
            param.pid(target, DRIVE_PARAMS, DRIVE_TOLERANCE, DRIVE_DIFF_TOLERANCE);
        } else {
            param.pid(target, SHORT_DRIVE_PARAMS, SHORT_DRIVE_TOLERANCE, SHORT_DRIVE_DIFF_TOLERANCE);
        }

        return new DriveTo(new DriveToParams[]{param});
    }

    public DriveTo heading(float heading) {
        robot.wheels.setTeleop(false);
        heading = Heading.normalize(heading);

        DriveToParams param = new DriveToParams(this, SENSOR_TYPE.GYROSCOPE);

        // Short vs long
        if (heading - robot.gyro.getHeading() > TURN_CUTOFF) {
            param.rotationPid(heading, TURN_PARAMS, TURN_TOLERANCE, TURN_DIFF_TOLERANCE);
        } else {
            param.rotationPid(heading, SHORT_TURN_PARAMS, SHORT_TURN_TOLERANCE, SHORT_TURN_DIFF_TOLERANCE);
        }

        return new DriveTo(new DriveToParams[]{param});
    }

    public DriveTo degrees(float degrees) {
        return heading(degrees + robot.gyro.getHeading());
    }

    @Override
    public void driveToStop(DriveToParams param) {
        if (DEBUG && param.comparator.pid()) {
            robot.telemetry.log().add("T(" + Round.truncate(param.pid.target) +
                    ") | E/A/D\t | " + Round.truncate(param.pid.error) +
                    "\t | " + Round.truncate(param.pid.accumulated) +
                    "\t | " + Round.truncate(param.pid.differential) +
                    "\t | (" + Round.truncate(param.pid.output()) + ")");
        }
        switch ((SENSOR_TYPE) param.reference) {
            case DRIVE_ENCODER:
            case REVHUB_ENCODER:
            case GYROSCOPE:
            case TIME:
            case TIME_TURN:
                robot.wheels.stop();
                break;
            default:
                robot.wheels.stop();
                throw new IllegalStateException("Unhandled driveToStop: " +
                        param.reference + " ::" + param.comparator);
        }
    }

    @Override
    public float driveToSensor(DriveToParams param) {
        float value;
        switch ((SENSOR_TYPE) param.reference) {
            case DRIVE_ENCODER:
                value = robot.wheels.getEncoder();
                break;
            case REVHUB_ENCODER:
                value = robot.wheels.onTarget() ? 1.0f : 0.0f;
                break;
            case GYROSCOPE:
                value = robot.gyro.getHeading();
                break;
            case TIME:
            case TIME_TURN:
                value = 0;
                break;
            default:
                throw new IllegalStateException("Unhandled driveToSensor: " +
                        param.reference + " ::" + param.comparator);
        }
        return value;
    }

    @Override
    public void driveToRun(DriveToParams param) {
        float speed;
        if (DEBUG && param.comparator.pid()) {
            robot.telemetry.log().add("T(" + Round.truncate(param.pid.target) +
                    ") | E/A/D\t | " + Round.truncate(param.pid.error) +
                    "\t | " + Round.truncate(param.pid.accumulated) +
                    "\t | " + Round.truncate(param.pid.differential) +
                    "\t | (" + Round.truncate(param.pid.output()) + ")");
        }
        switch ((SENSOR_TYPE) param.reference) {
            case REVHUB_ENCODER:
                if (!robot.wheels.isPositionPID()) {
                    robot.wheels.setPositionPID(true);
                    robot.wheels.setTarget(param.timeout);
                }
                robot.wheels.setSpeed(param.limitRange);
                break;
            case DRIVE_ENCODER:
                speed = param.pid.output();
                switch (param.comparator) {
                    case PID:
                        robot.wheels.setSpeed(speed);
                        break;
                    case TRANSLATION_PID:
                        robot.wheels.setSpeed(speed, 0, 0);
                        break;
                    default:
                        throw new IllegalStateException("Unhandled driveToRun: " +
                                param.reference + "::" + param.comparator);
                }
                break;
            case GYROSCOPE:
                switch (param.comparator) {
                    case ROTATION_PID:
                        speed = param.pid.output();
                        // Left spins forward when heading is increasing
                        robot.wheels.setSpeed(speed, MOTOR_SIDE.LEFT);
                        robot.wheels.setSpeed(-speed, MOTOR_SIDE.RIGHT);
                        break;
                    default:
                        throw new IllegalStateException("Unhandled driveToRun: " +
                                param.reference + "::" + param.comparator);
                }
                break;
            case TIME:
                robot.wheels.setSpeed(param.limitRange);
                break;
            case TIME_TURN:
                robot.wheels.setSpeed(param.limitRange, MOTOR_SIDE.LEFT);
                robot.wheels.setSpeed(-param.limitRange, MOTOR_SIDE.RIGHT);
                break;
            default:
                throw new IllegalStateException("Unhandled driveToRun: " +
                        param.reference + " ::" + param.comparator);
        }
    }

    public AutoDriver headingDistance(AutoDriver driver, float heading, int distance) {

        switch (headingDistState) {
            case INIT:
                driver.done = false;
                headingDistState = headingDistState.next();
                break;
            case HEADING:
                driver.drive = heading(heading);
                headingDistState = headingDistState.next();
                break;
            case DISTANCE:
                driver.drive = distance(distance);
                headingDistState = headingDistState.next();
                break;
            case DONE:
                driver.done = true;
                headingDistState = HEADING_DIST_STATE.INIT;
                break;
        }
        return driver;
    }

    // I could probably delegate 75% of this method to the one above but what the hell, copy paste is a thing
    public AutoDriver headingDistanceHeading(AutoDriver driver, float heading, int distance, float heading2) {

        switch (headingDistHeadingState) {
            case INIT:
                driver.done = false;
                headingDistHeadingState = headingDistHeadingState.next();
                break;
            case HEADING:
                driver.drive = heading(heading);
                headingDistHeadingState = headingDistHeadingState.next();
                break;
            case DISTANCE:
                driver.drive = distance(distance);
                headingDistHeadingState = headingDistHeadingState.next();
                break;
            case HEADING2:
                driver.drive = heading(heading2);
                headingDistHeadingState = headingDistHeadingState.next();
            case DONE:
                driver.done = true;
                headingDistHeadingState = HEADING_DIST_HEADING_STATE.INIT;
                break;
        }
        return driver;
    }

    enum HEADING_DIST_STATE implements OrderedEnum {
        INIT,
        HEADING,
        DISTANCE,
        DONE;

        public HEADING_DIST_STATE prev() {
            return OrderedEnumHelper.prev(this);
        }

        public HEADING_DIST_STATE next() {
            return OrderedEnumHelper.next(this);
        }

    }

    enum HEADING_DIST_HEADING_STATE implements OrderedEnum {
        INIT,
        HEADING,
        DISTANCE,
        HEADING2,
        DONE;

        public HEADING_DIST_HEADING_STATE prev() {
            return OrderedEnumHelper.prev(this);
        }

        public HEADING_DIST_HEADING_STATE next() {
            return OrderedEnumHelper.next(this);
        }

    }
}
