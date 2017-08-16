import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * configuration of color
 * Created by qiu on 8/16/17.
 */
public class CountColorConfiguration implements Configurable {

    private JPanel mPanel;
    private JTextField mOtherCount;
    private JTextField mZeroCount;
    private JTextField mOneCount;

    @Nls
    @Override
    public String getDisplayName() {
        return "Android Resource Usage Count";
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return "Set color for each count";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        return mPanel;
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public void apply() throws ConfigurationException {

    }

    @Override
    public void reset() {

    }

    @Override
    public void disposeUIResources() {

    }
}
