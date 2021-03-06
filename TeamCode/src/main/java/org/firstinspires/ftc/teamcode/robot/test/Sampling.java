package org.firstinspires.ftc.teamcode.robot.common;

import android.graphics.Color;

import org.firstinspires.ftc.teamcode.driveto.AutoDriver;
import org.firstinspires.ftc.teamcode.field.Field;
import org.firstinspires.ftc.teamcode.robot.Robot;
import org.firstinspires.ftc.teamcode.utils.OrderedEnum;
import org.firstinspires.ftc.teamcode.utils.OrderedEnumHelper;
import org.firstinspires.ftc.teamcode.vuforia.ImageFTC;

public class Sampling implements CommonTask {
    private static final boolean DEBUG = false;

    // Drive constants
    private static final float ARM_DELAY = 0.75f;
    private static final int PIVOT_MILLS = 200;

    // Image constants
    private int[] IMAGE_MAX = new int[]{1279, 719};
    private static final Field.AllianceColor PIVOT_CCW_COLOR = Field.AllianceColor.BLUE;

    // Jewel parse default values
    public int[] UL = new int[]{0, 0};
    public int[] LR = new int[]{800, 300};

    // Runtime
    private final Robot robot;
    private ImageFTC image;
    private Boolean isLeftRed;
    private PARSE_STATE parseState;
    private HIT_STATE hitState;

    public Sampling(Robot robot) {
        this.robot = robot;
        this.reset();
    }

    public void reset() {
        setImage(null);
        parseState = PARSE_STATE.values()[0];
        hitState = HIT_STATE.values()[0];
        isLeftRed = null;
    }

    public void setImage(ImageFTC image) {
        this.image = image;
        if (image != null) {
            int[] max = new int[2];
            max[0] = image.getWidth();
            max[1] = image.getHeight();
            makeAreaSafe(max, true);
            makeAreaSafe(max, false);
            IMAGE_MAX = max;
            isLeftRed = null;
        }
    }

    public ImageFTC getImage() {
        return image;
    }

    private void drawOutline() {

        // Vertical lines at UL[x] and LR[x]
        for (int i = UL[1]; i <= LR[1]; i++) {
            image.getBitmap().setPixel(UL[0], i, Color.GREEN);
            image.getBitmap().setPixel(UL[0] + 1, i, Color.GREEN);
            image.getBitmap().setPixel(LR[0], i, Color.GREEN);
            image.getBitmap().setPixel(LR[0] - 1, i, Color.GREEN);
        }

        // Horizontal lines at UL[y] and LR[y]
        for (int i = UL[0]; i <= LR[0]; i++) {
            image.getBitmap().setPixel(i, UL[1], Color.GREEN);
            image.getBitmap().setPixel(i, UL[1] + 1, Color.GREEN);
            image.getBitmap().setPixel(i, LR[1], Color.GREEN);
            image.getBitmap().setPixel(i, LR[1] - 1, Color.GREEN);
        }

        // Vertical line at the center divsion
        int middleX = ((LR[0] - UL[0]) / 2) + UL[0];
        for (int i = UL[1]; i <= LR[1]; i++) {
            image.getBitmap().setPixel(middleX, i, Color.RED);
            image.getBitmap().setPixel(middleX + 1, i, Color.RED);
        }
    }

    public boolean isLeftRed() {
        if (isLeftRed != null) {
            return isLeftRed;
        }
        if (!isAvailable()) {
            return false;
        }

        // Average color of each half
        int middleX = (LR[0] + UL[0]) / 2;
        int left = Color.red(image.rgb(new int[]{middleX + 1, UL[1]}, LR));
        int right = Color.red(image.rgb(UL, new int[]{middleX, LR[1]}));
        if (DEBUG) {
            robot.telemetry.log().add("Jewel Reds: " + left + ", " + right);
        }

        // Outline for humans
        drawOutline();

        isLeftRed = (left > right);
        return isLeftRed;
    }

    public boolean pivotCCW(Field.AllianceColor alliance) {
        return (isLeftRed()) == (alliance == PIVOT_CCW_COLOR);
    }

    public boolean isAvailable() {
        if (image == null) {
            robot.telemetry.log().add(this.getClass().getSimpleName() + ": Falling back to default image");
            setImage(robot.vuforia.getImage());
        }
        if (image == null) {
            robot.telemetry.log().add(this.getClass().getSimpleName() + ": No image available");
        }
        return image != null;
    }

    public void changeArea(boolean isLR, boolean isX, int interval) {
        int index = isX ? 0 : 1;
        int[] src = isLR ? LR : UL;
        int[] dst = new int[2];

        System.arraycopy(dst, 0, src, 0, 2);
        dst[index] += interval;
        makeAreaSafe(dst, isLR);

        if (isLR) {
            this.LR = dst;
        } else {
            this.UL = dst;
        }
    }

    private void makeAreaSafe(int[] a, boolean isLR) {
        if (isLR) {
            a[0] = Math.max(UL[0] + 1, Math.min(IMAGE_MAX[0], a[0]));
            a[1] = Math.max(UL[1] + 1, Math.min(IMAGE_MAX[1], a[1]));
        } else {
            a[0] = Math.max(0, Math.min(LR[0] - 1, a[0]));
            a[1] = Math.max(0, Math.min(LR[1] - 1, a[1]));
        }
    }

    public AutoDriver parse(AutoDriver driver) {
        if (DEBUG) {
            robot.telemetry.log().add("parseState: " + parseState);
        }

        switch (parseState) {
            case INIT:
                driver.done = false;
                parseState = parseState.next();
                break;
            case ENABLE_CAPTURE:
                parseState = parseState.next();
                image = robot.vuforia.getImage();
                break;
            case WAIT_FOR_IMAGE:
                if (image == null) {
                    robot.vuforia.capture();
                    image = robot.vuforia.getImage();
                } else {
                    setImage(image);
                    parseState = parseState.next();
                }
                break;
            case PARSE_JEWEL:
                isLeftRed();
                if (DEBUG) {
                    image.savePNG("auto-" + System.currentTimeMillis() + ".png");
                }
                parseState = parseState.next();
                break;
            case DONE:
                driver.done = true;
                break;
        }

        return driver;
    }

    enum HIT_STATE implements OrderedEnum {
        INIT,
        DEPLOY_ARM,         // Move the arm down so we can hit the jewel
        HIT_JEWEL,          // Pivot to hit the correct jewel
        RETRACT_ARM,        // Retract the arm so we don't accidentally hit the jewels again
        HIT_JEWEL_REVERSE,  // Blind pivot back toward the starting heading
        DONE;

        public HIT_STATE prev() {
            return OrderedEnumHelper.prev(this);
        }

        public HIT_STATE next() {
            return OrderedEnumHelper.next(this);
        }
    }

    enum PARSE_STATE implements OrderedEnum {
        INIT,
        ENABLE_CAPTURE,     // Enable vuforia image capture
        WAIT_FOR_IMAGE,     // Make sure we don't try to do anything before Vuforia returns an image to analyze.
        PARSE_JEWEL,        // Parse which jewel is on which side
        DONE;

        public PARSE_STATE prev() {
            return OrderedEnumHelper.prev(this);
        }

        public PARSE_STATE next() {
            return OrderedEnumHelper.next(this);
        }
    }
}