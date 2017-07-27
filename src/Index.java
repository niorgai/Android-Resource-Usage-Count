import com.intellij.find.findUsages.*;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.util.ProgressIndicatorBase;
import com.intellij.openapi.progress.util.ProgressWrapper;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Factory;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ex.ProgressIndicatorEx;
import com.intellij.psi.*;
import com.intellij.psi.search.*;
import com.intellij.psi.util.PsiUtilBase;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlToken;
import com.intellij.usageView.UsageInfo;
import com.intellij.usages.Usage;
import com.intellij.usages.UsageInfoToUsageConverter;
import com.intellij.usages.UsageSearcher;
import com.intellij.util.ArrayUtil;
import com.intellij.util.CommonProcessors;
import com.intellij.util.Processor;
import com.intellij.util.containers.ContainerUtil;
import org.apache.http.util.TextUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class Index extends AnAction {

    private AtomicInteger mCount;

    @Override
    public void actionPerformed(AnActionEvent e) {
        // TODO: insert action logic here
        Project project = e.getProject();
        VirtualFile file = e.getDataContext().getData(CommonDataKeys.VIRTUAL_FILE);
        if (file == null) {
            return;
        }
        String name = file.getName();
        if (TextUtils.isEmpty(name)) {
            return;
        }
        if (name.endsWith(".xml")) {
            startIndex(file, project);
        }
    }

    private void startIndex(VirtualFile file, Project project) {
        PsiFile psiFile = PsiUtilBase.getPsiFile(project, file);
        psiFile.accept(new XmlRecursiveElementVisitor() {

            @Override
            public void visitElement(PsiElement element) {
                super.visitElement(element);
//                if (element.getText().equalsIgnoreCase("app_name")) {
//
//                    return;
//                }
                if (element instanceof XmlTag) {
                    XmlTag tag = (XmlTag) element;
                    if (tag.getName().equalsIgnoreCase("string") && tag.getText().equalsIgnoreCase("<string name=\"app_name\">晴天壁纸</string>")) {
                        FindUsagesHandler handler = getFindUsagesHandler(element, project);
                        if (handler != null) {
                            FindUsagesOptions findUsagesOptions = handler.getFindUsagesOptions();
                            PsiElement2UsageTargetAdapter[] primaryTargets = convertToUsageTargets(Arrays.asList(handler.getPrimaryElements()), findUsagesOptions);
                            PsiElement2UsageTargetAdapter[] secondaryTargets = convertToUsageTargets(Arrays.asList(handler.getSecondaryElements()), findUsagesOptions);
                            PsiElement2UsageTargetAdapter[] targets = (PsiElement2UsageTargetAdapter[]) ArrayUtil.mergeArrays(primaryTargets, secondaryTargets);
                            Factory<UsageSearcher> factory = () -> {
                                return createUsageSearcher(primaryTargets, secondaryTargets, handler, findUsagesOptions, (PsiFile)null);
                            };
                            UsageSearcher usageSearcher = (UsageSearcher)factory.create();
                            mCount = new AtomicInteger(0);
                            usageSearcher.generate(new Processor<Usage>() {
                                @Override
                                public boolean process(Usage usage) {
                                    mCount.incrementAndGet();
                                    ProgressIndicator indicator1 = ProgressWrapper.unwrap(ProgressManager.getInstance().getProgressIndicator());
                                    return !indicator1.isCanceled();
                                }
                            });
                            handler.getFindUsagesOptions();
                        }
                        return;
                    }
                }
                if (element instanceof XmlToken) {
                    XmlToken token = (XmlToken) element;
                    String text = token.getText();
                }
            }
        });
    }

    private void startSearch(PsiElement element, Project project) {
        FindUsagesHandler handler = getFindUsagesHandler(element, project);
        if (handler != null) {
            FindUsagesOptions findUsagesOptions = handler.getFindUsagesOptions();
            PsiElement2UsageTargetAdapter[] primaryTargets = convertToUsageTargets(Arrays.asList(handler.getPrimaryElements()), findUsagesOptions);
            PsiElement2UsageTargetAdapter[] secondaryTargets = convertToUsageTargets(Arrays.asList(handler.getSecondaryElements()), findUsagesOptions);
            PsiElement2UsageTargetAdapter[] targets = (PsiElement2UsageTargetAdapter[]) ArrayUtil.mergeArrays(primaryTargets, secondaryTargets);
            Factory<UsageSearcher> factory = () -> {
                return createUsageSearcher(primaryTargets, secondaryTargets, handler, findUsagesOptions, (PsiFile)null);
            };
            UsageSearcher usageSearcher = (UsageSearcher)factory.create();
            mCount = new AtomicInteger(0);
            usageSearcher.generate(new Processor<Usage>() {
                @Override
                public boolean process(Usage usage) {
                    mCount.incrementAndGet();
                    return false;
                }
            });
            handler.getFindUsagesOptions();
        }
    }

    private FindUsagesHandler getFindUsagesHandler(PsiElement element, Project project) {
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
    private static PsiElement2UsageTargetAdapter[] convertToUsageTargets(@NotNull Iterable<PsiElement> elementsToSearch, @NotNull FindUsagesOptions findUsagesOptions) {
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
    private static UsageSearcher createUsageSearcher(@NotNull PsiElement2UsageTargetAdapter[] primaryTargets, @NotNull PsiElement2UsageTargetAdapter[] secondaryTargets, @NotNull FindUsagesHandler handler, @NotNull FindUsagesOptions options, PsiFile scopeFile) throws PsiInvalidElementAccessException {
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
