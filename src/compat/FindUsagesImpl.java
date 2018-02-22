package compat;

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
import com.intellij.openapi.util.Factory;
import com.intellij.openapi.wm.ex.ProgressIndicatorEx;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiInvalidElementAccessException;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.*;
import com.intellij.psi.xml.XmlTag;
import com.intellij.usageView.UsageInfo;
import com.intellij.usages.Usage;
import com.intellij.usages.UsageInfoToUsageConverter;
import com.intellij.usages.UsageSearcher;
import com.intellij.util.CommonProcessors;
import com.intellij.util.Function;
import com.intellij.util.Processor;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import utils.ResourceUsageCountUtils;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

class FindUsagesImpl {

    private static volatile FindUsagesImpl mInstance;

    static FindUsagesImpl getInstance() {
        if (mInstance == null) {
            synchronized (FindUsagesImpl.class) {
                if (mInstance == null) {
                    mInstance = new FindUsagesImpl();
                }
            }
        }
        return mInstance;
    }

    int findUsage(XmlTag element) {
        final AtomicInteger mCount = new AtomicInteger(0);
        try {
            com.intellij.find.findUsages.FindUsagesHandler handler = this.getFindUsagesHandler(element);
            if (handler != null) {
                AbstractFindUsagesDialog dialog = handler.getFindUsagesDialog(false, false, false);
                dialog.close(0);
                FindUsagesOptions findUsagesOptions = dialog.calcFindUsagesOptions();
                PsiElement[] primaryElements = handler.getPrimaryElements();
                PsiElement[] secondaryElements = handler.getSecondaryElements();
                PsiElement2UsageTargetAdapter[] primaryTargets = convertToUsageTargets(Arrays.asList(primaryElements), findUsagesOptions);
                PsiElement2UsageTargetAdapter[] secondaryTargets = convertToUsageTargets(Arrays.asList(secondaryElements), findUsagesOptions);
                final Factory<UsageSearcher> factory = new Factory<UsageSearcher>() {
                    @Override
                    public UsageSearcher create() {
                        return createUsageSearcher(primaryTargets, secondaryTargets, handler, findUsagesOptions, (PsiFile) null);
                    }
                };

                UsageSearcher usageSearcher = (UsageSearcher)factory.create();
                usageSearcher.generate((usage) -> {
                    if (ResourceUsageCountUtils.isUsefulUsageToCount(usage)) {
                        mCount.incrementAndGet();
                    }
                    return true;
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return mCount.get();
    }

    private FindUsagesHandler getFindUsagesHandler(@NotNull PsiElement element) {
        FindUsagesHandlerFactory[] var3 = (FindUsagesHandlerFactory[]) Extensions.getExtensions(FindUsagesHandlerFactory.EP_NAME, element.getProject());
        int var4 = var3.length;

        for(int var5 = 0; var5 < var4; ++var5) {
            FindUsagesHandlerFactory factory = var3[var5];
            if(factory.canFindUsages(element)) {
                com.intellij.find.findUsages.FindUsagesHandler handler = factory.createFindUsagesHandler(element, false);
                if (handler == com.intellij.find.findUsages.FindUsagesHandler.NULL_HANDLER) {
                    return null;
                }

                if (handler != null) {
                    return handler;
                }
            }
        }

        return null;
    }

    @NotNull
    private static PsiElement2UsageTargetAdapter[] convertToUsageTargets(@NotNull Iterable<PsiElement> elementsToSearch, @NotNull final FindUsagesOptions findUsagesOptions) {
        List<PsiElement2UsageTargetAdapter> targets = ContainerUtil.map(elementsToSearch, new Function<PsiElement, PsiElement2UsageTargetAdapter>() {
            @Override
            public PsiElement2UsageTargetAdapter fun(PsiElement element) {
                return convertToUsageTarget(element, findUsagesOptions);
            }
        });
        return (PsiElement2UsageTargetAdapter[])targets.toArray(new PsiElement2UsageTargetAdapter[targets.size()]);
    }

    private static PsiElement2UsageTargetAdapter convertToUsageTarget(@NotNull PsiElement elementToSearch, @NotNull FindUsagesOptions findUsagesOptions) {
        if(elementToSearch instanceof NavigationItem) {
            return new PsiElement2UsageTargetAdapter(elementToSearch, findUsagesOptions);
        } else {
            throw new IllegalArgumentException("Wrong usage target:" + elementToSearch + "; " + elementToSearch.getClass());
        }
    }

    @NotNull
    private static UsageSearcher createUsageSearcher(@NotNull PsiElement2UsageTargetAdapter[] primaryTargets, @NotNull PsiElement2UsageTargetAdapter[] secondaryTargets, @NotNull com.intellij.find.findUsages.FindUsagesHandler handler, @NotNull FindUsagesOptions options, PsiFile scopeFile) throws PsiInvalidElementAccessException {
        ReadAction.run(() -> {
            PsiElement[] primaryElements = PsiElement2UsageTargetAdapter.convertToPsiElements(primaryTargets);
            PsiElement[] secondaryElements = PsiElement2UsageTargetAdapter.convertToPsiElements(secondaryTargets);
            ContainerUtil.concat(new PsiElement[][]{primaryElements, secondaryElements}).forEach((psi) -> {
                if(psi == null || !psi.isValid()) {
                    throw new PsiInvalidElementAccessException(psi);
                }
            });
        });
        FindUsagesOptions optionsClone = options.clone();
        return (processor) -> {
            PsiElement[] primaryElements = (PsiElement[]) ReadAction.compute(() -> {
                return PsiElement2UsageTargetAdapter.convertToPsiElements(primaryTargets);
            });
            PsiElement[] secondaryElements = (PsiElement[])ReadAction.compute(() -> {
                return PsiElement2UsageTargetAdapter.convertToPsiElements(secondaryTargets);
            });
            Project project = (Project)ReadAction.compute(() -> {
                return scopeFile != null?scopeFile.getProject():primaryElements[0].getProject();
            });
            dropResolveCacheRegularly(ProgressManager.getInstance().getProgressIndicator(), project);
            if(scopeFile != null) {
                optionsClone.searchScope = new LocalSearchScope(scopeFile);
            }

            Processor<UsageInfo> usageInfoProcessor = new CommonProcessors.UniqueProcessor((usageInfo) -> {
                Usage usage = (Usage)ReadAction.compute(() -> {
                    return UsageInfoToUsageConverter.convert(primaryElements, (UsageInfo) usageInfo);
                });
                return processor.process(usage);
            });
            Iterable<PsiElement> elements = ContainerUtil.concat(new PsiElement[][]{primaryElements, secondaryElements});
            optionsClone.fastTrack = new SearchRequestCollector(new SearchSession());
            if(optionsClone.searchScope instanceof GlobalSearchScope) {
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
                    UsageInfo info = (UsageInfo)ReadAction.compute(() -> {
                        return !ref.getElement().isValid()?null:new UsageInfo(ref);
                    });
                    return info == null || usageInfoProcessor.process(info);
                });
            } finally {
                optionsClone.fastTrack = null;
            }

        };
    }

    private static void dropResolveCacheRegularly(ProgressIndicator indicator, @NotNull final Project project) {
        if(indicator instanceof ProgressIndicatorEx) {
            ((ProgressIndicatorEx)indicator).addStateDelegate(new ProgressIndicatorBase() {
                volatile long lastCleared = System.currentTimeMillis();

                public void setFraction(double fraction) {
                    super.setFraction(fraction);
                    long current = System.currentTimeMillis();
                    if(current - this.lastCleared >= 500L) {
                        this.lastCleared = current;
                        PsiManager.getInstance(project).dropResolveCaches();
                    }

                }
            });
        }

    }

}
