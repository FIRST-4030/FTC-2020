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
    public ArrayList<DcMotorEx> right;
    public ArrayList<DcMotorEx> left;
    public BNO055IMU imu;

    public NewAuto (String l1, String l2, String r1, String r2, HardwareMap map){
        imu = map.get(BNO055IMU.class, "imu");
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
        imu = map.get(BNO055IMU.class, "imu");
        left = new ArrayList<DcMotorEx>();
        right = new ArrayList<DcMotorEx>();
        left.add(map.get(DcMotorEx.class, l));
        right.add(map.get(DcMotorEx.class, r));
    }

    public void driveArc(double leftDistance, double rightDistance, float speedScale){
        float lastHeading = imu.getAngularOrientation().thirdAngle;
        for (DcMotorEx m:left) {
            m.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
            m.setTargetPosition((int) (leftDistance * TICKS_PER_INCH));
            m.setPower(speedScale);
            m.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        }
        for (DcMotorEx m:right) {
            m.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
            m.setTargetPosition((int) (rightDistance * TICKS_PER_INCH));
            m.setPower(speedScale);
            m.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        }
        for (DcMotorEx m:right) m.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        for (DcMotorEx m:left) m.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        while (left.get(0).isBusy() || right.get(0).isBusy()){
            double correction = lastHeading - imu.getAngularOrientation().thirdAngle;
            for (DcMotorEx m:right) m.setVelocity(speedScale*(double)1120 + correction);
            for (DcMotorEx m:left) m.setVelocity(speedScale*(double)1120 - correction);
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

    public void setPIDFCoefficients(double p, double i, double d){
        PIDFCoefficients newPID = new PIDFCoefficients(p, i, d, left.get(0).getPIDFCoefficients(DcMotor.RunMode.RUN_TO_POSITION).f);
        for (DcMotorEx m:right) m.setPIDFCoefficients(DcMotor.RunMode.RUN_TO_POSITION, newPID);
        for (DcMotorEx m:left) m.setPIDFCoefficients(DcMotor.RunMode.RUN_TO_POSITION, newPID);
    }
    public PIDFCoefficients getPIDFCoefficients(){
        return left.get(0).getPIDFCoefficients(DcMotor.RunMode.RUN_TO_POSITION);
    }
}
