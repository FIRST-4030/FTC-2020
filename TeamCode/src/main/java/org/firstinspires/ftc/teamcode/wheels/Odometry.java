package org.firstinspires.ftc.teamcode.wheels;

import com.acmerobotics.roadrunner.geometry.Pose2d;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;

public class Odometry {
    public static final String L_POD = "BL";
    public static final String R_POD = "BL";
    public static final double TICKS_PER_INCH = 1739.0 + 2.0/3.0;
    public static final double LR_POD_DISTANCE = 11.6;
    public static final float M_POD_DISTANCE = 4;
    // The three motors of whose encoders shall be used
    private DcMotor leftEncoder;
    private DcMotor rightEncoder;
    private DcMotor midEncoder;

    //Delta encoder values
    private double dl;
    private double dr;
    private double dm;

    //Values from previous loop
    private int ll;
    private int lr;
    private int lm;

    private HardwareMap hardwareMap;

    private Pose2d coords;

    // Constructors
    public Odometry (HardwareMap map, int startX, int startY, String left, String right){
        leftEncoder = map.dcMotor.get(left);
        leftEncoder.setDirection(DcMotorSimple.Direction.REVERSE);
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
        double pi = 3.1415926535897932384626433832795;
        int l = leftEncoder.getCurrentPosition();
        int r = rightEncoder.getCurrentPosition();
        int m = midEncoder.getCurrentPosition();
        double dXNew;
        double dYNew;
        double dGNew;
        Pose2d newPosition = new Pose2d(coords.getX(), coords.getY(), coords.getHeading());
        //Get change in encoder values and store them
        dl = (l - ll) / TICKS_PER_INCH;
        dr = (r - lr) / TICKS_PER_INCH;
        dm = (m - lm) / TICKS_PER_INCH;
        double theta = (dl-dr)*360/(2*pi*LR_POD_DISTANCE);
        if(theta != 0){
            double r1 = dl*360/(2*pi*theta);
            double r2 = r1 - LR_POD_DISTANCE;
            double turnRadius = r1 + LR_POD_DISTANCE/2;
            dXNew = -(LR_POD_DISTANCE/2 + r1) - turnRadius*Math.cos(theta*pi/180);
            dYNew = -turnRadius*Math.sin(theta*pi/180);

        }
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
    public double getLeftEncoder(){
        return leftEncoder.getCurrentPosition();
    }

}
