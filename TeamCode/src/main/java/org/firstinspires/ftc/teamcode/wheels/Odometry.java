package org.firstinspires.ftc.teamcode.wheels;

import com.acmerobotics.roadrunner.geometry.Pose2d;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.HardwareMap;

public class Odometry {
    public static final String L_POD = "BL";
    public static final String R_POD = "BL";
    public static final float TICKS_PER_INCH = 3;
    public static final float LR_POD_DISTANCE = 18;
    public static final float M_POD_DISTANCE = 8;
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
        coords = new Pose2d (startX, startY, 0);
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
        dl = (l - ll) * TICKS_PER_INCH;
        dr = (r - lr) * TICKS_PER_INCH;
        dm = (m - lm) * TICKS_PER_INCH;
        double dX = (dl * LR_POD_DISTANCE/2 + dr * LR_POD_DISTANCE/2)/(LR_POD_DISTANCE);
        double dH = (dr-dX) * LR_POD_DISTANCE/2;
        double dY = dm - (dH/M_POD_DISTANCE);
        double heading = coords.getHeading() + (dH / 2);
        dX = Math.cos(heading) * dX;
        dY = Math.sin(heading) * dY;
        double nH = coords.getHeading() + dH;
        double nX = coords.getX() + dX;
        double nY = coords.getY() + dY;
        coords = new Pose2d (nX, nY, nH);
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
