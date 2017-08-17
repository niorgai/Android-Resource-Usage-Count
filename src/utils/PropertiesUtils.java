package utils;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.ui.JBColor;
import org.apache.http.util.TextUtils;

import java.awt.*;

/**
 * save the color
 * Created by qiu on 8/16/17.
 */
public class PropertiesUtils {

    public static final String COLOR_ZERO = "zero";
    public static final String COLOR_ONE = "one";
    public static final String COLOR_OTHER = "other";

    public static Color getZeroColor() {
        String color = PropertiesComponent.getInstance().getValue(COLOR_ZERO, String.valueOf(JBColor.GRAY.getRGB()));
        return Color.decode(color);
    }

    public static Color getOneColor() {
        String color = PropertiesComponent.getInstance().getValue(COLOR_ONE, String.valueOf(JBColor.BLUE.getRGB()));
        return Color.decode(color);
    }

    public static Color getOtherColor() {
        String color = PropertiesComponent.getInstance().getValue(COLOR_OTHER, String.valueOf(JBColor.RED.getRGB()));
        return Color.decode(color);
    }

    public static boolean isValidColor(String color) {
        if (TextUtils.isEmpty(color)) {
            return false;
        }
        try {
            JBColor.decode(color);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static void saveColor(Color color, String type) {
        PropertiesComponent.getInstance().setValue(type, String.valueOf(color.getRGB()));
    }

}
