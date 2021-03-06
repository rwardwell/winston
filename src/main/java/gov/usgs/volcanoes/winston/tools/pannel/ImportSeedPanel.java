package gov.usgs.volcanoes.winston.tools.pannel;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JTextField;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

import gov.usgs.volcanoes.winston.in.ImportSeed;
import gov.usgs.volcanoes.winston.in.StaticImporter;
import gov.usgs.volcanoes.winston.tools.FilePanel;
import gov.usgs.volcanoes.winston.tools.WinstonToolsRunnablePanel;

public class ImportSeedPanel extends WinstonToolsRunnablePanel {

  private static final long serialVersionUID = 1L;
  ImportSeed is;
  private static FilePanel fileP;
  private static JTextField rsamDeltaF;
  private static JTextField rsamDurationF;
  private static JButton importB;

  public ImportSeedPanel() {
    super("Import SEED");
  }

  @Override
  protected void createUI() {
    this.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black),
        "Import SEED Volume"));

    createFields();

    final FormLayout layout = new FormLayout("right:max(40dlu;p), 4dlu, left:p", "");

    final DefaultFormBuilder builder = new DefaultFormBuilder(layout);
    builder.setDefaultDialogBorder();

    builder.append("file", fileP);
    builder.nextLine();
    builder.appendUnrelatedComponentsGapRow();
    builder.nextLine();
    builder.append("RSAM Delta", rsamDeltaF);
    builder.nextLine();
    builder.append("RSAM Duration", rsamDurationF);
    builder.nextLine();
    builder.appendUnrelatedComponentsGapRow();
    builder.nextLine();
    builder.append("", importB);
    this.add(builder.getPanel(), BorderLayout.CENTER);
  }

  @Override
  protected void createFields() {
    fileP = new FilePanel(FilePanel.Type.OPEN);
    rsamDeltaF = new JTextField(5);
    rsamDeltaF.setText("10");
    rsamDurationF = new JTextField(5);
    rsamDurationF.setText("60");
    importB = new JButton("import");
    importB.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        start();
      }
    });
  }

  @Override
  protected void go() {
    is = new ImportSeed();

    is.setRsamDelta(Integer.parseInt(rsamDeltaF.getText()));
    is.setRsamDuration(Integer.parseInt(rsamDurationF.getText()));
    final List<String> f = new ArrayList<String>();
    f.add(fileP.getFileName());
    StaticImporter.process(f, is);
  }

  @Override
  public boolean needsWinston() {
    return true;
  }
}

