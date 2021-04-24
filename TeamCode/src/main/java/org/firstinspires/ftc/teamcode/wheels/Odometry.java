package org.firstinspires.ftc.teamcode.wheels;

import com.acmerobotics.roadrunner.geometry.Pose2d;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.HardwareMap;

public class Odometry {
    public static final String L_POD = "BL";
    public static final String R_POD = "BL";
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

    private Pose2d coords;

    // Constructors
    public Odometry (HardwareMap map, int startX, int startY, String left, String right){
        leftEncoder = map.dcMotor.get(left);
        rightEncoder = map.dcMotor.get(right);
    }
    public Odometry (HardwareMap map, int startX, int startY, String left, String right, String mid){
        this(map, startX, startY, left, right);
        midEncoder = map.dcMotor.get(mid);
    }

    // Loop
    public void update (){
        // DO MATHY STUFF
        //Temp vars for easy reference
        int l = leftEncoder.getCurrentPosition();
        int r = rightEncoder.getCurrentPosition();
        int m = midEncoder.getCurrentPosition();

        //Get change in encoder values and store them
        dl = (l - ll) * TICKS_PER_MM;
        dr = (r - lr) * TICKS_PER_MM;
        dm = (m - lm) * TICKS_PER_MM;
        /*
        X = (d1cosθ1h1-d2cosθ2h2)/(cosθ1h1-cosθ2h2)
R (radians) = (d2-x)cosθ2h2
Y = (d3 - (R/cosθ3h3)
         */

        //Store current values for next loop
        ll = l;
        lr = r;
        lm = m;
    }

    public Pose2d getPosition (){
        return coords;
    }

}
