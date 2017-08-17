import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.ui.JBColor;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;
import utils.PropertiesUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * configuration of color
 * Created by qiu on 8/16/17.
 */
public class CountColorConfiguration implements Configurable {

    private JPanel mPanel;
    private JButton mZeroButton;
    private JButton mOneButton;
    private JButton mOtherButton;
    private JLabel mZeroCount;
    private JLabel mOneCount;
    private JLabel mOtherCount;

    private JColorChooser mColorChooser;

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
        mZeroCount.setForeground(PropertiesUtils.getZeroColor());
        mOneCount.setForeground(PropertiesUtils.getOneColor());
        mOtherCount.setForeground(PropertiesUtils.getOtherColor());

        mColorChooser = new JColorChooser();

        mZeroButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mColorChooser.setColor(PropertiesUtils.getZeroColor());
                Dialog dialog = JColorChooser.createDialog(mPanel, "Set color for zero count", true, mColorChooser, new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        mZeroCount.setForeground(mColorChooser.getColor());
                        PropertiesUtils.saveColor(mZeroCount.getForeground(), PropertiesUtils.COLOR_ZERO);
                    }
                }, null);
                dialog.setVisible(true);
            }
        });

        mOneButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mColorChooser.setColor(PropertiesUtils.getOneColor());
                Dialog dialog = JColorChooser.createDialog(mPanel, "Set color for one count", true, mColorChooser, new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        mOneCount.setForeground(mColorChooser.getColor());
                        PropertiesUtils.saveColor(mOneCount.getForeground(), PropertiesUtils.COLOR_ONE);
                    }
                }, null);
                dialog.setVisible(true);
            }
        });

        mOtherButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mColorChooser.setColor(PropertiesUtils.getOtherColor());
                Dialog dialog = JColorChooser.createDialog(mPanel, "Set color for other count", true, mColorChooser, new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        mOtherCount.setForeground(mColorChooser.getColor());
                        PropertiesUtils.saveColor(mOtherCount.getForeground(), PropertiesUtils.COLOR_OTHER);
                    }
                }, null);
                dialog.setVisible(true);
            }
        });
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
        PropertiesUtils.saveColor(JBColor.GRAY, PropertiesUtils.COLOR_ZERO);
        PropertiesUtils.saveColor(JBColor.BLUE, PropertiesUtils.COLOR_ONE);
        PropertiesUtils.saveColor(JBColor.RED, PropertiesUtils.COLOR_OTHER);
        mZeroCount.setForeground(PropertiesUtils.getZeroColor());
        mOneCount.setForeground(PropertiesUtils.getOneColor());
        mOtherCount.setForeground(PropertiesUtils.getOtherColor());
    }

    @Override
    public void disposeUIResources() {

    }
}
