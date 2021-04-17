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
    private static final double TICKS_PER_DEG = 10.2 / 1.5;
    private static final double TICKS_FROM_END_DRIVE = 10.0 * TICKS_PER_INCH;
    private static final double TICKS_FROM_END_SPIN = 5.0 * TICKS_PER_INCH;
    private static final double P = 25.0;
    private static final double I = 0.4;
    private static final double D = 0;
    private static final double F = 20;




    private static final int TOLERANCE = 15;
    public ArrayList<DcMotorEx> right;
    public ArrayList<DcMotorEx> left;
    public BNO055IMU imu;
    private Orientation lastAngles = new Orientation();
    private int target = 0;

    private void init(HardwareMap map){
        PIDFCoefficients pidf = new PIDFCoefficients();
        pidf.p = P;
        pidf.i = I;
        pidf.d = D;
        pidf.f = F;
       // this.setPIDFCoefficients(pidf);
        pidf = getPIDFCoefficients();
        left.get(0).setTargetPositionTolerance(TOLERANCE);
    }

    public NewAuto (String l1, String l2, String r1, String r2, HardwareMap map){

        left = new ArrayList<DcMotorEx>();
        right = new ArrayList<DcMotorEx>();
        left.add(map.get(DcMotorEx.class, l1));
        left.add(map.get(DcMotorEx.class, l2));
        right.add(map.get(DcMotorEx.class, r1));
        right.add(map.get(DcMotorEx.class, r2));
        for (DcMotorEx m:left){
            //m.setDirection(DcMotorSimple.Direction.REVERSE);
        }
        init(map);
    }

    public NewAuto (String l, String r, HardwareMap map){



        left = new ArrayList<DcMotorEx>();
        right = new ArrayList<DcMotorEx>();
        left.add(map.get(DcMotorEx.class, l));
        right.add(map.get(DcMotorEx.class, r));
        init(map);
    }

    public void drive(double distance,  float speedScale){
        double lEncStart = left.get(0).getCurrentPosition();
        double rEncStart = right.get(0).getCurrentPosition();
        double lSpeed = 0;
        double rSpeed = 0;
        target = (int)(distance * TICKS_PER_INCH);
        for (DcMotorEx m:left) {
            m.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
            m.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        }
        for (DcMotorEx m:right) {
            m.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
            m.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        }
        double lEnc = left.get(0).getCurrentPosition() - lEncStart;
        double rEnc;
        while (Math.abs(lEnc) + TOLERANCE < Math.abs(target)){
            lEnc = left.get(0).getCurrentPosition() - lEncStart;
            rEnc = right.get(0).getCurrentPosition() - rEncStart;
            int targ = Math.abs(target);
            int pos = (int)Math.abs(lEnc);
            float power = Math.min(speedScale, (((targ - pos) / (int)TICKS_FROM_END_DRIVE) * speedScale + 0.2f));

            double correction = Math.pow((Math.abs(lEnc) - Math.abs(rEnc)), 2);
            //if(distance < 0) correction = -correction;
            rSpeed = power * 1150 + correction;
            lSpeed = power * 1150 - correction;
            if(distance < 0){
                rSpeed = -rSpeed;
                lSpeed = -lSpeed;
            }
            for (DcMotorEx m:right) m.setVelocity(rSpeed);
            for (DcMotorEx m:left) m.setVelocity(lSpeed);
        }
        for (DcMotorEx m:left) {
            m.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
            m.setPower(0);
        }
        for (DcMotorEx m:right) {
            m.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
            m.setPower(0);
        }
        while(left.get(0).getVelocity() != 0 || right.get(0).getVelocity() != 0);
    }

    public void rotate(double degrees,  float speedScale){
        double lEncStart = left.get(0).getCurrentPosition();
        double rEncStart = right.get(0).getCurrentPosition();
        double lSpeed = 0;
        double rSpeed = 0;
        target = (int)(degrees * TICKS_PER_DEG);
        for (DcMotorEx m:left) {
            m.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
            m.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        }
        for (DcMotorEx m:right) {
            m.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
            m.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        }
        double lEnc = left.get(0).getCurrentPosition() - lEncStart;
        double rEnc = right.get(0).getCurrentPosition() - lEncStart;
        while (Math.abs(lEnc) + TOLERANCE < Math.abs(target) || Math.abs(rEnc) + TOLERANCE < Math.abs(target)){
            lEnc = left.get(0).getCurrentPosition() - lEncStart;
            rEnc = right.get(0).getCurrentPosition() - rEncStart;
            lEnc = left.get(0).getCurrentPosition() - lEncStart;
            int targ = Math.abs(target);
            int pos = (int)Math.abs(lEnc);
            float power = Math.min(speedScale, (((targ - pos) / (int)TICKS_FROM_END_SPIN) * speedScale + speedScale));

            double correction = 0;
            rSpeed = power * 1150 + correction;
            lSpeed = power * 1150 - correction;
            if(degrees < 0){
                lSpeed = -lSpeed;
            } else {
                rSpeed = -rSpeed;
            }
            if(Math.abs(lEnc) + TOLERANCE >= Math.abs(target)) lSpeed = 0;
            if(Math.abs(rEnc) + TOLERANCE >= Math.abs(target)) rSpeed = 0;
            for (DcMotorEx m:right) m.setVelocity(rSpeed);
            for (DcMotorEx m:left) m.setVelocity(lSpeed);
        }
        for (DcMotorEx m:left) {
            m.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
            m.setPower(0);
        }
        for (DcMotorEx m:right) {
            m.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
            m.setPower(0);
        }
        while(left.get(0).getVelocity() != 0 || right.get(0).getVelocity() != 0);
    }




    public void rotateLeftWheels(double degrees,  float speedScale){
        target = (int)(degrees * TICKS_PER_DEG * 2);
        for (DcMotorEx m:left) {
            m.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
            m.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        }
        for (DcMotorEx m:right) {
            m.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        }
        while (Math.abs(left.get(0).getCurrentPosition()) + TOLERANCE < Math.abs(target)){
            int targ = Math.abs(target);
            int pos = Math.abs(left.get(0).getCurrentPosition());
            float power = Math.min(speedScale, (float)(((targ - pos) / (int)TICKS_FROM_END_SPIN) * speedScale + speedScale));
            if(degrees < 0) power = -power;
            double correction = Math.pow((left.get(0).getCurrentPosition() + right.get(0).getCurrentPosition()), 2);
            for (DcMotorEx m:left) m.setVelocity((power * 1150));
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
    public void rotateRightWheels(double degrees,  float speedScale){
        target = (int)(degrees * TICKS_PER_DEG * 2);
        for (DcMotorEx m:left) {
            m.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        }
        for (DcMotorEx m:right) {
            m.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
            m.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        }
        while (Math.abs(right.get(0).getCurrentPosition()) + TOLERANCE < Math.abs(target)){
            int targ = Math.abs(target);
            int pos = Math.abs(right.get(0).getCurrentPosition());
            float power = Math.min(speedScale, (float)(((targ - pos) / (int)TICKS_FROM_END_SPIN) * speedScale + speedScale));
            if(degrees < 0) power = -power;
            double correction = Math.pow((left.get(0).getCurrentPosition() + right.get(0).getCurrentPosition()), 2);
            for (DcMotorEx m:right) m.setVelocity(-(power * 1150));
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
        PIDFCoefficients newPID = new PIDFCoefficients(p, i, d, left.get(0).getPIDFCoefficients(DcMotor.RunMode.RUN_USING_ENCODER).f);
        for (DcMotorEx m:right) m.setPIDFCoefficients(DcMotor.RunMode.RUN_TO_POSITION, newPID);
        for (DcMotorEx m:left) m.setPIDFCoefficients(DcMotor.RunMode.RUN_TO_POSITION, newPID);
    }

    public void setPIDFCoefficients(PIDFCoefficients PIDF){
        for (DcMotorEx m:right) m.setVelocityPIDFCoefficients(PIDF.p, PIDF.i, PIDF.d, PIDF.f);
        for (DcMotorEx m:left) m.setVelocityPIDFCoefficients(PIDF.p, PIDF.i, PIDF.d, PIDF.f);
    }
    public void setTolerance(int tolerance){
        for (DcMotorEx m:right) m.setTargetPositionTolerance(TOLERANCE);
        for (DcMotorEx m:left) m.setTargetPositionTolerance(TOLERANCE);
    }

    public PIDFCoefficients getPIDFCoefficients(){
        return left.get(0).getPIDFCoefficients(DcMotor.RunMode.RUN_USING_ENCODER);
    }
}
