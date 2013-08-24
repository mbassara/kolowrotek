package pl.mbassara.kolowrotek;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

@SuppressWarnings("serial")
public class FTPManagerGUI extends JFrame implements ActionListener,
		DocumentListener, PropertyChangeListener {

	private static FTPManagerGUI instance = null;
	public static String version = "1.02";

	public static FTPManagerGUI getInstance() {
		return instance;
	}

	public FTPManagerGUI() {
		super();
		log = Logger.getLogger(FTPManagerGUI.class.getName());

		currentDir = new File(System.getProperty("user.dir"));

		partyTextChanged = false;
		setTitle("Kołowrotek Manager v" + version);
		setLayout(new BorderLayout());
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setPreferredSize(new Dimension(700, 400));
		setResizable(false);
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}

		chooseImagesAskLabel = new JLabel(
				"Wybierz zdjęcia które chcesz dodać   ->");
		chooseImagesAskLabel.setEnabled(false);
		passwordAskLabel = new JLabel("Hasło do serwera FTP: ");
		passwordAskLabel.setEnabled(false);
		chooseImagesButton = new JButton("Wybierz");
		chooseImagesButton.setEnabled(false);
		chooseImagesButton.addActionListener(this);
		startButton = new JButton("START");
		startButton.setEnabled(false);
		startButton.addActionListener(this);
		startButton.setPreferredSize(new Dimension(100, 35));
		exitButton = new JButton("WYJŚCIE");
		exitButton.addActionListener(this);
		exitButton.setPreferredSize(new Dimension(100, 35));
		partyNameText = new JTextField("Wpisz tutaj nazwę imprezy");
		partyNameText.setEnabled(false);
		partyNameText.getDocument().addDocumentListener(this);
		passwordText = new JPasswordField();
		passwordText.setEnabled(false);
		passwordText.getDocument().addDocumentListener(this);
		passwordText.addActionListener(this);
		String[] yearsTab = { "2013/2014", "2012/2013", "2011/2012",
				"2010/2011", "2009/2010", "2008/2009", "2007/2008",
				"2006/2007", "2005/2006", "0000/0000" };
		yearComboBox = new JComboBox(yearsTab);
		yearComboBox.setEnabled(false);
		modeLabel = new JLabel("Wybierz tryb pracy:");
		genModeButton = new JRadioButton("Dodawanie zdjęć");
		genModeButton.addActionListener(this);
		delModeButton = new JRadioButton("Usuwanie zdjęć");
		delModeButton.addActionListener(this);
		ButtonGroup modeButtonsGroup = new ButtonGroup();
		modeButtonsGroup.add(genModeButton);
		modeButtonsGroup.add(delModeButton);
		logCheckBox = new JCheckBox("włącz logi");
		logCheckBox.addActionListener(this);

		progressBar = new JProgressBar();
		progressBar.setStringPainted(true);
		progressInfo = new JTextArea();
		progressInfo.setEnabled(false);
		progressInfo.setFont(new Font("SansSerif", Font.PLAIN, 14));
		JScrollPane progressInfoPane = new JScrollPane(progressInfo);

		JPanel northPane = new JPanel(new BorderLayout());
		northPane.add(chooseImagesAskLabel, BorderLayout.CENTER);
		northPane.add(chooseImagesButton, BorderLayout.LINE_END);

		JPanel passwordPane = new JPanel(new BorderLayout());
		passwordPane.add(passwordAskLabel, BorderLayout.LINE_START);
		passwordPane.add(passwordText, BorderLayout.CENTER);

		JPanel midPane = new JPanel(new BorderLayout());
		midPane.add(partyNameText, BorderLayout.CENTER);
		midPane.add(yearComboBox, BorderLayout.LINE_END);
		midPane.add(passwordPane, BorderLayout.PAGE_END);
		northPane.add(midPane, BorderLayout.PAGE_END);

		JPanel progressPane = new JPanel(new BorderLayout());
		progressPane.add(progressBar, BorderLayout.PAGE_START);
		progressPane.add(progressInfoPane, BorderLayout.CENTER);

		Box modeBox = new Box(BoxLayout.Y_AXIS);
		modeBox.add(Box.createVerticalGlue());
		modeBox.add(modeLabel);
		modeBox.add(genModeButton);
		modeBox.add(delModeButton);
		modeBox.add(Box.createVerticalGlue());
		// modeBox.add(logCheckBox);
		progressPane.add(modeBox, BorderLayout.LINE_END);

		JPanel buttonsPane = new JPanel();
		buttonsPane.add(startButton);
		buttonsPane.add(exitButton);

		this.add(northPane, BorderLayout.PAGE_START);
		this.add(progressPane, BorderLayout.CENTER);
		this.add(buttonsPane, BorderLayout.PAGE_END);

		pack();
		setVisible(true);

		String compInfos = "\n";

		try {
			compInfos += "Computer's canonical name:\t"
					+ InetAddress.getLocalHost().getCanonicalHostName() + "\n";
			compInfos += "Computer's name:\t\t"
					+ InetAddress.getLocalHost().getHostName() + "\n";
		} catch (UnknownHostException e) {
			log.log(Level.WARNING,
					ExceptionsUtilities.printStackTraceToString(e));
		}

		try {
			URL externalIP = new URL("http://api.externalip.net/ip/");
			BufferedReader input = new BufferedReader(new InputStreamReader(
					externalIP.openStream()));
			String IP = input.readLine();

			compInfos += "Computer's IP adress:\t\t" + IP + "\n";
		} catch (Exception e) {
			log.log(Level.WARNING,
					ExceptionsUtilities.printStackTraceToString(e));
		}

		try {
			URL externalIP = new URL("http://api.externalip.net/hostname/");
			BufferedReader input = new BufferedReader(new InputStreamReader(
					externalIP.openStream()));
			String hostname = input.readLine();

			compInfos += "Computer's hostname:\t\t" + hostname + "\n";
		} catch (Exception e) {
			log.log(Level.WARNING,
					ExceptionsUtilities.printStackTraceToString(e));
		}

		log.log(Level.INFO, "GUI created");
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource().equals(exitButton)) {
			log.log(Level.INFO, "exitButton pressed");

			GUI_LOG_FILE.delete();
			FTP_MANAGER_LOG_FILE.delete();
			FTP_SERVER_LOG_FILE.delete();

			if (task != null)
				task.doCancel(true);
			this.dispose();
		} else if (e.getSource().equals(genModeButton)
				&& genModeButton.isSelected()) {
			log.log(Level.INFO, "genModeButtonPressed");

			chooseImagesAskLabel.setEnabled(true);
			chooseImagesButton.setEnabled(true);
			partyNameText.setEnabled(true);
			yearComboBox.setEnabled(true);
			passwordAskLabel.setEnabled(true);
			passwordText.setEnabled(true);
			startButton.setEnabled(true);
		} else if (e.getSource().equals(delModeButton)
				&& delModeButton.isSelected()) {
			log.log(Level.INFO, "delModeButtonPressed");

			chooseImagesAskLabel.setEnabled(false);
			chooseImagesButton.setEnabled(false);
			partyNameText.setEnabled(true);
			yearComboBox.setEnabled(true);
			passwordAskLabel.setEnabled(true);
			passwordText.setEnabled(true);
			startButton.setEnabled(true);
		}
		// else if(e.getSource().equals(logCheckBox)){
		// enableFileLogger = logCheckBox.isSelected();
		// if(enableFileLogger){
		// JFileChooser chooser = new JFileChooser(".");
		// File selectedFile = new File("./GUI.log");
		// chooser.setSelectedFile(selectedFile);
		//
		// try {
		// if(chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION){
		// selectedFile = chooser.getSelectedFile();
		// if(!selectedFile.getCanonicalPath().equals(GUI_LOG_FILE.getCanonicalPath()))
		// {
		// logHandlerGUI = MyFileHandler.getHandler(selectedFile);
		// log.addHandler(logHandlerGUI);
		// log.log(Level.INFO, "GUI log file opened: " +
		// selectedFile.getCanonicalPath());
		// }
		// }
		//
		// String newPath = "";
		// newPath = selectedFile.getCanonicalPath();
		// newPath = newPath.substring(0, newPath.lastIndexOf(File.separator) +
		// 1);
		// newPath += "FTPManager.log";
		// selectedFile = new File(newPath);
		// chooser.setSelectedFile(selectedFile);
		// if(chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION){
		// selectedFile = chooser.getSelectedFile();
		// if(!selectedFile.getCanonicalPath().equals(FTP_MANAGER_LOG_FILE.getCanonicalPath()))
		// {
		// logHandlerManager = MyFileHandler.getHandler(selectedFile);
		// log.log(Level.INFO, "Manager log file opened: " +
		// selectedFile.getCanonicalPath());
		// }
		// }
		// else{
		// logCheckBox.setSelected(false);
		// return;
		// }
		// } catch (Exception e1) {
		// log.log(Level.WARNING,
		// ExceptionsUtilities.printStackTraceToString(e1));
		// }
		// }
		// else {
		// log.removeHandler(logHandlerGUI);
		// logHandlerGUI = null;
		// logHandlerManager = null;
		// log.log(Level.INFO, "Log files deleted");
		// }
		// }
		else if (e.getSource().equals(chooseImagesButton)) {
			log.log(Level.INFO, "chooseImagesButton pressed");

			JFileChooser chooser = new JFileChooser(currentDir);
			chooser.setMultiSelectionEnabled(true);
			chooser.setApproveButtonText("Wybierz");
			if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION
					&& chooser.getSelectedFiles().length > 0) {
				filesList = chooser.getSelectedFiles();
				chooseImagesAskLabel.setText("Wybrano " + filesList.length
						+ " plików.");
				currentDir = filesList[0].getParentFile();
			}

			log.log(Level.INFO, chooser.getSelectedFiles().length
					+ " files selected");
			log.log(Level.INFO, filesList.length + " files in filesList");
		} else if (e.getSource().equals(passwordText)) {
			startButton.doClick();
		} else if (e.getSource().equals(startButton)) {
			log.log(Level.INFO, "startButton pressed");

			if (filesList == null && genModeButton.isSelected()) {
				JOptionPane.showMessageDialog(this, "Wybierz zdjęcia!",
						"Nie tak szybko...", JOptionPane.WARNING_MESSAGE);
				log.log(Level.WARNING, "images were not selected - error");
			} else if (!partyTextChanged) {
				JOptionPane.showMessageDialog(this, "Wpisz nazwę imprezy!",
						"Nie tak szybko...", JOptionPane.WARNING_MESSAGE);
				log.log(Level.WARNING, "party name was not entered - error");
			} else if (passwordText.getPassword().length == 0) {
				JOptionPane.showMessageDialog(this, "Wpisz hasło!",
						"Nie tak szybko...", JOptionPane.WARNING_MESSAGE);
				log.log(Level.WARNING, "password was not entered - error");
			} else if (!genModeButton.isSelected()
					&& !delModeButton.isSelected()) {
				JOptionPane.showMessageDialog(this, "Wybierz tryb pracy!",
						"Nie tak szybko...", JOptionPane.WARNING_MESSAGE);
				log.log(Level.WARNING, "mode was not selected");
			} else {
				boolean mode;
				if (genModeButton.isSelected())
					mode = FTPManager.GENERATING_MODE;
				else
					mode = FTPManager.REMOVING_MODE;

				task = new FTPManager(mode, new String(
						passwordText.getPassword()), partyNameText.getText(),
						yearComboBox.getSelectedItem().toString(), filesList,
						logHandlerManager);

				task.addPropertyChangeListener(this);
				progressBar.setMinimum(0);
				progressBar.setMaximum(100);
				progressBar.setValue(0);
				progressBar.setValue(progressBar.getMinimum());
				chooseImagesButton.setEnabled(false);
				startButton.setEnabled(false);
				partyNameText.setEnabled(false);
				passwordAskLabel.setEnabled(false);
				passwordText.setEnabled(false);
				yearComboBox.setEnabled(false);
				genModeButton.setEnabled(false);
				delModeButton.setEnabled(false);
				logCheckBox.setEnabled(false);

				log.log(Level.INFO, (genModeButton.isSelected() ? "generating"
						: "removing") + " mode is ready to start");
				task.execute();
				log.log(Level.INFO, (genModeButton.isSelected() ? "generating"
						: "removing") + " mode was executed");
			}
		}
	}

	@Override
	public void changedUpdate(DocumentEvent e) {
	}

	@Override
	public void insertUpdate(DocumentEvent e) {
	}

	@Override
	public void removeUpdate(DocumentEvent e) {
		if (e.getDocument().equals(partyNameText.getDocument())) {
			partyTextChanged = true;
			log.log(Level.INFO, "party name changed");
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if (evt.getPropertyName().equals("error")) {
			if (evt.getNewValue().equals("partyNotFound")) {
				JOptionPane.showMessageDialog(this,
						"Nie istnieje impreza o takiej nazwie.", "Błąd!",
						JOptionPane.ERROR_MESSAGE);
				log.log(Level.WARNING, "party for entered name does not exists");
			} else if (evt.getNewValue().equals("dirExists")) {
				JOptionPane
						.showMessageDialog(
								this,
								"Istnieje na serwerze folder o nazwie: "
										+ FTPManager
												.createDirName(partyNameText
														.getText())
										+ "\n"
										+ "Możliwe, że folder jest pozostałością po nieudanym wysyłaniu plików,\nbądź po prostu została"
										+ " wygenerowana nazwa, która już istnieje\n(rzadka sytuacja ale możliwa). Proszę wprowadzić inną nazwę"
										+ " imprezy.\nMinimalna zmiana w nazwie powinna wystarczyć.",
								"Błąd!", JOptionPane.ERROR_MESSAGE);
				log.log(Level.WARNING,
						"dir for entered party name already exists");
			} else {
				JOptionPane
						.showMessageDialog(
								this,
								"Błąd podczas przesyłania plików. Sprawdź podane hasło.\n"
										+ "Jeśli hasło jest dobre to znaczy, że coś się zepsuło :(",
								"Błąd!", JOptionPane.ERROR_MESSAGE);
				log.log(Level.WARNING, "FTP uploading error");
			}
			progressBar.setValue(progressBar.getMinimum());
		} else if (evt.getPropertyName().equals("progress"))
			progressBar.setValue(task.getProgress());
		else if (evt.getPropertyName().equals("progress info")) {
			progressInfo.append((String) evt.getNewValue() + "\n");

			log.log(Level.INFO, (String) evt.getNewValue());

			if (evt.getNewValue().equals("Gotowe."))
				JOptionPane.showMessageDialog(this,
						(genModeButton.isSelected() ? "Dodawanie" : "Usuwanie")
								+ " zdjęć zakończyło się sukcesem!",
						"Informacja", JOptionPane.INFORMATION_MESSAGE);
		}
	}

	public static String getCurrentVersionFromServer() {
		String result = "error";

		try {
			URL remoteFile = new URL(
					"http://www.gim2brzeszcze.o12.pl/kolowrotek/galeria/app/version");
			InputStreamReader reader = new InputStreamReader(
					remoteFile.openStream());
			String tmp = "";
			int current;
			while ((current = reader.read()) != -1) {
				tmp += new Character((char) current);
			}

			result = tmp;
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return result;
	}

	public static void main(String[] args) {
		if (getCurrentVersionFromServer().equals(version))
			new FTPManagerGUI();
		else if (getCurrentVersionFromServer().equals("error")) {
			JOptionPane
					.showConfirmDialog(
							null,
							"Wystąpił błąd. Proszę sprawdzić czy komputer\njest połączony z internetem",
							"Błąd", JOptionPane.DEFAULT_OPTION,
							JOptionPane.ERROR_MESSAGE);
		} else {
			JOptionPane
					.showConfirmDialog(
							null,
							"Jest dostępna nowa wersja programu. Proszę pobrać\nnową wersję ze strony galerii.",
							"Nowa wersja", JOptionPane.DEFAULT_OPTION,
							JOptionPane.INFORMATION_MESSAGE);
		}
	}

	private JLabel chooseImagesAskLabel;
	private JLabel passwordAskLabel;
	private JLabel modeLabel;
	private JButton chooseImagesButton;
	private JButton startButton;
	private JButton exitButton;
	private JTextField partyNameText;
	private JPasswordField passwordText;
	private JComboBox yearComboBox;
	private JProgressBar progressBar;
	private JTextArea progressInfo;
	private JRadioButton genModeButton;
	private JRadioButton delModeButton;
	private JCheckBox logCheckBox;

	private File currentDir;
	private File[] filesList;
	private boolean partyTextChanged;
	private FTPManager task = null;
	private Logger log;
	private MyHandler logHandlerGUI = null;
	private MyHandler logHandlerManager = null;
	// private boolean enableFileLogger = false;

	public static final File GUI_LOG_FILE = new File("./GUI.log");
	public static final File FTP_MANAGER_LOG_FILE = new File("./FTPManager.log");
	public static final File FTP_SERVER_LOG_FILE = new File("./FTPServer.log");
}
