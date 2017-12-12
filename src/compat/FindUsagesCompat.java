package compat;

import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.psi.xml.XmlTag;

/**
 * Created by qiu on 12/12/17.
 */
public class FindUsagesCompat {

    public static final int ERROR = -1;

    public static int findUsage(XmlTag element) {
        try {
            int versionCode = ApplicationInfo.getInstance().getBuild().getComponents()[0];
            if (versionCode >= 171) {
                return FindUsagesImpl.getInstance().findUsage(element);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return FindUsagesImpl171.getInstance().findUsage(element);
    }

}
