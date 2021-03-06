package org.firstinspires.ftc.teamcode.wheels;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.HardwareMap;

public class Odometry {
    public static float TICKS_PER_MM;
    // The three motors of whose encoders shall be used
    private DcMotor leftEncoder;
    private DcMotor rightEncoder;
    private DcMotor midEncoder;

    //Delta encoder values
    private float dl;
    private float dr;
    private float dm;

    //Values from previous loop
    private int ll;
    private int lr;
    private int lm;

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
        //Temp vars for easy reference
        int l = leftEncoder.getCurrentPosition();
        int r = rightEncoder.getCurrentPosition();
        int m = midEncoder.getCurrentPosition();

        //Get change in encoder values and store them
        dl = (l - ll) * TICKS_PER_MM;
        dr = (r - lr) * TICKS_PER_MM;
        dm = (m - lm) * TICKS_PER_MM;

        //Store current values for next loop
        ll = l;
        lr = r;
        lm = m;
    }

    public CoordinatePoint getPosition (){
        return coords;
    }

}
