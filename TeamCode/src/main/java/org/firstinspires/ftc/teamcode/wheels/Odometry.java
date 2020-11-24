package org.firstinspires.ftc.teamcode.wheels;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.HardwareMap;

public class Odometry {
    // The three motors of whose encoders shall be used
    private DcMotor leftEncoder;
    private DcMotor rightEncoder;
    private DcMotor midEncoder;

    private HardwareMap hardwareMap;

    private CoordinatePoint coords;

    // Constructors
    public Odometry (int startX, int startY, String left, String right){
        leftEncoder = hardwareMap.dcMotor.get(left);
        rightEncoder = hardwareMap.dcMotor.get(right);
        coords = new CoordinatePoint (startX, startY);
    }
    public Odometry (int startX, int startY, String left, String right, String mid){
        this(startX, startY, left, right);
        midEncoder = hardwareMap.dcMotor.get(mid);
    }

    // Loop
    public void UpdatePosition (){

    }

    public CoordinatePoint getPosition (){
        return coords;
    }

}
