import com.intellij.codeHighlighting.Pass;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.find.findUsages.FindUsagesHandler;
import com.intellij.find.findUsages.FindUsagesOptions;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.editor.markup.SeparatorPlacement;
import com.intellij.openapi.util.Factory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.usages.Usage;
import com.intellij.usages.UsageSearcher;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import utils.FindUsageUtils;
import utils.PropertiesUtils;

import java.awt.*;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class UsageCountLineProvider implements LineMarkerProvider {

    @Nullable
    @Override
    public LineMarkerInfo getLineMarkerInfo(@NotNull PsiElement psiElement) {
        if (!ResourceUsageCountUtils.isTargetTagToCount(psiElement)) {
            return null;
        }
        int count = findTagUsage((XmlTag) psiElement);
        return new MyLineMarkerInfo(psiElement, count);
    }

    @Override
    public void collectSlowLineMarkers(@NotNull List<PsiElement> list, @NotNull Collection<LineMarkerInfo> collection) {

    }

    private class MyLineMarkerInfo extends LineMarkerInfo<PsiElement> {

        public MyLineMarkerInfo(PsiElement element, int count) {
            super(element, element.getTextRange(), new MyIcon(count), Pass.UPDATE_ALL, null, null, GutterIconRenderer.Alignment.RIGHT);
            separatorPlacement = SeparatorPlacement.BOTTOM;
        }

    }

    private class MyIcon extends com.intellij.util.ui.EmptyIcon {

        private int count;
        private int length;

        MyIcon(int count) {
            super(8, 8);
            this.count = count;
            int temp = count;
            length ++;
            while (temp / 10 != 0) {
                length ++;
                temp /= 10;
            }
        }

        @Override
        public void paintIcon(Component c, Graphics g, int i, int j) {
            g.setColor(count <= 0 ? PropertiesUtils.getZeroColor() : count == 1 ? PropertiesUtils.getOneColor() : PropertiesUtils.getOtherColor());
            g.drawString(String.valueOf(count), i, (int)(j + getIconHeight() + 1.5));
        }

        @Override
        public int getIconWidth() {
            return length * 5;
        }

        @Override
        public int getIconHeight() {
            return 8;
        }
    }

    private int findTagUsage(XmlTag element) {
        final FindUsagesHandler handler = FindUsageUtils.getFindUsagesHandler(element, element.getProject());
        if (handler != null) {
            final FindUsagesOptions findUsagesOptions = handler.getFindUsagesOptions();
            final PsiElement[] primaryElements = handler.getPrimaryElements();
            final PsiElement[] secondaryElements = handler.getSecondaryElements();
            Factory factory = new Factory() {
                public UsageSearcher create() {
                    return FindUsageUtils.createUsageSearcher(primaryElements, secondaryElements, handler, findUsagesOptions, (PsiFile) null);
                }
            };
            UsageSearcher usageSearcher = (UsageSearcher)factory.create();
            final AtomicInteger mCount = new AtomicInteger(0);
            usageSearcher.generate(new Processor<Usage>() {
                @Override
                public boolean process(Usage usage) {
                    if (ResourceUsageCountUtils.isUsefulUsageToCount(usage)) {
                        mCount.incrementAndGet();
                    }
                    return true;
                }
            });
            return mCount.get();
        }
        return 0;
    }

}