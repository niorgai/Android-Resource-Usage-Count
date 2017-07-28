import com.intellij.find.findUsages.*;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.util.ProgressIndicatorBase;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ex.ProgressIndicatorEx;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiInvalidElementAccessException;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.*;
import com.intellij.usageView.UsageInfo;
import com.intellij.usages.Usage;
import com.intellij.usages.UsageInfoToUsageConverter;
import com.intellij.usages.UsageSearcher;
import com.intellij.util.CommonProcessors;
import com.intellij.util.Processor;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.List;

/**
 * Utils for findUsage
 * Copy from FinsUsageAction & findUsageManager & SearchForUsagesRunnable
 */
class FindUsageUtils {

    static FindUsagesHandler getFindUsagesHandler(PsiElement element, Project project) {
        FindUsagesHandlerFactory[] arrs = Extensions.getExtensions(FindUsagesHandlerFactory.EP_NAME, project);
        FindUsagesHandler handler = null;
        for (FindUsagesHandlerFactory arr : arrs) {
            if (arr.canFindUsages(element)) {
                handler = arr.createFindUsagesHandler(element, false);
                break;
            }
        }
        return handler;
    }

    @NotNull
    public static PsiElement2UsageTargetAdapter[] convertToUsageTargets(@NotNull Iterable<PsiElement> elementsToSearch, @NotNull FindUsagesOptions findUsagesOptions) {
        List<PsiElement2UsageTargetAdapter> targets = ContainerUtil.map(elementsToSearch, (element) -> {
            return convertToUsageTarget(element, findUsagesOptions);
        });
        return (PsiElement2UsageTargetAdapter[])targets.toArray(new PsiElement2UsageTargetAdapter[targets.size()]);
    }

    private static PsiElement2UsageTargetAdapter convertToUsageTarget(@NotNull PsiElement elementToSearch, @NotNull FindUsagesOptions findUsagesOptions) {
        if (elementToSearch instanceof NavigationItem) {
            return new PsiElement2UsageTargetAdapter(elementToSearch, findUsagesOptions);
        } else {
            throw new IllegalArgumentException("Wrong usage target:" + elementToSearch + "; " + elementToSearch.getClass());
        }
    }

    @NotNull
    public static UsageSearcher createUsageSearcher(@NotNull PsiElement2UsageTargetAdapter[] primaryTargets, @NotNull PsiElement2UsageTargetAdapter[] secondaryTargets, @NotNull FindUsagesHandler handler, @NotNull FindUsagesOptions options, PsiFile scopeFile) throws PsiInvalidElementAccessException {
        ReadAction.run(() -> {
            PsiElement[] primaryElements = PsiElement2UsageTargetAdapter.convertToPsiElements(primaryTargets);
            PsiElement[] secondaryElements = PsiElement2UsageTargetAdapter.convertToPsiElements(secondaryTargets);
            ContainerUtil.concat(new PsiElement[][]{primaryElements, secondaryElements}).forEach((psi) -> {
                if (psi == null || !psi.isValid()) {
                    throw new PsiInvalidElementAccessException(psi);
                }
            });
        });
        FindUsagesOptions optionsClone = options.clone();
        return (processor) -> {
            PsiElement[] primaryElements = (PsiElement[])ReadAction.compute(() -> {
                return PsiElement2UsageTargetAdapter.convertToPsiElements(primaryTargets);
            });
            PsiElement[] secondaryElements = (PsiElement[])ReadAction.compute(() -> {
                return PsiElement2UsageTargetAdapter.convertToPsiElements(secondaryTargets);
            });
            Project project = (Project)ReadAction.compute(() -> {
                return scopeFile != null ? scopeFile.getProject() : primaryElements[0].getProject();
            });
            dropResolveCacheRegularly(ProgressManager.getInstance().getProgressIndicator(), project);
            if (scopeFile != null) {
                optionsClone.searchScope = new LocalSearchScope(scopeFile);
            }

            Processor<UsageInfo> usageInfoProcessor = new CommonProcessors.UniqueProcessor((usageInfo) -> {
                Usage usage = (Usage)ReadAction.compute(() -> UsageInfoToUsageConverter.convert(primaryElements, (UsageInfo) usageInfo));
                return processor.process(usage);
            });
            Iterable<PsiElement> elements = ContainerUtil.concat(new PsiElement[][]{primaryElements, secondaryElements});
            optionsClone.fastTrack = new SearchRequestCollector(new SearchSession());
            if (optionsClone.searchScope instanceof GlobalSearchScope) {
                optionsClone.searchScope = optionsClone.searchScope.union(GlobalSearchScope.projectScope(project));
            }

            try {
                Iterator var11 = elements.iterator();

                while(var11.hasNext()) {
                    PsiElement element = (PsiElement)var11.next();
                    handler.processElementUsages(element, usageInfoProcessor, optionsClone);
                    CustomUsageSearcher[] var13 = (CustomUsageSearcher[])Extensions.getExtensions(CustomUsageSearcher.EP_NAME);
                    int var14 = var13.length;

                    for(int var15 = 0; var15 < var14; ++var15) {
                        CustomUsageSearcher searcher = var13[var15];

                        try {
                            searcher.processElementUsages(element, processor, optionsClone);
                        } catch (IndexNotReadyException var23) {
                            DumbService.getInstance(element.getProject()).showDumbModeNotification("Find usages is not available during indexing");
                        } catch (ProcessCanceledException var24) {
                            throw var24;
                        } catch (Exception var25) {

                        }
                    }
                }

                PsiSearchHelper.SERVICE.getInstance(project).processRequests(optionsClone.fastTrack, (ref) -> {
                    UsageInfo info = (UsageInfo) ReadAction.compute(() -> {
                        return !ref.getElement().isValid() ? null : new UsageInfo(ref);
                    });
                    return info == null || usageInfoProcessor.process(info);
                });
            } finally {
                optionsClone.fastTrack = null;
            }

        };
    }

    private static void dropResolveCacheRegularly(ProgressIndicator indicator, @NotNull final Project project) {
        if (indicator instanceof ProgressIndicatorEx) {
            ((ProgressIndicatorEx)indicator).addStateDelegate(new ProgressIndicatorBase() {
                volatile long lastCleared = System.currentTimeMillis();

                public void setFraction(double fraction) {
                    super.setFraction(fraction);
                    long current = System.currentTimeMillis();
                    if (current - this.lastCleared >= 500L) {
                        this.lastCleared = current;
                        PsiManager.getInstance(project).dropResolveCaches();
                    }

                }
            });
        }

    }
}
