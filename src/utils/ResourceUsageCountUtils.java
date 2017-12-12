package utils;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.xml.XmlTag;
import com.intellij.usages.ReadWriteAccessUsageInfo2UsageAdapter;
import com.intellij.usages.Usage;
import org.apache.http.util.TextUtils;

/**
 * Utils for resource count
 */
public class ResourceUsageCountUtils {

    /**
     * valid tag to count
     */
    public static boolean isTargetTagToCount(PsiElement tag) {
        if (tag == null || !(tag instanceof XmlTag) || TextUtils.isEmpty(((XmlTag)tag).getName())) {
            return false;
        }
        String name = ((XmlTag)tag).getName();
        return name.equals("array")
                || name.equals("attr")
                || name.equals("bool")
                || name.equals("color")
                || name.equals("declare-styleable")
                || name.equals("dimen")
                || name.equals("drawable")
                || name.equals("eat-comment")
                || name.equals("fraction")
                || name.equals("integer")
                || name.equals("integer-array")
                || name.equals("item")
                || name.equals("plurals")
                || name.equals("string")
                || name.equals("string-array")
                || name.equals("style");
    }

    /**
     * It's useless to count build folder
     */
    public static boolean isUsefulUsageToCount(Usage usage) {
        if (usage instanceof ReadWriteAccessUsageInfo2UsageAdapter) {
            VirtualFile virtualFile = ((ReadWriteAccessUsageInfo2UsageAdapter) usage).getFile();
            if (virtualFile != null) {
                if (!virtualFile.getPath().contains("/bin/") && !virtualFile.getPath().contains("/build/")) {
                    return true;
                }
            }
        }
        return false;
    }
}
