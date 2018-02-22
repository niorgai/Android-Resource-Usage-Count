package compat;

import com.intellij.psi.xml.XmlTag;

/**
 * Created by qiu on 12/12/17.
 */
public class FindUsagesCompat {

    public static final int ERROR = -1;

    public static int findUsage(XmlTag element) {
//        if (ApplicationInfo.getInstance().getBuild().getBaselineVersion() < 171) {
//            return FindUsagesImpl171.getInstance().findUsage(element);
//        }
        return FindUsagesImpl.getInstance().findUsage(element);
//        return 1;
    }

}
