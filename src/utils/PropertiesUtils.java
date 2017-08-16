package utils;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.ui.JBColor;
import org.apache.http.util.TextUtils;

/**
 * save the color
 * Created by qiu on 8/16/17.
 */
public class PropertiesUtils {

    private static final String COLOR_ZERO = "zero";
    private static final String COLOR_ONE = "one";
    private static final String COLOR_OTHER = "other";

    public static String getZeroColor() {
        if (PropertiesComponent.getInstance().isValueSet(COLOR_ZERO)) {
            return PropertiesComponent.getInstance().getValue(COLOR_ZERO);
        }
        return JBColor.GRAY.toString();
    }

    public static String getOneColor() {
        if (PropertiesComponent.getInstance().isValueSet(COLOR_ONE)) {
            return PropertiesComponent.getInstance().getValue(COLOR_ONE);
        }
        return JBColor.BLUE.toString();
    }

    public static String getOtherColor() {
        if (PropertiesComponent.getInstance().isValueSet(COLOR_OTHER)) {
            return PropertiesComponent.getInstance().getValue(COLOR_OTHER);
        }
        return JBColor.RED.toString();
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

    public static void saveColor(String color, String type) {
        PropertiesComponent.getInstance().setValue(type, color);
    }

}
