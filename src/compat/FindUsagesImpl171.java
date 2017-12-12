package compat;

import com.intellij.find.findUsages.CustomUsageSearcher;
import com.intellij.find.findUsages.FindUsagesHandler;
import com.intellij.find.findUsages.FindUsagesHandlerFactory;
import com.intellij.find.findUsages.FindUsagesOptions;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.util.ProgressIndicatorBase;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Factory;
import com.intellij.openapi.wm.ex.ProgressIndicatorEx;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.*;
import com.intellij.psi.xml.XmlTag;
import com.intellij.usageView.UsageInfo;
import com.intellij.usages.Usage;
import com.intellij.usages.UsageInfoToUsageConverter;
import com.intellij.usages.UsageSearcher;
import com.intellij.util.CommonProcessors;
import com.intellij.util.Processor;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import utils.ResourceUsageCountUtils;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Utils for findUsage
 * Copy from FinsUsageAction & findUsageManager & SearchForUsagesRunnable
 */
class FindUsagesImpl171 {

    private static volatile FindUsagesImpl171 mInstance;

    static FindUsagesImpl171 getInstance() {
        if (mInstance == null) {
            synchronized (FindUsagesImpl171.class) {
                if (mInstance == null) {
                    mInstance = new FindUsagesImpl171();
                }
            }
        }
        return mInstance;
    }

    int findUsage(XmlTag element) {
        final FindUsagesHandler handler = FindUsagesImpl171.getFindUsagesHandler(element, element.getProject());
        if (handler != null) {
            final FindUsagesOptions findUsagesOptions = handler.getFindUsagesOptions();
            final PsiElement[] primaryElements = handler.getPrimaryElements();
            final PsiElement[] secondaryElements = handler.getSecondaryElements();
            Factory factory = new Factory() {
                public UsageSearcher create() {
                    return FindUsagesImpl171.createUsageSearcher(primaryElements, secondaryElements, handler, findUsagesOptions, (PsiFile) null);
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

    private static FindUsagesHandler getFindUsagesHandler(PsiElement element, Project project) {
        FindUsagesHandlerFactory[] arrs = (FindUsagesHandlerFactory[]) Extensions.getExtensions(FindUsagesHandlerFactory.EP_NAME, project);
        FindUsagesHandler handler = null;
        int length = arrs.length;
        for (int i = 0; i < length; i++) {
            FindUsagesHandlerFactory arr = arrs[i];
            if (arr.canFindUsages(element)) {
                handler = arr.createFindUsagesHandler(element, false);
                if(handler == FindUsagesHandler.NULL_HANDLER) {
                    return null;
                }

                if(handler != null) {
                    return handler;
                }
            }
        }
        return null;
    }

    private static void dropResolveCacheRegularly(ProgressIndicator indicator, final Project project) {
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



    @NotNull
    private static UsageSearcher createUsageSearcher(@NotNull final PsiElement[] primaryElements, @NotNull final PsiElement[] secondaryElements, @NotNull final FindUsagesHandler handler, @NotNull FindUsagesOptions options, final PsiFile scopeFile) {
        final FindUsagesOptions optionsClone = options.clone();
        return new UsageSearcher() {
            public void generate(@NotNull final Processor<Usage> processor) {
                Project project = (Project) ApplicationManager.getApplication().runReadAction(new Computable() {
                    public Project compute() {
                        return scopeFile != null?scopeFile.getProject():primaryElements[0].getProject();
                    }
                });
                dropResolveCacheRegularly(ProgressManager.getInstance().getProgressIndicator(), project);
                if(scopeFile != null) {
                    optionsClone.searchScope = new LocalSearchScope(scopeFile);
                }

                final CommonProcessors.UniqueProcessor usageInfoProcessor = new CommonProcessors.UniqueProcessor(new Processor<UsageInfo>() {
                    @Override
                    public boolean process(final UsageInfo usageInfo) {
                        Usage usage = (Usage)ApplicationManager.getApplication().runReadAction(new Computable() {
                            public Usage compute() {
                                return UsageInfoToUsageConverter.convert(primaryElements, usageInfo);
                            }
                        });
                        return processor.process(usage);
                    }
                });
                Iterable elements = ContainerUtil.concat(new PsiElement[][]{primaryElements, secondaryElements});
                optionsClone.fastTrack = new SearchRequestCollector(new SearchSession());
                if(optionsClone.searchScope instanceof GlobalSearchScope) {
                    optionsClone.searchScope = optionsClone.searchScope.union(GlobalSearchScope.projectScope(project));
                }

                try {
                    Iterator i$ = elements.iterator();

                    while(i$.hasNext()) {
                        final PsiElement element = (PsiElement)i$.next();
                        ApplicationManager.getApplication().runReadAction(new Runnable() {
                            public void run() {

                            }
                        });
                        handler.processElementUsages(element, usageInfoProcessor, optionsClone);
                        CustomUsageSearcher[] arr$ = (CustomUsageSearcher[])Extensions.getExtensions(CustomUsageSearcher.EP_NAME);
                        int len$ = arr$.length;

                        for(int i$1 = 0; i$1 < len$; ++i$1) {
                            CustomUsageSearcher searcher = arr$[i$1];

                            try {
                                searcher.processElementUsages(element, processor, optionsClone);
                            } catch (IndexNotReadyException var17) {
                                DumbService.getInstance(element.getProject()).showDumbModeNotification("Find usages is not available during indexing");
                            } catch (ProcessCanceledException var18) {
                                throw var18;
                            } catch (Exception var19) {

                            }
                        }
                    }

                    PsiSearchHelper.SERVICE.getInstance(project).processRequests(optionsClone.fastTrack, new Processor<PsiReference>() {
                        public boolean process(final PsiReference ref) {
                            UsageInfo info = (UsageInfo)ApplicationManager.getApplication().runReadAction(new Computable() {
                                public UsageInfo compute() {
                                    return !ref.getElement().isValid()?null:new UsageInfo(ref);
                                }
                            });
                            return info == null || usageInfoProcessor.process(info);
                        }
                    });
                } finally {
                    optionsClone.fastTrack = null;
                }

            }
        };
    }
}
