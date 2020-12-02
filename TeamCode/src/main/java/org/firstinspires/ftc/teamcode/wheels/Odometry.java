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
    public Odometry (HardwareMap map, int startX, int startY, String left, String right){
        leftEncoder = map.dcMotor.get(left);
        rightEncoder = map.dcMotor.get(right);
        coords = new CoordinatePoint (startX, startY);
    }
    public Odometry (HardwareMap map, int startX, int startY, String left, String right, String mid){
        this(map, startX, startY, left, right);
        midEncoder = map.dcMotor.get(mid);
    }

    // Loop
    public void UpdatePosition (){
        // DO MATHY STUFF
    }

    public CoordinatePoint getPosition (){
        return coords;
    }

}
