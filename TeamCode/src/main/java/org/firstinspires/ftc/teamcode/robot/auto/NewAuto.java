package org.firstinspires.ftc.teamcode.robot.auto;
import com.qualcomm.hardware.bosch.BNO055IMU;
import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.PIDCoefficients;
import com.qualcomm.robotcore.hardware.PIDFCoefficients;

import org.firstinspires.ftc.robotcore.external.navigation.Orientation;
import org.firstinspires.ftc.teamcode.actuators.Motor;
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

import java.util.ArrayList;

public class NewAuto {
    private static final double TICKS_PER_INCH = 27.27778;
    private static final double TICKS_PER_DEG = 27.27778;
    public ArrayList<DcMotorEx> right;
    public ArrayList<DcMotorEx> left;
    public BNO055IMU imu;
    private Orientation lastAngles = new Orientation();

    private void init(HardwareMap map){
        BNO055IMU.Parameters parameters = new BNO055IMU.Parameters();

        parameters.mode                = BNO055IMU.SensorMode.IMU;
        parameters.angleUnit           = BNO055IMU.AngleUnit.DEGREES;
        parameters.accelUnit           = BNO055IMU.AccelUnit.METERS_PERSEC_PERSEC;
        parameters.loggingEnabled      = false;
        imu = map.get(BNO055IMU.class, "imu");
        imu.initialize(parameters);
    }

    public NewAuto (String l1, String l2, String r1, String r2, HardwareMap map){
       // init(map);
        left = new ArrayList<DcMotorEx>();
        right = new ArrayList<DcMotorEx>();
        left.add(map.get(DcMotorEx.class, l1));
        left.add(map.get(DcMotorEx.class, l2));
        right.add(map.get(DcMotorEx.class, r1));
        right.add(map.get(DcMotorEx.class, r2));
        for (DcMotorEx m:left){
            //m.setDirection(DcMotorSimple.Direction.REVERSE);
        }
    }

    public NewAuto (String l, String r, HardwareMap map){
      //  init(map);
        left = new ArrayList<DcMotorEx>();
        right = new ArrayList<DcMotorEx>();
        left.add(map.get(DcMotorEx.class, l));
        right.add(map.get(DcMotorEx.class, r));
    }

    public void drive(double distance,  float speedScale){
        for (DcMotorEx m:left) {
            m.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
            m.setTargetPosition((int) (distance * TICKS_PER_INCH));
            m.setPower(speedScale);
            m.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        }
        for (DcMotorEx m:right) {
            m.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
            m.setTargetPosition((int) (distance * TICKS_PER_INCH));
            m.setPower(speedScale);
            m.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        }
        for (DcMotorEx m:right) m.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        for (DcMotorEx m:left) m.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        while (left.get(0).isBusy() || right.get(0).isBusy()){
            double correction = (left.get(0).getCurrentPosition() - right.get(0).getCurrentPosition());
            for (DcMotorEx m:right) m.setVelocity(speedScale*1150 + correction*5);
            for (DcMotorEx m:left) m.setVelocity(speedScale*1150 - correction*5);
        }
        for (DcMotorEx m:left) {
            m.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
            m.setPower(0);
        }
        for (DcMotorEx m:right) {
            m.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
            m.setPower(0);
        }
    }
    public void rotate(double degrees,  float speedScale){
        for (DcMotorEx m:left) {
            m.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
            m.setTargetPosition((int) (-degrees * TICKS_PER_DEG));
            m.setPower(speedScale);
            m.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        }
        for (DcMotorEx m:right) {
            m.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
            m.setTargetPosition((int) (degrees * TICKS_PER_DEG));
            m.setPower(speedScale);
            m.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        }
        for (DcMotorEx m:right) m.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        for (DcMotorEx m:left) m.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        while (left.get(0).isBusy() || right.get(0).isBusy()){

        }
        for (DcMotorEx m:left) {
            m.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
            m.setPower(0);
        }
        for (DcMotorEx m:right) {
            m.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
            m.setPower(0);
        }
    }
    /**
     * See if we are moving in a straight line and if not return a power correction value.
     * @return Power adjustment, + is adjust left - is adjust right.
     */


    private double checkDirection(double referenceAngle)
    {
        // The gain value determines how sensitive the correction is to direction changes.
        // You will have to experiment with your robot to get small smooth direction changes
        // to stay on a straight line.
       /* double correction, angle, gain = 1;

        angle = referenceAngle - imu.getAngularOrientation().thirdAngle;

        if (angle == 0)
            correction = 0;             // no adjustment.
        else
            correction = -angle;        // reverse sign of angle for correction.

        correction = correction * gain;

        */

        return 0;
    }

    public void setPIDFCoefficients(double p, double i, double d){
        PIDFCoefficients newPID = new PIDFCoefficients(p, i, d, left.get(0).getPIDFCoefficients(DcMotor.RunMode.RUN_TO_POSITION).f);
        for (DcMotorEx m:right) m.setPIDFCoefficients(DcMotor.RunMode.RUN_TO_POSITION, newPID);
        for (DcMotorEx m:left) m.setPIDFCoefficients(DcMotor.RunMode.RUN_TO_POSITION, newPID);
    }
    public PIDFCoefficients getPIDFCoefficients(){
        return left.get(0).getPIDFCoefficients(DcMotor.RunMode.RUN_TO_POSITION);
    }
}
